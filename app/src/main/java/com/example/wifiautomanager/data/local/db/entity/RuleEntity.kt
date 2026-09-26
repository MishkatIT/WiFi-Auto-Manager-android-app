package com.example.wifiautomanager.data.local.db.entity

// Room Entity for switching rules - implemented in Phase 2
data class RuleEntity(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val priority: Int = 1,
    val actionType: String,
    val targetNetworkId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
