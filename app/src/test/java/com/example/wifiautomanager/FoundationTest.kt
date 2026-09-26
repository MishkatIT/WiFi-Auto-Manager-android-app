package com.example.wifiautomanager

import com.example.wifiautomanager.domain.model.AppSettings
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.model.RuleAction
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.domain.rule.Condition
import com.example.wifiautomanager.domain.rule.ConditionGroup
import com.example.wifiautomanager.domain.rule.LogicalOperator
import com.example.wifiautomanager.ui.navigation.BottomNavItem
import com.example.wifiautomanager.ui.navigation.Screen
import com.example.wifiautomanager.util.SignalLevelMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoundationTest {

    @Test
    fun testDomainModelsInstantiation() {
        val network = WifiNetwork(
            id = 1L,
            ssid = "TestWiFi",
            securityType = SecurityType.WPA2_PSK,
            priority = 5
        )

        assertEquals("TestWiFi", network.ssid)
        assertEquals(SecurityType.WPA2_PSK, network.securityType)
        assertEquals(5, network.priority)

        val settings = AppSettings()
        assertFalse(settings.autoManagerEnabled)
        assertEquals(30, settings.scanIntervalSeconds)

        val state: WifiState = WifiState.Disabled
        assertEquals(WifiState.Disabled, state)
    }

    @Test
    fun testRuleAndDecisionModels() {
        val rule = Rule(
            id = 1L,
            name = "Weak Signal Switch",
            rootGroup = ConditionGroup(
                operator = LogicalOperator.AND,
                conditions = emptyList()
            ),
            action = RuleAction.PreferBestCandidate
        )
        assertEquals("Weak Signal Switch", rule.name)
        assertTrue(rule.enabled)

        val decision = Decision(
            id = 10L,
            action = DecisionAction.StayOnCurrent,
            selectedNetwork = null,
            reason = "Current connection is optimal",
            currentState = WifiState.Disconnected
        )
        assertEquals(DecisionAction.StayOnCurrent, decision.action)
        assertEquals("Current connection is optimal", decision.reason)
    }

    @Test
    fun testNavigationScreensAndTabs() {
        // Top-level 3 tabs
        assertEquals("dashboard", Screen.Dashboard.route)
        assertEquals("networks", Screen.Networks.route)
        assertEquals("more", Screen.More.route)

        // Sub-screens
        assertEquals("nearby", Screen.Nearby.route)
        assertEquals("rules", Screen.Rules.route)
        assertEquals("diagnostics", Screen.Diagnostics.route)
        assertEquals("settings", Screen.Settings.route)

        // Bottom nav items
        val bottomNavTabs = listOf(
            BottomNavItem.Dashboard,
            BottomNavItem.Networks,
            BottomNavItem.More
        )
        assertEquals(3, bottomNavTabs.size)
        assertEquals("Dashboard", bottomNavTabs[0].title)
        assertEquals("Networks", bottomNavTabs[1].title)
        assertEquals("More", bottomNavTabs[2].title)
    }

    @Test
    fun testSignalLevelMapper() {
        assertEquals("Excellent", SignalLevelMapper.getSignalCategory(-55))
        assertEquals("Good", SignalLevelMapper.getSignalCategory(-68))
        assertEquals("Fair", SignalLevelMapper.getSignalCategory(-75))
        assertEquals("Weak", SignalLevelMapper.getSignalCategory(-85))
    }
}
