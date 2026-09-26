package com.example.wifiautomanager.domain.model

data class Decision(
    val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val action: DecisionAction,
    val selectedNetwork: WifiNetwork?,
    val reason: String,
    val evaluatedCandidates: List<CandidateResult> = emptyList(),
    val currentState: WifiState
)

sealed class DecisionAction {
    data object StayOnCurrent : DecisionAction()
    data class SuggestNetwork(val network: WifiNetwork) : DecisionAction()
    data object NoCandidates : DecisionAction()
    data object CooldownActive : DecisionAction()
    data object RulesDisabled : DecisionAction()
}
