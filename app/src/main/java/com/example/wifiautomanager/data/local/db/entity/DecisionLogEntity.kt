package com.example.wifiautomanager.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decision_log")
data class DecisionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val actionType: String,
    val selectedNetworkId: Long? = null,
    val currentSsid: String? = null,
    val currentRssi: Int? = null,
    val reason: String,
    val detailJson: String = ""
)
