package com.example.wifiautomanager.util

object SignalLevelMapper {
    fun getSignalCategory(rssi: Int): String {
        return when {
            rssi >= -60 -> "Excellent"
            rssi >= -70 -> "Good"
            rssi >= -80 -> "Fair"
            else -> "Weak"
        }
    }
}
