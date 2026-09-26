package com.example.wifiautomanager.background

import android.content.Context
import android.content.Intent
import com.example.wifiautomanager.domain.model.AppSettings
import com.example.wifiautomanager.notification.FakeSettingsRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var fakeSettingsRepo: FakeSettingsRepo
    private lateinit var bootReceiver: BootReceiver

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        fakeSettingsRepo = FakeSettingsRepo(AppSettings(autoManagerEnabled = true))
        bootReceiver = BootReceiver().apply {
            settingsRepository = fakeSettingsRepo
        }
    }

    @Test
    fun testIgnoresNonBootAction() {
        val intent = Intent("android.intent.action.OTHER")
        bootReceiver.onReceiveInternal(context, intent, isAsyncSupported = false)
    }

    @Test
    fun testHandlesBootCompletedWithAutoManagerEnabled() = runTest {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        bootReceiver.onReceiveInternal(context, intent, isAsyncSupported = false)
        advanceUntilIdle()
    }

    @Test
    fun testHandlesBootCompletedWithAutoManagerDisabled() = runTest {
        fakeSettingsRepo.setAutoManagerEnabled(false)
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        bootReceiver.onReceiveInternal(context, intent, isAsyncSupported = false)
        advanceUntilIdle()
    }
}
