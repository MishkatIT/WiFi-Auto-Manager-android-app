package com.example.wifiautomanager.ui.diagnostics

import androidx.lifecycle.SavedStateHandle
import com.example.wifiautomanager.domain.model.CandidateResult
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.InternetStatus
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.domain.repository.DecisionRepository
import com.example.wifiautomanager.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FakeDecisionRepoWithGet : DecisionRepository {
    private val decisions = mutableListOf<Decision>()
    private val _flow = MutableStateFlow<List<Decision>>(emptyList())

    override fun getRecentDecisions(limit: Int): Flow<List<Decision>> = _flow.asStateFlow()
    override suspend fun getDecisionById(id: Long): Decision? = decisions.find { it.id == id }

    override suspend fun recordDecision(decision: Decision): Long {
        val id = if (decision.id == 0L) (decisions.maxOfOrNull { it.id } ?: 0L) + 1 else decision.id
        val newDecision = decision.copy(id = id)
        decisions.add(newDecision)
        _flow.value = decisions.toList()
        return id
    }

    override suspend fun pruneOldDecisions(retentionDays: Int) {}
    override suspend fun clearAll() {
        decisions.clear()
        _flow.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DecisionDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeDecisionRepo: FakeDecisionRepoWithGet

    @Before
    fun setup() {
        fakeDecisionRepo = FakeDecisionRepoWithGet()
    }

    @Test
    fun testLoadDecisionSuccessfully() = runTest {
        val target = WifiNetwork(
            id = 5L,
            ssid = "Campus_HighSpeed",
            securityType = SecurityType.WPA2_PSK,
            priority = 90
        )
        val decision = Decision(
            id = 42L,
            timestampMs = 1700000000000L,
            action = DecisionAction.SuggestNetwork(target),
            selectedNetwork = target,
            reason = "Significantly stronger signal (+18 dBm improvement)",
            evaluatedCandidates = listOf(
                CandidateResult(
                    network = target,
                    scannedNetwork = null,
                    qualified = true,
                    rejectionReason = null,
                    signalImprovement = 18
                )
            ),
            currentState = WifiState.Connected(
                ssid = "Slow_Wifi",
                bssid = "00:11:22:33:44:55",
                rssi = -82,
                frequencyMhz = 2412,
                internetStatus = InternetStatus.AVAILABLE,
                connectedSinceMs = 120_000L
            )
        )
        fakeDecisionRepo.recordDecision(decision)

        val handle = SavedStateHandle(mapOf("id" to 42L))
        val viewModel = DecisionDetailViewModel(fakeDecisionRepo, handle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.decision)
        assertEquals("Recommend Switch to Campus_HighSpeed", state.actionTitle)
        assertEquals("SWITCH_RECOMMENDED", state.actionTypeBadge)
        assertTrue(state.currentNetworkSummary.contains("Slow_Wifi"))
        assertEquals(1, state.candidates.size)
        assertTrue(state.whyNotSwitchExplanation.contains("Campus_HighSpeed"))
    }

    @Test
    fun testNonExistentDecisionHandlesGracefully() = runTest {
        val handle = SavedStateHandle(mapOf("id" to 999L))
        val viewModel = DecisionDetailViewModel(fakeDecisionRepo, handle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.decision)
    }

    @Test
    fun testStayOnCurrentExplainsCandidateRejection() = runTest {
        val candidate = WifiNetwork(
            id = 2L,
            ssid = "Weak_Guest",
            securityType = SecurityType.OPEN,
            priority = 30
        )
        val decision = Decision(
            id = 10L,
            action = DecisionAction.StayOnCurrent,
            selectedNetwork = null,
            reason = "Current network is optimal",
            evaluatedCandidates = listOf(
                CandidateResult(
                    network = candidate,
                    scannedNetwork = null,
                    qualified = false,
                    rejectionReason = "Signal below minimum threshold (-75 dBm)",
                    signalImprovement = null
                )
            ),
            currentState = WifiState.Connected("Home_Net", "aa:bb:cc:dd", -65, 5180, InternetStatus.AVAILABLE, 60000L)
        )
        fakeDecisionRepo.recordDecision(decision)

        val handle = SavedStateHandle(mapOf("id" to 10L))
        val viewModel = DecisionDetailViewModel(fakeDecisionRepo, handle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Stay on Current Network", state.actionTitle)
        assertEquals("STAY", state.actionTypeBadge)
        assertTrue(state.whyNotSwitchExplanation.contains("Signal below minimum threshold"))
    }
}
