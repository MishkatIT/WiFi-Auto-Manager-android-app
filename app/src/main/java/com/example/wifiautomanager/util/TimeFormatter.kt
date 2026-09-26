package com.example.wifiautomanager.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun formatTime(timestampMs: Long): String {
        return timeFormat.format(Date(timestampMs))
    }
}
