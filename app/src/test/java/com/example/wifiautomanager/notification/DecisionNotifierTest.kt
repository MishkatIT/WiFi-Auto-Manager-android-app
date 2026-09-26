package com.example.wifiautomanager.notification

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import com.example.wifiautomanager.domain.model.AppSettings
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

class FakeSettingsRepo(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val _settings = MutableStateFlow(initial)
    override fun getSettings(): Flow<AppSettings> = _settings.asStateFlow()
    override suspend fun updateSettings(settings: AppSettings) { _settings.value = settings }
    override suspend fun setAutoManagerEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoManagerEnabled = enabled)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DecisionNotifierTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var fakeSettingsRepo: FakeSettingsRepo
    private lateinit var notifier: DecisionNotifier

    @Before
    fun setup() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        context = app
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fakeSettingsRepo = FakeSettingsRepo(AppSettings(showDecisionNotifications = true))
        notifier = DecisionNotifier(context, fakeSettingsRepo)
        NotificationChannels.createChannels(context)
    }

    @Test
    fun testNotificationChannelsCreated() {
        val decisionsChannel = notificationManager.getNotificationChannel(NotificationChannels.CHANNEL_ID_DECISIONS)
        val foregroundChannel = notificationManager.getNotificationChannel(NotificationChannels.CHANNEL_ID_FOREGROUND)

        assertNotNull(decisionsChannel)
        assertNotNull(foregroundChannel)
        assertEquals("Wi-Fi Decisions", decisionsChannel.name)
        assertEquals("Wi-Fi Monitoring Service", foregroundChannel.name)
    }

    @Test
    fun testNotifySwitchDecisionPostsNotification() = runTest {
        val target = WifiNetwork(
            id = 1L,
            ssid = "Office_5G",
            securityType = SecurityType.WPA2_PSK,
            priority = 90
        )
        val decision = Decision(
            id = 1L,
            action = DecisionAction.SuggestNetwork(target),
            selectedNetwork = target,
            reason = "Signal is significantly stronger than current network.",
            currentState = WifiState.Disconnected
        )

        notifier.notifyDecision(decision)

        val shadow = shadowOf(notificationManager)
        assertTrue(shadow.allNotifications.isNotEmpty())
    }

    @Test
    fun testNotifySwitchWhenNotificationsDisabledDoesNotPost() = runTest {
        fakeSettingsRepo.updateSettings(AppSettings(showDecisionNotifications = false))

        val target = WifiNetwork(id = 2L, ssid = "Home_WiFi", securityType = SecurityType.WPA2_PSK, priority = 80)
        val decision = Decision(
            id = 2L,
            action = DecisionAction.SuggestNetwork(target),
            selectedNetwork = target,
            reason = "Switched to home",
            currentState = WifiState.Disconnected
        )

        notifier.notifyDecision(decision)

        val shadow = shadowOf(notificationManager)
        assertTrue(shadow.allNotifications.isEmpty())
    }

    @Test
    fun testNotifyStatusPostsNotification() {
        notifier.notifyStatus("Auto Manager Alert", "Wi-Fi scanning is active.")

        val shadow = shadowOf(notificationManager)
        assertTrue(shadow.allNotifications.isNotEmpty())
    }
}
