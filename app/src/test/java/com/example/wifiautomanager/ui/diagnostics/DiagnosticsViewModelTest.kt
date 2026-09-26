package com.example.wifiautomanager.ui.diagnostics

import com.example.wifiautomanager.domain.model.AppSettings
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.repository.DecisionRepository
import com.example.wifiautomanager.notification.FakeSettingsRepo
import com.example.wifiautomanager.ui.rules.FakeWifiRepositoryForRules
import com.example.wifiautomanager.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FakeDecisionRepo : DecisionRepository {
    private val decisions = mutableListOf<Decision>()
    private val _flow = MutableStateFlow<List<Decision>>(emptyList())

    override fun getRecentDecisions(limit: Int): Flow<List<Decision>> = _flow.asStateFlow()
    override suspend fun getDecisionById(id: Long): Decision? = decisions.find { it.id == id }
    override suspend fun recordDecision(decision: Decision): Long {
        decisions.add(decision)
        _flow.value = decisions.toList()
        return decisions.size.toLong()
    }
    override suspend fun pruneOldDecisions(retentionDays: Int) {}
    override suspend fun clearAll() {
        decisions.clear()
        _flow.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeWifiRepo: FakeWifiRepositoryForRules
    private lateinit var fakeDecisionRepo: FakeDecisionRepo
    private lateinit var fakeSettingsRepo: FakeSettingsRepo
    private lateinit var viewModel: DiagnosticsViewModel

    @Before
    fun setup() {
        fakeWifiRepo = FakeWifiRepositoryForRules()
        fakeDecisionRepo = FakeDecisionRepo()
        fakeSettingsRepo = FakeSettingsRepo(AppSettings(autoManagerEnabled = true))
        viewModel = DiagnosticsViewModel(fakeWifiRepo, fakeDecisionRepo, fakeSettingsRepo, null)
    }

    @Test
    fun testInitialDiagnosticsState() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.totalSavedNetworks)
        assertEquals(0, state.recentDecisions.size)
        assertTrue(state.autoManagerEnabled)
    }

    @Test
    fun testObservesSavedNetworksAndDecisions() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        fakeWifiRepo.insertNetwork(
            WifiNetwork(id = 1L, ssid = "CampusNet", securityType = SecurityType.OPEN, priority = 50)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.totalSavedNetworks)
    }
}
