package com.example.wifiautomanager.data.repository

import com.example.wifiautomanager.data.local.db.dao.DecisionLogDao
import com.example.wifiautomanager.data.local.db.entity.DecisionLogEntity
import com.example.wifiautomanager.domain.model.CandidateResult
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.InternetStatus
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.domain.repository.DecisionRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecisionRepositoryImpl @Inject constructor(
    private val decisionLogDao: DecisionLogDao,
    private val gson: Gson = Gson()
) : DecisionRepository {

    override fun getRecentDecisions(limit: Int): Flow<List<Decision>> {
        return decisionLogDao.getRecentLogs(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getDecisionById(id: Long): Decision? {
        val entity = decisionLogDao.getLogById(id) ?: return null
        return entity.toDomain()
    }

    override suspend fun recordDecision(decision: Decision): Long {
        val currentConnected = decision.currentState as? WifiState.Connected
        val detailJson = try {
            gson.toJson(decision.evaluatedCandidates)
        } catch (e: Exception) {
            ""
        }

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
            detailJson = detailJson
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
        val candidates: List<CandidateResult> = try {
            if (detailJson.isNotBlank()) {
                val type = object : TypeToken<List<CandidateResult>>() {}.type
                gson.fromJson(detailJson, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }

        val restoredCurrentState: WifiState = if (currentSsid != null) {
            WifiState.Connected(
                ssid = currentSsid,
                bssid = "",
                rssi = currentRssi ?: -70,
                frequencyMhz = 2412,
                internetStatus = InternetStatus.AVAILABLE,
                connectedSinceMs = 0L
            )
        } else {
            WifiState.Disconnected
        }

        val selectedCandidate = candidates.find { it.network.id == selectedNetworkId }
        val action = when (actionType) {
            "SUGGEST" -> {
                if (selectedCandidate != null) {
                    DecisionAction.SuggestNetwork(selectedCandidate.network)
                } else {
                    DecisionAction.StayOnCurrent
                }
            }
            "STAY" -> DecisionAction.StayOnCurrent
            "COOLDOWN" -> DecisionAction.CooldownActive
            "DISABLED" -> DecisionAction.RulesDisabled
            else -> DecisionAction.NoCandidates
        }

        return Decision(
            id = id,
            timestampMs = timestampMs,
            action = action,
            selectedNetwork = selectedCandidate?.network,
            reason = reason,
            evaluatedCandidates = candidates,
            currentState = restoredCurrentState
        )
    }
}
