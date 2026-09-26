package com.example.wifiautomanager.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.wifiautomanager.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val AUTO_MANAGER_ENABLED = booleanPreferencesKey("auto_manager_enabled")
        val SCAN_INTERVAL_SECONDS = intPreferencesKey("scan_interval_seconds")
        val INTERNET_CHECK_INTERVAL_SECONDS = intPreferencesKey("internet_check_interval_seconds")
        val INTERNET_UNAVAILABLE_TIMEOUT_SECONDS = intPreferencesKey("internet_unavailable_timeout_seconds")
        val SWITCH_COOLDOWN_SECONDS = intPreferencesKey("switch_cooldown_seconds")
        val STABILITY_WINDOW_SECONDS = intPreferencesKey("stability_window_seconds")
        val SHOW_DECISION_NOTIFICATIONS = booleanPreferencesKey("show_decision_notifications")
        val LOG_RETENTION_DAYS = intPreferencesKey("log_retention_days")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                autoManagerEnabled = preferences[PreferencesKeys.AUTO_MANAGER_ENABLED] ?: false,
                scanIntervalSeconds = preferences[PreferencesKeys.SCAN_INTERVAL_SECONDS] ?: 30,
                internetCheckIntervalSeconds = preferences[PreferencesKeys.INTERNET_CHECK_INTERVAL_SECONDS] ?: 10,
                internetUnavailableTimeoutSeconds = preferences[PreferencesKeys.INTERNET_UNAVAILABLE_TIMEOUT_SECONDS] ?: 15,
                switchCooldownSeconds = preferences[PreferencesKeys.SWITCH_COOLDOWN_SECONDS] ?: 60,
                stabilityWindowSeconds = preferences[PreferencesKeys.STABILITY_WINDOW_SECONDS] ?: 10,
                showDecisionNotifications = preferences[PreferencesKeys.SHOW_DECISION_NOTIFICATIONS] ?: true,
                logRetentionDays = preferences[PreferencesKeys.LOG_RETENTION_DAYS] ?: 7
            )
        }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_MANAGER_ENABLED] = settings.autoManagerEnabled
            preferences[PreferencesKeys.SCAN_INTERVAL_SECONDS] = settings.scanIntervalSeconds
            preferences[PreferencesKeys.INTERNET_CHECK_INTERVAL_SECONDS] = settings.internetCheckIntervalSeconds
            preferences[PreferencesKeys.INTERNET_UNAVAILABLE_TIMEOUT_SECONDS] = settings.internetUnavailableTimeoutSeconds
            preferences[PreferencesKeys.SWITCH_COOLDOWN_SECONDS] = settings.switchCooldownSeconds
            preferences[PreferencesKeys.STABILITY_WINDOW_SECONDS] = settings.stabilityWindowSeconds
            preferences[PreferencesKeys.SHOW_DECISION_NOTIFICATIONS] = settings.showDecisionNotifications
            preferences[PreferencesKeys.LOG_RETENTION_DAYS] = settings.logRetentionDays
        }
    }

    suspend fun setAutoManagerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_MANAGER_ENABLED] = enabled
        }
    }
}
