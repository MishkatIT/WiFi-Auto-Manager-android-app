# 02 — Architecture

## Layer Overview

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                         │
│         Jetpack Compose + Navigation                │
│    Screens / Components / Theme / State hoisting    │
└────────────────────┬────────────────────────────────┘
                     │ observes StateFlow / UiState
┌────────────────────▼────────────────────────────────┐
│                 ViewModel Layer                     │
│         HiltViewModel + SavedStateHandle            │
│     Transforms domain state → UI state              │
│     Dispatches user events → Use Cases              │
└────────────────────┬────────────────────────────────┘
                     │ calls / collects
┌────────────────────▼────────────────────────────────┐
│               Domain / Use Case Layer               │
│     Pure Kotlin — zero Android imports              │
│     ObserveConnectionUseCase                        │
│     EvaluateCandidatesUseCase                       │
│     ApplyRulesUseCase                               │
│     ManageNetworksUseCase                           │
│     CheckInternetUseCase                            │
└────────────────────┬────────────────────────────────┘
                     │ depends on (interfaces)
┌────────────────────▼────────────────────────────────┐
│               Repository Layer                      │
│     WifiRepository  (interface + impl)              │
│     RuleRepository  (interface + impl)              │
│     SettingsRepository (interface + impl)           │
└─────────┬────────────────────────┬──────────────────┘
          │                        │
┌─────────▼──────────┐   ┌─────────▼──────────────────┐
│  Local Data Layer  │   │  Android Wi-Fi Layer        │
│  Room Database     │   │  WifiScanner                │
│  DataStore Prefs   │   │  WifiConnectionMonitor      │
│  Android Keystore  │   │  WifiSuggestionManager      │
└────────────────────┘   │  InternetChecker            │
                         │  ConnectivityObserver       │
                         └────────────────────────────┘
```

---

## Dependency Rule

```
UI → ViewModel → Use Cases → Repository Interfaces
                                     ↓
                           Repository Implementations
                                     ↓
                           Data Sources (Room / WifiManager / etc.)
```

**The domain layer (Use Cases + Domain Models) must have zero Android framework imports.**  
This makes it fully unit-testable on the JVM without Robolectric.

---

## Package Structure

```
com.example.wifiautomanager
│
├── app
│   ├── MainActivity.kt
│   ├── WifiAutoManagerApp.kt           ← Application class (Hilt entry point)
│   └── di
│       ├── DatabaseModule.kt
│       ├── RepositoryModule.kt
│       ├── WifiModule.kt
│       └── UseCaseModule.kt
│
├── data
│   ├── local
│   │   ├── db
│   │   │   ├── AppDatabase.kt
│   │   │   ├── dao
│   │   │   │   ├── WifiNetworkDao.kt
│   │   │   │   ├── RuleDao.kt
│   │   │   │   ├── ConditionDao.kt
│   │   │   │   └── DecisionLogDao.kt
│   │   │   ├── entity
│   │   │   │   ├── WifiNetworkEntity.kt
│   │   │   │   ├── RuleEntity.kt
│   │   │   │   ├── ConditionEntity.kt
│   │   │   │   └── DecisionLogEntity.kt
│   │   │   └── migrations
│   │   │       └── Migration_1_2.kt
│   │   │
│   │   ├── keystore
│   │   │   └── CredentialStore.kt      ← Android Keystore wrapper
│   │   │
│   │   └── datastore
│   │       └── AppPreferencesDataStore.kt
│   │
│   └── repository
│       ├── WifiRepositoryImpl.kt
│       ├── RuleRepositoryImpl.kt
│       └── SettingsRepositoryImpl.kt
│
├── domain
│   ├── model
│   │   ├── WifiNetwork.kt              ← Pure Kotlin domain model
│   │   ├── Rule.kt
│   │   ├── Condition.kt
│   │   ├── WifiState.kt
│   │   ├── ScannedNetwork.kt
│   │   ├── Decision.kt
│   │   ├── CandidateResult.kt
│   │   └── AppSettings.kt
│   │
│   ├── repository
│   │   ├── WifiRepository.kt           ← Interface
│   │   ├── RuleRepository.kt           ← Interface
│   │   └── SettingsRepository.kt       ← Interface
│   │
│   ├── rule
│   │   ├── Condition.kt                ← Sealed hierarchy
│   │   ├── ConditionEvaluator.kt
│   │   ├── RuleEvaluator.kt
│   │   └── LogicalOperator.kt
│   │
│   ├── decision
│   │   ├── DecisionEngine.kt
│   │   ├── CandidateSelector.kt
│   │   ├── AntiFlappingGuard.kt
│   │   └── DecisionExplainer.kt
│   │
│   └── usecase
│       ├── ObserveCurrentConnectionUseCase.kt
│       ├── ObserveNearbyNetworksUseCase.kt
│       ├── EvaluateCandidatesUseCase.kt
│       ├── ManageSavedNetworksUseCase.kt
│       ├── ManageRulesUseCase.kt
│       ├── RunDecisionCycleUseCase.kt
│       └── CheckInternetUseCase.kt
│
├── wifi
│   ├── WifiScanner.kt
│   ├── WifiConnectionMonitor.kt
│   ├── WifiSuggestionManager.kt
│   ├── InternetChecker.kt
│   └── ConnectivityObserver.kt
│
├── background
│   ├── DecisionWorker.kt               ← WorkManager Worker
│   └── WifiForegroundService.kt        ← Optional foreground service
│
├── notification
│   ├── NotificationChannels.kt
│   └── DecisionNotifier.kt
│
├── ui
│   ├── theme
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   ├── Theme.kt
│   │   └── Shape.kt
│   │
│   ├── navigation
│   │   ├── AppNavGraph.kt
│   │   └── Screen.kt                   ← Sealed class of routes
│   │
│   ├── dashboard
│   │   ├── DashboardScreen.kt
│   │   └── DashboardViewModel.kt
│   │
│   ├── networks
│   │   ├── NetworkListScreen.kt
│   │   ├── NetworkListViewModel.kt
│   │   ├── AddEditNetworkScreen.kt
│   │   └── AddEditNetworkViewModel.kt
│   │
│   ├── rules
│   │   ├── RuleListScreen.kt
│   │   ├── RuleListViewModel.kt
│   │   ├── RuleBuilderScreen.kt
│   │   └── RuleBuilderViewModel.kt
│   │
│   ├── nearby
│   │   ├── NearbyNetworksScreen.kt
│   │   └── NearbyNetworksViewModel.kt
│   │
│   ├── diagnostics
│   │   ├── DiagnosticsScreen.kt
│   │   ├── DiagnosticsViewModel.kt
│   │   ├── DecisionDetailScreen.kt
│   │   └── DecisionDetailViewModel.kt
│   │
│   ├── settings
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   │
│   └── components
│       ├── SignalStrengthIndicator.kt
│       ├── InternetStatusBadge.kt
│       ├── NetworkCard.kt
│       ├── DecisionCard.kt
│       ├── ConditionRow.kt
│       ├── PermissionRationaleCard.kt
│       └── ErrorStateView.kt
│
└── util
    ├── PermissionHelper.kt
    ├── SignalLevelMapper.kt
    ├── TimeFormatter.kt
    └── Logger.kt
```

---

## Dependency Injection (Hilt)

All modules are Hilt `@Module` / `@InstallIn`.

```kotlin
// Example — DatabaseModule
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "wifi_manager.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideWifiNetworkDao(db: AppDatabase): WifiNetworkDao = db.wifiNetworkDao()
}
```

---

## Navigation Routes

```kotlin
sealed class Screen(val route: String) {
    data object Dashboard       : Screen("dashboard")
    data object Networks        : Screen("networks")
    data object AddNetwork      : Screen("networks/add")
    data class  EditNetwork(val id: Long) : Screen("networks/edit/{id}")
    data object Rules           : Screen("rules")
    data object RuleBuilder     : Screen("rules/builder")
    data class  EditRule(val id: Long) : Screen("rules/edit/{id}")
    data object Nearby          : Screen("nearby")
    data object Diagnostics     : Screen("diagnostics")
    data object DecisionDetail  : Screen("diagnostics/decision/{id}")
    data object Settings        : Screen("settings")
}
```

---

## Coroutine Scopes

| Scope | Usage |
|-------|-------|
| `viewModelScope` | UI-bound operations, cancelled when ViewModel is cleared |
| `ServiceScope` | Foreground service lifetime |
| `WorkerScope` | WorkManager worker (provided by WorkManager) |
| `applicationScope` (custom) | Repository-level flows that outlive ViewModels |

Never use `GlobalScope`.

---

## Key Design Decisions

### Why Hilt (not manual DI)?
The original spec omitted DI entirely. Without it, repository/ViewModel construction becomes error-prone at scale and untestable. Hilt is the officially recommended Android DI solution.

### Why DataStore (not SharedPreferences)?
DataStore provides coroutine-native, type-safe, crash-safe preference storage. SharedPreferences has known race conditions on the main thread.

### Why WorkManager (not a raw Service)?
Android background execution restrictions make raw services unreliable. WorkManager is battery-aware, constraint-aware, and integrates with Doze mode correctly.

### Why split `WifiState` into a sealed class?
Boolean flags (`isConnected`, `hasInternet`, `isScanning`) can form impossible combinations. A sealed class makes illegal states unrepresentable.
