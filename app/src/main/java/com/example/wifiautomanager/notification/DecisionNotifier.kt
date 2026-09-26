package com.example.wifiautomanager.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.wifiautomanager.app.MainActivity
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.repository.SettingsRepository
import com.example.wifiautomanager.util.PermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecisionNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    companion object {
        const val DECISION_NOTIFICATION_ID = 2001
        const val STATUS_NOTIFICATION_ID = 2002
    }

    suspend fun notifyDecision(decision: Decision) {
        if (!PermissionHelper.hasNotificationPermission(context)) return

        val settings = settingsRepository.getSettings().firstOrNull()
        if (settings?.showDecisionNotifications == false) return

        val (title, content) = when (val action = decision.action) {
            is DecisionAction.SuggestNetwork -> {
                "Recommended Wi-Fi Switch" to
                        "Recommends connecting to ${action.network.ssid}. ${decision.reason}"
            }
            else -> return
        }

        sendNotification(
            notificationId = DECISION_NOTIFICATION_ID,
            channelId = NotificationChannels.CHANNEL_ID_DECISIONS,
            title = title,
            content = content
        )
    }

    fun notifyStatus(title: String, message: String) {
        if (!PermissionHelper.hasNotificationPermission(context)) return

        sendNotification(
            notificationId = STATUS_NOTIFICATION_ID,
            channelId = NotificationChannels.CHANNEL_ID_DECISIONS,
            title = title,
            content = message
        )
    }

    private fun sendNotification(
        notificationId: Int,
        channelId: String,
        title: String,
        content: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager?.notify(notificationId, notification)
    }
}
