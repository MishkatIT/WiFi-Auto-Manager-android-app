package com.example.wifiautomanager.data.local.db.entity

// Room Entity for saved Wi-Fi networks - implemented in Phase 2
data class WifiNetworkEntity(
    val id: Long = 0,
    val ssid: String,
    val securityType: String,
    val enabled: Boolean = true,
    val priority: Int = 1,
    val minimumSignalDbm: Int = -70,
    val requiresInternet: Boolean = false,
    val minimumCandidateSignalDbm: Int = -65,
    val minimumImprovementDbm: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
