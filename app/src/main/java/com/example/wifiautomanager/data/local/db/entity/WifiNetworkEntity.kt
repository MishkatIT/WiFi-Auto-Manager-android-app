package com.example.wifiautomanager.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.WifiNetwork

@Entity(tableName = "wifi_networks")
data class WifiNetworkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ssid: String,
    val securityType: String,
    val enabled: Boolean = true,
    val priority: Int = 1,
    val minimumSignalDbm: Int = -70,
    val requiresInternet: Boolean = false,
    val minimumCandidateSignalDbm: Int = -65,
    val minimumImprovementDbm: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun WifiNetworkEntity.toDomain(): WifiNetwork = WifiNetwork(
    id = id,
    ssid = ssid,
    securityType = try {
        SecurityType.valueOf(securityType)
    } catch (e: Exception) {
        SecurityType.UNKNOWN
    },
    enabled = enabled,
    priority = priority,
    minimumSignalDbm = minimumSignalDbm,
    requiresInternet = requiresInternet,
    minimumCandidateSignalDbm = minimumCandidateSignalDbm,
    minimumImprovementDbm = minimumImprovementDbm,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WifiNetwork.toEntity(): WifiNetworkEntity = WifiNetworkEntity(
    id = id,
    ssid = ssid,
    securityType = securityType.name,
    enabled = enabled,
    priority = priority,
    minimumSignalDbm = minimumSignalDbm,
    requiresInternet = requiresInternet,
    minimumCandidateSignalDbm = minimumCandidateSignalDbm,
    minimumImprovementDbm = minimumImprovementDbm,
    createdAt = createdAt,
    updatedAt = updatedAt
)
