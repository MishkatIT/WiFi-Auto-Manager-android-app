package com.example.wifiautomanager.ui.diagnostics

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class DiagnosticsUiState(
    val locationPermissionGranted: Boolean = false,
    val wifiState: String = "Unknown",
    val recentLogCount: Int = 0
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState
}
