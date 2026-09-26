package com.example.wifiautomanager.domain.decision

import com.example.wifiautomanager.domain.model.InternetStatus
import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.model.RuleAction
import com.example.wifiautomanager.domain.model.ScannedNetwork
import com.example.wifiautomanager.domain.model.SecurityType
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.domain.rule.Condition
import com.example.wifiautomanager.domain.rule.ConditionEvaluator
import com.example.wifiautomanager.domain.rule.ConditionGroup
import com.example.wifiautomanager.domain.rule.ConditionNode
import com.example.wifiautomanager.domain.rule.LogicalOperator
import com.example.wifiautomanager.domain.rule.RuleEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleAndConditionTest {

    private val conditionEvaluator = ConditionEvaluator()
    private val ruleEvaluator = RuleEvaluator(conditionEvaluator)
    private val antiFlappingGuard = AntiFlappingGuard()

    @Test
    fun testSignalConditions() {
        val connected = WifiState.Connected(
            ssid = "OfficeNet",
            bssid = "00:11:22:33:44:55",
            rssi = -72,
            frequencyMhz = 5180,
            internetStatus = InternetStatus.AVAILABLE,
            connectedSinceMs = 0L
        )
        val context = EvaluationContext()

        // CurrentSignalBelow
        assertTrue(conditionEvaluator.evaluate(Condition.CurrentSignalBelow(-70), context, connected))
        assertFalse(conditionEvaluator.evaluate(Condition.CurrentSignalBelow(-75), context, connected))

        // CurrentSignalAbove
        assertTrue(conditionEvaluator.evaluate(Condition.CurrentSignalAbove(-75), context, connected))
        assertFalse(conditionEvaluator.evaluate(Condition.CurrentSignalAbove(-70), context, connected))
    }

    @Test
    fun testInternetConditions() {
        val withInternet = WifiState.Connected(
            ssid = "OfficeNet",
            bssid = "00:11:22:33:44:55",
            rssi = -60,
            frequencyMhz = 5180,
            internetStatus = InternetStatus.AVAILABLE,
            connectedSinceMs = 0L
        )
        val withoutInternet = withInternet.copy(internetStatus = InternetStatus.UNAVAILABLE)

        val context = EvaluationContext(
            currentTimeMs = 100_000L,
            internetUnavailableSinceMs = 80_000L // 20s
        )

        assertTrue(conditionEvaluator.evaluate(Condition.InternetAvailable, context, withInternet))
        assertFalse(conditionEvaluator.evaluate(Condition.InternetAvailable, context, withoutInternet))

        assertTrue(conditionEvaluator.evaluate(Condition.InternetUnavailable, context, withoutInternet))
        assertFalse(conditionEvaluator.evaluate(Condition.InternetUnavailable, context, withInternet))

        // Internet unavailable for at least 15s
        assertTrue(conditionEvaluator.evaluate(Condition.InternetUnavailableForAtLeast(15), context, withoutInternet))
        assertFalse(conditionEvaluator.evaluate(Condition.InternetUnavailableForAtLeast(30), context, withoutInternet))
    }

    @Test
    fun testSsidConditions() {
        val connected = WifiState.Connected(
            ssid = "HomeWiFi",
            bssid = "00:11:22:33:44:55",
            rssi = -60,
            frequencyMhz = 5180,
            internetStatus = InternetStatus.AVAILABLE,
            connectedSinceMs = 0L
        )
        val context = EvaluationContext()

        assertTrue(conditionEvaluator.evaluate(Condition.CurrentSsidIs("HomeWiFi"), context, connected))
        assertFalse(conditionEvaluator.evaluate(Condition.CurrentSsidIs("OfficeNet"), context, connected))

        assertTrue(conditionEvaluator.evaluate(Condition.CurrentSsidIsNot("OfficeNet"), context, connected))
        assertFalse(conditionEvaluator.evaluate(Condition.CurrentSsidIsNot("HomeWiFi"), context, connected))
    }

    @Test
    fun testRuleEvaluationAndOrNot() {
        val connected = WifiState.Connected(
            ssid = "HomeWiFi",
            bssid = "00:11:22:33:44:55",
            rssi = -75,
            frequencyMhz = 5180,
            internetStatus = InternetStatus.AVAILABLE,
            connectedSinceMs = 0L
        )
        val context = EvaluationContext()

        // AND group: signal below -70 AND ssid is HomeWiFi -> True
        val andGroup = ConditionGroup(
            operator = LogicalOperator.AND,
            conditions = listOf(
                ConditionNode.Leaf(Condition.CurrentSignalBelow(-70)),
                ConditionNode.Leaf(Condition.CurrentSsidIs("HomeWiFi"))
            )
        )
        val andRule = Rule(
            name = "Weak Home",
            rootGroup = andGroup,
            action = RuleAction.PreferBestCandidate
        )
        assertTrue(ruleEvaluator.evaluateRule(andRule, context, connected))

        // OR group: signal below -80 (false) OR ssid is HomeWiFi (true) -> True
        val orGroup = ConditionGroup(
            operator = LogicalOperator.OR,
            conditions = listOf(
                ConditionNode.Leaf(Condition.CurrentSignalBelow(-80)),
                ConditionNode.Leaf(Condition.CurrentSsidIs("HomeWiFi"))
            )
        )
        val orRule = Rule(
            name = "Or rule",
            rootGroup = orGroup,
            action = RuleAction.PreferBestCandidate
        )
        assertTrue(ruleEvaluator.evaluateRule(orRule, context, connected))

        // NOT node: NOT (ssid is OfficeNet) -> True
        val notNode = ConditionNode.Not(
            ConditionNode.Leaf(Condition.CurrentSsidIs("OfficeNet"))
        )
        assertTrue(ruleEvaluator.evaluateNode(notNode, context, connected))

        // Disabled rule -> False
        val disabledRule = andRule.copy(enabled = false)
        assertFalse(ruleEvaluator.evaluateRule(disabledRule, context, connected))
    }

    @Test
    fun testAntiFlappingGuardHysteresisAndCooldown() {
        // Cooldown test
        val blockedCooldown = antiFlappingGuard.shouldAllowSwitch(
            currentRssi = -70,
            candidateRssi = -55,
            requiredImprovementDbm = 10,
            lastSwitchTimeMs = 90_000L,
            cooldownSeconds = 60,
            currentTimeMs = 120_000L // only 30s elapsed < 60s
        )
        assertTrue(blockedCooldown is AntiFlappingResult.Blocked)
        assertTrue((blockedCooldown as AntiFlappingResult.Blocked).reason.contains("Cooldown active"))

        // Improvement test
        val blockedImprovement = antiFlappingGuard.shouldAllowSwitch(
            currentRssi = -70,
            candidateRssi = -65, // +5 dBm < 10 dBm required
            requiredImprovementDbm = 10,
            lastSwitchTimeMs = 50_000L,
            cooldownSeconds = 60,
            currentTimeMs = 120_000L // 70s elapsed, cooldown okay
        )
        assertTrue(blockedImprovement is AntiFlappingResult.Blocked)
        assertTrue((blockedImprovement as AntiFlappingResult.Blocked).reason.contains("Improvement 5 dBm < required 10 dBm"))

        // Allowed test
        val allowed = antiFlappingGuard.shouldAllowSwitch(
            currentRssi = -70,
            candidateRssi = -55, // +15 dBm >= 10 dBm required
            requiredImprovementDbm = 10,
            lastSwitchTimeMs = 50_000L,
            cooldownSeconds = 60,
            currentTimeMs = 120_000L
        )
        assertTrue(allowed is AntiFlappingResult.Allowed)
        assertEquals(15, (allowed as AntiFlappingResult.Allowed).improvementDbm)
    }
}
