package com.example.wifiautomanager.wifi

import com.example.wifiautomanager.domain.model.ScannedNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// WifiScanner scans for nearby Wi-Fi networks - implemented in Phase 4
class WifiScanner {
    fun getScannedNetworks(): Flow<List<ScannedNetwork>> = flowOf(emptyList())
    fun triggerScan(): Boolean = false
}
