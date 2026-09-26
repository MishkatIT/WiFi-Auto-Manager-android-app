package com.example.wifiautomanager.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.wifiautomanager.data.local.db.dao.DecisionLogDao
import com.example.wifiautomanager.data.local.db.dao.RuleDao
import com.example.wifiautomanager.data.local.db.dao.WifiNetworkDao
import com.example.wifiautomanager.data.local.db.entity.DecisionLogEntity
import com.example.wifiautomanager.data.local.db.entity.RuleEntity
import com.example.wifiautomanager.data.local.db.entity.WifiNetworkEntity

@Database(
    entities = [
        WifiNetworkEntity::class,
        RuleEntity::class,
        DecisionLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wifiNetworkDao(): WifiNetworkDao
    abstract fun ruleDao(): RuleDao
    abstract fun decisionLogDao(): DecisionLogDao
}
