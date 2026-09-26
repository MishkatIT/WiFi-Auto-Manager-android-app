# 01 — Project Overview

## Product Statement

**WiFi Auto Manager** is a personal, local-first Android application that gives the user fine-grained control over how their device evaluates and prefers trusted Wi-Fi networks.

It is a **Wi-Fi decision and automation engine** — not a simple settings manager.

Android remains the final authority over all network connections. This application advises, suggests, and explains — it does not override the OS.

---

## Core User Problems Solved

| Problem | Solution |
|---------|----------|
| "I'm connected to weak home Wi-Fi while office Wi-Fi is right there" | Priority rules + candidate evaluation |
| "My phone keeps flipping between two networks" | Anti-flapping engine with cooldowns |
| "I don't know why my phone connected to this bad network" | Decision explanation UI |
| "I'm connected to Wi-Fi but have no internet" | Internet detection separate from association |
| "I want work Wi-Fi only during office hours" | Time-based condition rules |

---

## What the App Can Do

- Save multiple trusted Wi-Fi networks with priorities and preferences
- Observe nearby Wi-Fi networks in real time
- Monitor the currently connected network and signal quality
- Detect whether internet is actually available (not just Wi-Fi association)
- Define configurable conditions (signal, internet, time, stability, priority)
- Combine conditions with AND / OR / NOT logic
- Evaluate and rank candidate networks
- Register preferred networks as Android `WifiNetworkSuggestion`
- Prevent unnecessary switching (anti-flapping)
- Explain why a network was selected or rejected
- Recover gracefully from all failure states

---

## What the App Cannot Do (Android Platform Limitations)

| Limitation | Reason |
|-----------|--------|
| Cannot guarantee immediate connection | Android controls final network selection |
| Cannot force disconnect from a network | Not permitted without root |
| Cannot scan Wi-Fi at arbitrary intervals | Android throttles scans (4 scans per 2 minutes in foreground) |
| Cannot read Wi-Fi passwords of saved networks | Android does not expose this |
| Cannot guarantee background scan frequency | Doze/App Standby may restrict it |
| Cannot override the system's network selection algorithm | Android's WifiService decides |

The UI must communicate these limitations honestly.

---

## Non-Goals

- No backend, cloud sync, or remote configuration
- No Firebase or any cloud service
- No root / hidden APIs / accessibility API abuse
- No continuous aggressive scanning
- No password transmission off-device
- No fake/simulated Wi-Fi behavior in any builds
- No bypassing of Android permission requirements

---

## Target Users

- Power users who manage devices in multiple environments (home/office/café)
- Users frustrated by automatic network switching behavior
- IT professionals managing personal devices
- Developers who want to understand Android Wi-Fi selection behavior

---

## Engineering Principles

1. Correctness over cleverness
2. Explicit state — no hidden boolean flag soup
3. Deterministic decision engine (same input → same output)
4. Android-specific code isolated from business logic
5. Every failure is observable and diagnosable
6. Never silently ignore errors
7. Never compromise security for convenience
8. Never assume Android will allow an operation
9. Never force a network switch unnecessarily
10. Battery usage is a first-class concern
11. Architecture must be extensible for new condition types
12. Tests for decision logic before adding complexity
13. Preserve user data during upgrades and failures
14. Explain automated decisions to the user

---

## Technology Stack

### Required
```
Language:           Kotlin 2.x
UI:                 Jetpack Compose (Material 3)
Navigation:         Navigation Compose
Architecture:       MVVM + Clean Architecture
DI:                 Hilt (Dagger Hilt)
Database:           Room 2.x
Async:              Kotlin Coroutines + Flow / StateFlow
Preferences:        DataStore (Preferences)
Background:         WorkManager
Wi-Fi:              WifiManager, ConnectivityManager, WifiNetworkSuggestion
Build:              Gradle Kotlin DSL
Min SDK:            26 (Android 8.0)
Target SDK:         35 (Android 15)
Compile SDK:        35
```

### Explicitly Excluded
```
Firebase
Any cloud/backend dependency
Root APIs
Hidden/private APIs
Accessibility API for automation
Third-party networking libraries (OkHttp not needed — no server)
```

---

## Platform Compatibility Matrix

| Feature | API 26 | API 29 | API 30 | API 33+ |
|---------|--------|--------|--------|---------|
| WifiNetworkSuggestion | ✅ basic | ✅ improved | ✅ | ✅ |
| WifiInfo (in NetworkCallback) | ❌ direct | ✅ via callback | ✅ | ✅ |
| ACCESS_FINE_LOCATION for scan | required | required | required | required |
| NEARBY_WIFI_DEVICES | ❌ | ❌ | ❌ | ✅ replaces location |
| ConnectivityManager.NetworkCallback | ✅ | ✅ | ✅ | ✅ |
| Background scan restriction | partial | stricter | stricter | stricter |

Implement API-level guards (`Build.VERSION.SDK_INT`) throughout.

---

## Localization

- Default language: English
- All user-facing strings in `res/values/strings.xml`
- No hardcoded strings in Composables or ViewModels
- Support RTL layouts via Compose defaults
- Date/time formatting must use device locale
