# 10 — Security

## Threat Model

| Threat | Mitigation |
|--------|-----------|
| Password leaked via logs | Never log passwords; Logger has credential filter |
| Password in Room DB | Passwords stored only in Keystore-backed EncryptedSharedPreferences |
| Password in crash report | Passwords not held in ViewModel UiState |
| Password in Git | `.gitignore` covers all credential files; no hardcoded values |
| Password exposed in debug UI | Password field never shown in diagnostics; edit screen uses masked input |
| App reinstall wipes Keystore | Documented limitation; user must re-enter passwords |
| Backup includes credentials | `android:allowBackup="false"` for credential store or use `BackupAgent` to exclude |

---

## Credential Storage Implementation

```kotlin
// Using AndroidX Security library
// implementation("androidx.security:security-crypto:1.1.0-alpha06")

class CredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false)   // no biometric required for auto operation
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun savePassword(networkId: Long, password: String) {
        encryptedPrefs.edit()
            .putString(keyFor(networkId), password)
            .apply()
    }

    fun getPassword(networkId: Long): String? =
        encryptedPrefs.getString(keyFor(networkId), null)

    fun deletePassword(networkId: Long) {
        encryptedPrefs.edit()
            .remove(keyFor(networkId))
            .apply()
    }

    fun hasPassword(networkId: Long): Boolean =
        encryptedPrefs.contains(keyFor(networkId))

    private fun keyFor(networkId: Long) = "wifipwd_$networkId"

    companion object {
        private const val FILE_NAME = "wifi_credentials_secure"
    }
}
```

---

## Password in UI

```kotlin
// ✅ Correct — never in UiState
data class AddEditNetworkUiState(
    val ssid: String = "",
    // ... no password field here ...
)

// Password is a separate local state in the Composable, never persisted in ViewModel
@Composable
fun AddEditNetworkScreen(viewModel: AddEditNetworkViewModel) {
    var password by rememberSaveable(stateSaver = /* secure saver */ ) { mutableStateOf("") }

    // Password only passed to ViewModel at save time
    Button(onClick = { viewModel.onSave(password) }) {
        Text("Save Network")
    }
}
```

---

## Password Transmission Policy

Passwords:
- Are NEVER transmitted over a network
- Are NEVER included in Intent extras beyond internal app use
- Are NEVER included in `Bundle` serialization to disk
- Are NEVER included in analytics or crash payloads
- Are passed to `WifiNetworkSuggestion.Builder` only at suggestion build time and then discarded

---

## AndroidManifest Security Settings

```xml
<application
    android:allowBackup="false"
    android:fullBackupContent="@xml/backup_rules"
    android:dataExtractionRules="@xml/data_extraction_rules"
    ...>
```

```xml
<!-- res/xml/backup_rules.xml -->
<full-backup-content>
    <exclude domain="sharedpref" path="wifi_credentials_secure.xml" />
</full-backup-content>
```

```xml
<!-- res/xml/data_extraction_rules.xml (API 31+) -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="wifi_credentials_secure.xml" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="wifi_credentials_secure.xml" />
    </device-transfer>
</data-extraction-rules>
```

---

## .gitignore

```gitignore
# Android
*.iml
.gradle/
local.properties
.idea/
.DS_Store
/build/
/captures/
.externalNativeBuild/
.cxx/
*.apk
*.aab
*.ap_
*.dex

# Keys and credentials
*.jks
*.keystore
keystore.properties
/release/

# Room schema exports (include in source control — needed for migration testing)
# Do NOT ignore: app/schemas/

# No secrets in these:
google-services.json   # not needed since no Firebase
```

---

## ProGuard / R8 Rules

```proguard
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# Domain models (serialized as JSON for rule storage)
-keep class com.example.wifiautomanager.domain.** { *; }

# Ensure no accidental logging in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
```

---

## Security Checklist

- [ ] No passwords in Room entities
- [ ] No passwords in ViewModels or UiState
- [ ] No passwords in logs at any log level
- [ ] EncryptedSharedPreferences used for credentials
- [ ] `android:allowBackup="false"` or exclusion rules applied
- [ ] Credential store excluded from cloud backup
- [ ] No network calls to external services
- [ ] ProGuard/R8 strips debug logs in release
- [ ] No hardcoded credentials or test passwords in source
- [ ] `.gitignore` covers keystore files
- [ ] Password field uses `KeyboardType.Password` and `VisualTransformation`
