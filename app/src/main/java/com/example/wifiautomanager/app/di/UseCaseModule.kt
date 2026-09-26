package com.example.wifiautomanager.app.di

import com.example.wifiautomanager.domain.repository.CredentialRepository
import com.example.wifiautomanager.domain.repository.WifiRepository
import com.example.wifiautomanager.domain.usecase.ManageSavedNetworksUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideManageSavedNetworksUseCase(
        wifiRepository: WifiRepository,
        credentialRepository: CredentialRepository
    ): ManageSavedNetworksUseCase {
        return ManageSavedNetworksUseCase(wifiRepository, credentialRepository)
    }

    @Provides
    @Singleton
    fun provideDecisionEngine(): com.example.wifiautomanager.domain.decision.DecisionEngine {
        return com.example.wifiautomanager.domain.decision.DecisionEngine()
    }

    @Provides
    @Singleton
    fun provideRunDecisionCycleUseCase(
        decisionEngine: com.example.wifiautomanager.domain.decision.DecisionEngine,
        wifiRepository: WifiRepository,
        ruleRepository: com.example.wifiautomanager.domain.repository.RuleRepository,
        settingsRepository: com.example.wifiautomanager.domain.repository.SettingsRepository,
        decisionRepository: com.example.wifiautomanager.domain.repository.DecisionRepository
    ): com.example.wifiautomanager.domain.usecase.RunDecisionCycleUseCase {
        return com.example.wifiautomanager.domain.usecase.RunDecisionCycleUseCase(
            decisionEngine,
            wifiRepository,
            ruleRepository,
            settingsRepository,
            decisionRepository
        )
    }
}

