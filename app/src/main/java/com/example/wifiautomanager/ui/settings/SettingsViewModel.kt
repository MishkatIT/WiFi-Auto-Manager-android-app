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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val autoManagerEnabled: Boolean = false,
    val scanIntervalSeconds: Int = 30,
    val internetCheckIntervalSeconds: Int = 10,
    val switchCooldownSeconds: Int = 60,
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
            switchCooldownSeconds = settings.switchCooldownSeconds,
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
        viewModelScope.launch {
            val current = uiState.value
            settingsRepository.updateSettings(
                AppSettings(
                    autoManagerEnabled = current.autoManagerEnabled,
                    scanIntervalSeconds = current.scanIntervalSeconds,
                    internetCheckIntervalSeconds = current.internetCheckIntervalSeconds,
                    switchCooldownSeconds = current.switchCooldownSeconds,
                    showDecisionNotifications = enabled
                )
            )
        }
    }

    fun onUpdateScanInterval(seconds: Int) {
        viewModelScope.launch {
            val current = uiState.value
            settingsRepository.updateSettings(
                AppSettings(
                    autoManagerEnabled = current.autoManagerEnabled,
                    scanIntervalSeconds = seconds.coerceIn(15, 300),
                    internetCheckIntervalSeconds = current.internetCheckIntervalSeconds,
                    switchCooldownSeconds = current.switchCooldownSeconds,
                    showDecisionNotifications = current.showDecisionNotifications
                )
            )
        }
    }
}
