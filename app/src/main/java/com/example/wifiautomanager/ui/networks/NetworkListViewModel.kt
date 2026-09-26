package com.example.wifiautomanager.ui.networks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.usecase.ManageSavedNetworksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedNetworkItem(
    val id: Long,
    val ssid: String,
    val securityType: SecurityType,
    val priority: Int,
    val minimumSignalDbm: Int,
    val requiresInternet: Boolean,
    val enabled: Boolean,
    val hasPassword: Boolean
)

data class NetworkListUiState(
    val networks: List<SavedNetworkItem> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class NetworkListViewModel @Inject constructor(
    private val manageNetworksUseCase: ManageSavedNetworksUseCase
) : ViewModel() {

    val uiState: StateFlow<NetworkListUiState> = manageNetworksUseCase.getNetworks()
        .map { list ->
            NetworkListUiState(
                networks = list.map { it.toSavedNetworkItem(manageNetworksUseCase.hasPassword(it.id)) },
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NetworkListUiState(isLoading = true)
        )

    fun onToggleNetwork(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            manageNetworksUseCase.toggleNetwork(id, enabled)
        }
    }

    fun onDeleteNetwork(id: Long) {
        viewModelScope.launch {
            manageNetworksUseCase.deleteNetwork(id)
        }
    }

    private fun WifiNetwork.toSavedNetworkItem(hasPassword: Boolean): SavedNetworkItem =
        SavedNetworkItem(
            id = id,
            ssid = ssid,
            securityType = securityType,
            priority = priority,
            minimumSignalDbm = minimumSignalDbm,
            requiresInternet = requiresInternet,
            enabled = enabled,
            hasPassword = hasPassword
        )
}

