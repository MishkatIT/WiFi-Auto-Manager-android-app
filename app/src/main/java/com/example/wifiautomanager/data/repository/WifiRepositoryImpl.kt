package com.example.wifiautomanager.data.repository

import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// WifiRepository implementation - wired to Room in Phase 2
class WifiRepositoryImpl : WifiRepository {
    override fun getAllNetworks(): Flow<List<WifiNetwork>> = flowOf(emptyList())
    override fun getEnabledNetworks(): Flow<List<WifiNetwork>> = flowOf(emptyList())
    override suspend fun getNetworkById(id: Long): WifiNetwork? = null
    override suspend fun getNetworkBySsid(ssid: String): WifiNetwork? = null
    override suspend fun insertNetwork(network: WifiNetwork): Long = 0L
    override suspend fun updateNetwork(network: WifiNetwork) {}
    override suspend fun deleteNetwork(network: WifiNetwork) {}
}
