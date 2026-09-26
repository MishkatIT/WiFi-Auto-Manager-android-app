package com.example.wifiautomanager.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.model.RuleAction
import com.example.wifiautomanager.domain.repository.RuleRepository
import com.example.wifiautomanager.domain.rule.Condition
import com.example.wifiautomanager.domain.rule.ConditionGroup
import com.example.wifiautomanager.domain.rule.ConditionNode
import com.example.wifiautomanager.domain.rule.LogicalOperator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RuleItem(
    val id: Long,
    val name: String,
    val enabled: Boolean,
    val priority: Int,
    val conditionsText: String,
    val actionText: String
)

data class RuleListUiState(
    val rules: List<RuleItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class RuleListViewModel @Inject constructor(
    private val ruleRepository: RuleRepository
) : ViewModel() {

    val uiState: StateFlow<RuleListUiState> = ruleRepository.getAllRules()
        .map { list ->
            RuleListUiState(
                rules = list.map { it.toRuleItem() },
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RuleListUiState(isLoading = true)
        )

    fun onToggleRule(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            val rule = ruleRepository.getRuleById(id) ?: return@launch
            ruleRepository.updateRule(rule.copy(enabled = enabled))
        }
    }

    fun onDeleteRule(id: Long) {
        viewModelScope.launch {
            val rule = ruleRepository.getRuleById(id) ?: return@launch
            ruleRepository.deleteRule(rule)
        }
    }

    private fun Rule.toRuleItem(): RuleItem {
        val conditionsSummary = rootGroup.formatConditions()
        val actionDescription = when (action) {
            RuleAction.PreferBestCandidate -> "Prefer best available candidate"
            is RuleAction.PreferNetwork -> "Prefer specific network (ID: ${action.networkId})"
            RuleAction.StayOnCurrent -> "Stay on current network"
            RuleAction.DoNothing -> "Take no action"
        }

        return RuleItem(
            id = id,
            name = name,
            enabled = enabled,
            priority = priority,
            conditionsText = conditionsSummary,
            actionText = actionDescription
        )
    }

    private fun ConditionGroup.formatConditions(): String {
        if (conditions.isEmpty()) return "Always true"
        val op = if (operator == LogicalOperator.AND) "AND" else "OR"
        return conditions.joinToString(" $op ") { it.formatNode() }
    }

    private fun ConditionNode.formatNode(): String = when (this) {
        is ConditionNode.Leaf -> formatCondition(condition)
        is ConditionNode.Group -> "(${group.formatConditions()})"
        is ConditionNode.Not -> "NOT (${inner.formatNode()})"
    }

    private fun formatCondition(c: Condition): String = when (c) {
        is Condition.CurrentSignalBelow -> "signal < ${c.thresholdDbm} dBm"
        is Condition.CurrentSignalAbove -> "signal > ${c.thresholdDbm} dBm"
        is Condition.CandidateSignalAbove -> "candidate signal > ${c.thresholdDbm} dBm"
        is Condition.SignalImprovementAtLeast -> "signal improvement ≥ ${c.improvementDbm} dBm"
        Condition.InternetAvailable -> "internet available"
        Condition.InternetUnavailable -> "internet unavailable"
        is Condition.InternetUnavailableForAtLeast -> "internet unavailable ≥ ${c.seconds}s"
        is Condition.CurrentSsidIs -> "current SSID is '${c.ssid}'"
        is Condition.CurrentSsidIsNot -> "current SSID is not '${c.ssid}'"
        is Condition.CandidateNetworkAvailable -> "candidate available (ID: ${c.networkId})"
        is Condition.TimeBetween -> "time between ${String.format("%02d:%02d", c.startHour, c.startMin)}–${String.format("%02d:%02d", c.endHour, c.endMin)}"
        is Condition.DayOfWeek -> "days in ${c.days.joinToString { it.name.take(3) }}"
        is Condition.ConnectedForAtLeast -> "connected ≥ ${c.seconds}s"
        is Condition.SwitchCooldownExpired -> "cooldown expired (${c.seconds}s)"
        is Condition.CandidatePriorityHigherThan -> "priority > target"
    }
}
