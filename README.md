# WiFi-Auto-Manager-android-app

[![Android Min SDK 26](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen.svg)](https://developer.android.com)
[![Android Target SDK 35](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-blue.svg)](https://developer.android.com)
[![Kotlin 2.0.21](https://img.shields.io/badge/Kotlin-2.0.21-orange.svg)](https://kotlinlang.org)
[![Compose Material 3](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-purple.svg)](https://developer.android.com/jetpack/compose)

A production-quality, local-first Android Wi-Fi decision and automation engine built with modern Android development standards. Evaluates Wi-Fi network conditions, prevents flapping, integrates with the Android `WifiNetworkSuggestion` API, and gives users full control and observability over connection decisions.

---

## Key Features

1. **Intelligent Decision Engine**
   - Pure Kotlin domain logic evaluating signal strength (RSSI), frequency bands (2.4 GHz vs 5 GHz vs 6 GHz), captive portal detection, and user-defined rules.
   - Built-in anti-flapping guard enforcing cooldown periods and signal delta thresholds to prevent rapid, battery-draining connection toggling.
   - Transparent, human-readable explanations generated for every decision.

2. **User-Defined Rules Engine**
   - Visual Rule Builder supporting composite boolean conditions (`AND`, `OR`, `NOT`).
   - Evaluates criteria like minimum RSSI, SSID matches, frequency band preferences, and captive portal statuses.
   - Dynamically re-evaluates candidates on network state and scan changes.

3. **Modern Wi-Fi Suggestion API Integration**
   - Compliant with Android 10+ (API 29+) `WifiNetworkSuggestion` architecture.
   - Automatic registration, update, and removal of Wi-Fi suggestions with Android OS.
   - Accurate tracking and display of OS suggestion statuses (`Registered`, `Duplicate`, `NotAllowed`, `Failed`, `ApiNotSupported`).

4. **Background Monitoring & Battery Efficiency**
   - Battery-aware WorkManager periodic tasks (`DecisionWorker`) with battery-not-low constraints.
   - Optional foreground service for continuous mission-critical Wi-Fi monitoring.
   - Reboot recovery via `RECEIVE_BOOT_COMPLETED` broadcast receiver.
   - Adaptive scan throttling respecting Android OS scan throttle policies.

5. **Deep Diagnostics & Observability**
   - Real-time Dashboard tracking current Wi-Fi status, link speed, frequency, IP, and internet reachability.
   - Diagnostics Screen displaying recent decision logs, permission states, and engine status.
   - Decision Detail Screen breaking down candidate scoring, qualified vs. disqualified networks, and anti-flapping checks.

6. **Hardware-Backed Security & Privacy**
   - Wi-Fi credentials stored exclusively using `EncryptedSharedPreferences` backed by the Android Keystore (AES-256 GCM).
   - Passwords never logged, exported, or leaked into database tables or UI states.
   - Complete exclusion from automated cloud backups via `backup_rules.xml` and `data_extraction_rules.xml`.
   - Zero remote telemetry — 100% local-first operation.

---

## Architecture Overview

Built using **Clean Architecture**, **MVVM**, and the **Repository Pattern**:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│    Jetpack Compose UI  •  Material 3  •  ViewModels         │
│  (Dashboard, Networks, Rules, Diagnostics, Settings)        │
└──────────────────────────────┬──────────────────────────────┘
                               │ Observes UiState via Flow
┌──────────────────────────────▼──────────────────────────────┐
│                       Domain Layer                          │
│   Use Cases  •  DecisionEngine  •  RuleEvaluator            │
│   AntiFlappingGuard  •  Domain Models (Pure Kotlin)         │
└──────────────────────────────┬──────────────────────────────┘
                               │ Implements interfaces
┌──────────────────────────────▼──────────────────────────────┐
│                        Data Layer                           │
│  Repositories  •  Room Database (Cipher/SQLite)             │
│  EncryptedSharedPreferences (Keystore)  •  DataStore        │
│  WifiScanner  •  WifiConnectionMonitor  •  SuggestionManager │
└─────────────────────────────────────────────────────────────┘
```

---

## Tech Stack & Libraries

- **Language:** Kotlin 2.0.21
- **UI Toolkit:** Jetpack Compose (BOM 2024.10.01) + Material 3 Design
- **Dependency Injection:** Dagger Hilt 2.52
- **Database:** Room 2.6.1 with Coroutines & Flow support
- **Preferences:** Jetpack DataStore Preferences 1.1.1
- **Cryptography:** AndroidX Security Crypto 1.1.0-alpha06 (MasterKeys / EncryptedSharedPreferences)
- **Background Execution:** AndroidX WorkManager 2.10.0
- **Serialization:** Google Gson 2.11.0
- **Testing:** JUnit 4, Robolectric 4.13, Coroutines Test 1.9.0, AndroidX Test Core

---

## Project Structure

```
app/src/main/java/com/example/wifiautomanager/
├── background/         # WorkManager Worker, Foreground Service, Boot Receiver
├── data/
│   ├── local/          # Room DB, DAOs, Entities, Converters, Encrypted DataStore
│   ├── mapper/         # Entity <-> Domain mappers
│   └── repository/     # Repository implementations
├── di/                 # Dagger Hilt dependency injection modules
├── domain/
│   ├── model/          # Pure Kotlin domain data classes and sealed types
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Use cases (Decision cycle, network management, etc.)
├── engine/             # DecisionEngine, RuleEvaluator, AntiFlapping, CandidateSelector
├── notification/       # Notification channels and DecisionNotifier
├── ui/
│   ├── dashboard/      # Current network status & quick decision logs
│   ├── diagnostics/    # System status, decision history & candidate breakdown
│   ├── navigation/     # NavHost and screen routing
│   ├── networks/       # Saved networks, Add/Edit with Keystore password security
│   ├── rules/          # Rule list and visual dynamic rule builder
│   ├── settings/       # Configurable intervals, thresholds, and service toggles
│   └── theme/          # Material 3 colors, typography, shapes
└── wifi/               # WifiScanner, WifiConnectionMonitor, WifiSuggestionManager
```

---

## Build & Run

### Prerequisites
- JDK 17 (configured via `JAVA_HOME`)
- Android SDK 35 (Platform Tools, Build Tools 35.0.0+)
- Gradle 8.10.2 (via included `gradlew` wrapper)

### Command-line Build
```bash
# Set Java 17 path (Windows PowerShell example)
$env:JAVA_HOME = "D:\WiFi Auto Manager android app\jdk-17\jdk-17.0.12+7"

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Build debug APK
.\gradlew.bat assembleDebug

# Build release APK (R8/ProGuard enabled)
.\gradlew.bat assembleRelease
```

---

## Documentation Index

- [01 — Project Overview](01_PROJECT_OVERVIEW.md)
- [02 — Architecture](02_ARCHITECTURE.md)
- [03 — Data Model](03_DATA_MODEL.md)
- [04 — Decision Engine](04_DECISION_ENGINE.md)
- [05 — Wi-Fi Integration](05_WIFI_INTEGRATION.md)
- [06 — Permissions](06_PERMISSIONS.md)
- [07 — UI Design](07_UI_DESIGN.md)
- [08 — State Management](08_STATE_MANAGEMENT.md)
- [09 — Error Handling](09_ERROR_HANDLING.md)
- [10 — Security](10_SECURITY.md)
- [11 — Testing](11_TESTING.md)
- [12 — Implementation Phases](12_IMPLEMENTATION_PHASES.md)
- [13 — Gaps and Fixes](13_GAPS_AND_FIXES.md)

---

## License

This project is licensed under the Apache 2.0 License.
