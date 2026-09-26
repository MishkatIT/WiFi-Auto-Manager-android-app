package com.example.wifiautomanager.domain.decision

import com.example.wifiautomanager.domain.model.CandidateResult
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.WifiState

class DecisionExplainer {

    fun explain(
        action: DecisionAction,
        currentState: WifiState,
        candidates: List<CandidateResult>
    ): String = buildString {
        when (action) {
            is DecisionAction.StayOnCurrent -> {
                val currentSsid = (currentState as? WifiState.Connected)?.ssid ?: "current network"
                append("Staying on $currentSsid.")
                val rejected = candidates.filter { !it.qualified && it.scannedNetwork != null }
                if (rejected.isNotEmpty()) {
                    append(" Candidates rejected: ")
                    append(rejected.joinToString("; ") { "${it.network.ssid} (${it.rejectionReason})" })
                }
            }

            is DecisionAction.SuggestNetwork -> {
                val c = candidates.firstOrNull { it.qualified && it.network.id == action.network.id }
                append("Suggesting switch to ${action.network.ssid}.")
                if (c?.signalImprovement != null) {
                    append(" Signal improvement: +${c.signalImprovement} dBm (Required: ${action.network.minimumImprovementDbm} dBm).")
                }
                append(" Priority: ${action.network.priority}.")
            }

            is DecisionAction.NoCandidates -> {
                append("No eligible candidate networks found in range.")
            }

            is DecisionAction.CooldownActive -> {
                append("Switch cooldown is active. No switch attempted to prevent network flapping.")
            }

            is DecisionAction.RulesDisabled -> {
                append("Auto manager is disabled in settings.")
            }
        }
    }

    fun explain(decision: Decision): String {
        return explain(decision.action, decision.currentState, decision.evaluatedCandidates)
    }
}
