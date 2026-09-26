package com.example.wifiautomanager.domain.rule

enum class LogicalOperator { AND, OR }

data class ConditionGroup(
    val operator: LogicalOperator = LogicalOperator.AND,
    val conditions: List<ConditionNode> = emptyList()
)

sealed class ConditionNode {
    data class Leaf(val condition: Condition) : ConditionNode()
    data class Group(val group: ConditionGroup) : ConditionNode()
    data class Not(val inner: ConditionNode) : ConditionNode()
}

sealed class Condition {
    // Signal
    data class CurrentSignalBelow(val thresholdDbm: Int) : Condition()
    data class CurrentSignalAbove(val thresholdDbm: Int) : Condition()
    data class CandidateSignalAbove(val thresholdDbm: Int) : Condition()
    data class SignalImprovementAtLeast(val improvementDbm: Int) : Condition()

    // Internet
    data object InternetAvailable : Condition()
    data object InternetUnavailable : Condition()
    data class InternetUnavailableForAtLeast(val seconds: Int) : Condition()

    // Network identity
    data class CurrentSsidIs(val ssid: String) : Condition()
    data class CurrentSsidIsNot(val ssid: String) : Condition()
    data class CandidateNetworkAvailable(val networkId: Long) : Condition()

    // Time
    data class TimeBetween(
        val startHour: Int,
        val startMin: Int,
        val endHour: Int,
        val endMin: Int
    ) : Condition()

    data class DayOfWeek(val days: Set<java.time.DayOfWeek>) : Condition()

    // Stability
    data class ConnectedForAtLeast(val seconds: Int) : Condition()
    data class SwitchCooldownExpired(val seconds: Int) : Condition()

    // Priority
    data class CandidatePriorityHigherThan(val networkId: Long) : Condition()
}
