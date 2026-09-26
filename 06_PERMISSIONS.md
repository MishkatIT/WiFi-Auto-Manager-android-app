# 06 — Permissions

## Required Permissions by Feature

| Permission | Min API | Feature | Notes |
|-----------|---------|---------|-------|
| `ACCESS_WIFI_STATE` | All | Read Wi-Fi state, current SSID | Normal permission |
| `CHANGE_WIFI_STATE` | All | Register suggestions, start scan | Normal permission |
| `ACCESS_NETWORK_STATE` | All | ConnectivityManager | Normal permission |
| `INTERNET` | All | Internet check | Normal permission |
| `ACCESS_FINE_LOCATION` | All–32 | Wi-Fi scanning (SSID visibility) | Dangerous permission |
| `ACCESS_COARSE_LOCATION` | All–32 | Fallback scan permission | Dangerous permission |
| `NEARBY_WIFI_DEVICES` | 33+ | Replaces location for Wi-Fi scan | Dangerous permission, API 33+ |
| `ACCESS_BACKGROUND_LOCATION` | 29+ | Background scanning | Dangerous; request separately; NOT required if scan is foreground-only |
| `CHANGE_NETWORK_STATE` | All | NetworkSuggestion API | Normal permission |
| `POST_NOTIFICATIONS` | 33+ | Decision notifications | Dangerous permission, API 33+ |
| `FOREGROUND_SERVICE` | All | Foreground monitoring service | Normal permission |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | 34+ | Foreground service type for Wi-Fi | Normal permission |
| `RECEIVE_BOOT_COMPLETED` | All | Start monitoring after reboot | Normal permission |

---

## AndroidManifest.xml (Permissions Block)

```xml
<!-- Normal permissions — granted at install -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"
    android:minSdkVersion="34" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Dangerous permissions — must be requested at runtime -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"
    android:maxSdkVersion="32" />
<uses-permission
    android:name="android.permission.NEARBY_WIFI_DEVICES"
    android:usesPermissionFlags="neverForLocation"
    tools:targetApi="33" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"
    tools:targetApi="33" />
```

> `ACCESS_BACKGROUND_LOCATION` is intentionally **not** requested unless background scanning  
> is explicitly needed. If required, it must be requested separately after `ACCESS_FINE_LOCATION`  
> is already granted, and explained clearly to the user.

---

## Permission Request Flow

```
Feature requires permission
        ↓
Check if already granted
        ↓
    ┌───┴───┐
  Yes       No
   │         │
Proceed   Should show rationale?
               ┌──────┴──────┐
              Yes             No
               │               │
          Show rationale     Request directly
          screen/dialog
               │
          User reads
               │
          [Grant] or [Not Now]
               │         │
          Request      Record denied
          permission   Show reduced UI
               │
      ┌────────┴────────┐
   Granted           Denied
      │                 │
   Continue       Permanent denial?
                    ┌───┴───┐
                  Yes       No
                   │         │
               Show         Mark as
               "Open        temporarily
               Settings"    unavailable
               button
```

---

## Permission Helper

```kotlin
class PermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getRequiredScanPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            listOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        else
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )

    fun hasScanPermissions(): Boolean =
        getRequiredScanPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }

    fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else
            true    // Granted implicitly on older APIs
}
```

---

## UI Behavior When Permission Is Missing

Never crash. Always show a clear, actionable message.

```
┌─────────────────────────────────────────────────┐
│  ⚠ Wi-Fi Scanning Unavailable                  │
│                                                 │
│  To see nearby networks, this app needs         │
│  permission to access nearby Wi-Fi devices.     │
│                                                 │
│  [Grant Permission]      [Learn More]           │
└─────────────────────────────────────────────────┘
```

For permanent denial:

```
┌─────────────────────────────────────────────────┐
│  ⚠ Permission Required                         │
│                                                 │
│  Nearby Wi-Fi permission was denied.            │
│  Please enable it in system settings.           │
│                                                 │
│  [Open Settings]                                │
└─────────────────────────────────────────────────┘
```

---

## What Happens Without Each Permission

| Permission Missing | App Behavior |
|-------------------|--------------|
| Scan permissions | Nearby tab shows rationale card; decision engine cannot evaluate by signal |
| POST_NOTIFICATIONS | Decision notifications silently disabled; shown in settings as "not available" |
| FOREGROUND_SERVICE | Monitoring in background disabled; explain in settings |
| RECEIVE_BOOT_COMPLETED | Auto-start on reboot disabled; shown in settings |
