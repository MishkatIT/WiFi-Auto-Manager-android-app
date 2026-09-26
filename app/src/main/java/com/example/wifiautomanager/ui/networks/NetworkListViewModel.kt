package com.example.wifiautomanager.ui.networks

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class NetworkListUiState(
    val networks: List<SavedNetworkItem> = emptyList(),
    val isLoading: Boolean = false
)

data class SavedNetworkItem(
    val id: Long,
    val ssid: String,
    val securityType: String,
    val priority: Int,
    val enabled: Boolean
)

@HiltViewModel
class NetworkListViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(NetworkListUiState())
    val uiState: StateFlow<NetworkListUiState> = _uiState
}
