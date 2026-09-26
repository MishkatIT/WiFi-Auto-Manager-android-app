# 08 — State Management

## Principle

Every screen has a single `UiState` data class.  
ViewModels expose `StateFlow<UiState>`.  
Composables observe and render state — they do not contain logic.

No scattered boolean flags. No nullable fields that create ambiguous combinations.

---

## Dashboard UiState

```kotlin
data class DashboardUiState(
    val connectionCard: ConnectionCardState,
    val autoManagerState: AutoManagerState,
    val currentDecision: DecisionCardState?,
    val nearbyCandidates: List<CandidateSummary>,
    val permissionsState: PermissionsState,
)

sealed class ConnectionCardState {
    data object Loading : ConnectionCardState()
    data object WifiDisabled : ConnectionCardState()
    data object Disconnected : ConnectionCardState()
    data class  Connected(
        val ssid: String,
        val rssiDbm: Int,
        val signalLevel: SignalLevel,     // POOR / FAIR / GOOD / EXCELLENT
        val frequencyGhz: String,         // "2.4 GHz" or "5 GHz" or "6 GHz"
        val internetStatus: InternetStatus,
        val connectedDuration: String,    // "2h 13m"
    ) : ConnectionCardState()
    data class  Error(val message: String) : ConnectionCardState()
}

enum class AutoManagerState { ENABLED, DISABLED, ERROR }

data class DecisionCardState(
    val actionText: String,
    val reasonText: String,
    val decisionId: Long,
    val timestampText: String,
)

data class CandidateSummary(
    val ssid: String,
    val rssiDbm: Int,
    val signalLevel: SignalLevel,
    val internetAvailable: Boolean,
    val qualified: Boolean,
    val rejectionReason: String?,
)

enum class SignalLevel { POOR, FAIR, GOOD, EXCELLENT }
```

---

## Network List UiState

```kotlin
data class NetworkListUiState(
    val networks: List<NetworkItemState>,
    val isLoading: Boolean,
    val error: String?,
)

data class NetworkItemState(
    val id: Long,
    val ssid: String,
    val securityType: SecurityType,
    val enabled: Boolean,
    val priority: Int,
    val suggestionState: SuggestionState,
    val isCurrentlyConnected: Boolean,
)
```

---

## Add/Edit Network UiState

```kotlin
data class AddEditNetworkUiState(
    val ssid: String = "",
    val securityType: SecurityType = SecurityType.WPA2_PSK,
    val priority: Int = 50,
    val minimumSignalDbm: Int = -70,
    val minimumCandidateSignalDbm: Int = -65,
    val minimumImprovementDbm: Int = 10,
    val requiresInternet: Boolean = false,
    val registerSuggestion: Boolean = true,
    val ssidError: String? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val generalError: String? = null,
    val nearbySsids: List<String> = emptyList(),   // for SSID picker
)
```

---

## Rule Builder UiState

```kotlin
data class RuleBuilderUiState(
    val name: String = "",
    val operator: LogicalOperator = LogicalOperator.AND,
    val conditions: List<ConditionRowState> = emptyList(),
    val action: RuleActionState = RuleActionState.PreferBest,
    val nameError: String? = null,
    val conditionsError: String? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val preview: String = "",              // human-readable rule preview
)

data class ConditionRowState(
    val id: String,                         // local UUID for list key
    val type: ConditionType,
    val operator: ConditionOperator,
    val value: String,
    val validationError: String?,
)

sealed class RuleActionState {
    data object PreferBest : RuleActionState()
    data class  PreferNetwork(val networkId: Long, val ssid: String) : RuleActionState()
    data object StayOnCurrent : RuleActionState()
}
```

---

## Nearby Networks UiState

```kotlin
data class NearbyNetworksUiState(
    val scanState: ScanUiState,
    val savedNetworks: List<NearbyNetworkItem>,
    val otherNetworks: List<NearbyNetworkItem>,
    val lastScanTime: String?,
    val nextScanCountdown: Int?,
)

sealed class ScanUiState {
    data object Idle : ScanUiState()
    data object Scanning : ScanUiState()
    data class  Throttled(val secondsRemaining: Int) : ScanUiState()
    data class  PermissionRequired(val permissions: List<String>) : ScanUiState()
    data object WifiDisabled : ScanUiState()
    data class  Error(val message: String) : ScanUiState()
}

data class NearbyNetworkItem(
    val ssid: String,
    val rssiDbm: Int,
    val signalLevel: SignalLevel,
    val frequencyMhz: Int,
    val securityType: SecurityType,
    val isSaved: Boolean,
    val savedNetworkId: Long?,
    val isConnected: Boolean,
)
```

---

## Diagnostics UiState

```kotlin
data class DiagnosticsUiState(
    val systemStatus: SystemStatusState,
    val currentState: CurrentStateSnapshot,
    val engineStatus: EngineStatusState,
    val suggestionStates: List<SuggestionStatusItem>,
    val recentDecisions: List<DecisionLogItem>,
)

data class SystemStatusState(
    val autoManagerRunning: Boolean,
    val wifiEnabled: Boolean,
    val scanPermissionGranted: Boolean,
    val notificationPermissionGranted: Boolean,
    val backgroundLocationGranted: Boolean,
)

data class EngineStatusState(
    val lastScanTime: String?,
    val lastDecisionTime: String?,
    val lastSwitchTime: String?,
    val cooldownRemainingSeconds: Int?,
)
```

---

## ViewModel Contracts

All ViewModels follow this pattern:

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeConnectionUseCase: ObserveCurrentConnectionUseCase,
    private val observeDecisionUseCase: ObserveLastDecisionUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(/* defaults */))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // One-shot events (navigation, snackbars)
    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events: Flow<DashboardEvent> = _events.receiveAsFlow()

    init {
        observeConnection()
        observeDecisions()
    }

    fun onAutoManagerToggled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoManagerEnabled(enabled)
        }
    }

    fun onDecisionDetailsClicked(decisionId: Long) {
        viewModelScope.launch {
            _events.send(DashboardEvent.NavigateToDecisionDetail(decisionId))
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            observeConnectionUseCase().collect { wifiState ->
                _uiState.update { state ->
                    state.copy(connectionCard = wifiState.toConnectionCardState())
                }
            }
        }
    }
}

sealed class DashboardEvent {
    data class NavigateToDecisionDetail(val id: Long) : DashboardEvent()
    data class ShowError(val message: String) : DashboardEvent()
}
```

---

## One-Shot Events (Channel Pattern)

Navigation, snackbars, and dialogs use `Channel` rather than `StateFlow` to avoid re-triggering on recomposition:

```kotlin
// In Composable
LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
        when (event) {
            is DashboardEvent.NavigateToDecisionDetail ->
                navController.navigate(Screen.DecisionDetail(event.id).route)
            is DashboardEvent.ShowError ->
                snackbarHostState.showSnackbar(event.message)
        }
    }
}
```
