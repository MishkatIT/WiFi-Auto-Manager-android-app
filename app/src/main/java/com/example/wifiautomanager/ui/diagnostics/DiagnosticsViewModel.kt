package com.example.wifiautomanager.ui.diagnostics

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiautomanager.domain.model.SuggestionState
import com.example.wifiautomanager.domain.repository.DecisionRepository
import com.example.wifiautomanager.domain.repository.WifiRepository
import com.example.wifiautomanager.wifi.WifiSuggestionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DiagnosticsUiState(
    val locationPermissionGranted: Boolean = true,
    val wifiState: String = "Active",
    val recentLogCount: Int = 0,
    val isSuggestionSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
    val androidSdkInt: Int = Build.VERSION.SDK_INT,
    val totalSavedNetworks: Int = 0,
    val activeSuggestionsCount: Int = 0,
    val suggestionStates: Map<Long, SuggestionState> = emptyMap()
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val wifiRepository: WifiRepository,
    private val decisionRepository: DecisionRepository,
    private val wifiSuggestionManager: WifiSuggestionManager? = null
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
        val suggestionFlow = wifiSuggestionManager?.suggestionStates ?: flowOf(emptyMap())

        combine(
            wifiRepository.getAllNetworks(),
            decisionRepository.getRecentDecisions(50),
            suggestionFlow
        ) { networks, decisions, suggestions ->
            val activeCount = suggestions.values.count { it is SuggestionState.Registered }
            _uiState.update {
                it.copy(
                    totalSavedNetworks = networks.size,
                    recentLogCount = decisions.size,
                    activeSuggestionsCount = activeCount,
                    suggestionStates = suggestions
                )
            }
        }.launchIn(viewModelScope)
    }
}
