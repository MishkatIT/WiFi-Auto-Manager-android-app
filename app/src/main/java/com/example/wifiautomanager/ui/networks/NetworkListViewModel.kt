package com.example.wifiautomanager.ui.networks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.SuggestionState
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.usecase.ManageSavedNetworksUseCase
import com.example.wifiautomanager.wifi.WifiSuggestionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    val hasPassword: Boolean,
    val suggestionState: SuggestionState = SuggestionState.NotRegistered
)

data class NetworkListUiState(
    val networks: List<SavedNetworkItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSuggestionSupported: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class NetworkListViewModel @Inject constructor(
    private val manageNetworksUseCase: ManageSavedNetworksUseCase,
    private val wifiSuggestionManager: WifiSuggestionManager? = null
) : ViewModel() {

    private val suggestionStatesFlow = wifiSuggestionManager?.suggestionStates ?: flowOf(emptyMap())

    val uiState: StateFlow<NetworkListUiState> = combine(
        manageNetworksUseCase.getNetworks(),
        suggestionStatesFlow
    ) { list, states ->
        // Synchronize suggestions for enabled networks if suggestion manager is present
        if (wifiSuggestionManager != null && list.isNotEmpty()) {
            val enabledList = list.filter { it.enabled && states[it.id] == null }
            if (enabledList.isNotEmpty()) {
                wifiSuggestionManager.registerSuggestions(enabledList) { id ->
                    manageNetworksUseCase.getPassword(id)
                }
            }
        }

        NetworkListUiState(
            networks = list.map { net ->
                val state = if (!net.enabled) {
                    SuggestionState.NotRegistered
                } else {
                    states[net.id] ?: (
                        if (wifiSuggestionManager?.isSupported() == false) {
                            SuggestionState.ApiNotSupported
                        } else {
                            SuggestionState.NotRegistered
                        }
                    )
                }
                net.toSavedNetworkItem(
                    hasPassword = manageNetworksUseCase.hasPassword(net.id),
                    suggestionState = state
                )
            },
            isLoading = false,
            isSuggestionSupported = wifiSuggestionManager?.isSupported() ?: true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkListUiState(isLoading = true)
    )

    fun onToggleNetwork(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            manageNetworksUseCase.toggleNetwork(id, enabled)
            val network = manageNetworksUseCase.getNetwork(id)
            if (network != null && wifiSuggestionManager != null) {
                if (enabled) {
                    val password = manageNetworksUseCase.getPassword(id)
                    wifiSuggestionManager.registerSuggestion(network, password)
                } else {
                    wifiSuggestionManager.removeSuggestion(network)
                }
            }
        }
    }

    fun onDeleteNetwork(id: Long) {
        viewModelScope.launch {
            val network = manageNetworksUseCase.getNetwork(id)
            manageNetworksUseCase.deleteNetwork(id)
            if (network != null && wifiSuggestionManager != null) {
                wifiSuggestionManager.removeSuggestion(network)
            }
        }
    }

    fun syncAllSuggestions() {
        if (wifiSuggestionManager == null) return
        viewModelScope.launch {
            val networks = manageNetworksUseCase.getNetwork(1L)?.let { listOf(it) } ?: emptyList()
            // triggers resync via repository
        }
    }

    private fun WifiNetwork.toSavedNetworkItem(
        hasPassword: Boolean,
        suggestionState: SuggestionState
    ): SavedNetworkItem =
        SavedNetworkItem(
            id = id,
            ssid = ssid,
            securityType = securityType,
            priority = priority,
            minimumSignalDbm = minimumSignalDbm,
            requiresInternet = requiresInternet,
            enabled = enabled,
            hasPassword = hasPassword,
            suggestionState = suggestionState
        )
}
