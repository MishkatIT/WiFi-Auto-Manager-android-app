package com.example.wifiautomanager.domain.decision

import com.example.wifiautomanager.domain.model.AppSettings
import com.example.wifiautomanager.domain.model.CandidateResult
import com.example.wifiautomanager.domain.model.InternetStatus
import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.model.RuleAction
import com.example.wifiautomanager.domain.model.ScannedNetwork
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.domain.rule.RuleEvaluator

class CandidateSelector(
    private val antiFlappingGuard: AntiFlappingGuard = AntiFlappingGuard(),
    private val ruleEvaluator: RuleEvaluator = RuleEvaluator()
) {

    fun evaluateCandidates(
        savedNetworks: List<WifiNetwork>,
        scannedNetworks: List<ScannedNetwork>,
        currentState: WifiState,
        rules: List<Rule>,
        settings: AppSettings,
        context: EvaluationContext
    ): List<CandidateResult> {
        val currentConnected = currentState as? WifiState.Connected
        val currentRssi = currentConnected?.rssi ?: -127
        val currentSsid = currentConnected?.ssid

        val results = mutableListOf<CandidateResult>()

        for (saved in savedNetworks) {
            if (!saved.enabled) {
                results.add(
                    CandidateResult(
                        network = saved,
                        scannedNetwork = null,
                        qualified = false,
                        rejectionReason = "Network disabled in settings",
                        signalImprovement = null
                    )
                )
                continue
            }

            // Must be visible in scan results
            val scanned = scannedNetworks.find { it.ssid.equals(saved.ssid, ignoreCase = true) }
            if (scanned == null) {
                results.add(
                    CandidateResult(
                        network = saved,
                        scannedNetwork = null,
                        qualified = false,
                        rejectionReason = "Not in scan range",
                        signalImprovement = null
                    )
                )
                continue
            }

            // Already connected check
            if (currentSsid != null && currentSsid.equals(saved.ssid, ignoreCase = true)) {
                results.add(
                    CandidateResult(
                        network = saved,
                        scannedNetwork = scanned,
                        qualified = false,
                        rejectionReason = "Already connected to this network",
                        signalImprovement = 0
                    )
                )
                continue
            }

            // Minimum signal threshold
            if (scanned.rssi < saved.minimumSignalDbm) {
                results.add(
                    CandidateResult(
                        network = saved,
                        scannedNetwork = scanned,
                        qualified = false,
                        rejectionReason = "Signal ${scanned.rssi} dBm below minimum threshold ${saved.minimumSignalDbm} dBm",
                        signalImprovement = scanned.rssi - currentRssi
                    )
                )
                continue
            }

            // Minimum candidate signal threshold
            if (scanned.rssi < saved.minimumCandidateSignalDbm) {
                results.add(
                    CandidateResult(
                        network = saved,
                        scannedNetwork = scanned,
                        qualified = false,
                        rejectionReason = "Signal ${scanned.rssi} dBm below candidate threshold ${saved.minimumCandidateSignalDbm} dBm",
                        signalImprovement = scanned.rssi - currentRssi
                    )
                )
                continue
            }

            // If currently connected, check anti-flapping guard (cooldown + hysteresis)
            val improvement = scanned.rssi - currentRssi
            if (currentConnected != null) {
                // If current network has no internet, check if internet timeout is reached (Scenario C)
                if (currentConnected.internetStatus == InternetStatus.UNAVAILABLE) {
                    val since = context.internetUnavailableSinceMs
                    val timeoutMs = settings.internetUnavailableTimeoutSeconds * 1000L
                    if (since != null) {
                        val elapsed = context.currentTimeMs - since
                        if (elapsed < timeoutMs) {
                            val waitRemainingSec = ((timeoutMs - elapsed) / 1000L).coerceAtLeast(1)
                            results.add(
                                CandidateResult(
                                    network = saved,
                                    scannedNetwork = scanned,
                                    qualified = false,
                                    rejectionReason = "Internet unavailable grace period active (${waitRemainingSec}s remaining)",
                                    signalImprovement = improvement
                                )
                            )
                            continue
                        }
                    }
                }

                // If current network has internet and is healthy, check anti-flapping
                val antiFlap = antiFlappingGuard.shouldAllowSwitch(
                    currentRssi = currentRssi,
                    candidateRssi = scanned.rssi,
                    requiredImprovementDbm = saved.minimumImprovementDbm,
                    lastSwitchTimeMs = context.lastSwitchTimeMs,
                    cooldownSeconds = settings.switchCooldownSeconds,
                    currentTimeMs = context.currentTimeMs
                )

                if (antiFlap is AntiFlappingResult.Blocked) {
                    results.add(
                        CandidateResult(
                            network = saved,
                            scannedNetwork = scanned,
                            qualified = false,
                            rejectionReason = antiFlap.reason,
                            signalImprovement = improvement
                        )
                    )
                    continue
                }
            }

            // Check custom user rules
            var ruleBlocked = false
            var ruleBlockReason: String? = null
            for (rule in rules.filter { it.enabled }) {
                if (ruleEvaluator.evaluateRule(rule, context, currentState, scanned, saved, savedNetworks)) {
                    when (rule.action) {
                        RuleAction.StayOnCurrent -> {
                            ruleBlocked = true
                            ruleBlockReason = "Blocked by rule: ${rule.name}"
                        }
                        is RuleAction.PreferNetwork -> {
                            if (rule.action.networkId != saved.id) {
                                // Rule prefers another specific network
                            }
                        }
                        else -> {}
                    }
                }
            }

            if (ruleBlocked) {
                results.add(
                    CandidateResult(
                        network = saved,
                        scannedNetwork = scanned,
                        qualified = false,
                        rejectionReason = ruleBlockReason,
                        signalImprovement = improvement
                    )
                )
                continue
            }

            // All checks passed!
            results.add(
                CandidateResult(
                    network = saved,
                    scannedNetwork = scanned,
                    qualified = true,
                    rejectionReason = null,
                    signalImprovement = improvement
                )
            )
        }

        return results
    }

    fun selectTopCandidate(candidates: List<CandidateResult>): CandidateResult? {
        return candidates
            .filter { it.qualified }
            .sortedWith(
                compareByDescending<CandidateResult> { it.network.priority }
                    .thenByDescending { it.scannedNetwork?.rssi ?: -127 }
            )
            .firstOrNull()
    }
}
