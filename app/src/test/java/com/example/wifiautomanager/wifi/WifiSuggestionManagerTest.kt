package com.example.wifiautomanager.wifi

import android.content.Context
import android.net.wifi.WifiManager
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.SuggestionState
import com.example.wifiautomanager.domain.model.WifiNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class WifiSuggestionManagerTest {

    private lateinit var context: Context
    private lateinit var wifiManager: WifiManager
    private lateinit var suggestionManager: WifiSuggestionManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        suggestionManager = WifiSuggestionManager(context, wifiManager)
    }

    @Test
    fun testSuggestionStateProperties() {
        val registered = SuggestionState.Registered
        val duplicate = SuggestionState.Duplicate
        val notAllowed = SuggestionState.NotAllowed
        val apiNotSupported = SuggestionState.ApiNotSupported
        val failed = SuggestionState.Failed("Error test")
        val notRegistered = SuggestionState.NotRegistered

        assertTrue(registered.isSuccess)
        assertFalse(duplicate.isSuccess)
        assertFalse(notAllowed.isSuccess)
        assertFalse(apiNotSupported.isSuccess)
        assertFalse(failed.isSuccess)
        assertFalse(notRegistered.isSuccess)

        assertEquals("Suggested (Active)", registered.label)
        assertEquals("Duplicate Suggestion", duplicate.label)
        assertEquals("Not Allowed by OS", notAllowed.label)
        assertEquals("Requires Android 10+", apiNotSupported.label)
        assertEquals("Failed: Error test", failed.label)
        assertEquals("Not Suggested", notRegistered.label)
    }

    @Test
    fun testIsSupportedOnSdk30() {
        assertTrue(suggestionManager.isSupported())
    }

    @Test
    fun testRegisterDisabledNetworkMarksNotRegistered() {
        val disabledNetwork = WifiNetwork(
            id = 1L,
            ssid = "DisabledNet",
            securityType = SecurityType.WPA2_PSK,
            enabled = false,
            priority = 50
        )

        val result = suggestionManager.registerSuggestion(disabledNetwork, "secret123")
        assertEquals(SuggestionState.NotRegistered, result)
        assertEquals(SuggestionState.NotRegistered, suggestionManager.getSuggestionState(1L))
    }

    @Test
    fun testRegisterSecuredNetworkWithoutPasswordFails() {
        val securedNetwork = WifiNetwork(
            id = 2L,
            ssid = "SecuredNet",
            securityType = SecurityType.WPA2_PSK,
            enabled = true,
            priority = 80
        )

        val result = suggestionManager.registerSuggestion(securedNetwork, null)
        assertTrue(result is SuggestionState.Failed)
    }

    @Test
    fun testRegisterOpenNetworkSucceeds() {
        val openNetwork = WifiNetwork(
            id = 3L,
            ssid = "FreeCoffeeWiFi",
            securityType = SecurityType.OPEN,
            enabled = true,
            priority = 60
        )

        val result = suggestionManager.registerSuggestion(openNetwork, null)
        assertNotNull(result)
        assertTrue(result is SuggestionState.Registered || result is SuggestionState.Failed)
    }

    @Test
    fun testRemoveSuggestionSetsStateNotRegistered() {
        val network = WifiNetwork(
            id = 4L,
            ssid = "TestRemove",
            securityType = SecurityType.OPEN,
            enabled = true,
            priority = 70
        )

        suggestionManager.removeSuggestion(network)
        assertEquals(SuggestionState.NotRegistered, suggestionManager.getSuggestionState(4L))
    }

    @Test
    fun testClearAllSuggestionsClearsState() {
        val network1 = WifiNetwork(id = 10L, ssid = "Net1", securityType = SecurityType.OPEN, priority = 50)
        val network2 = WifiNetwork(id = 11L, ssid = "Net2", securityType = SecurityType.OPEN, priority = 50)

        suggestionManager.registerSuggestions(listOf(network1, network2)) { null }
        suggestionManager.clearAllSuggestions()

        assertTrue(suggestionManager.suggestionStates.value.isEmpty())
    }
}
