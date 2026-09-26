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
}

