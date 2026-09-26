package com.example.wifiautomanager.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiautomanager.domain.model.ScannedNetwork
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.wifi.ScanState
import com.example.wifiautomanager.wifi.WifiScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NearbyNetworksUiState(
    val savedNetworks: List<ScannedNetwork> = emptyList(),
    val otherNetworks: List<ScannedNetwork> = emptyList(),
    val isScanning: Boolean = false,
    val isWifiDisabled: Boolean = false,
    val permissionMissing: Boolean = false,
    val missingPermissions: List<String> = emptyList(),
    val throttleCountdownSeconds: Int = 0,
    val errorMessage: String? = null,
    val lastScanTimeMs: Long? = null
)

@HiltViewModel
class NearbyNetworksViewModel @Inject constructor(
    private val wifiScanner: WifiScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyNetworksUiState())
    val uiState: StateFlow<NearbyNetworksUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        observeScanState()
        requestScan()
    }

    private fun observeScanState() {
        viewModelScope.launch {
            wifiScanner.scanState.collect { state ->
                when (state) {
                    is ScanState.Idle -> {
                        _uiState.update { it.copy(isScanning = false) }
                    }
                    is ScanState.Scanning -> {
                        _uiState.update { it.copy(isScanning = true, errorMessage = null) }
                    }
                    is ScanState.WifiDisabled -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                isWifiDisabled = true,
                                permissionMissing = false,
                                errorMessage = "Wi-Fi is disabled on your device."
                            )
                        }
                    }
                    is ScanState.PermissionMissing -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                permissionMissing = true,
                                missingPermissions = state.permissions,
                                errorMessage = "Nearby Wi-Fi / Location permission is required to scan."
                            )
                        }
                    }
                    is ScanState.Throttled -> {
                        val remainingMs = state.nextAllowedMs - System.currentTimeMillis()
                        val remainingSeconds = (remainingMs / 1000L).coerceAtLeast(1).toInt()
                        startThrottleCountdown(remainingSeconds)
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                throttleCountdownSeconds = remainingSeconds
                            )
                        }
                    }
                    is ScanState.Failed -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                errorMessage = state.reason
                            )
                        }
                    }
                    is ScanState.Results -> {
                        val saved = state.networks.filter { it.isSaved }
                        val others = state.networks.filter { !it.isSaved }
                        _uiState.update {
                            it.copy(
                                savedNetworks = saved,
                                otherNetworks = others,
                                isScanning = false,
                                isWifiDisabled = false,
                                permissionMissing = false,
                                errorMessage = null,
                                lastScanTimeMs = state.timestampMs
                            )
                        }
                    }
                }
            }
        }
    }

    fun requestScan() {
        val triggered = wifiScanner.requestScan()
        if (!triggered && _uiState.value.throttleCountdownSeconds <= 0) {
            // Scanner handles emitting Throttle or Error state
        }
    }

    private fun startThrottleCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                _uiState.update { it.copy(throttleCountdownSeconds = remaining) }
                delay(1000)
                remaining--
            }
            _uiState.update { it.copy(throttleCountdownSeconds = 0) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        wifiScanner.unregister()
    }
}
