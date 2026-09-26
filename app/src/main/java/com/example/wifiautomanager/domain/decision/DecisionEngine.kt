package com.example.wifiautomanager.domain.decision

import com.example.wifiautomanager.domain.model.AppSettings
import com.example.wifiautomanager.domain.model.CandidateResult
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.model.ScannedNetwork
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.model.WifiState

class DecisionEngine(
    private val candidateSelector: CandidateSelector = CandidateSelector(),
    private val antiFlappingGuard: AntiFlappingGuard = AntiFlappingGuard(),
    private val decisionExplainer: DecisionExplainer = DecisionExplainer()
) {

    fun evaluate(
        currentState: WifiState,
        availableNetworks: List<ScannedNetwork>,
        savedNetworks: List<WifiNetwork>,
        rules: List<Rule>,
        settings: AppSettings,
        context: EvaluationContext
    ): Decision {
        // 1. Guard check: Auto manager enabled?
        if (!settings.autoManagerEnabled) {
            val action = DecisionAction.RulesDisabled
            return Decision(
                action = action,
                selectedNetwork = null,
                reason = decisionExplainer.explain(action, currentState, emptyList()),
                evaluatedCandidates = emptyList(),
                currentState = currentState
            )
        }

        // 2. Cooldown check
        if (context.lastSwitchTimeMs != null) {
            val elapsed = context.currentTimeMs - context.lastSwitchTimeMs
            if (elapsed < (settings.switchCooldownSeconds * 1000L)) {
                val action = DecisionAction.CooldownActive
                return Decision(
                    action = action,
                    selectedNetwork = null,
                    reason = decisionExplainer.explain(action, currentState, emptyList()),
                    evaluatedCandidates = emptyList(),
                    currentState = currentState
                )
            }
        }

        // 3. Evaluate candidate networks
        val candidates = candidateSelector.evaluateCandidates(
            savedNetworks = savedNetworks,
            scannedNetworks = availableNetworks,
            currentState = currentState,
            rules = rules,
            settings = settings,
            context = context
        )

        // 4. Select top qualified candidate
        val topCandidate = candidateSelector.selectTopCandidate(candidates)

        val action = when {
            topCandidate != null -> DecisionAction.SuggestNetwork(topCandidate.network)
            currentState is WifiState.Connected -> DecisionAction.StayOnCurrent
            else -> DecisionAction.NoCandidates
        }

        val reason = decisionExplainer.explain(action, currentState, candidates)

        return Decision(
            action = action,
            selectedNetwork = topCandidate?.network,
            reason = reason,
            evaluatedCandidates = candidates,
            currentState = currentState
        )
    }
}
