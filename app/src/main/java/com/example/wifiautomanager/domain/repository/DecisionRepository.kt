package com.example.wifiautomanager.domain.repository

import com.example.wifiautomanager.domain.model.Decision
import kotlinx.coroutines.flow.Flow

interface DecisionRepository {
    fun getRecentDecisions(limit: Int = 50): Flow<List<Decision>>
    suspend fun recordDecision(decision: Decision): Long
    suspend fun pruneOldDecisions(retentionDays: Int)
    suspend fun clearAll()
}
