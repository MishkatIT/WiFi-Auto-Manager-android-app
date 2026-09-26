package com.example.wifiautomanager.domain.repository

import com.example.wifiautomanager.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun setAutoManagerEnabled(enabled: Boolean)
}
