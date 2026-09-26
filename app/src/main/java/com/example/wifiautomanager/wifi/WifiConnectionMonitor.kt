package com.example.wifiautomanager.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.example.wifiautomanager.domain.model.InternetStatus
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.util.PermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiConnectionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectivityManager: ConnectivityManager,
    private val wifiManager: WifiManager,
    private val internetChecker: InternetChecker
) {
    private val _connectionState = MutableStateFlow<WifiState>(WifiState.Disconnected)
    val connectionState: StateFlow<WifiState> = _connectionState.asStateFlow()

    private var currentConnectedSinceMs: Long = 0L
    private var isMonitoring = false

    private val networkCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onAvailable(network: Network) {
                if (currentConnectedSinceMs == 0L) {
                    currentConnectedSinceMs = System.currentTimeMillis()
                }
                updateWifiState(network)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                updateWifiState(network, networkCapabilities)
            }

            override fun onLost(network: Network) {
                currentConnectedSinceMs = 0L
                _connectionState.value = WifiState.Disconnected
            }

            override fun onUnavailable() {
                currentConnectedSinceMs = 0L
                _connectionState.value = WifiState.Disconnected
            }
        }
    } else {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (currentConnectedSinceMs == 0L) {
                    currentConnectedSinceMs = System.currentTimeMillis()
                }
                updateWifiState(network)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                updateWifiState(network, networkCapabilities)
            }

            override fun onLost(network: Network) {
                currentConnectedSinceMs = 0L
                _connectionState.value = WifiState.Disconnected
            }

            override fun onUnavailable() {
                currentConnectedSinceMs = 0L
                _connectionState.value = WifiState.Disconnected
            }
        }
    }

    fun start() {
        if (isMonitoring) return

        if (!wifiManager.isWifiEnabled) {
            _connectionState.value = WifiState.Disabled
            return
        }

        try {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isMonitoring = true

            // Trigger immediate check with active network if already connected
            val active = connectivityManager.activeNetwork
            if (active != null) {
                updateWifiState(active)
            }
        } catch (e: Exception) {
            _connectionState.value = WifiState.Error("Failed to register network callback: ${e.message}", e)
        }
    }

    fun stop() {
        if (!isMonitoring) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (ignored: Exception) {
        } finally {
            isMonitoring = false
        }
    }

    private fun updateWifiState(network: Network, capabilities: NetworkCapabilities? = null) {
        if (!wifiManager.isWifiEnabled) {
            _connectionState.value = WifiState.Disabled
            return
        }

        val caps = capabilities ?: connectivityManager.getNetworkCapabilities(network)
        if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            _connectionState.value = WifiState.Disconnected
            return
        }

        val wifiInfo = getWifiInfo(caps)
        val rawSsid = wifiInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"") ?: "<unknown ssid>"
        val bssid = wifiInfo?.bssid ?: ""
        val rssi = wifiInfo?.rssi ?: -127
        val frequency = wifiInfo?.frequency ?: 0

        val internetStatus = if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            InternetStatus.AVAILABLE
        } else if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            InternetStatus.CHECKING
        } else {
            InternetStatus.UNAVAILABLE
        }

        if (rawSsid == "<unknown ssid>" && !PermissionHelper.hasScanPermissions(context)) {
            _connectionState.value = WifiState.PermissionRequired(PermissionHelper.getRequiredScanPermissions())
            return
        }

        if (currentConnectedSinceMs == 0L) {
            currentConnectedSinceMs = System.currentTimeMillis()
        }

        _connectionState.value = WifiState.Connected(
            ssid = rawSsid,
            bssid = bssid,
            rssi = rssi,
            frequencyMhz = frequency,
            internetStatus = internetStatus,
            connectedSinceMs = currentConnectedSinceMs
        )
    }

    @Suppress("DEPRECATION")
    private fun getWifiInfo(caps: NetworkCapabilities): WifiInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            caps.transportInfo as? WifiInfo
        } else {
            wifiManager.connectionInfo
        }
    }
}
