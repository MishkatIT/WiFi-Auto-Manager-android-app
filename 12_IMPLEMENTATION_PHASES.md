# 12 — Implementation Phases

## Phase Gate Rule

> **Every phase must compile and pass its tests before moving to the next.**  
> Do not implement fake/placeholder Wi-Fi behavior at any phase.

---

## Phase 1 — Project Foundation

**Goal:** Compilable, navigable shell.

### Tasks
- [x] Create Android project (minSdk 26, targetSdk 35, Kotlin 2.x)
- [x] Configure `build.gradle.kts` with all dependencies (Compose BOM, Hilt, Room, DataStore, WorkManager)
- [x] Set up Hilt in `Application` class
- [x] Create package structure (all directories, empty placeholder files)
- [x] Set up Material 3 theme (color tokens, typography, shapes)
- [x] Create `Screen` sealed class and `AppNavGraph`
- [x] Implement bottom navigation bar with 3 tabs
- [x] Create stub screens (Dashboard, Networks, More) that compile and navigate
- [x] Add `.gitignore`

### Compile Check
```bash
./gradlew assembleDebug
./gradlew test
```

### Done When
- App builds and runs
- Navigation between tabs works
- Theme applied correctly (light + dark mode)

---

## Phase 2 — Local Database

**Goal:** Persisted network and rule storage.

### Tasks
- [ ] Create `AppDatabase` with Room
- [ ] Create `WifiNetworkEntity`, `RuleEntity`, `DecisionLogEntity`
- [ ] Create DAOs with Flow-returning queries
- [ ] Configure Room schema export
- [ ] Implement `CredentialStore` (Android Keystore + EncryptedSharedPreferences)
- [ ] Implement `AppPreferencesDataStore`
- [ ] Create `WifiRepository` interface and `WifiRepositoryImpl`
- [ ] Create `SettingsRepository` interface and `SettingsRepositoryImpl`
- [ ] Set up Hilt DI modules (`DatabaseModule`, `RepositoryModule`)
- [ ] Write Room DAO unit tests

### Done When
- Networks can be saved and retrieved across app restarts
- Passwords encrypted and separate from Room
- All DAO tests pass

---

## Phase 3 — Network Management UI

**Goal:** Full CRUD for saved networks.

### Tasks
- [ ] `NetworkListScreen` + `NetworkListViewModel`
- [ ] `AddEditNetworkScreen` + `AddEditNetworkViewModel`
- [ ] SSID input with "Pick from nearby" placeholder (scanner not yet connected)
- [ ] Security type selection
- [ ] Password input (masked, never in UiState)
- [ ] Priority slider
- [ ] Signal threshold sliders
- [ ] Minimum improvement slider
- [ ] "Requires internet" toggle
- [ ] Enable/Disable toggle per network
- [ ] Delete with confirmation dialog
- [ ] `ManageSavedNetworksUseCase`
- [ ] Input validation with inline errors
- [ ] Empty state for network list
- [ ] ViewModel unit tests

### Done When
- Networks can be added, edited, deleted
- All fields persist correctly
- Password never appears in logs or state
- Tests pass

---

## Phase 4 — Wi-Fi Observation

**Goal:** Real Wi-Fi data in the UI.

### Tasks
- [ ] Implement `PermissionHelper` with API-level guards
- [ ] Implement permission request flow in UI (rationale cards)
- [ ] Implement `WifiConnectionMonitor` (NetworkCallback-based)
- [ ] Implement `ConnectivityObserver`
- [ ] Implement `InternetChecker` (NET_CAPABILITY_VALIDATED)
- [ ] Implement `WifiScanner` with throttle handling
- [ ] Connect `WifiScanner` to `NearbyNetworksScreen`
- [ ] Wire `WifiConnectionMonitor` to `DashboardScreen`
- [ ] Track `InternetState` including unavailable duration
- [ ] Display scan throttle countdown
- [ ] Handle all scan error states in UI
- [ ] Handle permission denial gracefully in all screens

### Done When
- Dashboard shows real SSID, RSSI, internet status
- Nearby screen shows real scan results
- Permission denial shows actionable UI (not crash)
- Scan throttle shows countdown

---

## Phase 5 — Decision Engine

**Goal:** Core logic working, fully tested.

### Tasks
- [ ] Implement `Condition` sealed hierarchy
- [ ] Implement `ConditionEvaluator`
- [ ] Implement `RuleEvaluator` (AND/OR/NOT)
- [ ] Implement `AntiFlappingGuard`
- [ ] Implement `CandidateSelector`
- [ ] Implement `DecisionEngine`
- [ ] Implement `DecisionExplainer`
- [ ] Implement `RunDecisionCycleUseCase`
- [ ] Implement `DecisionLogDao` and persistence
- [ ] Wire decision cycle to connection/scan events
- [ ] Emit decisions to Dashboard
- [ ] Unit tests for all 7 documented scenarios
- [ ] Unit tests for all condition types
- [ ] Unit tests for AND/OR/NOT combinations
- [ ] Unit tests for anti-flapping

### Done When
- All decision scenarios produce correct outcomes
- Decision includes human-readable explanation
- Anti-flapping blocks unnecessary switches
- 90%+ test coverage on engine

---

## Phase 6 — Rule Builder UI

**Goal:** User can create and manage rules.

### Tasks
- [ ] `RuleListScreen` + `RuleListViewModel`
- [ ] `RuleBuilderScreen` + `RuleBuilderViewModel`
- [ ] Condition row component (type picker, operator picker, value input)
- [ ] Dynamic add/remove conditions
- [ ] Rule preview text
- [ ] AND/OR operator toggle
- [ ] Action selector
- [ ] Rule persistence (serialized to JSON in Room)
- [ ] Enable/disable rules
- [ ] Delete rules with confirmation
- [ ] Connect rules to `DecisionEngine`
- [ ] Rule ViewModel unit tests

### Done When
- Rules can be built, saved, enabled/disabled
- Decision engine uses active rules
- Invalid rules show clear UI errors

---

## Phase 7 — Android Wi-Fi Suggestion Integration

**Goal:** Suggestions registered with Android.

### Tasks
- [ ] Implement `WifiSuggestionManager`
- [ ] API 26–29 compatibility handling
- [ ] Register suggestions when networks are added/updated/enabled
- [ ] Remove suggestions when networks are deleted/disabled
- [ ] Track `SuggestionState` per network
- [ ] Display suggestion state in `NetworkListScreen`
- [ ] Display suggestion state in `DiagnosticsScreen`
- [ ] Handle: duplicate, not-allowed, failed, api-not-supported states
- [ ] Notify user: "Android may not immediately use suggestions"

### Done When
- Suggestions registered for enabled networks
- Status displayed accurately (not assumed success)
- API-level differences handled without crash

---

## Phase 8 — Notifications & Background

**Goal:** App useful when not in foreground.

### Tasks
- [ ] Create notification channels (`DecisionChannel`, `StatusChannel`)
- [ ] Implement `DecisionNotifier` — post notification on significant decision
- [ ] Request `POST_NOTIFICATIONS` on API 33+ with rationale
- [ ] Implement `DecisionWorker` (WorkManager)
- [ ] Configure periodic work with battery-aware constraints
- [ ] Implement optional `WifiForegroundService` for continuous monitoring
- [ ] Handle `RECEIVE_BOOT_COMPLETED` to restart monitoring
- [ ] Respect Doze mode — reduce scan frequency when device is idle
- [ ] Add background execution status to `DiagnosticsScreen`
- [ ] Settings toggle for notifications

### Done When
- Decisions trigger notifications (when permitted)
- Background monitoring survives screen-off
- App starts monitoring after device reboot
- Battery usage is reasonable

---

## Phase 9 — Diagnostics & Decision Detail

**Goal:** Full observability of app behavior.

### Tasks
- [ ] `DiagnosticsScreen` + `DiagnosticsViewModel`
- [ ] System status section (all permissions, service status)
- [ ] Current state snapshot
- [ ] Engine status (last scan, last decision, cooldown)
- [ ] Android suggestion states per network
- [ ] Decision log list (recent 20)
- [ ] `DecisionDetailScreen` — full breakdown of one decision
- [ ] Candidate evaluation table in decision detail
- [ ] Rules applied / not applied section
- [ ] Anti-flapping status in decision detail
- [ ] "Why didn't it switch?" always answerable from this screen

### Done When
- User can always understand why the app made a decision
- All system states visible in one place
- Decision log persists across restarts

---

## Phase 10 — Settings, Reliability & Polish

**Goal:** Production-ready.

### Tasks
- [ ] `SettingsScreen` + `SettingsViewModel`
- [ ] All configurable values with sane defaults and valid ranges
- [ ] Input validation on all settings sliders/fields
- [ ] Error handling audit — every code path verified
- [ ] Accessibility audit (content descriptions, touch targets, contrast)
- [ ] Dark mode QA pass on all screens
- [ ] Memory leak check (LeakCanary in debug build)
- [ ] Startup time check
- [ ] All strings in `strings.xml` (no hardcoded strings)
- [ ] ProGuard/R8 rules verified
- [ ] Backup exclusion rules verified
- [ ] Final unit test sweep — all gaps filled
- [ ] Compose UI tests for critical flows
- [ ] README with setup and usage instructions

### Done When
- All checklist items in §32 Definition of Done satisfied
- No crashes in 30 minutes of manual testing
- All tests pass
- Release APK builds

---

## Dependency Matrix

```
Phase 1 (Foundation)
    ↓
Phase 2 (Database)
    ↓
Phase 3 (Network UI)       Phase 4 (Wi-Fi Observation)
    ↓                                ↓
    └─────────────┬──────────────────┘
                  ↓
            Phase 5 (Decision Engine)
                  ↓
    ┌─────────────┼──────────────┐
    ↓             ↓              ↓
Phase 6        Phase 7        Phase 8
(Rule UI)   (Suggestions)  (Background)
    └─────────────┼──────────────┘
                  ↓
            Phase 9 (Diagnostics)
                  ↓
            Phase 10 (Polish)
```

---

## Time Estimate (Solo Developer)

| Phase | Estimated Effort |
|-------|-----------------|
| 1 | 1–2 days |
| 2 | 1–2 days |
| 3 | 2–3 days |
| 4 | 2–3 days |
| 5 | 3–4 days |
| 6 | 2–3 days |
| 7 | 1–2 days |
| 8 | 2–3 days |
| 9 | 2–3 days |
| 10 | 2–3 days |
| **Total** | **~18–28 days** |
