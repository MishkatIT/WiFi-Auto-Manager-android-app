package com.example.wifiautomanager.domain.usecase

import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.repository.WifiRepository
import kotlinx.coroutines.flow.Flow

class ManageSavedNetworksUseCase(
    private val wifiRepository: WifiRepository
) {
    fun getNetworks(): Flow<List<WifiNetwork>> = wifiRepository.getAllNetworks()
    suspend fun saveNetwork(network: WifiNetwork): Long = wifiRepository.insertNetwork(network)
    suspend fun updateNetwork(network: WifiNetwork) = wifiRepository.updateNetwork(network)
    suspend fun deleteNetwork(network: WifiNetwork) = wifiRepository.deleteNetwork(network)
}
