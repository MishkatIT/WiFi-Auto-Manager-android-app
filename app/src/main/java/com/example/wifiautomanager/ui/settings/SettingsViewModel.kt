package com.example.wifiautomanager.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class SettingsUiState(
    val scanIntervalSeconds: Int = 30,
    val internetCheckIntervalSeconds: Int = 10,
    val switchCooldownSeconds: Int = 60,
    val showDecisionNotifications: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState
}
