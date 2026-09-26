package com.example.wifiautomanager.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.example.wifiautomanager.domain.model.ScannedNetwork
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.repository.WifiRepository
import com.example.wifiautomanager.util.PermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class ScanState {
    data object Idle : ScanState()
    data object Scanning : ScanState()
    data object WifiDisabled : ScanState()
    data class PermissionMissing(val permissions: List<String>) : ScanState()
    data class Failed(val reason: String) : ScanState()
    data class Throttled(val nextAllowedMs: Long) : ScanState()
    data class Results(
        val networks: List<ScannedNetwork>,
        val timestampMs: Long,
        val isFresh: Boolean,
    ) : ScanState()
}

@Singleton
class WifiScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiManager: WifiManager,
    private val wifiRepository: WifiRepository
) {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val scanHistoryTimestamps = mutableListOf<Long>()
    private var isReceiverRegistered = false

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                processScanResults(success)
            }
        }
    }

    fun register() {
        if (isReceiverRegistered) return
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(scanReceiver, filter)
        isReceiverRegistered = true
    }

    fun unregister() {
        if (!isReceiverRegistered) return
        try {
            context.unregisterReceiver(scanReceiver)
        } catch (ignored: Exception) {
        } finally {
            isReceiverRegistered = false
        }
    }

    /**
     * Request a Wi-Fi scan with Android throttle handling (max 4 scans per 2 minutes in foreground).
     */
    fun requestScan(): Boolean {
        register()

        if (!wifiManager.isWifiEnabled) {
            _scanState.value = ScanState.WifiDisabled
            return false
        }

        if (!PermissionHelper.hasScanPermissions(context)) {
            _scanState.value = ScanState.PermissionMissing(PermissionHelper.getRequiredScanPermissions())
            return false
        }

        val now = System.currentTimeMillis()
        val twoMinutesAgo = now - 120_000L
        scanHistoryTimestamps.removeAll { it < twoMinutesAgo }

        if (scanHistoryTimestamps.size >= 4) {
            val oldest = scanHistoryTimestamps.first()
            val nextAllowedMs = oldest + 120_000L
            _scanState.value = ScanState.Throttled(nextAllowedMs)
            return false
        }

        _scanState.value = ScanState.Scanning

        @Suppress("DEPRECATION")
        val started = wifiManager.startScan()
        return if (started) {
            scanHistoryTimestamps.add(now)
            true
        } else {
            // Even if startScan returns false, we can still read existing scan results
            processScanResults(isFresh = false)
            _scanState.value = ScanState.Failed("Scan request rejected by OS (throttled)")
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun processScanResults(isFresh: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rawResults: List<ScanResult> = wifiManager.scanResults ?: emptyList()
                val savedNetworks = wifiRepository.getAllNetworks().firstOrNull() ?: emptyList()
                val savedMap = savedNetworks.associateBy { it.ssid }

                // Group by SSID, taking the strongest signal (highest RSSI)
                val mappedNetworks = rawResults
                    .filter { !it.SSID.isNullOrBlank() }
                    .groupBy { it.SSID }
                    .map { (ssid, results) ->
                        val best = results.maxByOrNull { it.level } ?: results.first()
                        val saved = savedMap[ssid]
                        ScannedNetwork(
                            ssid = ssid,
                            bssid = best.BSSID ?: "",
                            rssi = best.level,
                            frequencyMhz = best.frequency,
                            capabilities = best.capabilities ?: "",
                            securityType = parseCapabilities(best.capabilities ?: ""),
                            isSaved = saved != null,
                            savedNetworkId = saved?.id,
                            timestampMs = System.currentTimeMillis()
                        )
                    }
                    .sortedByDescending { it.rssi }

                _scanState.value = ScanState.Results(
                    networks = mappedNetworks,
                    timestampMs = System.currentTimeMillis(),
                    isFresh = isFresh
                )
            } catch (e: Exception) {
                _scanState.value = ScanState.Failed("Failed to parse scan results: ${e.message}")
            }
        }
    }

    private fun parseCapabilities(capabilities: String): SecurityType {
        return when {
            capabilities.contains("SAE") || capabilities.contains("WPA3") -> SecurityType.WPA3_SAE
            capabilities.contains("WPA2") || capabilities.contains("PSK") -> SecurityType.WPA2_PSK
            capabilities.contains("EAP") -> SecurityType.WPA2_EAP
            capabilities.contains("WEP") -> SecurityType.WEP
            else -> SecurityType.OPEN
        }
    }
}
