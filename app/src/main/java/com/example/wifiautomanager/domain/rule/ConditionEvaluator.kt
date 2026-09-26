package com.example.wifiautomanager.domain.rule

import com.example.wifiautomanager.domain.model.CandidateResult
import com.example.wifiautomanager.domain.model.WifiState

class ConditionEvaluator {
    fun evaluate(
        condition: Condition,
        currentState: WifiState,
        candidate: CandidateResult? = null
    ): Boolean {
        return true
    }
}
