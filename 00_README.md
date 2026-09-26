# WiFi Auto Manager — Complete Engineering Documentation

> A production-quality, local-first Android Wi-Fi decision and automation engine.

---

## Document Index

| # | File | Contents |
|---|------|----------|
| 01 | [PROJECT_OVERVIEW.md](01_PROJECT_OVERVIEW.md) | Goals, non-goals, constraints, engineering principles |
| 02 | [ARCHITECTURE.md](02_ARCHITECTURE.md) | Layered architecture, package structure, dependency rules |
| 03 | [DATA_MODEL.md](03_DATA_MODEL.md) | Domain models, Room schema, relationships, migrations |
| 04 | [DECISION_ENGINE.md](04_DECISION_ENGINE.md) | Rule engine, conditions, evaluation, anti-flapping |
| 05 | [WIFI_INTEGRATION.md](05_WIFI_INTEGRATION.md) | Scanning, connection monitoring, suggestions, internet detection |
| 06 | [PERMISSIONS.md](06_PERMISSIONS.md) | All required permissions, API-level handling, denial flows |
| 07 | [UI_DESIGN.md](07_UI_DESIGN.md) | Screen-by-screen wireframes, Material 3 design language, interactions |
| 08 | [STATE_MANAGEMENT.md](08_STATE_MANAGEMENT.md) | Sealed classes, state flows, ViewModel contracts |
| 09 | [ERROR_HANDLING.md](09_ERROR_HANDLING.md) | Error taxonomy, recovery strategies, logging rules |
| 10 | [SECURITY.md](10_SECURITY.md) | Credential storage, Android Keystore, policy |
| 11 | [TESTING.md](11_TESTING.md) | Unit, integration, UI test strategy and scenarios |
| 12 | [IMPLEMENTATION_PHASES.md](12_IMPLEMENTATION_PHASES.md) | Phased plan with gates, milestones, and done criteria |
| 13 | [GAPS_AND_FIXES.md](13_GAPS_AND_FIXES.md) | Analysis of the original spec — what was missing and why it matters |

---

## Quick Reference

### Minimum API Level
`minSdk 26` (Android 8.0) — required for `WifiNetworkSuggestion` baseline support.  
`targetSdk 35` (Android 15).  
`compileSdk 35`.

### Core Stack
```
Kotlin 2.x
Jetpack Compose (BOM latest stable)
Material 3
Room 2.x
Hilt (dependency injection — added vs original spec)
Kotlin Coroutines + Flow
Navigation Compose
WorkManager (background scheduling)
DataStore Preferences
```

### Key Missing Items Found in Original Spec
1. No dependency injection framework specified
2. No background worker strategy (WorkManager)
3. No DataStore vs SharedPreferences decision
4. No UI design / wireframes / design language
5. No Keystore integration details
6. No Room migration strategy
7. No Hilt/DI setup
8. No notification system for decisions
9. No app widget consideration
10. No localization plan

All gaps are documented and resolved in [13_GAPS_AND_FIXES.md](13_GAPS_AND_FIXES.md).
