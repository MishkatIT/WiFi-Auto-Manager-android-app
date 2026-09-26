package com.example.wifiautomanager.domain.usecase

import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.repository.CredentialRepository
import com.example.wifiautomanager.domain.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageSavedNetworksUseCase @Inject constructor(
    private val wifiRepository: WifiRepository,
    private val credentialRepository: CredentialRepository
) {
    fun getNetworks(): Flow<List<WifiNetwork>> = wifiRepository.getAllNetworks()

    fun getEnabledNetworks(): Flow<List<WifiNetwork>> = wifiRepository.getEnabledNetworks()

    suspend fun getNetwork(id: Long): WifiNetwork? = wifiRepository.getNetworkById(id)

    suspend fun saveNetwork(network: WifiNetwork, password: String? = null): Long {
        val id = if (network.id == 0L) {
            wifiRepository.insertNetwork(network)
        } else {
            wifiRepository.updateNetwork(network)
            network.id
        }

        if (!password.isNullOrEmpty()) {
            credentialRepository.savePassword(id, password)
        }
        return id
    }

    suspend fun toggleNetwork(id: Long, enabled: Boolean) {
        val existing = wifiRepository.getNetworkById(id) ?: return
        wifiRepository.updateNetwork(existing.copy(enabled = enabled, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNetwork(id: Long) {
        val existing = wifiRepository.getNetworkById(id) ?: return
        wifiRepository.deleteNetwork(existing)
        credentialRepository.deletePassword(id)
    }

    fun hasPassword(networkId: Long): Boolean = credentialRepository.hasPassword(networkId)

    fun getPassword(networkId: Long): String? = credentialRepository.getPassword(networkId)
}

