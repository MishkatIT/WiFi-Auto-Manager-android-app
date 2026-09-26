package com.example.wifiautomanager.wifi

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.wifiautomanager.domain.model.InternetStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InternetChecker @Inject constructor(
    private val connectivityManager: ConnectivityManager
) {
    fun checkInternet(network: Network? = connectivityManager.activeNetwork): InternetStatus {
        if (network == null) return InternetStatus.UNAVAILABLE
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return InternetStatus.UNAVAILABLE
        return if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        ) {
            InternetStatus.AVAILABLE
        } else if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            InternetStatus.CHECKING
        } else {
            InternetStatus.UNAVAILABLE
        }
    }

    fun hasInternet(network: Network): Boolean {
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
