package com.example.wifiautomanager.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiautomanager.background.DecisionWorker
import com.example.wifiautomanager.background.WifiForegroundService
import com.example.wifiautomanager.domain.model.AppSettings
import com.example.wifiautomanager.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val autoManagerEnabled: Boolean = false,
    val scanIntervalSeconds: Int = 30,
    val internetCheckIntervalSeconds: Int = 10,
    val internetUnavailableTimeoutSeconds: Int = 15,
    val switchCooldownSeconds: Int = 60,
    val stabilityWindowSeconds: Int = 10,
    val logRetentionDays: Int = 7,
    val showDecisionNotifications: Boolean = true,
    val isForegroundServiceRunning: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.getSettings(),
        WifiForegroundService.isRunning
    ) { settings, isServiceRunning ->
        SettingsUiState(
            autoManagerEnabled = settings.autoManagerEnabled,
            scanIntervalSeconds = settings.scanIntervalSeconds,
            internetCheckIntervalSeconds = settings.internetCheckIntervalSeconds,
            internetUnavailableTimeoutSeconds = settings.internetUnavailableTimeoutSeconds,
            switchCooldownSeconds = settings.switchCooldownSeconds,
            stabilityWindowSeconds = settings.stabilityWindowSeconds,
            logRetentionDays = settings.logRetentionDays,
            showDecisionNotifications = settings.showDecisionNotifications,
            isForegroundServiceRunning = isServiceRunning
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun onToggleAutoManager(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoManagerEnabled(enabled)
            if (enabled) {
                DecisionWorker.schedule(context)
                try {
                    WifiForegroundService.start(context)
                } catch (e: Exception) {
                    // Handled gracefully if background start restrictions apply
                }
            } else {
                DecisionWorker.cancel(context)
                WifiForegroundService.stop(context)
            }
        }
    }

    fun onToggleNotifications(enabled: Boolean) {
        updateSetting { it.copy(showDecisionNotifications = enabled) }
    }

    fun onUpdateScanInterval(seconds: Int) {
        updateSetting { it.copy(scanIntervalSeconds = seconds.coerceIn(15, 300)) }
    }

    fun onUpdateInternetCheckInterval(seconds: Int) {
        updateSetting { it.copy(internetCheckIntervalSeconds = seconds.coerceIn(5, 60)) }
    }

    fun onUpdateSwitchCooldown(seconds: Int) {
        updateSetting { it.copy(switchCooldownSeconds = seconds.coerceIn(30, 300)) }
    }

    fun onUpdateInternetTimeout(seconds: Int) {
        updateSetting { it.copy(internetUnavailableTimeoutSeconds = seconds.coerceIn(5, 60)) }
    }

    fun onUpdateStabilityWindow(seconds: Int) {
        updateSetting { it.copy(stabilityWindowSeconds = seconds.coerceIn(5, 60)) }
    }

    fun onUpdateLogRetention(days: Int) {
        updateSetting { it.copy(logRetentionDays = days.coerceIn(1, 30)) }
    }

    private fun updateSetting(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().firstOrNull() ?: AppSettings()
            settingsRepository.updateSettings(transform(current))
        }
    }
}
