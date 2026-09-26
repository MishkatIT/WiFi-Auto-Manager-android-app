package com.example.wifiautomanager.domain.decision

import com.example.wifiautomanager.domain.model.Decision

class DecisionExplainer {
    fun explain(decision: Decision): String {
        return decision.reason
    }
}
