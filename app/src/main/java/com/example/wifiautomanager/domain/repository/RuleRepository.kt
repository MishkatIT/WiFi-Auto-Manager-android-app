package com.example.wifiautomanager.domain.repository

import com.example.wifiautomanager.domain.model.Rule
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    fun getAllRules(): Flow<List<Rule>>
    fun getEnabledRules(): Flow<List<Rule>>
    suspend fun getRuleById(id: Long): Rule?
    suspend fun insertRule(rule: Rule): Long
    suspend fun updateRule(rule: Rule)
    suspend fun deleteRule(rule: Rule)
}
