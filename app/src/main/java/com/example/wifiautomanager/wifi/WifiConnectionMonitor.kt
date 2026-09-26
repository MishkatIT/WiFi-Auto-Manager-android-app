package com.example.wifiautomanager.wifi

import com.example.wifiautomanager.domain.model.WifiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// WifiConnectionMonitor monitors real-time connection status - implemented in Phase 4
class WifiConnectionMonitor {
    val connectionState: Flow<WifiState> = flowOf(WifiState.Disconnected)
}
