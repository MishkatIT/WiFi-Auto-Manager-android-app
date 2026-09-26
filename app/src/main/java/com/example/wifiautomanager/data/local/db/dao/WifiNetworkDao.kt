package com.example.wifiautomanager.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.wifiautomanager.data.local.db.entity.WifiNetworkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiNetworkDao {
    @Query("SELECT * FROM wifi_networks ORDER BY priority DESC, ssid ASC")
    fun getAllNetworks(): Flow<List<WifiNetworkEntity>>

    @Query("SELECT * FROM wifi_networks WHERE enabled = 1 ORDER BY priority DESC, ssid ASC")
    fun getEnabledNetworks(): Flow<List<WifiNetworkEntity>>

    @Query("SELECT * FROM wifi_networks WHERE id = :id LIMIT 1")
    suspend fun getNetworkById(id: Long): WifiNetworkEntity?

    @Query("SELECT * FROM wifi_networks WHERE ssid = :ssid LIMIT 1")
    suspend fun getNetworkBySsid(ssid: String): WifiNetworkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetwork(network: WifiNetworkEntity): Long

    @Update
    suspend fun updateNetwork(network: WifiNetworkEntity)

    @Delete
    suspend fun deleteNetwork(network: WifiNetworkEntity)

    @Query("DELETE FROM wifi_networks WHERE id = :id")
    suspend fun deleteNetworkById(id: Long)

    @Query("SELECT COUNT(*) FROM wifi_networks")
    suspend fun getNetworkCount(): Int
}
