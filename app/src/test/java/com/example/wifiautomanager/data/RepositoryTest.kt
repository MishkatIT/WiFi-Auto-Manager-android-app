package com.example.wifiautomanager.data

import android.content.Context
import androidx.room.Room
import com.example.wifiautomanager.data.local.db.AppDatabase
import com.example.wifiautomanager.data.repository.WifiRepositoryImpl
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.WifiNetwork
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class RepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var wifiRepository: WifiRepositoryImpl

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        wifiRepository = WifiRepositoryImpl(database.wifiNetworkDao())
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun testWifiRepositorySavesAndRetrievesDomainModel() = runTest {
        val domainNetwork = WifiNetwork(
            ssid = "CampusWiFi",
            securityType = SecurityType.WPA2_PSK,
            enabled = true,
            priority = 75,
            minimumSignalDbm = -68,
            requiresInternet = true,
            minimumImprovementDbm = 15
        )

        val id = wifiRepository.insertNetwork(domainNetwork)
        val retrieved = wifiRepository.getNetworkById(id)

        assertNotNull(retrieved)
        assertEquals("CampusWiFi", retrieved?.ssid)
        assertEquals(SecurityType.WPA2_PSK, retrieved?.securityType)
        assertEquals(75, retrieved?.priority)
        assertEquals(true, retrieved?.requiresInternet)
        assertEquals(15, retrieved?.minimumImprovementDbm)

        val all = wifiRepository.getAllNetworks().first()
        assertEquals(1, all.size)
        assertEquals("CampusWiFi", all[0].ssid)

        // Delete network
        wifiRepository.deleteNetwork(retrieved!!)
        assertNull(wifiRepository.getNetworkById(id))
    }
}
