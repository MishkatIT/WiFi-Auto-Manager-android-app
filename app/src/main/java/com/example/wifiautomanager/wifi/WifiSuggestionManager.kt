package com.example.wifiautomanager.wifi

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.SuggestionState
import com.example.wifiautomanager.domain.model.WifiNetwork
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WifiSuggestionManager"

@Singleton
class WifiSuggestionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiManager: WifiManager
) {
    private val _suggestionStates = MutableStateFlow<Map<Long, SuggestionState>>(emptyMap())
    val suggestionStates: StateFlow<Map<Long, SuggestionState>> = _suggestionStates.asStateFlow()

    // Cache created suggestions by network ID so they can be removed properly
    private val registeredSuggestions = mutableMapOf<Long, Any>()

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * Registers network suggestions for enabled networks with Android OS.
     * Compatible with Android 10+ (API 29+). On older APIs, marks as ApiNotSupported without crash.
     */
    fun registerSuggestions(
        networks: List<WifiNetwork>,
        getPassword: (Long) -> String?
    ): Map<Long, SuggestionState> {
        if (!isSupported()) {
            val unsupported = networks.associate { it.id to SuggestionState.ApiNotSupported }
            _suggestionStates.update { it + unsupported }
            return unsupported
        }

        if (networks.isEmpty()) return emptyMap()

        val results = mutableMapOf<Long, SuggestionState>()
        val suggestionsToAdd = mutableListOf<WifiNetworkSuggestion>()
        val suggestionToNetworkMap = mutableMapOf<WifiNetworkSuggestion, WifiNetwork>()

        for (network in networks) {
            if (!network.enabled) {
                // If network is disabled, remove existing suggestion if any
                removeSuggestion(network)
                results[network.id] = SuggestionState.NotRegistered
                continue
            }

            val password = getPassword(network.id)
            val suggestion = try {
                buildSuggestion(network, password)
            } catch (e: Exception) {
                Log.e(TAG, "Error building suggestion for ${network.ssid}", e)
                null
            }

            if (suggestion != null) {
                suggestionsToAdd.add(suggestion)
                suggestionToNetworkMap[suggestion] = network
            } else {
                results[network.id] = SuggestionState.Failed("Unsupported configuration or invalid passphrase")
            }
        }

        if (suggestionsToAdd.isNotEmpty()) {
            try {
                val statusCode = wifiManager.addNetworkSuggestions(suggestionsToAdd)
                val statusState = mapStatusCodeToSuggestionState(statusCode)

                for (suggestion in suggestionsToAdd) {
                    val network = suggestionToNetworkMap[suggestion] ?: continue
                    results[network.id] = statusState
                    if (statusState is SuggestionState.Registered) {
                        registeredSuggestions[network.id] = suggestion
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed calling addNetworkSuggestions", e)
                val errorState = SuggestionState.Failed(e.message ?: "Unknown error")
                for (suggestion in suggestionsToAdd) {
                    val network = suggestionToNetworkMap[suggestion] ?: continue
                    results[network.id] = errorState
                }
            }
        }

        _suggestionStates.update { current -> current + results }
        return results
    }

    /**
     * Registers a single network suggestion.
     */
    fun registerSuggestion(network: WifiNetwork, password: String?): SuggestionState {
        if (!isSupported()) {
            val state = SuggestionState.ApiNotSupported
            _suggestionStates.update { it + (network.id to state) }
            return state
        }

        if (!network.enabled) {
            removeSuggestion(network)
            return SuggestionState.NotRegistered
        }

        val suggestion = try {
            buildSuggestion(network, password)
        } catch (e: Exception) {
            Log.e(TAG, "Error building suggestion for ${network.ssid}", e)
            val failedState = SuggestionState.Failed(e.message ?: "Invalid parameters")
            _suggestionStates.update { it + (network.id to failedState) }
            return failedState
        }

        if (suggestion == null) {
            val failedState = SuggestionState.Failed("Unsupported security type or missing password")
            _suggestionStates.update { it + (network.id to failedState) }
            return failedState
        }

        return try {
            val statusCode = wifiManager.addNetworkSuggestions(listOf(suggestion))
            val state = mapStatusCodeToSuggestionState(statusCode)
            if (state is SuggestionState.Registered) {
                registeredSuggestions[network.id] = suggestion
            }
            _suggestionStates.update { it + (network.id to state) }
            state
        } catch (e: Exception) {
            Log.e(TAG, "Exception in registerSuggestion", e)
            val errorState = SuggestionState.Failed(e.message ?: "Failed to add suggestion")
            _suggestionStates.update { it + (network.id to errorState) }
            errorState
        }
    }

    /**
     * Removes a network suggestion for the given network from Android OS.
     */
    fun removeSuggestion(network: WifiNetwork): Boolean {
        if (!isSupported()) {
            _suggestionStates.update { it - network.id }
            return false
        }

        val cached = registeredSuggestions.remove(network.id) as? WifiNetworkSuggestion
        val toRemove = cached ?: try {
            buildSuggestion(network, null)
        } catch (e: Exception) {
            null
        }

        if (toRemove == null) {
            _suggestionStates.update { it + (network.id to SuggestionState.NotRegistered) }
            return true
        }

        return try {
            val statusCode = wifiManager.removeNetworkSuggestions(listOf(toRemove))
            val success = statusCode == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
            _suggestionStates.update { it + (network.id to SuggestionState.NotRegistered) }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Exception removing suggestion for ${network.ssid}", e)
            _suggestionStates.update { it + (network.id to SuggestionState.NotRegistered) }
            false
        }
    }

    /**
     * Removes suggestions for multiple networks.
     */
    fun removeSuggestions(networks: List<WifiNetwork>): Boolean {
        if (!isSupported()) {
            val ids = networks.map { it.id }.toSet()
            _suggestionStates.update { current -> current.filterKeys { it !in ids } }
            return false
        }

        val suggestionsToRemove = mutableListOf<WifiNetworkSuggestion>()
        for (net in networks) {
            val cached = registeredSuggestions.remove(net.id) as? WifiNetworkSuggestion
            val suggestion = cached ?: try {
                buildSuggestion(net, null)
            } catch (e: Exception) {
                null
            }
            if (suggestion != null) {
                suggestionsToRemove.add(suggestion)
            }
        }

        val removedStates = networks.associate { it.id to SuggestionState.NotRegistered }
        _suggestionStates.update { it + removedStates }

        if (suggestionsToRemove.isEmpty()) return true

        return try {
            val status = wifiManager.removeNetworkSuggestions(suggestionsToRemove)
            status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Error removing suggestions", e)
            false
        }
    }

    /**
     * Clears all registered suggestions.
     */
    fun clearAllSuggestions() {
        if (!isSupported()) {
            _suggestionStates.value = emptyMap()
            return
        }

        val toRemove = registeredSuggestions.values.filterIsInstance<WifiNetworkSuggestion>()
        if (toRemove.isNotEmpty()) {
            try {
                wifiManager.removeNetworkSuggestions(toRemove)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing suggestions", e)
            }
        }
        registeredSuggestions.clear()
        _suggestionStates.value = emptyMap()
    }

    fun getSuggestionState(networkId: Long): SuggestionState {
        return _suggestionStates.value[networkId] ?: SuggestionState.NotRegistered
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun buildSuggestion(network: WifiNetwork, password: String?): WifiNetworkSuggestion? {
        val builder = WifiNetworkSuggestion.Builder()
            .setSsid(network.ssid)
            .setPriority(network.priority.coerceIn(0, 1000))
            .setIsUserInteractionRequired(false)

        when (network.securityType) {
            SecurityType.OPEN -> {
                // Open network has no passphrase
            }
            SecurityType.WPA2_PSK -> {
                if (password.isNullOrEmpty()) return null
                builder.setWpa2Passphrase(password)
            }
            SecurityType.WPA3_SAE -> {
                if (password.isNullOrEmpty()) return null
                builder.setWpa3Passphrase(password)
            }
            SecurityType.WEP,
            SecurityType.WPA2_EAP,
            SecurityType.UNKNOWN -> {
                // Enterprise or legacy WEP are not directly supported via simple suggestion passphrase
                return null
            }
        }

        return builder.build()
    }

    private fun mapStatusCodeToSuggestionState(status: Int): SuggestionState {
        return when (status) {
            WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS -> SuggestionState.Registered
            WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE -> SuggestionState.Duplicate
            WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED -> SuggestionState.NotAllowed
            WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP ->
                SuggestionState.Failed("Exceeded max suggestions per app")
            WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID ->
                SuggestionState.Failed("Invalid suggestion parameters")
            else -> SuggestionState.Failed("Status code $status")
        }
    }
}
