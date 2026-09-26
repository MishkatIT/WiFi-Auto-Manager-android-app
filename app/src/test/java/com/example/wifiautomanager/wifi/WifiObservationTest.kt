package com.example.wifiautomanager.wifi

import com.example.wifiautomanager.domain.model.ScannedNetwork
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.util.SignalLevelMapper
import com.example.wifiautomanager.util.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiObservationTest {

    @Test
    fun testSignalLevelMapperCategories() {
        assertEquals("Excellent", SignalLevelMapper.getSignalCategory(-50))
        assertEquals("Excellent", SignalLevelMapper.getSignalCategory(-60))
        assertEquals("Good", SignalLevelMapper.getSignalCategory(-65))
        assertEquals("Good", SignalLevelMapper.getSignalCategory(-70))
        assertEquals("Fair", SignalLevelMapper.getSignalCategory(-75))
        assertEquals("Fair", SignalLevelMapper.getSignalCategory(-80))
        assertEquals("Weak", SignalLevelMapper.getSignalCategory(-85))
    }

    @Test
    fun testSignalBars() {
        assertEquals(4, SignalLevelMapper.getBars(-50))
        assertEquals(3, SignalLevelMapper.getBars(-65))
        assertEquals(2, SignalLevelMapper.getBars(-75))
        assertEquals(1, SignalLevelMapper.getBars(-85))
        assertEquals(0, SignalLevelMapper.getBars(-95))
    }

    @Test
    fun testTimeFormatterDuration() {
        val now = System.currentTimeMillis()
        assertEquals("Just now", TimeFormatter.formatDuration(0L))
        assertEquals("Just now", TimeFormatter.formatDuration(now))
        assertEquals("45s", TimeFormatter.formatDuration(now - 45_000L))
        assertEquals("15m", TimeFormatter.formatDuration(now - 15 * 60_000L))
        assertEquals("2h 10m", TimeFormatter.formatDuration(now - (2 * 3600_000L + 10 * 60_000L)))
    }

    @Test
    fun testScannedNetworkModelProperties() {
        val network = ScannedNetwork(
            ssid = "Campus_Guest",
            bssid = "aa:bb:cc:dd:ee:ff",
            rssi = -68,
            frequencyMhz = 5240,
            capabilities = "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]",
            securityType = SecurityType.WPA2_PSK,
            isSaved = true,
            savedNetworkId = 5L,
            timestampMs = 123456789L
        )

        assertEquals("Campus_Guest", network.ssid)
        assertEquals(-68, network.rssi)
        assertTrue(network.isSaved)
        assertEquals(5L, network.savedNetworkId)
        assertEquals(SecurityType.WPA2_PSK, network.securityType)
    }
}
