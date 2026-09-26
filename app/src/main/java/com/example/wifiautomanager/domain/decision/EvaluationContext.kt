package com.example.wifiautomanager.domain.decision

data class EvaluationContext(
    val currentTimeMs: Long = System.currentTimeMillis(),
    val lastSwitchTimeMs: Long? = null,
    val internetUnavailableSinceMs: Long? = null,
    val connectedSinceMs: Long? = null
)
