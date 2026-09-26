# 05 — Wi-Fi Integration

## Component Overview

```
WifiScanner              → scans for nearby networks
WifiConnectionMonitor    → observes current connection state
InternetChecker          → validates actual internet access
WifiSuggestionManager    → registers / removes WifiNetworkSuggestion
ConnectivityObserver     → wraps ConnectivityManager callbacks as Flow
```

All of these expose `Flow` or `StateFlow` interfaces.  
They are injected via Hilt and depend only on `Context` + Android framework classes.

---

## WifiScanner

### Responsibilities
- Trigger scans via `WifiManager.startScan()`
- Observe `SCAN_RESULTS_AVAILABLE_ACTION` broadcast
- Parse `WifiManager.scanResults` into `List<ScannedNetwork>`
- Handle: throttle, failure, permission missing, Wi-Fi disabled

### Throttle Awareness
Android limits foreground scans to **4 per 2 minutes** (since API 28).  
The scanner must not exceed this. Use minimum 30-second intervals by default.

```kotlin
class WifiScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiManager: WifiManager,
) {
    private val _scanResults = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanResults: StateFlow<ScanState> = _scanResults.asStateFlow()

    fun requestScan() {
        if (!wifiManager.isWifiEnabled) {
            _scanResults.value = ScanState.WifiDisabled
            return
        }
        val started = wifiManager.startScan()
        if (!started) {
            _scanResults.value = ScanState.Failed("startScan() returned false — possibly throttled")
        }
    }

    // Register BroadcastReceiver for SCAN_RESULTS_AVAILABLE_ACTION
    // Parse results and emit ScanState.Results(list)
    // Never assume empty results == no networks
}

sealed class ScanState {
    data object Idle : ScanState()
    data object Scanning : ScanState()
    data object WifiDisabled : ScanState()
    data class  PermissionMissing(val permissions: List<String>) : ScanState()
    data class  Failed(val reason: String) : ScanState()
    data class  Throttled(val nextAllowedMs: Long) : ScanState()
    data class  Results(
        val networks: List<ScannedNetwork>,
        val timestampMs: Long,
        val isFresh: Boolean,           // false if results are from a previous scan
    ) : ScanState()
}
```

---

## WifiConnectionMonitor

### Responsibilities
- Observe `ConnectivityManager.NetworkCallback`
- Track: SSID, BSSID, RSSI, frequency, connection time
- Handle API differences (WifiInfo from callback vs direct on API <29)

```kotlin
class WifiConnectionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectivityManager: ConnectivityManager,
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Unknown)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback(
        FLAG_INCLUDE_LOCATION_INFO     // API 31+ — needed for SSID
    ) {
        override fun onAvailable(network: Network) { /* update state */ }
        override fun onLost(network: Network) { _connectionState.value = ConnectionState.Disconnected }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val wifiInfo = caps.transportInfo as? WifiInfo ?: return
            // Build ConnectionState.Connected from wifiInfo
        }
    }

    fun start() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun stop() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}

sealed class ConnectionState {
    data object Unknown : ConnectionState()
    data object Disconnected : ConnectionState()
    data class  Connected(
        val ssid: String,
        val bssid: String,
        val rssi: Int,
        val frequencyMhz: Int,
        val connectedSinceMs: Long,
    ) : ConnectionState()
}
```

### API Differences

| API Level | How to get WifiInfo |
|-----------|---------------------|
| < 29 | `WifiManager.connectionInfo` — direct but deprecated |
| 29–30 | `NetworkCapabilities.transportInfo` as `WifiInfo` |
| 31+ | `NetworkCapabilities.transportInfo` with `FLAG_INCLUDE_LOCATION_INFO` |

Use `Build.VERSION.SDK_INT` guards.

---

## InternetChecker

**Important:** Wi-Fi association ≠ internet connectivity.  
Always check separately.

### Strategy
Use `ConnectivityManager.NetworkCapabilities.NET_CAPABILITY_VALIDATED` as primary signal.  
This is provided by Android itself via `NetworkCallback.onCapabilitiesChanged`.

```kotlin
class InternetChecker @Inject constructor(
    private val connectivityManager: ConnectivityManager,
) {
    fun hasInternet(network: Network): Boolean {
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

### Internet State Tracking
Track how long internet has been unavailable:

```kotlin
data class InternetState(
    val status: InternetStatus,
    val unavailableSinceMs: Long?,  // null if available or unknown
) 
```

Do **not** make independent HTTP pings unless `NET_CAPABILITY_VALIDATED` is insufficient.  
Unnecessary HTTP calls waste battery.

---

## WifiSuggestionManager

### What WifiNetworkSuggestion Can and Cannot Do

| Can | Cannot |
|-----|--------|
| Suggest preferred networks to Android | Force immediate connection |
| Set a priority score | Override Android's selection algorithm |
| Mark a network as metered/unmetered | Guarantee Android uses the suggestion |
| Remove suggestions when a network is deleted | Read back whether Android connected because of suggestion |

Always display the actual suggestion state — not an assumed success.

```kotlin
class WifiSuggestionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiManager: WifiManager,
) {
    private val _suggestionStates = MutableStateFlow<Map<Long, SuggestionState>>(emptyMap())
    val suggestionStates: StateFlow<Map<Long, SuggestionState>> = _suggestionStates.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.Q)
    fun registerSuggestions(networks: List<WifiNetwork>, getPassword: (Long) -> String?) {
        val suggestions = networks.mapNotNull { network ->
            buildSuggestion(network, getPassword(network.id))
        }

        val status = wifiManager.addNetworkSuggestions(suggestions)
        updateStates(networks, status)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun buildSuggestion(network: WifiNetwork, password: String?): WifiNetworkSuggestion? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        return WifiNetworkSuggestion.Builder()
            .setSsid(network.ssid)
            .apply {
                when (network.securityType) {
                    SecurityType.WPA2_PSK, SecurityType.WPA3_SAE -> {
                        if (!password.isNullOrEmpty()) setWpa2Passphrase(password)
                    }
                    SecurityType.OPEN -> { /* no passphrase */ }
                    else -> return null   // unsupported type — skip
                }
            }
            .setPriority(network.priority)
            .setIsUserInteractionRequired(false)
            .build()
    }

    private fun updateStates(networks: List<WifiNetwork>, status: Int) {
        val newStates = networks.associate { n ->
            n.id to when (status) {
                WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS -> SuggestionState.Registered
                WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE -> SuggestionState.Duplicate
                WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED -> SuggestionState.NotAllowed
                else -> SuggestionState.Failed("status=$status")
            }
        }
        _suggestionStates.value = newStates
    }
}

sealed class SuggestionState {
    data object NotRegistered : SuggestionState()
    data object Registered : SuggestionState()
    data object Duplicate : SuggestionState()
    data object NotAllowed : SuggestionState()
    data class  Failed(val reason: String) : SuggestionState()
    data object ApiNotSupported : SuggestionState()
}
```

### API Level Guards

```kotlin
if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
    // WifiNetworkSuggestion not available
    // Show: "Network suggestions require Android 10 or later"
    _suggestionStates.value = mapOf(networkId to SuggestionState.ApiNotSupported)
    return
}
```

---

## ConnectivityObserver

A unified `Flow` of all connectivity changes:

```kotlin
interface ConnectivityObserver {
    val networkState: Flow<NetworkState>
}

sealed class NetworkState {
    data object Available : NetworkState()
    data object Unavailable : NetworkState()
    data object Losing : NetworkState()
    data object Lost : NetworkState()
}
```
