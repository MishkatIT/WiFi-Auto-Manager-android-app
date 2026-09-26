package com.example.wifiautomanager.data.repository

import com.example.wifiautomanager.data.local.datastore.AppPreferencesDataStore
import com.example.wifiautomanager.domain.model.AppSettings
import com.example.wifiautomanager.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val appPreferencesDataStore: AppPreferencesDataStore
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> =
        appPreferencesDataStore.settingsFlow

    override suspend fun updateSettings(settings: AppSettings) {
        appPreferencesDataStore.updateSettings(settings)
    }

    override suspend fun setAutoManagerEnabled(enabled: Boolean) {
        appPreferencesDataStore.setAutoManagerEnabled(enabled)
    }
}
