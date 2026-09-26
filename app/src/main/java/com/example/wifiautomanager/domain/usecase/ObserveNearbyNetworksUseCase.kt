package com.example.wifiautomanager.domain.usecase

import com.example.wifiautomanager.domain.model.ScannedNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ObserveNearbyNetworksUseCase {
    operator fun invoke(): Flow<List<ScannedNetwork>> {
        return flowOf(emptyList())
    }
}
