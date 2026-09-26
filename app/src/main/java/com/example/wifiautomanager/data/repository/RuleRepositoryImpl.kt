package com.example.wifiautomanager.data.repository

import com.example.wifiautomanager.data.local.db.dao.RuleDao
import com.example.wifiautomanager.data.local.db.entity.RuleEntity
import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.model.RuleAction
import com.example.wifiautomanager.domain.repository.RuleRepository
import com.example.wifiautomanager.domain.rule.ConditionGroup
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val ruleDao: RuleDao,
    private val gson: Gson
) : RuleRepository {

    override fun getAllRules(): Flow<List<Rule>> =
        ruleDao.getAllRules().map { list -> list.map { it.toDomain(gson) } }

    override fun getEnabledRules(): Flow<List<Rule>> =
        ruleDao.getEnabledRules().map { list -> list.map { it.toDomain(gson) } }

    override suspend fun getRuleById(id: Long): Rule? =
        ruleDao.getRuleById(id)?.toDomain(gson)

    override suspend fun insertRule(rule: Rule): Long =
        ruleDao.insertRule(rule.toEntity(gson))

    override suspend fun updateRule(rule: Rule) =
        ruleDao.updateRule(rule.toEntity(gson))

    override suspend fun deleteRule(rule: Rule) =
        ruleDao.deleteRule(rule.toEntity(gson))
}

private fun RuleEntity.toDomain(gson: Gson): Rule {
    val conditionGroup = try {
        gson.fromJson(conditionGroupJson, ConditionGroup::class.java) ?: ConditionGroup()
    } catch (e: Exception) {
        ConditionGroup()
    }
    val action = try {
        gson.fromJson(actionJson, RuleAction::class.java) ?: RuleAction.PreferBestCandidate
    } catch (e: Exception) {
        RuleAction.PreferBestCandidate
    }
    return Rule(
        id = id,
        name = name,
        enabled = enabled,
        priority = priority,
        rootGroup = conditionGroup,
        action = action,
        createdAt = createdAt
    )
}

private fun Rule.toEntity(gson: Gson): RuleEntity = RuleEntity(
    id = id,
    name = name,
    enabled = enabled,
    priority = priority,
    conditionGroupJson = gson.toJson(rootGroup),
    actionJson = gson.toJson(action),
    createdAt = createdAt
)
