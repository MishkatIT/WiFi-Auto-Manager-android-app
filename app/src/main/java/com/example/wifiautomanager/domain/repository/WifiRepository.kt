package com.example.wifiautomanager.domain.repository

import com.example.wifiautomanager.domain.model.WifiNetwork
import kotlinx.coroutines.flow.Flow

interface WifiRepository {
    fun getAllNetworks(): Flow<List<WifiNetwork>>
    fun getEnabledNetworks(): Flow<List<WifiNetwork>>
    suspend fun getNetworkById(id: Long): WifiNetwork?
    suspend fun getNetworkBySsid(ssid: String): WifiNetwork?
    suspend fun insertNetwork(network: WifiNetwork): Long
    suspend fun updateNetwork(network: WifiNetwork)
    suspend fun deleteNetwork(network: WifiNetwork)
}
