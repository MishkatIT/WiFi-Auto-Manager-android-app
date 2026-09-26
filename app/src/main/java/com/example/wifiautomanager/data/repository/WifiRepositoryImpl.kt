package com.example.wifiautomanager.data.repository

import com.example.wifiautomanager.data.local.db.dao.WifiNetworkDao
import com.example.wifiautomanager.data.local.db.entity.toDomain
import com.example.wifiautomanager.data.local.db.entity.toEntity
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiRepositoryImpl @Inject constructor(
    private val wifiNetworkDao: WifiNetworkDao
) : WifiRepository {

    override fun getAllNetworks(): Flow<List<WifiNetwork>> =
        wifiNetworkDao.getAllNetworks().map { list -> list.map { it.toDomain() } }

    override fun getEnabledNetworks(): Flow<List<WifiNetwork>> =
        wifiNetworkDao.getEnabledNetworks().map { list -> list.map { it.toDomain() } }

    override suspend fun getNetworkById(id: Long): WifiNetwork? =
        wifiNetworkDao.getNetworkById(id)?.toDomain()

    override suspend fun getNetworkBySsid(ssid: String): WifiNetwork? =
        wifiNetworkDao.getNetworkBySsid(ssid)?.toDomain()

    override suspend fun insertNetwork(network: WifiNetwork): Long =
        wifiNetworkDao.insertNetwork(network.toEntity())

    override suspend fun updateNetwork(network: WifiNetwork) =
        wifiNetworkDao.updateNetwork(network.toEntity())

    override suspend fun deleteNetwork(network: WifiNetwork) =
        wifiNetworkDao.deleteNetwork(network.toEntity())
}
