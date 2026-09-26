package com.example.wifiautomanager.domain.decision

import com.example.wifiautomanager.domain.model.CandidateResult
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.model.WifiState

class DecisionEngine(
    private val candidateSelector: CandidateSelector,
    private val antiFlappingGuard: AntiFlappingGuard,
    private val decisionExplainer: DecisionExplainer
) {
    fun evaluate(
        currentState: WifiState,
        rules: List<Rule>,
        candidates: List<CandidateResult>
    ): Decision {
        return Decision(
            action = DecisionAction.StayOnCurrent,
            selectedNetwork = null,
            reason = "Engine evaluation placeholder",
            evaluatedCandidates = candidates,
            currentState = currentState
        )
    }
}
