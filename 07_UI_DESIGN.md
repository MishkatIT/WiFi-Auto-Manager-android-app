# 07 — UI Design

## Design Language

**Framework:** Jetpack Compose + Material 3  
**Color scheme:** System dynamic color (Material You) with a custom seed  
**Typography:** Material 3 type scale — no custom fonts unless explicitly added  
**Shape:** Material 3 shape tokens (slightly rounded cards)  
**Icons:** Material Symbols (Outlined) from `androidx.compose.material:material-icons-extended`  
**Dark mode:** Fully supported — all colors via MaterialTheme tokens only  

---

## Color Tokens (Custom Seed)

```kotlin
// Seed color: Deep Blue (Wi-Fi branding feel)
private val SeedColor = Color(0xFF1565C0)

// Status colors (accessible contrast ratios)
val InternetAvailableColor  = Color(0xFF2E7D32)  // Green 800
val InternetUnavailableColor = Color(0xFFC62828) // Red 800
val WeakSignalColor          = Color(0xFFF57F17) // Amber 800
val GoodSignalColor          = Color(0xFF1B5E20) // Green 900
val NeutralSignalColor       = Color(0xFF37474F) // Blue Grey 800
```

Signal strength and internet status **must not rely on color alone** — always include an icon or label.

---

## Navigation Structure

```
Bottom Navigation Bar (3 main tabs)
├── Dashboard        (home icon)
├── Networks         (wifi icon)  
└── More             (menu icon)
         └── Drawer / nested navigation
             ├── Nearby Wi-Fi
             ├── Rules
             ├── Diagnostics
             └── Settings
```

Top-level screens use `NavigationBar` (Material 3).  
Secondary screens use `TopAppBar` with back navigation.

---

## Screen 1 — Dashboard

```
╔══════════════════════════════════════════╗
║  WiFi Auto Manager          [●] Active  ║
╠══════════════════════════════════════════╣
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  🟢 Connected                   │   ║
║  │  Home WiFi                      │   ║
║  │  ▓▓▓▓░  -68 dBm  •  5 GHz      │   ║
║  │  🌐 Internet Available          │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  Auto Manager                            ║
║  ┌──────────────────────────────────┐   ║
║  │  Status:  Monitoring             │   ║
║  │  Last scan:  14:32:05            │   ║
║  │  Last decision:  14:32:08        │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  Current Decision                        ║
║  ┌──────────────────────────────────┐   ║
║  │  ✅ Staying on Home WiFi        │   ║
║  │  Signal is acceptable (-68 dBm) │   ║
║  │  No better candidate found      │   ║
║  │           [Details →]           │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  Nearby Candidates                       ║
║  ┌──────────────────────────────────┐   ║
║  │  Office WiFi   -52 dBm   ★ Saved│   ║
║  │  (insufficient improvement)      │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
╠══════════════════════════════════════════╣
║ [Dashboard]  [Networks]  [More]          ║
╚══════════════════════════════════════════╝
```

### Dashboard States

**No Permission:**
```
╔══════════════════════════════════════════╗
║  WiFi Auto Manager                       ║
╠══════════════════════════════════════════╣
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  ⚠️  Permissions Required        │   ║
║  │                                  │   ║
║  │  Nearby Wi-Fi permission is      │   ║
║  │  needed to scan networks.        │   ║
║  │                                  │   ║
║  │        [Grant Permission]        │   ║
║  └──────────────────────────────────┘   ║
╚══════════════════════════════════════════╝
```

**Wi-Fi Disabled:**
```
  ┌──────────────────────────────────┐
  │  📶 Wi-Fi is turned off          │
  │  Enable Wi-Fi to use this app    │
  │        [Enable Wi-Fi]            │
  └──────────────────────────────────┘
```

---

## Screen 2 — Network List

```
╔══════════════════════════════════════════╗
║  ← Saved Networks              [+ Add]  ║
╠══════════════════════════════════════════╣
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  🟢 Home WiFi          [●] ON   │   ║
║  │  Priority: 80  •  WPA2          │   ║
║  │  Min signal: -70 dBm            │   ║
║  │  🌐 Internet required           │   ║
║  │  Suggestion: Registered         │   ║
║  │                      [Edit] [⋮] │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  🟡 Office WiFi        [●] ON   │   ║
║  │  Priority: 90  •  WPA2          │   ║
║  │  Min signal: -65 dBm            │   ║
║  │  🌐 Internet required           │   ║
║  │  Suggestion: Registered         │   ║
║  │                      [Edit] [⋮] │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  ⚫ Café Backup         [○] OFF  │   ║
║  │  Priority: 20  •  Open          │   ║
║  │  (Disabled)                     │   ║
║  │                      [Edit] [⋮] │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
╠══════════════════════════════════════════╣
║ [Dashboard]  [Networks]  [More]          ║
╚══════════════════════════════════════════╝
```

Overflow menu `[⋮]` per card:
```
• Enable / Disable
• Edit
• Remove suggestion
• Delete
```

---

## Screen 3 — Add / Edit Network

```
╔══════════════════════════════════════════╗
║  ← Add Network                          ║
╠══════════════════════════════════════════╣
║                                          ║
║  Network Name (SSID)                     ║
║  ┌──────────────────────────────────┐   ║
║  │  Home WiFi                       │   ║
║  └──────────────────────────────────┘   ║
║  [Select from nearby networks ▼]         ║
║                                          ║
║  Security Type                           ║
║  ● WPA2     ○ WPA3     ○ Open           ║
║                                          ║
║  Password                                ║
║  ┌──────────────────────────────────┐   ║
║  │  ••••••••••••••              👁  │   ║
║  └──────────────────────────────────┘   ║
║  🔒 Stored securely in device keystore  ║
║                                          ║
║  ─── Connection Preferences ───          ║
║                                          ║
║  Priority  (1 = low, 100 = high)         ║
║  ┌──────────────────────────────────┐   ║
║  │  80         ────●──────          │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  Minimum Signal Threshold                ║
║  ┌──────────────────────────────────┐   ║
║  │  -70 dBm    ─────●─────         │   ║
║  └──────────────────────────────────┘   ║
║  (Do not use if weaker than this)        ║
║                                          ║
║  Minimum Candidate Signal                ║
║  ┌──────────────────────────────────┐   ║
║  │  -65 dBm    ────●──────         │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  Minimum Improvement Required            ║
║  ┌──────────────────────────────────┐   ║
║  │  10 dBm     ──●────────         │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  [✓] Require internet to prefer          ║
║                                          ║
║  ─── Android Suggestion ───              ║
║                                          ║
║  [✓] Register as Android suggestion      ║
║  Status: Not registered                  ║
║                                          ║
║         [Cancel]     [Save Network]      ║
║                                          ║
╚══════════════════════════════════════════╝
```

---

## Screen 4 — Rule List

```
╔══════════════════════════════════════════╗
║  ← Rules                       [+ Add]  ║
╠══════════════════════════════════════════╣
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  Weak Signal Switch      [●] ON │   ║
║  │                                  │   ║
║  │  WHEN signal < -70 dBm          │   ║
║  │  AND internet unavailable ≥ 15s │   ║
║  │  AND candidate signal > -60 dBm │   ║
║  │  → Prefer best candidate        │   ║
║  │                      [Edit] [⋮] │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  Work Hours Rule         [●] ON │   ║
║  │                                  │   ║
║  │  WHEN time between 08:00–17:00  │   ║
║  │  AND Office WiFi available       │   ║
║  │  → Prefer Office WiFi            │   ║
║  │                      [Edit] [⋮] │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  No rules yet?                           ║
║  Create rules to automate switching.     ║
║                                          ║
╠══════════════════════════════════════════╣
║ [Dashboard]  [Networks]  [More]          ║
╚══════════════════════════════════════════╝
```

---

## Screen 5 — Rule Builder

```
╔══════════════════════════════════════════╗
║  ← Build Rule                           ║
╠══════════════════════════════════════════╣
║                                          ║
║  Rule Name                               ║
║  ┌──────────────────────────────────┐   ║
║  │  Weak Signal Switch              │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  ─── WHEN (Conditions) ───               ║
║                                          ║
║  Operator: [AND ▼]                       ║
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  [Signal ▼] [less than ▼] [-70 dBm]│   ║
║  │                               [✕]│   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  [Internet ▼] [unavailable for ▼]│   ║
║  │  [15 seconds ▼]              [✕]│   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  [Candidate ▼] [signal > ▼] [-60 dBm]│
║  │                               [✕]│   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  [+ Add Condition]                       ║
║                                          ║
║  ─── THEN (Action) ───                   ║
║                                          ║
║  ● Prefer best available candidate       ║
║  ○ Prefer specific network [─────────]  ║
║  ○ Stay on current network              ║
║                                          ║
║  ─── Preview ───                         ║
║  ┌──────────────────────────────────┐   ║
║  │  If: signal < -70 dBm           │   ║
║  │  AND internet unavailable ≥ 15s │   ║
║  │  AND candidate signal > -60 dBm │   ║
║  │  Then: suggest best network      │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║       [Cancel]       [Save Rule]         ║
║                                          ║
╚══════════════════════════════════════════╝
```

---

## Screen 6 — Nearby Networks

```
╔══════════════════════════════════════════╗
║  ← Nearby Wi-Fi             [↻ Refresh] ║
╠══════════════════════════════════════════╣
║                                          ║
║  Last scan: 14:33:01                     ║
║  Next scan in: 28s                       ║
║                                          ║
║  ── Saved Networks ──                    ║
║  ┌──────────────────────────────────┐   ║
║  │  ★ Home WiFi                    │   ║
║  │  ▓▓▓▓░  -68 dBm  •  WPA2       │   ║
║  │  🌐 Internet available  •  5GHz  │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  ★ Office WiFi                  │   ║
║  │  ▓▓▓░░  -74 dBm  •  WPA2       │   ║
║  │  2.4 GHz  •  Not in range       │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  ── Other Networks ──                    ║
║  ┌──────────────────────────────────┐   ║
║  │  Neighbor_5G                    │   ║
║  │  ▓▓░░░  -79 dBm  •  WPA2       │   ║
║  │                    [+ Save]      │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  AndroidAP                      │   ║
║  │  ▓░░░░  -88 dBm  •  Open       │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
╚══════════════════════════════════════════╝
```

Scan throttle warning:
```
  ┌──────────────────────────────────┐
  │  ℹ Scan throttled               │
  │  Android limits scan frequency.  │
  │  Next scan available in 12s.     │
  └──────────────────────────────────┘
```

---

## Screen 7 — Diagnostics

```
╔══════════════════════════════════════════╗
║  ← Diagnostics                          ║
╠══════════════════════════════════════════╣
║                                          ║
║  System Status                           ║
║  ┌──────────────────────────────────┐   ║
║  │  Auto Manager   🟢 Running       │   ║
║  │  Wi-Fi          🟢 Enabled       │   ║
║  │  Scan perms     🟢 Granted       │   ║
║  │  Notifications  🟡 Not granted   │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  Current State                           ║
║  ┌──────────────────────────────────┐   ║
║  │  Connected: Home WiFi            │   ║
║  │  RSSI: -68 dBm                  │   ║
║  │  Frequency: 5180 MHz            │   ║
║  │  Internet: Available            │   ║
║  │  Connected since: 1h 23m        │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  Engine                                  ║
║  ┌──────────────────────────────────┐   ║
║  │  Last scan:  14:32:05            │   ║
║  │  Last decision:  14:32:08        │   ║
║  │  Last switch:  Never             │   ║
║  │  Cooldown remaining:  —          │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  Android Suggestions                     ║
║  ┌──────────────────────────────────┐   ║
║  │  Home WiFi    🟢 Registered      │   ║
║  │  Office WiFi  🟢 Registered      │   ║
║  │  Café Backup  ⚫ Not registered  │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  Decision Log                            ║
║  ┌──────────────────────────────────┐   ║
║  │  14:32:08  Stay on Home WiFi    │   ║
║  │  14:31:38  Stay on Home WiFi    │   ║
║  │  14:01:12  Suggested Office WiFi│   ║
║  │           [View all decisions →] │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
╚══════════════════════════════════════════╝
```

---

## Screen 8 — Decision Detail

```
╔══════════════════════════════════════════╗
║  ← Decision  14:32:08                   ║
╠══════════════════════════════════════════╣
║                                          ║
║  Result: ✅ Stay on current network      ║
║                                          ║
║  ─── At time of decision ───             ║
║  Connected: Home WiFi  (-68 dBm)         ║
║  Internet: Available                     ║
║                                          ║
║  ─── Candidates Evaluated ───            ║
║                                          ║
║  ┌──────────────────────────────────┐   ║
║  │  Office WiFi          ❌ Rejected│   ║
║  │  Signal: -52 dBm                │   ║
║  │  Improvement: +16 dBm           │   ║
║  │  Required improvement: 10 dBm ✓ │   ║
║  │  Internet: Available ✓           │   ║
║  │  ❌ Reason: Cooldown active     │   ║
║  │     (32s remaining)              │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  ─── Rules Applied ───                   ║
║  Weak Signal Switch: NOT triggered       ║
║  (Current signal -68 dBm ≥ -70 dBm)     ║
║                                          ║
║  ─── Anti-Flapping ───                   ║
║  Cooldown: Active (32s remaining)        ║
║  Last switch: 14:31:36                   ║
║                                          ║
╚══════════════════════════════════════════╝
```

---

## Screen 9 — Settings

```
╔══════════════════════════════════════════╗
║  ← Settings                             ║
╠══════════════════════════════════════════╣
║                                          ║
║  ── Auto Manager ──                      ║
║  Enable Auto Manager                [●] ║
║                                          ║
║  ── Scanning ──                          ║
║  Scan interval                           ║
║  [30 seconds ▼]                          ║
║                                          ║
║  ── Internet Detection ──                ║
║  Internet check interval                 ║
║  [10 seconds ▼]                          ║
║                                          ║
║  Internet unavailable timeout            ║
║  ┌──────────────────────────────────┐   ║
║  │  15 seconds    ────●─────        │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  ── Anti-Flapping ──                     ║
║  Switch cooldown                         ║
║  ┌──────────────────────────────────┐   ║
║  │  60 seconds    ──────●────       │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  Stability window                        ║
║  ┌──────────────────────────────────┐   ║
║  │  10 seconds    ──●────────       │   ║
║  └──────────────────────────────────┘   ║
║                                          ║
║  ── Notifications ──                     ║
║  Show decision notifications        [●] ║
║                                          ║
║  ── Data ──                              ║
║  Decision log retention                  ║
║  [7 days ▼]                              ║
║                                          ║
║  Clear decision log             [Clear] ║
║                                          ║
║  ── About ──                             ║
║  Version: 1.0.0                          ║
║  Permissions                    [View →] ║
║                                          ║
╚══════════════════════════════════════════╝
```

---

## Reusable Components

### SignalStrengthIndicator
- 5-bar graphic (filled/empty bars)
- Color: Good (-50 to -65), Fair (-66 to -75), Poor (below -75)
- Never color-only: always show dBm value

### InternetStatusBadge
- `🟢 Internet Available` or `🔴 No Internet` or `🟡 Checking...`
- Text + icon (never icon-only)

### NetworkCard
- SSID, security type icon, signal bars, internet badge
- Saved indicator (★)
- Currently connected indicator (highlighted border)

### PermissionRationaleCard
- Icon + plain-language explanation
- Action button (Grant / Open Settings)
- Dismiss option where appropriate

### DecisionCard
- Action taken (icon + text)
- One-line reason
- "Details →" link to Decision Detail screen

---

## Empty States

| Screen | Empty State Message |
|--------|---------------------|
| Networks | "No saved networks. Tap + to add one." |
| Rules | "No rules yet. Create a rule to automate switching." |
| Nearby | "No networks found. Try refreshing or enable Wi-Fi." |
| Decision Log | "No decisions recorded yet." |

---

## Accessibility Requirements

- All icons have `contentDescription`
- Signal bars have text alternative (e.g. "Signal: -68 dBm, Good")
- Touch targets ≥ 48×48 dp
- Text scales with system font size
- No information conveyed by color alone
- Support TalkBack navigation order
- Interactive elements have clear focus indicators
