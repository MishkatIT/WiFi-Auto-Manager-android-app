package com.example.wifiautomanager.ui.nearby

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class NearbyNetworksUiState(
    val scannedNetworks: List<NearbyItem> = emptyList(),
    val isScanning: Boolean = false,
    val throttleCountdownSeconds: Int = 0
)

data class NearbyItem(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val securityType: String,
    val isSaved: Boolean
)

@HiltViewModel
class NearbyNetworksViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(NearbyNetworksUiState())
    val uiState: StateFlow<NearbyNetworksUiState> = _uiState
}
