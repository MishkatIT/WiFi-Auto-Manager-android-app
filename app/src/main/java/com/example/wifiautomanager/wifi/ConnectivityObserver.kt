package com.example.wifiautomanager.wifi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// ConnectivityObserver tracks default network connectivity status - implemented in Phase 4
class ConnectivityObserver {
    val isConnected: Flow<Boolean> = flowOf(false)
}
