package com.example.wifiautomanager.domain.rule

import com.example.wifiautomanager.domain.model.CandidateResult
import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.model.WifiState

class RuleEvaluator(
    private val conditionEvaluator: ConditionEvaluator
) {
    fun evaluateRule(
        rule: Rule,
        currentState: WifiState,
        candidate: CandidateResult? = null
    ): Boolean {
        if (!rule.enabled) return false
        return true
    }
}
