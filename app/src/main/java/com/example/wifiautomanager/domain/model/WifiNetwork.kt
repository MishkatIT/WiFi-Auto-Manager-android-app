package com.example.wifiautomanager.domain.model

data class WifiNetwork(
    val id: Long = 0,
    val ssid: String,
    val securityType: SecurityType,
    val enabled: Boolean = true,
    val priority: Int,
    val minimumSignalDbm: Int = -70,
    val requiresInternet: Boolean = false,
    val minimumCandidateSignalDbm: Int = -65,
    val minimumImprovementDbm: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class SecurityType {
    OPEN,
    WEP,
    WPA2_PSK,
    WPA3_SAE,
    WPA2_EAP,
    UNKNOWN
}
