package com.example.wifiautomanager.data.repository

import com.example.wifiautomanager.data.local.db.dao.DecisionLogDao
import com.example.wifiautomanager.data.local.db.entity.DecisionLogEntity
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.domain.repository.DecisionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecisionRepositoryImpl @Inject constructor(
    private val decisionLogDao: DecisionLogDao
) : DecisionRepository {

    override fun getRecentDecisions(limit: Int): Flow<List<Decision>> {
        return decisionLogDao.getRecentLogs(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun recordDecision(decision: Decision): Long {
        val currentConnected = decision.currentState as? WifiState.Connected
        val entity = DecisionLogEntity(
            id = decision.id,
            timestampMs = decision.timestampMs,
            actionType = when (decision.action) {
                is DecisionAction.SuggestNetwork -> "SUGGEST"
                is DecisionAction.StayOnCurrent -> "STAY"
                is DecisionAction.NoCandidates -> "NO_CANDIDATES"
                is DecisionAction.CooldownActive -> "COOLDOWN"
                is DecisionAction.RulesDisabled -> "DISABLED"
            },
            selectedNetworkId = decision.selectedNetwork?.id,
            currentSsid = currentConnected?.ssid,
            currentRssi = currentConnected?.rssi,
            reason = decision.reason,
            detailJson = ""
        )
        return decisionLogDao.insertLog(entity)
    }

    override suspend fun pruneOldDecisions(retentionDays: Int) {
        val cutoff = System.currentTimeMillis() - (retentionDays * 86_400_000L)
        decisionLogDao.deleteLogsOlderThan(cutoff)
    }

    override suspend fun clearAll() {
        decisionLogDao.clearAllLogs()
    }

    private fun DecisionLogEntity.toDomain(): Decision {
        val action = when (actionType) {
            "SUGGEST" -> DecisionAction.StayOnCurrent // restored summary
            "STAY" -> DecisionAction.StayOnCurrent
            "COOLDOWN" -> DecisionAction.CooldownActive
            "DISABLED" -> DecisionAction.RulesDisabled
            else -> DecisionAction.NoCandidates
        }
        return Decision(
            id = id,
            timestampMs = timestampMs,
            action = action,
            selectedNetwork = null,
            reason = reason,
            evaluatedCandidates = emptyList(),
            currentState = WifiState.Disconnected
        )
    }
}
