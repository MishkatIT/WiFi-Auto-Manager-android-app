package com.example.wifiautomanager.data.repository

import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// RuleRepository implementation - wired to Room in Phase 2
class RuleRepositoryImpl : RuleRepository {
    override fun getAllRules(): Flow<List<Rule>> = flowOf(emptyList())
    override fun getEnabledRules(): Flow<List<Rule>> = flowOf(emptyList())
    override suspend fun getRuleById(id: Long): Rule? = null
    override suspend fun insertRule(rule: Rule): Long = 0L
    override suspend fun updateRule(rule: Rule) {}
    override suspend fun deleteRule(rule: Rule) {}
}
