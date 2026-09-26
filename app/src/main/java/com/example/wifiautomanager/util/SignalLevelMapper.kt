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

    fun formatSignalDescription(rssi: Int): String {
        return "${rssi} dBm (${getSignalCategory(rssi)})"
    }

    /**
     * Maps RSSI to 0..4 bars
     */
    fun getBars(rssi: Int): Int {
        return when {
            rssi >= -55 -> 4
            rssi >= -67 -> 3
            rssi >= -78 -> 2
            rssi >= -88 -> 1
            else -> 0
        }
    }
}
