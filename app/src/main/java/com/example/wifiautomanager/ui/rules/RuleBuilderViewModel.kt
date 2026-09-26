package com.example.wifiautomanager.ui.rules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.model.RuleAction
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.repository.RuleRepository
import com.example.wifiautomanager.domain.repository.WifiRepository
import com.example.wifiautomanager.domain.rule.Condition
import com.example.wifiautomanager.domain.rule.ConditionGroup
import com.example.wifiautomanager.domain.rule.ConditionNode
import com.example.wifiautomanager.domain.rule.LogicalOperator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ConditionTypeUi(val label: String) {
    CURRENT_SIGNAL_BELOW("Current signal < threshold"),
    CURRENT_SIGNAL_ABOVE("Current signal > threshold"),
    CANDIDATE_SIGNAL_ABOVE("Candidate signal > threshold"),
    INTERNET_AVAILABLE("Internet is available"),
    INTERNET_UNAVAILABLE("Internet is unavailable"),
    INTERNET_UNAVAILABLE_FOR("Internet unavailable for ≥ X sec"),
    CURRENT_SSID_IS("Current SSID is"),
    TIME_BETWEEN("Time between (hours)")
}

data class EditableCondition(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ConditionTypeUi = ConditionTypeUi.CURRENT_SIGNAL_BELOW,
    val intValue: Int = -70,
    val textValue: String = "",
    val startHour: Int = 8,
    val endHour: Int = 17
)

data class RuleBuilderUiState(
    val ruleId: Long = 0,
    val isEditMode: Boolean = false,
    val name: String = "",
    val nameError: String? = null,
    val operator: LogicalOperator = LogicalOperator.AND,
    val conditions: List<EditableCondition> = listOf(EditableCondition()),
    val selectedActionType: String = "PREFER_BEST",
    val selectedNetworkId: Long? = null,
    val savedNetworks: List<WifiNetwork> = emptyList(),
    val previewText: String = "",
    val isLoading: Boolean = false
)

sealed interface RuleBuilderEvent {
    data object SaveSuccess : RuleBuilderEvent
    data class ShowError(val message: String) : RuleBuilderEvent
}

@HiltViewModel
class RuleBuilderViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val wifiRepository: WifiRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val ruleId: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val _uiState = MutableStateFlow(RuleBuilderUiState(ruleId = ruleId, isEditMode = ruleId > 0))
    val uiState: StateFlow<RuleBuilderUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RuleBuilderEvent>()
    val events: SharedFlow<RuleBuilderEvent> = _events.asSharedFlow()

    init {
        loadSavedNetworks()
        if (ruleId > 0) {
            loadRule(ruleId)
        } else {
            updatePreview()
        }
    }

    private fun loadSavedNetworks() {
        viewModelScope.launch {
            val networks = wifiRepository.getAllNetworks().firstOrNull() ?: emptyList()
            _uiState.update { it.copy(savedNetworks = networks) }
        }
    }

    fun loadRule(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val rule = ruleRepository.getRuleById(id)
            if (rule != null) {
                val conditions = rule.rootGroup.conditions.mapNotNull { node ->
                    (node as? ConditionNode.Leaf)?.condition?.toEditableCondition()
                }.ifEmpty { listOf(EditableCondition()) }

                val (actionType, netId) = when (rule.action) {
                    RuleAction.PreferBestCandidate -> "PREFER_BEST" to null
                    is RuleAction.PreferNetwork -> "PREFER_SPECIFIC" to rule.action.networkId
                    RuleAction.StayOnCurrent -> "STAY" to null
                    RuleAction.DoNothing -> "NOTHING" to null
                }

                _uiState.update {
                    it.copy(
                        ruleId = rule.id,
                        isEditMode = true,
                        name = rule.name,
                        operator = rule.rootGroup.operator,
                        conditions = conditions,
                        selectedActionType = actionType,
                        selectedNetworkId = netId,
                        isLoading = false
                    )
                }
                updatePreview()
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(RuleBuilderEvent.ShowError("Rule not found"))
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                nameError = if (name.isBlank()) "Rule name is required" else null
            )
        }
        updatePreview()
    }

    fun onOperatorChanged(operator: LogicalOperator) {
        _uiState.update { it.copy(operator = operator) }
        updatePreview()
    }

    fun onAddCondition() {
        _uiState.update { it.copy(conditions = it.conditions + EditableCondition()) }
        updatePreview()
    }

    fun onRemoveCondition(index: Int) {
        if (_uiState.value.conditions.size <= 1) return
        _uiState.update {
            val updated = it.conditions.toMutableList().apply { removeAt(index) }
            it.copy(conditions = updated)
        }
        updatePreview()
    }

    fun onConditionTypeChanged(index: Int, type: ConditionTypeUi) {
        _uiState.update {
            val updated = it.conditions.toMutableList()
            val defaultInt = when (type) {
                ConditionTypeUi.CURRENT_SIGNAL_BELOW,
                ConditionTypeUi.CURRENT_SIGNAL_ABOVE,
                ConditionTypeUi.CANDIDATE_SIGNAL_ABOVE -> -70
                ConditionTypeUi.INTERNET_UNAVAILABLE_FOR -> 15
                else -> 0
            }
            updated[index] = updated[index].copy(type = type, intValue = defaultInt)
            it.copy(conditions = updated)
        }
        updatePreview()
    }

    fun onConditionIntValueChange(index: Int, value: Int) {
        _uiState.update {
            val updated = it.conditions.toMutableList()
            updated[index] = updated[index].copy(intValue = value)
            it.copy(conditions = updated)
        }
        updatePreview()
    }

    fun onConditionTextValueChange(index: Int, text: String) {
        _uiState.update {
            val updated = it.conditions.toMutableList()
            updated[index] = updated[index].copy(textValue = text)
            it.copy(conditions = updated)
        }
        updatePreview()
    }

    fun onActionTypeSelected(actionType: String) {
        _uiState.update { it.copy(selectedActionType = actionType) }
        updatePreview()
    }

    fun onSpecificNetworkSelected(networkId: Long) {
        _uiState.update { it.copy(selectedNetworkId = networkId) }
        updatePreview()
    }

    private fun updatePreview() {
        val state = _uiState.value
        val op = if (state.operator == LogicalOperator.AND) "AND" else "OR"
        val conds = state.conditions.joinToString(" $op ") { it.formatSummary() }
        val action = when (state.selectedActionType) {
            "PREFER_BEST" -> "Suggest best available candidate"
            "PREFER_SPECIFIC" -> {
                val net = state.savedNetworks.find { it.id == state.selectedNetworkId }
                "Prefer ${net?.ssid ?: "specific network"}"
            }
            "STAY" -> "Stay on current network"
            else -> "Take no action"
        }

        val preview = "IF: $conds\nTHEN: $action"
        _uiState.update { it.copy(previewText = preview) }
    }

    fun onSaveRule() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Rule name is required") }
            return
        }

        viewModelScope.launch {
            try {
                val conditionNodes = state.conditions.map { ConditionNode.Leaf(it.toCondition()) }
                val rootGroup = ConditionGroup(
                    operator = state.operator,
                    conditions = conditionNodes
                )

                val action = when (state.selectedActionType) {
                    "PREFER_SPECIFIC" -> RuleAction.PreferNetwork(state.selectedNetworkId ?: 0L)
                    "STAY" -> RuleAction.StayOnCurrent
                    "NOTHING" -> RuleAction.DoNothing
                    else -> RuleAction.PreferBestCandidate
                }

                val rule = Rule(
                    id = state.ruleId,
                    name = state.name.trim(),
                    enabled = true,
                    priority = 1,
                    rootGroup = rootGroup,
                    action = action
                )

                if (rule.id == 0L) {
                    ruleRepository.insertRule(rule)
                } else {
                    ruleRepository.updateRule(rule)
                }
                _events.emit(RuleBuilderEvent.SaveSuccess)
            } catch (e: Exception) {
                _events.emit(RuleBuilderEvent.ShowError(e.message ?: "Failed to save rule"))
            }
        }
    }

    private fun EditableCondition.toCondition(): Condition = when (type) {
        ConditionTypeUi.CURRENT_SIGNAL_BELOW -> Condition.CurrentSignalBelow(intValue)
        ConditionTypeUi.CURRENT_SIGNAL_ABOVE -> Condition.CurrentSignalAbove(intValue)
        ConditionTypeUi.CANDIDATE_SIGNAL_ABOVE -> Condition.CandidateSignalAbove(intValue)
        ConditionTypeUi.INTERNET_AVAILABLE -> Condition.InternetAvailable
        ConditionTypeUi.INTERNET_UNAVAILABLE -> Condition.InternetUnavailable
        ConditionTypeUi.INTERNET_UNAVAILABLE_FOR -> Condition.InternetUnavailableForAtLeast(intValue)
        ConditionTypeUi.CURRENT_SSID_IS -> Condition.CurrentSsidIs(textValue)
        ConditionTypeUi.TIME_BETWEEN -> Condition.TimeBetween(startHour, 0, endHour, 0)
    }

    private fun EditableCondition.formatSummary(): String = when (type) {
        ConditionTypeUi.CURRENT_SIGNAL_BELOW -> "signal < $intValue dBm"
        ConditionTypeUi.CURRENT_SIGNAL_ABOVE -> "signal > $intValue dBm"
        ConditionTypeUi.CANDIDATE_SIGNAL_ABOVE -> "candidate signal > $intValue dBm"
        ConditionTypeUi.INTERNET_AVAILABLE -> "internet is available"
        ConditionTypeUi.INTERNET_UNAVAILABLE -> "internet is unavailable"
        ConditionTypeUi.INTERNET_UNAVAILABLE_FOR -> "internet unavailable ≥ ${intValue}s"
        ConditionTypeUi.CURRENT_SSID_IS -> "current SSID is '${textValue}'"
        ConditionTypeUi.TIME_BETWEEN -> "time between ${startHour}:00–${endHour}:00"
    }

    private fun Condition.toEditableCondition(): EditableCondition? = when (this) {
        is Condition.CurrentSignalBelow -> EditableCondition(type = ConditionTypeUi.CURRENT_SIGNAL_BELOW, intValue = thresholdDbm)
        is Condition.CurrentSignalAbove -> EditableCondition(type = ConditionTypeUi.CURRENT_SIGNAL_ABOVE, intValue = thresholdDbm)
        is Condition.CandidateSignalAbove -> EditableCondition(type = ConditionTypeUi.CANDIDATE_SIGNAL_ABOVE, intValue = thresholdDbm)
        Condition.InternetAvailable -> EditableCondition(type = ConditionTypeUi.INTERNET_AVAILABLE)
        Condition.InternetUnavailable -> EditableCondition(type = ConditionTypeUi.INTERNET_UNAVAILABLE)
        is Condition.InternetUnavailableForAtLeast -> EditableCondition(type = ConditionTypeUi.INTERNET_UNAVAILABLE_FOR, intValue = seconds)
        is Condition.CurrentSsidIs -> EditableCondition(type = ConditionTypeUi.CURRENT_SSID_IS, textValue = ssid)
        is Condition.TimeBetween -> EditableCondition(type = ConditionTypeUi.TIME_BETWEEN, startHour = startHour, endHour = endHour)
        else -> null
    }
}
