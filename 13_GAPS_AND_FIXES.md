# 13 — Gap Analysis & Fixes

This document records every gap found in the original specification, the risk of leaving it unaddressed, and how the revised spec resolves it.

---

## Gap 1: No Dependency Injection Framework

**Original spec:** Lists Hilt nowhere. Says "use AndroidX" but does not specify DI.

**Risk:**  
Without DI, the project devolves into manual constructor injection and singletons. ViewModels become untestable. Repositories become hard to swap for fakes in tests.

**Fix:**  
Added **Hilt (Dagger Hilt)** as a required dependency throughout all architecture layers. `DatabaseModule`, `RepositoryModule`, `WifiModule`, `UseCaseModule` all defined. See `02_ARCHITECTURE.md`.

---

## Gap 2: No Background Processing Strategy

**Original spec:** Says "respect Android background restrictions" and "use supported mechanisms" without naming any.

**Risk:**  
Without WorkManager, the app relies on a raw `Service` that Android may kill at any time, making background monitoring unreliable on modern Android. The spec had no plan for what to do when the OS kills the background process.

**Fix:**  
Added **WorkManager** as the primary background processing mechanism. Defined `DecisionWorker`, constraints (network available, not low battery), and periodic scheduling. Defined optional `WifiForegroundService` for when continuous monitoring is explicitly needed. See Phase 8 in `12_IMPLEMENTATION_PHASES.md`.

---

## Gap 3: No DataStore vs SharedPreferences Decision

**Original spec:** Lists `data/preferences` package but doesn't say what to use.

**Risk:**  
`SharedPreferences` has well-known race conditions, blocking main-thread reads, and no coroutine integration. Using it would conflict with the spec's coroutine-first design.

**Fix:**  
Specified **Jetpack DataStore (Preferences)** explicitly. Defined `AppPreferencesDataStore` and `AppSettings` data class. See `03_DATA_MODEL.md`.

---

## Gap 4: No UI Design or Wireframes

**Original spec:** Lists screen names but provides zero layout, visual design, component, or interaction specification.

**Risk:**  
Without a UI spec, implementation is entirely guesswork. Screens may be inconsistent, cluttered, or inaccessible. The dashboard "decision" card was mentioned in prose but never designed.

**Fix:**  
Added complete `07_UI_DESIGN.md` covering:
- Design language (Material 3, color tokens, typography)
- ASCII wireframes for all 9 screens
- Empty states for every screen
- Reusable component specifications
- All error/permission/disabled states
- Accessibility requirements with specific dp targets

---

## Gap 5: No Android Keystore Integration Details

**Original spec:** Says "prefer secure Android storage mechanisms" but gives no implementation.

**Risk:**  
Passwords end up in Room (plaintext in SQLite), SharedPreferences (plaintext), or `DataStore` (plaintext). None of these are secure.

**Fix:**  
Added complete `CredentialStore` implementation using **EncryptedSharedPreferences** backed by **Android Keystore** (AES256-GCM). Documented that passwords are excluded from Room entirely and only fetched at suggestion build time. See `10_SECURITY.md`.

---

## Gap 6: No Room Migration Strategy

**Original spec:** Lists Room but says nothing about migrations.

**Risk:**  
First schema change would either crash the app (missing migration) or wipe all user data (`fallbackToDestructiveMigration()`). Both are unacceptable.

**Fix:**  
Defined:
- `exportSchema = true` on `@Database`
- Mandatory `Migration` objects for every schema version change
- Example `MIGRATION_1_2` 
- Explicit prohibition of `fallbackToDestructiveMigration()` in production
See `03_DATA_MODEL.md`.

---

## Gap 7: No Notification System

**Original spec:** Never mentions notifications despite having a background decision engine.

**Risk:**  
If the app runs in the background and switches (or attempts to switch) networks, the user has no way to know. The app becomes a black box.

**Fix:**  
Added:
- `NotificationChannels` (Decision and Status channels)
- `DecisionNotifier` — posts when a suggestion is made
- `POST_NOTIFICATIONS` permission handling (API 33+)
- Settings toggle to enable/disable notifications
- Notification permission in `06_PERMISSIONS.md`
See `12_IMPLEMENTATION_PHASES.md` Phase 8.

---

## Gap 8: No Boot Receiver

**Original spec:** Does not address what happens when the device reboots.

**Risk:**  
After reboot, the auto manager does not restart. The app silently stops protecting the user without any indication.

**Fix:**  
Added:
- `RECEIVE_BOOT_COMPLETED` permission
- Boot receiver to restart WorkManager scheduling
- Status visible in Diagnostics screen
See `06_PERMISSIONS.md` and `12_IMPLEMENTATION_PHASES.md`.

---

## Gap 9: No API Level Compatibility Matrix

**Original spec:** Mentions `WifiNetworkSuggestion` and API differences vaguely.

**Risk:**  
Key APIs differ significantly across Android versions:
- `WifiNetworkSuggestion` requires API 29+
- `WifiInfo` from `NetworkCapabilities` requires API 29+ (with FLAG_INCLUDE_LOCATION_INFO on 31+)
- `NEARBY_WIFI_DEVICES` replaces location permission on API 33+

Without explicit guards, the app either crashes on older devices or silently fails on newer ones.

**Fix:**  
Documented complete API compatibility matrix in `01_PROJECT_OVERVIEW.md` and per-feature guards throughout `05_WIFI_INTEGRATION.md` and `06_PERMISSIONS.md`. Every API-sensitive code block has `Build.VERSION.SDK_INT` guards.

---

## Gap 10: No One-Shot Event Strategy for UI

**Original spec:** Describes ViewModels and StateFlow but doesn't address navigation events, snackbars, or dialogs.

**Risk:**  
Using `StateFlow` for one-shot events (navigation, snackbars) causes them to re-trigger on recomposition. Standard Android bug that causes double-navigation and repeated snackbars.

**Fix:**  
Specified `Channel<Event>` pattern for all one-shot events. `LaunchedEffect` collection in Composables. Documented in `08_STATE_MANAGEMENT.md`.

---

## Gap 11: No Serialization Strategy for Rules

**Original spec:** Defines a complex nested `ConditionGroup` structure in Room but doesn't say how to store it.

**Risk:**  
A nested sealed class hierarchy cannot be stored directly in Room. Without a serialization plan, the rule structure cannot be persisted.

**Fix:**  
Specified JSON serialization (`conditionGroupJson: String`) for `ConditionGroup` and `RuleAction` in `RuleEntity`. Added note about using Gson/Moshi. Added ProGuard keep rules for domain models. See `03_DATA_MODEL.md`.

---

## Gap 12: No WifiInfo SSID Visibility Issue Addressed

**Original spec:** Does not address the well-known issue that `WifiInfo.getSSID()` returns `<unknown ssid>` when location permission is missing.

**Risk:**  
The SSID displayed as current connection would be `<unknown ssid>` for users who deny location, making the entire dashboard misleading.

**Fix:**  
Documented in `05_WIFI_INTEGRATION.md`:
- API < 29: requires `ACCESS_FINE_LOCATION` for SSID
- API 29–30: `FLAG_INCLUDE_LOCATION_INFO` approach
- API 33+: `NEARBY_WIFI_DEVICES` replaces location requirement
Permission handling shows appropriate UI when SSID cannot be read.

---

## Gap 13: No Empty State Design

**Original spec:** Shows screen names but never defines what to show when a list is empty.

**Risk:**  
Empty lists show blank screens. Users don't know if the app is broken or working correctly with no data.

**Fix:**  
Defined empty states for every list screen in `07_UI_DESIGN.md` with:
- Clear explanatory text
- Action button where applicable (e.g., "Add first network")

---

## Gap 14: No `.gitignore` Content

**Original spec:** Says "add `.gitignore` appropriately" but doesn't specify what.

**Risk:**  
Developers may accidentally commit keystores, `local.properties`, or IDE files.

**Fix:**  
Provided complete Android `.gitignore` in `10_SECURITY.md` including keystore files, local.properties, IDE configs, and build artifacts.

---

## Gap 15: No SSID "Quick Add" from Nearby

**Original spec:** Shows "Add Network" screen and "Nearby Networks" screen separately but doesn't connect them.

**Risk:**  
Users who want to save a network they can see in the Nearby list would have to manually type the SSID.

**Fix:**  
Defined in `07_UI_DESIGN.md`:
- "+" button on nearby network cards → pre-fills Add Network screen with SSID
- "Select from nearby networks" dropdown in Add Network screen
- `nearbySsids: List<String>` in `AddEditNetworkUiState`

---

## Summary of Added Components (Not in Original Spec)

| Component | Why Added |
|-----------|-----------|
| Hilt DI | Testability, clean wiring |
| WorkManager | Reliable background processing |
| DataStore | Safe async preferences |
| NotificationChannels + DecisionNotifier | User observability of background decisions |
| Boot receiver | Persistence across reboots |
| CredentialStore (Keystore) | Secure password storage implementation |
| AppError sealed class | Structured error handling |
| One-shot event Channel pattern | Correct single-fire UI events |
| API compatibility matrix | Prevent crashes across Android versions |
| Rule JSON serialization strategy | Room-compatible rule persistence |
| UI wireframes (all 9 screens) | Consistent, accessible implementation target |
| Empty states | Clear UX when no data exists |
| Gitignore content | Security hygiene |
| Time estimate | Planning guidance |
| Test coverage targets | Accountability |
