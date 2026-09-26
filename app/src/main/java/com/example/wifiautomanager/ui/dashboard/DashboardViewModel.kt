package com.example.wifiautomanager.ui.dashboard

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class DashboardUiState(
    val isAutoManagerEnabled: Boolean = false,
    val currentSsid: String? = null,
    val currentRssi: Int? = null,
    val internetStatusText: String = "Unknown",
    val lastDecisionSummary: String = "No decisions evaluated yet."
)

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState
}
