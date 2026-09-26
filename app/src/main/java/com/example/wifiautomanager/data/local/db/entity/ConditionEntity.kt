package com.example.wifiautomanager.data.local.db.entity

// Room Entity for conditions - implemented in Phase 2
data class ConditionEntity(
    val id: Long = 0,
    val ruleId: Long,
    val conditionType: String,
    val parametersJson: String
)
