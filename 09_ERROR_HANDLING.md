# 09 — Error Handling & Recovery

## Error Taxonomy

```kotlin
sealed class AppError {

    // ── Permissions ──────────────────────────────────────
    data class PermissionDenied(val permissions: List<String>) : AppError()
    data class PermissionPermanentlyDenied(val permissions: List<String>) : AppError()

    // ── Wi-Fi ────────────────────────────────────────────
    data object WifiDisabled : AppError()
    data class  ScanFailed(val reason: String) : AppError()
    data object ScanThrottled : AppError()
    data object ScanResultEmpty : AppError()    // may be throttle/permission, not truly empty

    // ── Connectivity ─────────────────────────────────────
    data object InternetCheckTimeout : AppError()
    data class  NetworkCallbackFailed(val cause: Throwable) : AppError()

    // ── Suggestion API ───────────────────────────────────
    data class SuggestionRegistrationFailed(val status: Int) : AppError()
    data object SuggestionApiNotSupported : AppError()
    data class  SuggestionDuplicate(val ssid: String) : AppError()

    // ── Database ─────────────────────────────────────────
    data class DatabaseReadFailed(val cause: Throwable) : AppError()
    data class DatabaseWriteFailed(val cause: Throwable) : AppError()
    data class MigrationFailed(val cause: Throwable) : AppError()

    // ── Configuration ────────────────────────────────────
    data class InvalidNetworkConfiguration(val field: String, val reason: String) : AppError()
    data class InvalidRuleConfiguration(val reason: String) : AppError()

    // ── System ───────────────────────────────────────────
    data class UnexpectedException(val cause: Throwable) : AppError()
    data object BackgroundExecutionLimited : AppError()
}
```

---

## User-Facing vs Developer Messages

Every error has two representations:

```kotlin
fun AppError.toUserMessage(): String = when (this) {
    is AppError.WifiDisabled         -> "Wi-Fi is turned off. Enable it to continue."
    is AppError.ScanThrottled        -> "Scan temporarily unavailable. Android limits scan frequency."
    is AppError.PermissionDenied     -> "Permission required for Wi-Fi scanning."
    is AppError.SuggestionRegistrationFailed -> "Could not register network preference with Android."
    is AppError.DatabaseWriteFailed  -> "Could not save changes. Please try again."
    is AppError.InternetCheckTimeout -> "Internet check timed out."
    is AppError.BackgroundExecutionLimited -> "Background monitoring may be limited by the system."
    else                             -> "An unexpected error occurred."
}

fun AppError.toDeveloperLog(): String = when (this) {
    is AppError.ScanFailed  -> "WifiManager.startScan() returned false: $reason"
    is AppError.SuggestionRegistrationFailed -> "addNetworkSuggestions() status=$status"
    is AppError.DatabaseWriteFailed -> "Room write failed: ${cause.message}"
    is AppError.UnexpectedException -> "Unexpected: ${cause.stackTraceToString()}"
    else -> this.toString()
}
```

---

## Recovery Strategies

| Error | Recovery |
|-------|----------|
| `PermissionDenied` | Show rationale card + Grant button |
| `PermissionPermanentlyDenied` | Show "Open Settings" button |
| `WifiDisabled` | Show "Enable Wi-Fi" button (opens system Wi-Fi settings) |
| `ScanThrottled` | Show countdown timer; retry automatically |
| `ScanFailed` | Log + show retry button; do not mark empty = no networks |
| `SuggestionRegistrationFailed` | Preserve local config; show suggestion status; allow retry |
| `SuggestionDuplicate` | Remove existing + re-add; log; show "updated" to user |
| `DatabaseWriteFailed` | Show error; do not destroy existing data; offer retry |
| `DatabaseReadFailed` | Show empty+error state; preserve existing data |
| `MigrationFailed` | Log; offer "Reset database" as last resort (with confirmation) |
| `InternetCheckTimeout` | Treat as UNKNOWN, not UNAVAILABLE; retry |
| `BackgroundExecutionLimited` | Show notification in diagnostics; reduce check frequency |
| `UnexpectedException` | Log full stacktrace; show generic user message |

---

## Repository Error Pattern

Repositories return `Result<T>`:

```kotlin
suspend fun saveNetwork(network: WifiNetwork): Result<Unit> =
    runCatching {
        dao.upsert(network.toEntity())
    }.mapError { cause ->
        AppError.DatabaseWriteFailed(cause)
    }
```

Use cases unwrap and map errors:

```kotlin
class ManageSavedNetworksUseCase @Inject constructor(
    private val repository: WifiRepository,
    private val credentialStore: CredentialStore,
) {
    suspend fun addNetwork(network: WifiNetwork, password: String): Result<Unit> {
        if (network.ssid.isBlank()) {
            return Result.failure(AppError.InvalidNetworkConfiguration("ssid", "SSID cannot be empty"))
        }
        return repository.saveNetwork(network).onSuccess { savedId ->
            if (password.isNotEmpty()) {
                credentialStore.savePassword(savedId, password)
            }
        }
    }
}
```

ViewModels map errors to UiState:

```kotlin
viewModelScope.launch {
    useCase.addNetwork(network, password)
        .onSuccess { _uiState.update { it.copy(savedSuccessfully = true) } }
        .onFailure { error ->
            val msg = (error as? AppError)?.toUserMessage() ?: "Unexpected error"
            _uiState.update { it.copy(generalError = msg) }
            Logger.e("AddNetworkVM", (error as? AppError)?.toDeveloperLog() ?: error.toString())
        }
}
```

---

## Never Do

```kotlin
// ❌ Silent swallow
try { dao.insert(entity) } catch (e: Exception) { }

// ❌ Raw stacktrace in UI
Text(text = exception.stackTraceToString())

// ❌ Crash on permission denial
val result = wifiManager.startScan()  // no null/failure check

// ❌ Log credentials
Log.d("TAG", "Saving password: $password")

// ❌ Assume empty = no networks
if (wifiManager.scanResults.isEmpty()) showNoNetworksMessage()
```

---

## Logging Policy

```kotlin
object Logger {
    fun d(tag: String, message: String) = Log.d("WAM/$tag", message)
    fun i(tag: String, message: String) = Log.i("WAM/$tag", message)
    fun w(tag: String, message: String) = Log.w("WAM/$tag", message)
    fun e(tag: String, message: String, throwable: Throwable? = null) =
        Log.e("WAM/$tag", message, throwable)
}
```

### Never log:
- Passwords or credentials
- Full SSID lists in production builds (can be obfuscated in production)
- Personal device identifiers beyond what's needed for diagnosis

### Always log:
- Scan start / result count / failure reason
- Decision engine input and output (without credentials)
- Suggestion registration status
- Permission state changes
- Background execution state changes

---

## Crash Safety

- No `!!` (non-null assertion) in production code
- All Android API calls wrapped in null checks or try-catch
- Room operations always in `try-catch` or `runCatching`
- `StateFlow` always has a safe initial value
- Network callbacks always check for null network/capabilities
