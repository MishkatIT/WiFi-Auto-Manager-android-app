package com.example.wifiautomanager.data.local.db.entity

// Room Entity for decision logging - implemented in Phase 2
data class DecisionLogEntity(
    val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val action: String,
    val selectedSsid: String?,
    val reason: String,
    val candidatesCount: Int
)
