package com.example.wifiautomanager.data

import android.content.Context
import androidx.room.Room
import com.example.wifiautomanager.data.local.db.AppDatabase
import com.example.wifiautomanager.data.local.db.dao.DecisionLogDao
import com.example.wifiautomanager.data.local.db.dao.RuleDao
import com.example.wifiautomanager.data.local.db.dao.WifiNetworkDao
import com.example.wifiautomanager.data.local.db.entity.DecisionLogEntity
import com.example.wifiautomanager.data.local.db.entity.RuleEntity
import com.example.wifiautomanager.data.local.db.entity.WifiNetworkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class RoomDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var wifiNetworkDao: WifiNetworkDao
    private lateinit var ruleDao: RuleDao
    private lateinit var decisionLogDao: DecisionLogDao

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        wifiNetworkDao = database.wifiNetworkDao()
        ruleDao = database.ruleDao()
        decisionLogDao = database.decisionLogDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun testWifiNetworkDaoCrudAndOrdering() = runTest {
        val net1 = WifiNetworkEntity(
            ssid = "HomeWiFi",
            securityType = "WPA2_PSK",
            enabled = true,
            priority = 10,
            minimumSignalDbm = -70
        )
        val net2 = WifiNetworkEntity(
            ssid = "OfficeWiFi",
            securityType = "WPA3_SAE",
            enabled = true,
            priority = 50,
            minimumSignalDbm = -65
        )
        val net3 = WifiNetworkEntity(
            ssid = "DisabledWiFi",
            securityType = "OPEN",
            enabled = false,
            priority = 30,
            minimumSignalDbm = -80
        )

        val id1 = wifiNetworkDao.insertNetwork(net1)
        val id2 = wifiNetworkDao.insertNetwork(net2)
        val id3 = wifiNetworkDao.insertNetwork(net3)

        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
        assertTrue(id3 > 0)

        // Verify retrieval by ID
        val retrieved = wifiNetworkDao.getNetworkById(id1)
        assertNotNull(retrieved)
        assertEquals("HomeWiFi", retrieved?.ssid)
        assertEquals(10, retrieved?.priority)

        // Verify retrieval by SSID
        val retrievedBySsid = wifiNetworkDao.getNetworkBySsid("OfficeWiFi")
        assertNotNull(retrievedBySsid)
        assertEquals(id2, retrievedBySsid?.id)

        // Verify getAllNetworks orders by priority DESC (Office=50, Disabled=30, Home=10)
        val all = wifiNetworkDao.getAllNetworks().first()
        assertEquals(3, all.size)
        assertEquals("OfficeWiFi", all[0].ssid)
        assertEquals("DisabledWiFi", all[1].ssid)
        assertEquals("HomeWiFi", all[2].ssid)

        // Verify getEnabledNetworks excludes net3 (enabled=false)
        val enabledOnly = wifiNetworkDao.getEnabledNetworks().first()
        assertEquals(2, enabledOnly.size)
        assertEquals("OfficeWiFi", enabledOnly[0].ssid)
        assertEquals("HomeWiFi", enabledOnly[1].ssid)

        // Update network
        val updated = retrieved!!.copy(priority = 90)
        wifiNetworkDao.updateNetwork(updated)
        val afterUpdate = wifiNetworkDao.getNetworkById(id1)
        assertEquals(90, afterUpdate?.priority)

        // Delete network
        wifiNetworkDao.deleteNetworkById(id2)
        assertNull(wifiNetworkDao.getNetworkById(id2))
        assertEquals(2, wifiNetworkDao.getNetworkCount())
    }

    @Test
    fun testRuleDaoCrud() = runTest {
        val rule1 = RuleEntity(
            name = "Low Signal Rule",
            enabled = true,
            priority = 20,
            conditionGroupJson = "{}",
            actionJson = "{\"type\":\"PreferBestCandidate\"}"
        )
        val rule2 = RuleEntity(
            name = "Disabled Rule",
            enabled = false,
            priority = 5,
            conditionGroupJson = "{}",
            actionJson = "{\"type\":\"StayOnCurrent\"}"
        )

        val rId1 = ruleDao.insertRule(rule1)
        val rId2 = ruleDao.insertRule(rule2)

        val fetchedRule = ruleDao.getRuleById(rId1)
        assertNotNull(fetchedRule)
        assertEquals("Low Signal Rule", fetchedRule?.name)

        val allRules = ruleDao.getAllRules().first()
        assertEquals(2, allRules.size)
        assertEquals("Low Signal Rule", allRules[0].name)

        val enabledRules = ruleDao.getEnabledRules().first()
        assertEquals(1, enabledRules.size)
        assertEquals("Low Signal Rule", enabledRules[0].name)

        ruleDao.deleteRuleById(rId1)
        assertNull(ruleDao.getRuleById(rId1))
        assertEquals(1, ruleDao.getRuleCount())
    }

    @Test
    fun testDecisionLogDaoAndCleanup() = runTest {
        val now = System.currentTimeMillis()
        val log1 = DecisionLogEntity(
            timestampMs = now - 100_000,
            actionType = "StayOnCurrent",
            reason = "Signal is adequate",
            detailJson = "{\"candidates\": 1}"
        )
        val log2 = DecisionLogEntity(
            timestampMs = now - 10_000,
            actionType = "SuggestNetwork",
            selectedNetworkId = 2L,
            reason = "Better network found",
            detailJson = "{\"candidates\": 3}"
        )

        decisionLogDao.insertLog(log1)
        decisionLogDao.insertLog(log2)

        val logs = decisionLogDao.getAllLogs().first()
        assertEquals(2, logs.size)
        // Log2 is newer so should appear first
        assertEquals("SuggestNetwork", logs[0].actionType)
        assertEquals("StayOnCurrent", logs[1].actionType)

        val recent = decisionLogDao.getRecentLogs(1).first()
        assertEquals(1, recent.size)
        assertEquals("SuggestNetwork", recent[0].actionType)

        // Delete logs older than now - 50_000
        val deletedCount = decisionLogDao.deleteLogsOlderThan(now - 50_000)
        assertEquals(1, deletedCount)
        assertEquals(1, decisionLogDao.getLogCount())

        // Clear all
        decisionLogDao.clearAllLogs()
        assertEquals(0, decisionLogDao.getLogCount())
    }
}
