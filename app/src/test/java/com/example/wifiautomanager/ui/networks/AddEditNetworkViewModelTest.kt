package com.example.wifiautomanager.ui.networks

import androidx.lifecycle.SavedStateHandle
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.usecase.ManageSavedNetworksUseCase
import com.example.wifiautomanager.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditNetworkViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeWifiRepository: FakeWifiRepository
    private lateinit var fakeCredentialRepository: FakeCredentialRepository
    private lateinit var manageUseCase: ManageSavedNetworksUseCase

    @Before
    fun setup() {
        fakeWifiRepository = FakeWifiRepository()
        fakeCredentialRepository = FakeCredentialRepository()
        manageUseCase = ManageSavedNetworksUseCase(fakeWifiRepository, fakeCredentialRepository)
    }

    @Test
    fun testEmptySsidShowsValidationError() = runTest {
        val viewModel = AddEditNetworkViewModel(manageUseCase, SavedStateHandle())
        viewModel.onSsidChanged("")
        viewModel.onSave("password123")

        val state = viewModel.uiState.value
        assertNotNull(state.ssidError)
    }

    @Test
    fun testEmptyPasswordForSecuredNetworkShowsValidationError() = runTest {
        val viewModel = AddEditNetworkViewModel(manageUseCase, SavedStateHandle())
        viewModel.onSsidChanged("ValidSSID")
        viewModel.onSecurityTypeChanged(SecurityType.WPA2_PSK)
        viewModel.onSave("")

        val state = viewModel.uiState.value
        assertNotNull(state.passwordError)
    }

    @Test
    fun testShortPasswordShowsValidationError() = runTest {
        val viewModel = AddEditNetworkViewModel(manageUseCase, SavedStateHandle())
        viewModel.onSsidChanged("ValidSSID")
        viewModel.onSecurityTypeChanged(SecurityType.WPA2_PSK)
        viewModel.onSave("123")

        val state = viewModel.uiState.value
        assertEquals("Password must be at least 8 characters", state.passwordError)
    }

    @Test
    fun testSaveNewSecuredNetworkPersistsNetworkAndKeystorePassword() = runTest {
        val viewModel = AddEditNetworkViewModel(manageUseCase, SavedStateHandle())
        viewModel.onSsidChanged("OfficeNet")
        viewModel.onSecurityTypeChanged(SecurityType.WPA2_PSK)
        viewModel.onPriorityChanged(85)
        viewModel.onMinimumSignalChanged(-65)
        viewModel.onRequiresInternetChanged(true)

        viewModel.onSave("SecretPass123")
        advanceUntilIdle()

        val savedList = fakeWifiRepository.getAllNetworks().first()
        assertEquals(1, savedList.size)
        val savedNet = savedList.first()
        assertEquals("OfficeNet", savedNet.ssid)
        assertEquals(85, savedNet.priority)
        assertEquals(-65, savedNet.minimumSignalDbm)
        assertTrue(savedNet.requiresInternet)

        // Verify password in credential store
        assertTrue(fakeCredentialRepository.hasPassword(savedNet.id))
        assertEquals("SecretPass123", fakeCredentialRepository.getPassword(savedNet.id))
    }

    @Test
    fun testSaveOpenNetworkDoesNotRequirePassword() = runTest {
        val viewModel = AddEditNetworkViewModel(manageUseCase, SavedStateHandle())
        viewModel.onSsidChanged("AirportFree")
        viewModel.onSecurityTypeChanged(SecurityType.OPEN)
        viewModel.onSave("")
        advanceUntilIdle()

        val savedList = fakeWifiRepository.getAllNetworks().first()
        assertEquals(1, savedList.size)
        val savedNet = savedList.first()
        assertEquals("AirportFree", savedNet.ssid)
        assertEquals(SecurityType.OPEN, savedNet.securityType)
        assertFalse(fakeCredentialRepository.hasPassword(savedNet.id))
    }

    @Test
    fun testLoadExistingNetworkForEditing() = runTest {
        val existingId = fakeWifiRepository.insertNetwork(
            WifiNetwork(
                id = 42,
                ssid = "ExistingWiFi",
                securityType = SecurityType.WPA3_SAE,
                priority = 90,
                minimumSignalDbm = -60,
                requiresInternet = false,
                enabled = true
            )
        )
        fakeCredentialRepository.savePassword(existingId, "ExistingPass")

        val handle = SavedStateHandle(mapOf("id" to existingId))
        val viewModel = AddEditNetworkViewModel(manageUseCase, handle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditMode)
        assertEquals("ExistingWiFi", state.ssid)
        assertEquals(SecurityType.WPA3_SAE, state.securityType)
        assertEquals(90, state.priority)
        assertTrue(state.hasStoredPassword)
    }

    @Test
    fun testEditNetworkPreservesPasswordWhenLeftBlank() = runTest {
        val existingId = fakeWifiRepository.insertNetwork(
            WifiNetwork(
                id = 10,
                ssid = "HomeWiFi",
                securityType = SecurityType.WPA2_PSK,
                priority = 50
            )
        )
        fakeCredentialRepository.savePassword(existingId, "OriginalPass")

        val handle = SavedStateHandle(mapOf("id" to existingId))
        val viewModel = AddEditNetworkViewModel(manageUseCase, handle)
        advanceUntilIdle()

        viewModel.onPriorityChanged(95)
        viewModel.onSave("") // Leave blank
        advanceUntilIdle()

        val updated = fakeWifiRepository.getNetworkById(existingId)
        assertEquals(95, updated?.priority)
        assertEquals("OriginalPass", fakeCredentialRepository.getPassword(existingId))
    }
}
