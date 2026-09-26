package com.example.wifiautomanager.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val priority: Int = 1,
    val conditionGroupJson: String,
    val actionJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
