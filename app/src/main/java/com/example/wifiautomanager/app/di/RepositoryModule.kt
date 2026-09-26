package com.example.wifiautomanager.app.di

import com.example.wifiautomanager.data.repository.RuleRepositoryImpl
import com.example.wifiautomanager.data.repository.SettingsRepositoryImpl
import com.example.wifiautomanager.data.repository.WifiRepositoryImpl
import com.example.wifiautomanager.domain.repository.RuleRepository
import com.example.wifiautomanager.domain.repository.SettingsRepository
import com.example.wifiautomanager.domain.repository.WifiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWifiRepository(impl: WifiRepositoryImpl): WifiRepository

    @Binds
    @Singleton
    abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
