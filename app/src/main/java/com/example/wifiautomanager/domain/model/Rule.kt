package com.example.wifiautomanager.domain.model

import com.example.wifiautomanager.domain.rule.ConditionGroup

data class Rule(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val priority: Int = 1,
    val rootGroup: ConditionGroup,
    val action: RuleAction,
    val createdAt: Long = System.currentTimeMillis()
)

sealed class RuleAction {
    data object PreferBestCandidate : RuleAction()
    data class PreferNetwork(val networkId: Long) : RuleAction()
    data object StayOnCurrent : RuleAction()
    data object DoNothing : RuleAction()
}
