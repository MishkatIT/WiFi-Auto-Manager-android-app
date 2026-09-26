package com.example.wifiautomanager.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun formatTime(timestampMs: Long): String {
        return timeFormat.format(Date(timestampMs))
    }

    fun formatDuration(sinceMs: Long): String {
        if (sinceMs <= 0L) return "Just now"
        val elapsedSec = (System.currentTimeMillis() - sinceMs) / 1000L
        if (elapsedSec <= 5L) return "Just now"
        if (elapsedSec < 60) return "${elapsedSec}s"
        val minutes = elapsedSec / 60
        if (minutes < 60) return "${minutes}m"
        val hours = minutes / 60
        val remMinutes = minutes % 60
        return "${hours}h ${remMinutes}m"
    }
}
