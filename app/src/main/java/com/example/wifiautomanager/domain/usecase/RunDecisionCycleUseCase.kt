package com.example.wifiautomanager.domain.usecase

import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.WifiState

class RunDecisionCycleUseCase {
    operator fun invoke(): Decision {
        return Decision(
            action = DecisionAction.StayOnCurrent,
            selectedNetwork = null,
            reason = "Default initial state",
            currentState = WifiState.Disconnected
        )
    }
}
