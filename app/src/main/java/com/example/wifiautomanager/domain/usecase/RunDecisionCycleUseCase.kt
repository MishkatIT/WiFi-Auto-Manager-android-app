package com.example.wifiautomanager.domain.usecase

import com.example.wifiautomanager.domain.decision.DecisionEngine
import com.example.wifiautomanager.domain.decision.EvaluationContext
import com.example.wifiautomanager.domain.model.AppSettings
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.ScannedNetwork
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.domain.repository.DecisionRepository
import com.example.wifiautomanager.domain.repository.RuleRepository
import com.example.wifiautomanager.domain.repository.SettingsRepository
import com.example.wifiautomanager.domain.repository.WifiRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class RunDecisionCycleUseCase @Inject constructor(
    private val decisionEngine: DecisionEngine,
    private val wifiRepository: WifiRepository,
    private val ruleRepository: RuleRepository,
    private val settingsRepository: SettingsRepository,
    private val decisionRepository: DecisionRepository
) {

    suspend operator fun invoke(
        currentState: WifiState,
        availableNetworks: List<ScannedNetwork>,
        context: EvaluationContext = EvaluationContext()
    ): Decision {
        val savedNetworks = wifiRepository.getEnabledNetworks().firstOrNull() ?: emptyList()
        val rules = ruleRepository.getEnabledRules().firstOrNull() ?: emptyList()
        val settings = settingsRepository.getSettings().firstOrNull() ?: AppSettings()

        val decision = decisionEngine.evaluate(
            currentState = currentState,
            availableNetworks = availableNetworks,
            savedNetworks = savedNetworks,
            rules = rules,
            settings = settings,
            context = context
        )

        decisionRepository.recordDecision(decision)
        return decision
    }
}
