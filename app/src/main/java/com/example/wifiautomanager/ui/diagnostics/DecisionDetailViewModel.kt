package com.example.wifiautomanager.ui.diagnostics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiautomanager.domain.model.CandidateResult
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.domain.repository.DecisionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DecisionDetailUiState(
    val isLoading: Boolean = true,
    val decision: Decision? = null,
    val formattedTimestamp: String = "",
    val actionTitle: String = "",
    val actionTypeBadge: String = "",
    val currentNetworkSummary: String = "",
    val reasonExplanation: String = "",
    val candidates: List<CandidateResult> = emptyList(),
    val whyNotSwitchExplanation: String = ""
)

@HiltViewModel
class DecisionDetailViewModel @Inject constructor(
    private val decisionRepository: DecisionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val decisionId: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val _uiState = MutableStateFlow(DecisionDetailUiState())
    val uiState: StateFlow<DecisionDetailUiState> = _uiState.asStateFlow()

    init {
        loadDecision(decisionId)
    }

    fun loadDecision(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val decision = decisionRepository.getDecisionById(id)

            if (decision != null) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                val dateStr = dateFormat.format(Date(decision.timestampMs))

                val (actionTitle, badge) = when (val action = decision.action) {
                    is DecisionAction.SuggestNetwork ->
                        "Recommend Switch to ${action.network.ssid}" to "SWITCH_RECOMMENDED"
                    is DecisionAction.StayOnCurrent ->
                        "Stay on Current Network" to "STAY"
                    is DecisionAction.CooldownActive ->
                        "Switch Blocked by Anti-Flapping Cooldown" to "COOLDOWN_ACTIVE"
                    is DecisionAction.NoCandidates ->
                        "No Eligible Candidates Found" to "NO_CANDIDATE"
                    is DecisionAction.RulesDisabled ->
                        "Rules Evaluation Disabled" to "RULES_DISABLED"
                }

                val currentSummary = when (val state = decision.currentState) {
                    is WifiState.Connected ->
                        "Connected to ${state.ssid} (${state.rssi} dBm, ${state.frequencyMhz} MHz)"
                    is WifiState.Disconnected -> "Disconnected from Wi-Fi"
                    is WifiState.Disabled -> "Wi-Fi is turned off"
                    else -> "Wi-Fi State: ${state::class.simpleName}"
                }

                val whyNotSwitch = buildWhyNotSwitchExplanation(decision)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        decision = decision,
                        formattedTimestamp = dateStr,
                        actionTitle = actionTitle,
                        actionTypeBadge = badge,
                        currentNetworkSummary = currentSummary,
                        reasonExplanation = decision.reason,
                        candidates = decision.evaluatedCandidates,
                        whyNotSwitchExplanation = whyNotSwitch
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun buildWhyNotSwitchExplanation(decision: Decision): String {
        return when (val action = decision.action) {
            is DecisionAction.SuggestNetwork ->
                "The engine recommended switching to ${action.network.ssid} because it satisfied all criteria and provided a substantial signal/priority upgrade over the current network."
            is DecisionAction.StayOnCurrent -> {
                if (decision.evaluatedCandidates.isEmpty()) {
                    "Current network signal remains sufficient, and no alternative saved networks were detected in scan results."
                } else {
                    val reasons = decision.evaluatedCandidates.mapNotNull { it.rejectionReason }
                    if (reasons.isNotEmpty()) {
                        "Candidates were rejected due to: " + reasons.joinToString("; ")
                    } else {
                        "Current network satisfies configured preferences better than any nearby candidate."
                    }
                }
            }
            is DecisionAction.CooldownActive ->
                "A network switch was recently executed. The anti-flapping guard paused switches to prevent rapid connection ping-pong."
            is DecisionAction.NoCandidates ->
                "No configured saved networks were detected within range during the scan cycle."
            is DecisionAction.RulesDisabled ->
                "Automatic Wi-Fi management or rule evaluation is currently disabled in app settings."
        }
    }
}
