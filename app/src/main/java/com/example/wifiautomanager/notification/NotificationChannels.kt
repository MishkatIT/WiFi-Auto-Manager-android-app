package com.example.wifiautomanager.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_ID_DECISIONS = "channel_decisions"
    const val CHANNEL_ID_FOREGROUND = "channel_foreground_service"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // Channel for network switch decisions
        val decisionsChannel = NotificationChannel(
            CHANNEL_ID_DECISIONS,
            "Wi-Fi Decisions",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when WiFi Auto Manager recommends or switches network"
            enableVibration(true)
        }

        // Channel for ongoing foreground monitoring service (silent)
        val foregroundChannel = NotificationChannel(
            CHANNEL_ID_FOREGROUND,
            "Wi-Fi Monitoring Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing notification required for continuous background Wi-Fi observation"
            setShowBadge(false)
        }

        notificationManager.createNotificationChannels(listOf(decisionsChannel, foregroundChannel))
    }
}
