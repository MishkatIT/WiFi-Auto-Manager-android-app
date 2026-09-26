package com.example.wifiautomanager.domain.decision

import com.example.wifiautomanager.domain.model.AppSettings
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.InternetStatus
import com.example.wifiautomanager.domain.model.ScannedNetwork
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.model.WifiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DecisionEngineScenariosTest {

    private lateinit var engine: DecisionEngine
    private lateinit var settings: AppSettings

    @Before
    fun setup() {
        engine = DecisionEngine()
        settings = AppSettings(
            autoManagerEnabled = true,
            switchCooldownSeconds = 60,
            internetUnavailableTimeoutSeconds = 15
        )
    }

    // Scenario A: -76 dBm, no internet -> -55 dBm candidate with internet -> Suggest candidate
    @Test
    fun testScenarioA_weakSignalNoInternet_suggestsStrongCandidate() {
        val current = WifiState.Connected(
            ssid = "CurrentNet",
            bssid = "00:11:22:33:44:55",
            rssi = -76,
            frequencyMhz = 2412,
            internetStatus = InternetStatus.UNAVAILABLE,
            connectedSinceMs = 100_000L
        )

        val candidateSaved = WifiNetwork(
            id = 1,
            ssid = "BetterNet",
            securityType = SecurityType.WPA2_PSK,
            priority = 50,
            minimumSignalDbm = -70,
            minimumImprovementDbm = 10
        )

        val candidateScanned = ScannedNetwork(
            ssid = "BetterNet",
            bssid = "aa:bb:cc:dd:ee:01",
            rssi = -55,
            frequencyMhz = 5180,
            capabilities = "WPA2",
            securityType = SecurityType.WPA2_PSK,
            isSaved = true,
            savedNetworkId = 1,
            timestampMs = System.currentTimeMillis()
        )

        val context = EvaluationContext(
            currentTimeMs = 200_000L,
            lastSwitchTimeMs = 100_000L, // 100s ago, cooldown expired
            internetUnavailableSinceMs = 150_000L // 50s ago > 15s timeout
        )

        val decision = engine.evaluate(
            currentState = current,
            availableNetworks = listOf(candidateScanned),
            savedNetworks = listOf(candidateSaved),
            rules = emptyList(),
            settings = settings,
            context = context
        )

        assertTrue(decision.action is DecisionAction.SuggestNetwork)
        assertEquals("BetterNet", (decision.action as DecisionAction.SuggestNetwork).network.ssid)
    }

    // Scenario B: -68 dBm with internet -> candidate -66 dBm (only 2 dBm improvement < 10) -> Stay
    @Test
    fun testScenarioB_insufficientSignalImprovement_staysOnCurrent() {
        val current = WifiState.Connected(
            ssid = "CurrentNet",
            bssid = "00:11:22:33:44:55",
            rssi = -68,
            frequencyMhz = 5180,
            internetStatus = InternetStatus.AVAILABLE,
            connectedSinceMs = 100_000L
        )

        val candidateSaved = WifiNetwork(
            id = 1,
            ssid = "SlightlyBetterNet",
            securityType = SecurityType.WPA2_PSK,
            priority = 50,
            minimumSignalDbm = -70,
            minimumImprovementDbm = 10
        )

        val candidateScanned = ScannedNetwork(
            ssid = "SlightlyBetterNet",
            bssid = "aa:bb:cc:dd:ee:01",
            rssi = -66, // only 2 dBm better
            frequencyMhz = 5180,
            capabilities = "WPA2",
            securityType = SecurityType.WPA2_PSK,
            isSaved = true,
            savedNetworkId = 1,
            timestampMs = System.currentTimeMillis()
        )

        val context = EvaluationContext(
            currentTimeMs = 200_000L,
            lastSwitchTimeMs = 100_000L
        )

        val decision = engine.evaluate(
            currentState = current,
            availableNetworks = listOf(candidateScanned),
            savedNetworks = listOf(candidateSaved),
            rules = emptyList(),
            settings = settings,
            context = context
        )

        assertEquals(DecisionAction.StayOnCurrent, decision.action)
    }

    // Scenario C: -45 dBm strong, no internet for only 5s (< 15s timeout) -> Stay (grace period)
    @Test
    fun testScenarioC_internetUnavailableGracePeriod_staysOnCurrent() {
        val current = WifiState.Connected(
            ssid = "CurrentNet",
            bssid = "00:11:22:33:44:55",
            rssi = -45,
            frequencyMhz = 5180,
            internetStatus = InternetStatus.UNAVAILABLE,
            connectedSinceMs = 100_000L
        )

        val candidateSaved = WifiNetwork(
            id = 1,
            ssid = "OtherNet",
            securityType = SecurityType.WPA2_PSK,
            priority = 50,
            minimumSignalDbm = -70,
            minimumImprovementDbm = 10
        )

        val candidateScanned = ScannedNetwork(
            ssid = "OtherNet",
            bssid = "aa:bb:cc:dd:ee:01",
            rssi = -60,
            frequencyMhz = 5180,
            capabilities = "WPA2",
            securityType = SecurityType.WPA2_PSK,
            isSaved = true,
            savedNetworkId = 1,
            timestampMs = System.currentTimeMillis()
        )

        val context = EvaluationContext(
            currentTimeMs = 200_000L,
            lastSwitchTimeMs = 100_000L,
            internetUnavailableSinceMs = 195_000L // only 5s ago < 15s timeout
        )

        val decision = engine.evaluate(
            currentState = current,
            availableNetworks = listOf(candidateScanned),
            savedNetworks = listOf(candidateSaved),
            rules = emptyList(),
            settings = settings,
            context = context
        )

        assertEquals(DecisionAction.StayOnCurrent, decision.action)
    }

    // Scenario D: -75 dBm -> -73 dBm candidate (only 2 dBm improvement) -> Stay
    @Test
    fun testScenarioD_marginalImprovement_staysOnCurrent() {
        val current = WifiState.Connected(
            ssid = "CurrentNet",
            bssid = "00:11:22:33:44:55",
            rssi = -75,
            frequencyMhz = 2412,
            internetStatus = InternetStatus.AVAILABLE,
            connectedSinceMs = 100_000L
        )

        val candidateSaved = WifiNetwork(
            id = 1,
            ssid = "CandidateNet",
            securityType = SecurityType.WPA2_PSK,
            priority = 50,
            minimumImprovementDbm = 10
        )

        val candidateScanned = ScannedNetwork(
            ssid = "CandidateNet",
            bssid = "aa:bb:cc:dd:ee:01",
            rssi = -73,
            frequencyMhz = 2412,
            capabilities = "WPA2",
            securityType = SecurityType.WPA2_PSK,
            isSaved = true,
            savedNetworkId = 1,
            timestampMs = System.currentTimeMillis()
        )

        val decision = engine.evaluate(
            currentState = current,
            availableNetworks = listOf(candidateScanned),
            savedNetworks = listOf(candidateSaved),
            rules = emptyList(),
            settings = settings,
            context = EvaluationContext(currentTimeMs = 200_000L, lastSwitchTimeMs = 100_000L)
        )

        assertEquals(DecisionAction.StayOnCurrent, decision.action)
    }

    // Scenario E: Cooldown 30s remaining -> CooldownActive
    @Test
    fun testScenarioE_cooldownActive_blocksSwitch() {
        val current = WifiState.Connected(
            ssid = "CurrentNet",
            bssid = "00:11:22:33:44:55",
            rssi = -80,
            frequencyMhz = 2412,
            internetStatus = InternetStatus.AVAILABLE,
            connectedSinceMs = 100_000L
        )

        val candidateSaved = WifiNetwork(
            id = 1,
            ssid = "GreatNet",
            securityType = SecurityType.WPA2_PSK,
            priority = 90
        )

        val candidateScanned = ScannedNetwork(
            ssid = "GreatNet",
            bssid = "aa:bb:cc:dd:ee:01",
            rssi = -50,
            frequencyMhz = 5180,
            capabilities = "WPA2",
            securityType = SecurityType.WPA2_PSK,
            isSaved = true,
            savedNetworkId = 1,
            timestampMs = System.currentTimeMillis()
        )

        val context = EvaluationContext(
            currentTimeMs = 130_000L,
            lastSwitchTimeMs = 100_000L // 30s elapsed < 60s cooldown
        )

        val decision = engine.evaluate(
            currentState = current,
            availableNetworks = listOf(candidateScanned),
            savedNetworks = listOf(candidateSaved),
            rules = emptyList(),
            settings = settings,
            context = context
        )

        assertEquals(DecisionAction.CooldownActive, decision.action)
    }

    // Scenario F: No candidates visible -> StayOnCurrent / NoCandidates
    @Test
    fun testScenarioF_noCandidatesVisible_reportsNoCandidates() {
        val current = WifiState.Connected(
            ssid = "CurrentNet",
            bssid = "00:11:22:33:44:55",
            rssi = -70,
            frequencyMhz = 2412,
            internetStatus = InternetStatus.AVAILABLE,
            connectedSinceMs = 100_000L
        )

        val decision = engine.evaluate(
            currentState = current,
            availableNetworks = emptyList(),
            savedNetworks = listOf(WifiNetwork(id = 1, ssid = "UnreachableNet", securityType = SecurityType.WPA2_PSK, priority = 80)),
            rules = emptyList(),
            settings = settings,
            context = EvaluationContext(currentTimeMs = 200_000L, lastSwitchTimeMs = 100_000L)
        )

        assertEquals(DecisionAction.StayOnCurrent, decision.action)
    }

    // Scenario G: Candidate 1 (-52 dBm, pri=20) vs Candidate 2 (-60 dBm, pri=80) -> Candidate 2 chosen (higher priority)
    @Test
    fun testScenarioG_prefersHigherPriorityOverMarginalRssi() {
        val current = WifiState.Connected(
            ssid = "CurrentNet",
            bssid = "00:11:22:33:44:55",
            rssi = -78,
            frequencyMhz = 2412,
            internetStatus = InternetStatus.AVAILABLE,
            connectedSinceMs = 100_000L
        )

        val net1 = WifiNetwork(id = 1, ssid = "NetLowPri", securityType = SecurityType.WPA2_PSK, priority = 20, minimumImprovementDbm = 10)
        val scan1 = ScannedNetwork(ssid = "NetLowPri", bssid = "11:11:11:11:11:11", rssi = -52, frequencyMhz = 5180, capabilities = "WPA2", securityType = SecurityType.WPA2_PSK, isSaved = true, savedNetworkId = 1, timestampMs = 0L)

        val net2 = WifiNetwork(id = 2, ssid = "NetHighPri", securityType = SecurityType.WPA2_PSK, priority = 80, minimumImprovementDbm = 10)
        val scan2 = ScannedNetwork(ssid = "NetHighPri", bssid = "22:22:22:22:22:22", rssi = -60, frequencyMhz = 5180, capabilities = "WPA2", securityType = SecurityType.WPA2_PSK, isSaved = true, savedNetworkId = 2, timestampMs = 0L)

        val context = EvaluationContext(
            currentTimeMs = 200_000L,
            lastSwitchTimeMs = 100_000L
        )

        val decision = engine.evaluate(
            currentState = current,
            availableNetworks = listOf(scan1, scan2),
            savedNetworks = listOf(net1, net2),
            rules = emptyList(),
            settings = settings,
            context = context
        )

        assertTrue(decision.action is DecisionAction.SuggestNetwork)
        assertEquals("NetHighPri", (decision.action as DecisionAction.SuggestNetwork).network.ssid)
    }
}
