package com.example.wifiautomanager.ui.rules

import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.model.RuleAction
import com.example.wifiautomanager.domain.rule.Condition
import com.example.wifiautomanager.domain.rule.ConditionGroup
import com.example.wifiautomanager.domain.rule.ConditionNode
import com.example.wifiautomanager.domain.rule.LogicalOperator
import com.example.wifiautomanager.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule as JUnitTestRule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RuleListViewModelTest {

    @get:JUnitTestRule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRuleRepository: FakeRuleRepository

    @Before
    fun setup() {
        fakeRuleRepository = FakeRuleRepository()
    }

    @Test
    fun testRulesLoadedAndFormatted() = runTest {
        val rule1 = Rule(
            id = 1L,
            name = "Rule 1",
            rootGroup = ConditionGroup(
                operator = LogicalOperator.AND,
                conditions = listOf(ConditionNode.Leaf(Condition.InternetUnavailable))
            ),
            action = RuleAction.PreferBestCandidate,
            enabled = true
        )
        fakeRuleRepository.insertRule(rule1)

        val viewModel = RuleListViewModel(fakeRuleRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val items = viewModel.uiState.value.rules
        assertEquals(1, items.size)
        assertEquals("Rule 1", items[0].name)
        assertTrue(items[0].enabled)
        assertEquals("Prefer best available candidate", items[0].actionText)
    }

    @Test
    fun testToggleRuleEnabled() = runTest {
        val rule1 = Rule(
            id = 1L,
            name = "Rule 1",
            rootGroup = ConditionGroup(
                operator = LogicalOperator.AND,
                conditions = listOf(ConditionNode.Leaf(Condition.InternetUnavailable))
            ),
            action = RuleAction.PreferBestCandidate,
            enabled = true
        )
        fakeRuleRepository.insertRule(rule1)

        val viewModel = RuleListViewModel(fakeRuleRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onToggleRule(1L, false)
        advanceUntilIdle()

        val updated = fakeRuleRepository.getRuleById(1L)
        assertFalse(updated?.enabled ?: true)
    }

    @Test
    fun testDeleteRule() = runTest {
        val rule1 = Rule(
            id = 1L,
            name = "Rule 1",
            rootGroup = ConditionGroup(
                operator = LogicalOperator.AND,
                conditions = listOf(ConditionNode.Leaf(Condition.InternetUnavailable))
            ),
            action = RuleAction.PreferBestCandidate,
            enabled = true
        )
        fakeRuleRepository.insertRule(rule1)

        val viewModel = RuleListViewModel(fakeRuleRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDeleteRule(1L)
        advanceUntilIdle()

        val all = fakeRuleRepository.getAllRules().first()
        assertTrue(all.isEmpty())
    }
}
