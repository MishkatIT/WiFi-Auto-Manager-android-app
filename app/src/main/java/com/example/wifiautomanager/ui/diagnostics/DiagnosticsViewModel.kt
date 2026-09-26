package com.example.wifiautomanager.ui.diagnostics

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiautomanager.background.WifiForegroundService
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.InternetStatus
import com.example.wifiautomanager.domain.model.SuggestionState
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.domain.repository.DecisionRepository
import com.example.wifiautomanager.domain.repository.SettingsRepository
import com.example.wifiautomanager.domain.repository.WifiRepository
import com.example.wifiautomanager.wifi.ScanState
import com.example.wifiautomanager.wifi.WifiConnectionMonitor
import com.example.wifiautomanager.wifi.WifiScanner
import com.example.wifiautomanager.wifi.WifiSuggestionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DiagnosticsUiState(
    val locationPermissionGranted: Boolean = true,
    val wifiStateSummary: String = "Active",
    val currentSsid: String? = null,
    val currentRssi: Int? = null,
    val currentFrequency: Int? = null,
    val currentInternetStatus: InternetStatus = InternetStatus.UNKNOWN,
    val isSuggestionSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
    val androidSdkInt: Int = Build.VERSION.SDK_INT,
    val totalSavedNetworks: Int = 0,
    val activeSuggestionsCount: Int = 0,
    val autoManagerEnabled: Boolean = false,
    val isForegroundServiceRunning: Boolean = false,
    val lastScanTimeText: String = "None recorded",
    val recentDecisions: List<Decision> = emptyList(),
    val suggestionStates: Map<Long, SuggestionState> = emptyMap()
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val wifiRepository: WifiRepository,
    private val decisionRepository: DecisionRepository,
    private val settingsRepository: SettingsRepository,
    private val wifiSuggestionManager: WifiSuggestionManager? = null,
    private val wifiConnectionMonitor: WifiConnectionMonitor? = null,
    private val wifiScanner: WifiScanner? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DiagnosticsUiState(
            isSuggestionSupported = wifiSuggestionManager?.isSupported() ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q),
            androidSdkInt = Build.VERSION.SDK_INT
        )
    )
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        val connectionFlow = wifiConnectionMonitor?.connectionState ?: flowOf(WifiState.Disconnected)

        combine(
            wifiRepository.getAllNetworks(),
            decisionRepository.getRecentDecisions(20),
            settingsRepository.getSettings(),
            WifiForegroundService.isRunning,
            connectionFlow
        ) { networks, decisions, settings, isServiceRunning, connection ->
            val suggestions = wifiSuggestionManager?.suggestionStates?.value ?: emptyMap()
            val scanState = wifiScanner?.scanState?.value ?: ScanState.Idle
            val activeCount = suggestions.values.count { it is SuggestionState.Registered }

            val (ssid, rssi, freq, internet, summary) = when (connection) {
                is WifiState.Connected ->
                    Tuple5(connection.ssid, connection.rssi, connection.frequencyMhz, connection.internetStatus, "Connected")
                WifiState.Disconnected -> Tuple5(null, null, null, InternetStatus.UNAVAILABLE, "Disconnected")
                WifiState.Disabled -> Tuple5(null, null, null, InternetStatus.UNAVAILABLE, "Wi-Fi Disabled")
                is WifiState.PermissionRequired -> Tuple5(null, null, null, InternetStatus.UNKNOWN, "Permission Required")
                is WifiState.Error -> Tuple5(null, null, null, InternetStatus.UNKNOWN, "Error: ${connection.message}")
                else -> Tuple5(null, null, null, InternetStatus.UNKNOWN, "Idle")
            }

            val lastScanText = when (scanState) {
                is ScanState.Results -> "${scanState.networks.size} networks found (${if (scanState.isFresh) "fresh" else "cached"})"
                is ScanState.Scanning -> "Scan in progress..."
                is ScanState.Throttled -> "Scan throttled by OS"
                else -> "Ready to scan"
            }

            _uiState.update {
                it.copy(
                    totalSavedNetworks = networks.size,
                    activeSuggestionsCount = activeCount,
                    autoManagerEnabled = settings.autoManagerEnabled,
                    isForegroundServiceRunning = isServiceRunning,
                    recentDecisions = decisions,
                    suggestionStates = suggestions,
                    currentSsid = ssid,
                    currentRssi = rssi,
                    currentFrequency = freq,
                    currentInternetStatus = internet,
                    wifiStateSummary = summary,
                    lastScanTimeText = lastScanText
                )
            }
        }.launchIn(viewModelScope)
    }

    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
