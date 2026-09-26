package com.example.wifiautomanager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiautomanager.domain.model.InternetStatus
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.domain.repository.SettingsRepository
import com.example.wifiautomanager.util.SignalLevelMapper
import com.example.wifiautomanager.wifi.WifiConnectionMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isAutoManagerEnabled: Boolean = true,
    val wifiState: WifiState = WifiState.Disconnected,
    val currentSsid: String? = null,
    val currentBssid: String? = null,
    val currentRssi: Int? = null,
    val signalLevelLabel: String = "No Signal",
    val currentFrequencyMhz: Int? = null,
    val internetStatus: InternetStatus = InternetStatus.UNKNOWN,
    val connectedSinceMs: Long? = null,
    val isWifiDisabled: Boolean = false,
    val requiresPermission: Boolean = false,
    val missingPermissions: List<String> = emptyList(),
    val lastDecisionSummary: String = "Evaluating network signals and priority rules."
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val wifiConnectionMonitor: WifiConnectionMonitor,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    init {
        wifiConnectionMonitor.start()
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        wifiConnectionMonitor.connectionState,
        settingsRepository.getSettings()
    ) { wifiState, settings ->
        when (wifiState) {
            is WifiState.Connected -> {
                DashboardUiState(
                    isAutoManagerEnabled = settings.autoManagerEnabled,
                    wifiState = wifiState,
                    currentSsid = wifiState.ssid,
                    currentBssid = wifiState.bssid,
                    currentRssi = wifiState.rssi,
                    signalLevelLabel = SignalLevelMapper.formatSignalDescription(wifiState.rssi),
                    currentFrequencyMhz = wifiState.frequencyMhz,
                    internetStatus = wifiState.internetStatus,
                    connectedSinceMs = wifiState.connectedSinceMs,
                    isWifiDisabled = false,
                    requiresPermission = false,
                    lastDecisionSummary = "Connected to preferred network ${wifiState.ssid}."
                )
            }
            is WifiState.Disabled -> {
                DashboardUiState(
                    isAutoManagerEnabled = settings.autoManagerEnabled,
                    wifiState = wifiState,
                    isWifiDisabled = true,
                    requiresPermission = false,
                    lastDecisionSummary = "Wi-Fi is currently turned off on device."
                )
            }
            is WifiState.PermissionRequired -> {
                DashboardUiState(
                    isAutoManagerEnabled = settings.autoManagerEnabled,
                    wifiState = wifiState,
                    requiresPermission = true,
                    missingPermissions = wifiState.missing,
                    lastDecisionSummary = "Location / Nearby Wi-Fi permission required to inspect connection."
                )
            }
            is WifiState.Error -> {
                DashboardUiState(
                    isAutoManagerEnabled = settings.autoManagerEnabled,
                    wifiState = wifiState,
                    lastDecisionSummary = wifiState.message
                )
            }
            else -> {
                DashboardUiState(
                    isAutoManagerEnabled = settings.autoManagerEnabled,
                    wifiState = wifiState,
                    lastDecisionSummary = "Searching for known networks..."
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun onToggleAutoSwitcher(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoManagerEnabled(enabled)
        }
    }

    fun retryConnection() {
        wifiConnectionMonitor.start()
    }

    override fun onCleared() {
        super.onCleared()
        wifiConnectionMonitor.stop()
    }
}
