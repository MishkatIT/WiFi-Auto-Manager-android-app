package com.example.wifiautomanager.domain.model

sealed class WifiState {
    data object Disabled : WifiState()
    data object Disconnected : WifiState()
    data object Scanning : WifiState()

    data class Connected(
        val ssid: String,
        val bssid: String,
        val rssi: Int,
        val frequencyMhz: Int,
        val internetStatus: InternetStatus,
        val connectedSinceMs: Long,
    ) : WifiState()

    data class PermissionRequired(val missing: List<String>) : WifiState()
    data class Error(val message: String, val cause: Throwable? = null) : WifiState()
}

enum class InternetStatus {
    AVAILABLE,
    UNAVAILABLE,
    CHECKING,
    UNKNOWN
}
