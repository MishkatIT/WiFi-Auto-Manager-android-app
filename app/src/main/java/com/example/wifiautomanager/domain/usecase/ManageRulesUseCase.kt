package com.example.wifiautomanager.domain.usecase

import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow

class ManageRulesUseCase(
    private val ruleRepository: RuleRepository
) {
    fun getRules(): Flow<List<Rule>> = ruleRepository.getAllRules()
    suspend fun saveRule(rule: Rule): Long = ruleRepository.insertRule(rule)
    suspend fun updateRule(rule: Rule) = ruleRepository.updateRule(rule)
    suspend fun deleteRule(rule: Rule) = ruleRepository.deleteRule(rule)
}
