package com.example.wifiautomanager.domain.model

data class ScannedNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val capabilities: String,
    val securityType: SecurityType,
    val isSaved: Boolean,
    val savedNetworkId: Long?,
    val timestampMs: Long,
)
