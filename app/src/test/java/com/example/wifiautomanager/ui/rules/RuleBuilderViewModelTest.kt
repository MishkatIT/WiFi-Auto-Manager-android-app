package com.example.wifiautomanager.ui.rules

import androidx.lifecycle.SavedStateHandle
import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.model.RuleAction
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.repository.RuleRepository
import com.example.wifiautomanager.domain.repository.WifiRepository
import com.example.wifiautomanager.domain.rule.Condition
import com.example.wifiautomanager.domain.rule.ConditionGroup
import com.example.wifiautomanager.domain.rule.ConditionNode
import com.example.wifiautomanager.domain.rule.LogicalOperator
import com.example.wifiautomanager.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule as JUnitTestRule
import org.junit.Test

class FakeRuleRepository : RuleRepository {
    private val rules = mutableListOf<Rule>()
    private val _rulesFlow = MutableStateFlow<List<Rule>>(emptyList())

    override fun getAllRules(): Flow<List<Rule>> = _rulesFlow.asStateFlow()

    override fun getEnabledRules(): Flow<List<Rule>> = MutableStateFlow(rules.filter { it.enabled })

    override suspend fun getRuleById(id: Long): Rule? = rules.find { it.id == id }

    override suspend fun insertRule(rule: Rule): Long {
        val id = if (rule.id == 0L) (rules.maxOfOrNull { it.id } ?: 0L) + 1 else rule.id
        val newRule = rule.copy(id = id)
        rules.add(newRule)
        _rulesFlow.value = rules.toList()
        return id
    }

    override suspend fun updateRule(rule: Rule) {
        val index = rules.indexOfFirst { it.id == rule.id }
        if (index != -1) {
            rules[index] = rule
            _rulesFlow.value = rules.toList()
        }
    }

    override suspend fun deleteRule(rule: Rule) {
        rules.removeAll { it.id == rule.id }
        _rulesFlow.value = rules.toList()
    }
}

class FakeWifiRepositoryForRules : WifiRepository {
    private val networks = mutableListOf<WifiNetwork>()
    private val _flow = MutableStateFlow<List<WifiNetwork>>(emptyList())

    override fun getAllNetworks(): Flow<List<WifiNetwork>> = _flow.asStateFlow()
    override fun getEnabledNetworks(): Flow<List<WifiNetwork>> = MutableStateFlow(networks.filter { it.enabled })
    override suspend fun getNetworkById(id: Long): WifiNetwork? = networks.find { it.id == id }
    override suspend fun getNetworkBySsid(ssid: String): WifiNetwork? = networks.find { it.ssid == ssid }
    override suspend fun insertNetwork(network: WifiNetwork): Long {
        val id = if (network.id == 0L) (networks.maxOfOrNull { it.id } ?: 0L) + 1 else network.id
        networks.add(network.copy(id = id))
        _flow.value = networks.toList()
        return id
    }
    override suspend fun updateNetwork(network: WifiNetwork) {
        val idx = networks.indexOfFirst { it.id == network.id }
        if (idx != -1) networks[idx] = network
        _flow.value = networks.toList()
    }
    override suspend fun deleteNetwork(network: WifiNetwork) {
        networks.removeAll { it.id == network.id }
        _flow.value = networks.toList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RuleBuilderViewModelTest {

    @get:JUnitTestRule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRuleRepository: FakeRuleRepository
    private lateinit var fakeWifiRepository: FakeWifiRepositoryForRules

    @Before
    fun setup() {
        fakeRuleRepository = FakeRuleRepository()
        fakeWifiRepository = FakeWifiRepositoryForRules()
    }

    @Test
    fun testInitialStateForNewRule() = runTest {
        val viewModel = RuleBuilderViewModel(fakeRuleRepository, fakeWifiRepository, SavedStateHandle())
        val state = viewModel.uiState.value

        assertFalse(state.isEditMode)
        assertEquals("", state.name)
        assertEquals(LogicalOperator.AND, state.operator)
        assertEquals(1, state.conditions.size)
        assertEquals("PREFER_BEST", state.selectedActionType)
    }

    @Test
    fun testValidationRequiresName() = runTest {
        val viewModel = RuleBuilderViewModel(fakeRuleRepository, fakeWifiRepository, SavedStateHandle())
        viewModel.onSaveRule()

        val state = viewModel.uiState.value
        assertNotNull(state.nameError)
    }

    @Test
    fun testAddAndRemoveConditions() = runTest {
        val viewModel = RuleBuilderViewModel(fakeRuleRepository, fakeWifiRepository, SavedStateHandle())
        viewModel.onAddCondition()
        assertEquals(2, viewModel.uiState.value.conditions.size)

        viewModel.onRemoveCondition(0)
        assertEquals(1, viewModel.uiState.value.conditions.size)
    }

    @Test
    fun testSaveNewRuleSuccessfully() = runTest {
        val viewModel = RuleBuilderViewModel(fakeRuleRepository, fakeWifiRepository, SavedStateHandle())
        viewModel.onNameChanged("Weak Signal Fallback")
        viewModel.onOperatorChanged(LogicalOperator.OR)
        viewModel.onConditionTypeChanged(0, ConditionTypeUi.CURRENT_SIGNAL_BELOW)
        viewModel.onConditionIntValueChange(0, -78)
        viewModel.onActionTypeSelected("STAY")

        viewModel.onSaveRule()
        advanceUntilIdle()

        val savedRules = fakeRuleRepository.getAllRules().first()
        assertEquals(1, savedRules.size)
        val rule = savedRules.first()
        assertEquals("Weak Signal Fallback", rule.name)
        assertEquals(LogicalOperator.OR, rule.rootGroup.operator)
        assertEquals(RuleAction.StayOnCurrent, rule.action)
        assertEquals(1, rule.rootGroup.conditions.size)
        val leaf = (rule.rootGroup.conditions.first() as ConditionNode.Leaf).condition
        assertTrue(leaf is Condition.CurrentSignalBelow)
        assertEquals(-78, (leaf as Condition.CurrentSignalBelow).thresholdDbm)
    }

    @Test
    fun testEditExistingRuleLoadsCorrectly() = runTest {
        val existingRule = Rule(
            id = 5L,
            name = "Office Preference",
            rootGroup = ConditionGroup(
                operator = LogicalOperator.AND,
                conditions = listOf(
                    ConditionNode.Leaf(Condition.CurrentSsidIs("Home_WiFi")),
                    ConditionNode.Leaf(Condition.InternetUnavailable)
                )
            ),
            action = RuleAction.PreferBestCandidate,
            enabled = true
        )
        fakeRuleRepository.insertRule(existingRule)

        val handle = SavedStateHandle(mapOf("id" to 5L))
        val viewModel = RuleBuilderViewModel(fakeRuleRepository, fakeWifiRepository, handle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditMode)
        assertEquals("Office Preference", state.name)
        assertEquals(2, state.conditions.size)
        assertEquals(ConditionTypeUi.CURRENT_SSID_IS, state.conditions[0].type)
        assertEquals("Home_WiFi", state.conditions[0].textValue)
        assertEquals(ConditionTypeUi.INTERNET_UNAVAILABLE, state.conditions[1].type)

        // Modify and save
        viewModel.onNameChanged("Updated Office Preference")
        viewModel.onSaveRule()
        advanceUntilIdle()

        val updated = fakeRuleRepository.getRuleById(5L)
        assertEquals("Updated Office Preference", updated?.name)
    }
}
