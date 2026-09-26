package com.example.wifiautomanager.ui.networks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.usecase.ManageSavedNetworksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditNetworkUiState(
    val networkId: Long = 0,
    val isEditMode: Boolean = false,
    val ssid: String = "",
    val securityType: SecurityType = SecurityType.WPA2_PSK,
    val priority: Int = 80,
    val minimumSignalDbm: Int = -70,
    val minimumCandidateSignalDbm: Int = -65,
    val minimumImprovementDbm: Int = 10,
    val requiresInternet: Boolean = true,
    val enabled: Boolean = true,
    val hasStoredPassword: Boolean = false,
    val ssidError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false
    // NOTE: Password is intentionally excluded from UiState for security.
)

sealed interface AddEditNetworkEvent {
    data object SaveSuccess : AddEditNetworkEvent
    data class ShowError(val message: String) : AddEditNetworkEvent
}

@HiltViewModel
class AddEditNetworkViewModel @Inject constructor(
    private val manageNetworksUseCase: ManageSavedNetworksUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val networkId: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val _uiState = MutableStateFlow(AddEditNetworkUiState(networkId = networkId, isEditMode = networkId > 0))
    val uiState: StateFlow<AddEditNetworkUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddEditNetworkEvent>()
    val events: SharedFlow<AddEditNetworkEvent> = _events.asSharedFlow()

    init {
        if (networkId > 0) {
            loadNetwork(networkId)
        }
    }

    fun loadNetwork(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val network = manageNetworksUseCase.getNetwork(id)
            if (network != null) {
                val hasPwd = manageNetworksUseCase.hasPassword(id)
                _uiState.update {
                    it.copy(
                        networkId = network.id,
                        isEditMode = true,
                        ssid = network.ssid,
                        securityType = network.securityType,
                        priority = network.priority,
                        minimumSignalDbm = network.minimumSignalDbm,
                        minimumCandidateSignalDbm = network.minimumCandidateSignalDbm,
                        minimumImprovementDbm = network.minimumImprovementDbm,
                        requiresInternet = network.requiresInternet,
                        enabled = network.enabled,
                        hasStoredPassword = hasPwd,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(AddEditNetworkEvent.ShowError("Network not found"))
            }
        }
    }

    fun onSsidChanged(ssid: String) {
        _uiState.update {
            it.copy(
                ssid = ssid,
                ssidError = if (ssid.isBlank()) "SSID cannot be blank" else if (ssid.length > 32) "SSID maximum length is 32 characters" else null
            )
        }
    }

    fun onSecurityTypeChanged(securityType: SecurityType) {
        _uiState.update {
            it.copy(
                securityType = securityType,
                passwordError = null
            )
        }
    }

    fun onPriorityChanged(priority: Int) {
        _uiState.update { it.copy(priority = priority.coerceIn(1, 100)) }
    }

    fun onMinimumSignalChanged(dbm: Int) {
        _uiState.update { it.copy(minimumSignalDbm = dbm.coerceIn(-90, -40)) }
    }

    fun onMinimumCandidateSignalChanged(dbm: Int) {
        _uiState.update { it.copy(minimumCandidateSignalDbm = dbm.coerceIn(-90, -40)) }
    }

    fun onMinimumImprovementChanged(dbm: Int) {
        _uiState.update { it.copy(minimumImprovementDbm = dbm.coerceIn(0, 30)) }
    }

    fun onRequiresInternetChanged(requiresInternet: Boolean) {
        _uiState.update { it.copy(requiresInternet = requiresInternet) }
    }

    fun onEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(enabled = enabled) }
    }

    /**
     * Saves the network.
     * Note: password is never persisted in ViewModel state, only passed at submission time.
     */
    fun onSave(password: String) {
        val state = _uiState.value

        // Validate SSID
        if (state.ssid.isBlank()) {
            _uiState.update { it.copy(ssidError = "SSID cannot be blank") }
            return
        }
        if (state.ssid.length > 32) {
            _uiState.update { it.copy(ssidError = "SSID maximum length is 32 characters") }
            return
        }

        // Validate password
        if (state.securityType != SecurityType.OPEN) {
            val needsNewPassword = !state.isEditMode || (!state.hasStoredPassword && password.isEmpty())
            if (needsNewPassword && password.isBlank()) {
                _uiState.update { it.copy(passwordError = "Password is required for secured networks") }
                return
            }
            if (password.isNotEmpty() && password.length < 8) {
                _uiState.update { it.copy(passwordError = "Password must be at least 8 characters") }
                return
            }
        }

        viewModelScope.launch {
            try {
                val networkToSave = WifiNetwork(
                    id = state.networkId,
                    ssid = state.ssid.trim(),
                    securityType = state.securityType,
                    priority = state.priority,
                    minimumSignalDbm = state.minimumSignalDbm,
                    minimumCandidateSignalDbm = state.minimumCandidateSignalDbm,
                    minimumImprovementDbm = state.minimumImprovementDbm,
                    requiresInternet = state.requiresInternet,
                    enabled = state.enabled,
                    updatedAt = System.currentTimeMillis()
                )

                val pwdToStore = if (state.securityType == SecurityType.OPEN) null else password.ifBlank { null }
                manageNetworksUseCase.saveNetwork(networkToSave, pwdToStore)
                _events.emit(AddEditNetworkEvent.SaveSuccess)
            } catch (e: Exception) {
                _events.emit(AddEditNetworkEvent.ShowError(e.message ?: "Failed to save network"))
            }
        }
    }
}
