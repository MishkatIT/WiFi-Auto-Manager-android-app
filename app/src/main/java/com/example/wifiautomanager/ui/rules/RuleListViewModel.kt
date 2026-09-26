package com.example.wifiautomanager.ui.rules

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class RuleListUiState(
    val rules: List<RuleItem> = emptyList()
)

data class RuleItem(
    val id: Long,
    val name: String,
    val enabled: Boolean,
    val priority: Int
)

@HiltViewModel
class RuleListViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(RuleListUiState())
    val uiState: StateFlow<RuleListUiState> = _uiState
}
