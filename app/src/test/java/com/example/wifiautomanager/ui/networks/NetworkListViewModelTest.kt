package com.example.wifiautomanager.ui.networks

import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.repository.CredentialRepository
import com.example.wifiautomanager.domain.repository.WifiRepository
import com.example.wifiautomanager.domain.usecase.ManageSavedNetworksUseCase
import com.example.wifiautomanager.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeWifiRepository: FakeWifiRepository
    private lateinit var fakeCredentialRepository: FakeCredentialRepository
    private lateinit var manageUseCase: ManageSavedNetworksUseCase
    private lateinit var viewModel: NetworkListViewModel

    @Before
    fun setup() {
        fakeWifiRepository = FakeWifiRepository()
        fakeCredentialRepository = FakeCredentialRepository()
        manageUseCase = ManageSavedNetworksUseCase(fakeWifiRepository, fakeCredentialRepository)
        viewModel = NetworkListViewModel(manageUseCase)
    }

    @Test
    fun testInitialEmptyState() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val state = viewModel.uiState.value
        assertTrue(state.networks.isEmpty())
    }

    @Test
    fun testNetworksLoadedAndSortedByPriority() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        fakeWifiRepository.insertNetwork(
            WifiNetwork(id = 1, ssid = "NetLow", securityType = SecurityType.OPEN, priority = 10)
        )
        fakeWifiRepository.insertNetwork(
            WifiNetwork(id = 2, ssid = "NetHigh", securityType = SecurityType.WPA2_PSK, priority = 90)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.networks.size)
        assertEquals("NetHigh", state.networks[0].ssid)
        assertEquals("NetLow", state.networks[1].ssid)
    }

    @Test
    fun testToggleNetworkEnabled() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        fakeWifiRepository.insertNetwork(
            WifiNetwork(id = 1, ssid = "HomeWiFi", securityType = SecurityType.WPA2_PSK, priority = 50, enabled = true)
        )
        advanceUntilIdle()

        viewModel.onToggleNetwork(1L, false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.networks.first { it.id == 1L }.enabled)
    }

    @Test
    fun testDeleteNetwork() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        fakeWifiRepository.insertNetwork(
            WifiNetwork(id = 1, ssid = "HomeWiFi", securityType = SecurityType.WPA2_PSK, priority = 50)
        )
        fakeCredentialRepository.savePassword(1L, "supersecret")
        advanceUntilIdle()

        viewModel.onDeleteNetwork(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.networks.isEmpty())
        assertFalse(fakeCredentialRepository.hasPassword(1L))
    }
}

class FakeWifiRepository : WifiRepository {
    private val networks = mutableListOf<WifiNetwork>()
    private val flow = MutableStateFlow<List<WifiNetwork>>(emptyList())
    private var nextId = 1L

    override fun getAllNetworks(): Flow<List<WifiNetwork>> = flow

    override fun getEnabledNetworks(): Flow<List<WifiNetwork>> =
        flow.map { list -> list.filter { it.enabled } }

    override suspend fun getNetworkById(id: Long): WifiNetwork? =
        networks.find { it.id == id }

    override suspend fun getNetworkBySsid(ssid: String): WifiNetwork? =
        networks.find { it.ssid == ssid }

    override suspend fun insertNetwork(network: WifiNetwork): Long {
        val id = if (network.id == 0L) nextId++ else network.id
        val newNet = network.copy(id = id)
        networks.removeAll { it.id == id }
        networks.add(newNet)
        flow.value = networks.sortedByDescending { it.priority }
        return id
    }

    override suspend fun updateNetwork(network: WifiNetwork) {
        val index = networks.indexOfFirst { it.id == network.id }
        if (index >= 0) {
            networks[index] = network
            flow.value = networks.sortedByDescending { it.priority }
        }
    }

    override suspend fun deleteNetwork(network: WifiNetwork) {
        networks.removeAll { it.id == network.id }
        flow.value = networks.sortedByDescending { it.priority }
    }
}

class FakeCredentialRepository : CredentialRepository {
    private val passwords = mutableMapOf<Long, String>()

    override fun savePassword(networkId: Long, password: String) {
        passwords[networkId] = password
    }

    override fun getPassword(networkId: Long): String? = passwords[networkId]

    override fun deletePassword(networkId: Long) {
        passwords.remove(networkId)
    }

    override fun hasPassword(networkId: Long): Boolean = passwords.containsKey(networkId)

    override fun clearAll() {
        passwords.clear()
    }
}
