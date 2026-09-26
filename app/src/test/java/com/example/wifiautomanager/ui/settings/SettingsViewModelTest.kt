package com.example.wifiautomanager.ui.settings

import android.content.Context
import com.example.wifiautomanager.domain.model.AppSettings
import com.example.wifiautomanager.notification.FakeSettingsRepo
import com.example.wifiautomanager.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var fakeSettingsRepo: FakeSettingsRepo
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        fakeSettingsRepo = FakeSettingsRepo(
            AppSettings(
                autoManagerEnabled = false,
                scanIntervalSeconds = 30,
                showDecisionNotifications = true
            )
        )
        viewModel = SettingsViewModel(fakeSettingsRepo, context)
    }

    @Test
    fun testInitialStateLoaded() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.autoManagerEnabled)
        assertEquals(30, state.scanIntervalSeconds)
        assertTrue(state.showDecisionNotifications)
    }

    @Test
    fun testToggleAutoManagerUpdatesRepository() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onToggleAutoManager(true)
        advanceUntilIdle()

        val updated = fakeSettingsRepo.getSettings().first()
        assertTrue(updated.autoManagerEnabled)
        assertTrue(viewModel.uiState.value.autoManagerEnabled)
    }

    @Test
    fun testToggleNotificationsUpdatesRepository() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onToggleNotifications(false)
        advanceUntilIdle()

        val updated = fakeSettingsRepo.getSettings().first()
        assertFalse(updated.showDecisionNotifications)
        assertFalse(viewModel.uiState.value.showDecisionNotifications)
    }

    @Test
    fun testUpdateScanIntervalEnforcesBounds() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onUpdateScanInterval(5) // Below 15s limit
        advanceUntilIdle()

        val updated = fakeSettingsRepo.getSettings().first()
        assertEquals(15, updated.scanIntervalSeconds)
    }

    @Test
    fun testUpdateInternetCheckIntervalEnforcesBounds() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onUpdateInternetCheckInterval(100) // Upper limit is 60
        advanceUntilIdle()
        assertEquals(60, fakeSettingsRepo.getSettings().first().internetCheckIntervalSeconds)

        viewModel.onUpdateInternetCheckInterval(2) // Lower limit is 5
        advanceUntilIdle()
        assertEquals(5, fakeSettingsRepo.getSettings().first().internetCheckIntervalSeconds)
    }

    @Test
    fun testUpdateSwitchCooldownEnforcesBounds() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onUpdateSwitchCooldown(45)
        advanceUntilIdle()
        assertEquals(45, fakeSettingsRepo.getSettings().first().switchCooldownSeconds)

        viewModel.onUpdateSwitchCooldown(500) // Upper limit is 300
        advanceUntilIdle()
        assertEquals(300, fakeSettingsRepo.getSettings().first().switchCooldownSeconds)

        viewModel.onUpdateSwitchCooldown(10) // Lower limit is 30
        advanceUntilIdle()
        assertEquals(30, fakeSettingsRepo.getSettings().first().switchCooldownSeconds)
    }

    @Test
    fun testUpdateInternetTimeoutEnforcesBounds() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onUpdateInternetTimeout(25)
        advanceUntilIdle()
        assertEquals(25, fakeSettingsRepo.getSettings().first().internetUnavailableTimeoutSeconds)

        viewModel.onUpdateInternetTimeout(100) // Upper limit 60
        advanceUntilIdle()
        assertEquals(60, fakeSettingsRepo.getSettings().first().internetUnavailableTimeoutSeconds)

        viewModel.onUpdateInternetTimeout(2) // Lower limit 5
        advanceUntilIdle()
        assertEquals(5, fakeSettingsRepo.getSettings().first().internetUnavailableTimeoutSeconds)
    }

    @Test
    fun testUpdateStabilityWindowEnforcesBounds() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onUpdateStabilityWindow(20)
        advanceUntilIdle()
        assertEquals(20, fakeSettingsRepo.getSettings().first().stabilityWindowSeconds)

        viewModel.onUpdateStabilityWindow(120) // Upper limit 60
        advanceUntilIdle()
        assertEquals(60, fakeSettingsRepo.getSettings().first().stabilityWindowSeconds)

        viewModel.onUpdateStabilityWindow(1) // Lower limit 5
        advanceUntilIdle()
        assertEquals(5, fakeSettingsRepo.getSettings().first().stabilityWindowSeconds)
    }

    @Test
    fun testUpdateLogRetentionEnforcesBounds() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onUpdateLogRetention(14)
        advanceUntilIdle()
        assertEquals(14, fakeSettingsRepo.getSettings().first().logRetentionDays)

        viewModel.onUpdateLogRetention(50) // Upper limit 30
        advanceUntilIdle()
        assertEquals(30, fakeSettingsRepo.getSettings().first().logRetentionDays)

        viewModel.onUpdateLogRetention(0) // Lower limit 1
        advanceUntilIdle()
        assertEquals(1, fakeSettingsRepo.getSettings().first().logRetentionDays)
    }
}
