package com.example.wifiautomanager.data.local.datastore

import com.example.wifiautomanager.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

// App preferences backed by DataStore - implemented in Phase 2
interface AppPreferencesDataStore {
    val settingsFlow: Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun setAutoManagerEnabled(enabled: Boolean)
}
