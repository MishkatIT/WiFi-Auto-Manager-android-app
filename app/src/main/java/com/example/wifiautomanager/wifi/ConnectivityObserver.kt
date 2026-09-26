package com.example.wifiautomanager.wifi

import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

sealed class NetworkState {
    data object Available : NetworkState()
    data object Unavailable : NetworkState()
    data object Losing : NetworkState()
    data object Lost : NetworkState()
}

interface ConnectivityObserver {
    val networkState: Flow<NetworkState>
}

@Singleton
class ConnectivityObserverImpl @Inject constructor(
    private val connectivityManager: ConnectivityManager
) : ConnectivityObserver {

    override val networkState: Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkState.Available)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                trySend(NetworkState.Losing)
            }

            override fun onLost(network: Network) {
                trySend(NetworkState.Lost)
            }

            override fun onUnavailable() {
                trySend(NetworkState.Unavailable)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
