package com.example.wifiautomanager.data.repository

import com.example.wifiautomanager.domain.model.AppSettings
import com.example.wifiautomanager.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// SettingsRepository implementation - wired to DataStore in Phase 2
class SettingsRepositoryImpl : SettingsRepository {
    override fun getSettings(): Flow<AppSettings> = flowOf(AppSettings())
    override suspend fun updateSettings(settings: AppSettings) {}
    override suspend fun setAutoManagerEnabled(enabled: Boolean) {}
}
