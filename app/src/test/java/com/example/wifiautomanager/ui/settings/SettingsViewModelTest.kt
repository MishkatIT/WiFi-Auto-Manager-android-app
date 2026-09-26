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
}
