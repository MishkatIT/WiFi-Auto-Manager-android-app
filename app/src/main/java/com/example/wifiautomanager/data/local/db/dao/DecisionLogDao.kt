package com.example.wifiautomanager.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.wifiautomanager.data.local.db.entity.DecisionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DecisionLogDao {
    @Query("SELECT * FROM decision_log ORDER BY timestampMs DESC")
    fun getAllLogs(): Flow<List<DecisionLogEntity>>

    @Query("SELECT * FROM decision_log ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<DecisionLogEntity>>

    @Query("SELECT * FROM decision_log WHERE id = :id")
    suspend fun getLogById(id: Long): DecisionLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DecisionLogEntity): Long

    @Query("DELETE FROM decision_log WHERE timestampMs < :cutoffTimestampMs")
    suspend fun deleteLogsOlderThan(cutoffTimestampMs: Long): Int

    @Query("DELETE FROM decision_log")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM decision_log")
    suspend fun getLogCount(): Int
}
