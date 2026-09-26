# 03 — Data Model

## Domain Models (Pure Kotlin — no Android imports)

### WifiNetwork

```kotlin
data class WifiNetwork(
    val id: Long = 0,
    val ssid: String,
    val securityType: SecurityType,
    val enabled: Boolean = true,
    val priority: Int,                      // 1 (lowest) to 100 (highest)
    val minimumSignalDbm: Int = -70,        // e.g. -70 means reject if weaker than -70
    val requiresInternet: Boolean = false,
    val minimumCandidateSignalDbm: Int = -65,
    val minimumImprovementDbm: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class SecurityType {
    OPEN,
    WEP,
    WPA2_PSK,
    WPA3_SAE,
    WPA2_EAP,
    UNKNOWN
}
```

> **Note:** Passwords are NOT stored in this domain model.  
> They are stored separately in `CredentialStore` (Android Keystore) keyed by `WifiNetwork.id`.  
> The domain layer never sees a plaintext password.

---

### Rule

```kotlin
data class Rule(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val priority: Int,
    val rootGroup: ConditionGroup,
    val action: RuleAction,
    val createdAt: Long = System.currentTimeMillis(),
)

data class ConditionGroup(
    val operator: LogicalOperator,          // AND / OR
    val conditions: List<ConditionNode>,
)

sealed class ConditionNode {
    data class Leaf(val condition: Condition) : ConditionNode()
    data class Group(val group: ConditionGroup) : ConditionNode()
    data class Not(val inner: ConditionNode) : ConditionNode()
}

enum class LogicalOperator { AND, OR }

sealed class RuleAction {
    data object PreferBestCandidate : RuleAction()
    data class  PreferNetwork(val networkId: Long) : RuleAction()
    data object StayOnCurrent : RuleAction()
    data object DoNothing : RuleAction()
}
```

---

### Condition (Sealed Hierarchy)

```kotlin
sealed class Condition {

    // ── Signal ──────────────────────────────────────────
    data class CurrentSignalBelow(val thresholdDbm: Int) : Condition()
    data class CurrentSignalAbove(val thresholdDbm: Int) : Condition()
    data class CandidateSignalAbove(val thresholdDbm: Int) : Condition()
    data class SignalImprovementAtLeast(val improvementDbm: Int) : Condition()

    // ── Internet ─────────────────────────────────────────
    data object InternetAvailable : Condition()
    data object InternetUnavailable : Condition()
    data class  InternetUnavailableForAtLeast(val seconds: Int) : Condition()

    // ── Network identity ─────────────────────────────────
    data class  CurrentSsidIs(val ssid: String) : Condition()
    data class  CurrentSsidIsNot(val ssid: String) : Condition()
    data class  CandidateNetworkAvailable(val networkId: Long) : Condition()

    // ── Time ─────────────────────────────────────────────
    data class  TimeBetween(val startHour: Int, val startMin: Int,
                            val endHour: Int,   val endMin: Int) : Condition()
    data class  DayOfWeek(val days: Set<java.time.DayOfWeek>) : Condition()

    // ── Stability ─────────────────────────────────────────
    data class  ConnectedForAtLeast(val seconds: Int) : Condition()
    data class  SwitchCooldownExpired(val seconds: Int) : Condition()

    // ── Priority ─────────────────────────────────────────
    data class  CandidatePriorityHigherThan(val networkId: Long) : Condition()
}
```

---

### Wi-Fi State (Sealed)

```kotlin
sealed class WifiState {
    data object Disabled : WifiState()
    data object Disconnected : WifiState()
    data object Scanning : WifiState()

    data class Connected(
        val ssid: String,
        val bssid: String,
        val rssi: Int,
        val frequencyMhz: Int,
        val internetStatus: InternetStatus,
        val connectedSinceMs: Long,
    ) : WifiState()

    data class PermissionRequired(val missing: List<String>) : WifiState()
    data class Error(val message: String, val cause: Throwable? = null) : WifiState()
}

enum class InternetStatus {
    AVAILABLE,
    UNAVAILABLE,
    CHECKING,
    UNKNOWN
}
```

---

### ScannedNetwork

```kotlin
data class ScannedNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val capabilities: String,
    val securityType: SecurityType,
    val isSaved: Boolean,
    val savedNetworkId: Long?,
    val timestampMs: Long,
)
```

---

### Decision

```kotlin
data class Decision(
    val id: Long = 0,
    val timestampMs: Long,
    val action: DecisionAction,
    val selectedNetwork: WifiNetwork?,
    val reason: String,
    val evaluatedCandidates: List<CandidateResult>,
    val currentState: WifiState,
)

sealed class DecisionAction {
    data object StayOnCurrent : DecisionAction()
    data class  SuggestNetwork(val network: WifiNetwork) : DecisionAction()
    data object NoCandidates : DecisionAction()
    data object CooldownActive : DecisionAction()
    data object RulesDisabled : DecisionAction()
}

data class CandidateResult(
    val network: WifiNetwork,
    val scannedNetwork: ScannedNetwork?,
    val qualified: Boolean,
    val rejectionReason: String?,
    val signalImprovement: Int?,
)
```

---

## Room Entities

### WifiNetworkEntity

```kotlin
@Entity(tableName = "wifi_networks")
data class WifiNetworkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ssid: String,
    val securityType: String,                   // enum name
    val enabled: Boolean,
    val priority: Int,
    val minimumSignalDbm: Int,
    val requiresInternet: Boolean,
    val minimumCandidateSignalDbm: Int,
    val minimumImprovementDbm: Int,
    val createdAt: Long,
    val updatedAt: Long,
    // NO password field here — stored in Keystore
)
```

### RuleEntity

```kotlin
@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean,
    val priority: Int,
    val conditionGroupJson: String,             // serialized ConditionGroup (Gson/Moshi)
    val actionJson: String,                     // serialized RuleAction
    val createdAt: Long,
)
```

### DecisionLogEntity

```kotlin
@Entity(tableName = "decision_log")
data class DecisionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val actionType: String,
    val selectedNetworkId: Long?,
    val currentSsid: String?,
    val currentRssi: Int?,
    val reason: String,
    val detailJson: String,                     // full serialized Decision
)
```

---

## Room Database

```kotlin
@Database(
    entities = [
        WifiNetworkEntity::class,
        RuleEntity::class,
        DecisionLogEntity::class,
    ],
    version = 1,
    exportSchema = true                         // Required for migration tracking
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wifiNetworkDao(): WifiNetworkDao
    abstract fun ruleDao(): RuleDao
    abstract fun decisionLogDao(): DecisionLogDao
}
```

---

## Migration Strategy

- Schema export directory: `room.schemaLocation` in `build.gradle.kts`
- Every schema change requires an explicit migration object
- Never use `fallbackToDestructiveMigration()` in production
- Use `fallbackToDestructiveMigrationOnDowngrade()` only as a safety net

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE wifi_networks ADD COLUMN minimumImprovementDbm INTEGER NOT NULL DEFAULT 10")
    }
}
```

---

## Credential Storage

Passwords are stored separately using Android Keystore-backed `EncryptedSharedPreferences`:

```kotlin
class CredentialStore @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "wifi_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePassword(networkId: Long, password: String) {
        prefs.edit().putString("pwd_$networkId", password).apply()
    }

    fun getPassword(networkId: Long): String? =
        prefs.getString("pwd_$networkId", null)

    fun deletePassword(networkId: Long) {
        prefs.edit().remove("pwd_$networkId").apply()
    }
}
```

**Rules:**
- Passwords are never placed in Room entities
- Passwords are never returned from repository to ViewModel
- Passwords are accessed only at the moment a `WifiNetworkSuggestion` is built
- Passwords are never logged, printed, or included in crash data

---

## AppSettings (DataStore)

```kotlin
data class AppSettings(
    val autoManagerEnabled: Boolean = false,
    val scanIntervalSeconds: Int = 30,
    val internetCheckIntervalSeconds: Int = 10,
    val internetUnavailableTimeoutSeconds: Int = 15,
    val switchCooldownSeconds: Int = 60,
    val stabilityWindowSeconds: Int = 10,
    val showDecisionNotifications: Boolean = true,
    val logRetentionDays: Int = 7,
)
```
