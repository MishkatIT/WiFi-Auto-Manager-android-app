package com.example.wifiautomanager.domain.usecase

import com.example.wifiautomanager.domain.model.WifiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ObserveCurrentConnectionUseCase {
    operator fun invoke(): Flow<WifiState> {
        return flowOf(WifiState.Disabled)
    }
}
