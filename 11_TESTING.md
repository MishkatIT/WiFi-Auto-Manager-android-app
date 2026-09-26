# 11 — Testing Strategy

## Test Pyramid

```
         ┌───────────────┐
         │  UI Tests     │  ← Small set, critical flows only
         │  (Compose)    │    Slow, require emulator
         └───────┬───────┘
         ┌───────┴───────┐
         │  Integration  │  ← Repository + Database
         │  Tests (Room) │    Medium speed
         └───────┬───────┘
         ┌───────┴───────────────────────────────┐
         │           Unit Tests                  │
         │  Decision engine, rule evaluator,     │
         │  condition evaluator, anti-flapping,  │
         │  ViewModels (with fakes)              │
         └───────────────────────────────────────┘
```

---

## Unit Test — Condition Evaluator

```kotlin
class ConditionEvaluatorTest {

    private val evaluator = ConditionEvaluator()
    private val baseContext = EvaluationContext(
        currentTimeMs = System.currentTimeMillis(),
        lastSwitchTimeMs = null,
        internetUnavailableSinceMs = null,
        connectedSinceMs = null,
    )

    @Test
    fun `CurrentSignalBelow returns true when rssi is below threshold`() {
        val state = WifiState.Connected(
            ssid = "Home", bssid = "", rssi = -72,
            frequencyMhz = 5180, internetStatus = InternetStatus.AVAILABLE,
            connectedSinceMs = 0
        )
        val result = evaluator.evaluate(
            Condition.CurrentSignalBelow(-70), baseContext, state, null, null
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `CurrentSignalBelow returns false when rssi is at threshold`() {
        val state = connectedState(rssi = -70)
        val result = evaluator.evaluate(Condition.CurrentSignalBelow(-70), baseContext, state, null, null)
        assertThat(result).isFalse()
    }

    @Test
    fun `InternetUnavailableForAtLeast returns false when duration not reached`() {
        val now = 1_000_000L
        val ctx = baseContext.copy(
            currentTimeMs = now,
            internetUnavailableSinceMs = now - 10_000   // 10 seconds ago
        )
        val result = evaluator.evaluate(
            Condition.InternetUnavailableForAtLeast(15), ctx, connectedState(), null, null
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `InternetUnavailableForAtLeast returns true when duration exceeded`() {
        val now = 1_000_000L
        val ctx = baseContext.copy(
            currentTimeMs = now,
            internetUnavailableSinceMs = now - 20_000   // 20 seconds ago
        )
        val result = evaluator.evaluate(
            Condition.InternetUnavailableForAtLeast(15), ctx, connectedState(), null, null
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `SwitchCooldownExpired returns true when no previous switch`() {
        val result = evaluator.evaluate(
            Condition.SwitchCooldownExpired(60), baseContext, connectedState(), null, null
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `SwitchCooldownExpired returns false when within cooldown`() {
        val now = 1_000_000L
        val ctx = baseContext.copy(
            currentTimeMs = now,
            lastSwitchTimeMs = now - 30_000   // 30 seconds ago
        )
        val result = evaluator.evaluate(
            Condition.SwitchCooldownExpired(60), ctx, connectedState(), null, null
        )
        assertThat(result).isFalse()
    }
}
```

---

## Unit Test — Decision Engine Scenarios

```kotlin
class DecisionEngineTest {

    private val engine: DecisionEngine = DecisionEngineImpl(
        conditionEvaluator = ConditionEvaluator(),
        ruleEvaluator = RuleEvaluator(ConditionEvaluator()),
        antiFlappingGuard = AntiFlappingGuard(),
        decisionExplainer = DecisionExplainer(),
    )

    // Scenario A: Home weak + no internet, Office strong + internet → suggest Office
    @Test
    fun `scenario A - suggests stronger candidate with internet`() {
        val decision = engine.evaluate(
            currentState = connected("Home WiFi", rssi = -76, internet = InternetStatus.UNAVAILABLE),
            availableNetworks = listOf(scanned("Office WiFi", rssi = -55)),
            savedNetworks = listOf(
                home(rssi = -70),
                office(rssi = -65, requiresInternet = false, minImprovement = 10),
            ),
            rules = emptyList(),
            settings = defaultSettings(),
            context = noSwitchContext(),
        )
        assertThat(decision.action).isInstanceOf(DecisionAction.SuggestNetwork::class.java)
        assertThat((decision.action as DecisionAction.SuggestNetwork).network.ssid)
            .isEqualTo("Office WiFi")
    }

    // Scenario B: Insufficient improvement → stay
    @Test
    fun `scenario B - stays when improvement is insufficient`() {
        val decision = engine.evaluate(
            currentState = connected("Home WiFi", rssi = -68, internet = InternetStatus.AVAILABLE),
            availableNetworks = listOf(scanned("Office WiFi", rssi = -66)),
            savedNetworks = listOf(home(rssi = -70), office(minImprovement = 10)),
            rules = emptyList(),
            settings = defaultSettings(),
            context = noSwitchContext(),
        )
        assertThat(decision.action).isEqualTo(DecisionAction.StayOnCurrent)
    }

    // Scenario C: Internet unavailable, timeout not reached → stay
    @Test
    fun `scenario C - does not switch before internet timeout`() {
        val now = 100_000L
        val decision = engine.evaluate(
            currentState = connected("Home WiFi", rssi = -45, internet = InternetStatus.UNAVAILABLE),
            availableNetworks = listOf(scanned("Office WiFi", rssi = -60)),
            savedNetworks = listOf(home(), office(requiresInternet = false)),
            rules = emptyList(),
            settings = defaultSettings().copy(internetUnavailableTimeoutSeconds = 15),
            context = EvaluationContext(
                currentTimeMs = now,
                lastSwitchTimeMs = null,
                internetUnavailableSinceMs = now - 5_000,  // only 5s ago
                connectedSinceMs = now - 60_000,
            ),
        )
        assertThat(decision.action).isEqualTo(DecisionAction.StayOnCurrent)
    }

    // Scenario D: Both candidates insufficient → stay
    @Test
    fun `scenario D - stays when no candidate provides enough improvement`() {
        val decision = engine.evaluate(
            currentState = connected("Home WiFi", rssi = -75),
            availableNetworks = listOf(scanned("Office WiFi", rssi = -73)),
            savedNetworks = listOf(home(), office(minImprovement = 10)),
            rules = emptyList(),
            settings = defaultSettings(),
            context = noSwitchContext(),
        )
        assertThat(decision.action).isEqualTo(DecisionAction.StayOnCurrent)
        assertThat(decision.evaluatedCandidates.first().rejectionReason).contains("Improvement")
    }

    // Cooldown active → stay
    @Test
    fun `cooldown blocks switch`() {
        val now = 100_000L
        val decision = engine.evaluate(
            currentState = connected("Home WiFi", rssi = -76),
            availableNetworks = listOf(scanned("Office WiFi", rssi = -50)),
            savedNetworks = listOf(home(), office()),
            rules = emptyList(),
            settings = defaultSettings().copy(switchCooldownSeconds = 60),
            context = EvaluationContext(
                currentTimeMs = now,
                lastSwitchTimeMs = now - 30_000,    // 30s ago, cooldown=60s
                internetUnavailableSinceMs = null,
                connectedSinceMs = now - 120_000,
            ),
        )
        assertThat(decision.action).isEqualTo(DecisionAction.CooldownActive)
    }
}
```

---

## Unit Test — Anti-Flapping Guard

```kotlin
class AntiFlappingGuardTest {

    private val guard = AntiFlappingGuard()

    @Test
    fun `allows switch when improvement exceeds threshold`() {
        val result = guard.shouldAllowSwitch(
            currentRssi = -75,
            candidateRssi = -55,
            requiredImprovementDbm = 10,
            lastSwitchTimeMs = null,
            cooldownSeconds = 60,
            currentTimeMs = 100_000L,
        )
        assertThat(result).isInstanceOf(AntiFlappingResult.Allowed::class.java)
        assertThat((result as AntiFlappingResult.Allowed).improvementDbm).isEqualTo(20)
    }

    @Test
    fun `blocks switch when improvement is insufficient`() {
        val result = guard.shouldAllowSwitch(
            currentRssi = -68, candidateRssi = -65,
            requiredImprovementDbm = 10,
            lastSwitchTimeMs = null, cooldownSeconds = 60, currentTimeMs = 100_000L,
        )
        assertThat(result).isInstanceOf(AntiFlappingResult.Blocked::class.java)
    }

    @Test
    fun `blocks switch during cooldown`() {
        val now = 100_000L
        val result = guard.shouldAllowSwitch(
            currentRssi = -75, candidateRssi = -50,
            requiredImprovementDbm = 10,
            lastSwitchTimeMs = now - 30_000,
            cooldownSeconds = 60,
            currentTimeMs = now,
        )
        assertThat(result).isInstanceOf(AntiFlappingResult.Blocked::class.java)
        assertThat((result as AntiFlappingResult.Blocked).reason).contains("Cooldown")
    }
}
```

---

## Integration Test — Room Database

```kotlin
@RunWith(AndroidJUnit4::class)
class WifiNetworkDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WifiNetworkDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.wifiNetworkDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun insertAndRetrieveNetwork() = runTest {
        val entity = WifiNetworkEntity(ssid = "TestNet", securityType = "WPA2_PSK",
            enabled = true, priority = 50, minimumSignalDbm = -70,
            requiresInternet = false, minimumCandidateSignalDbm = -65,
            minimumImprovementDbm = 10, createdAt = 0, updatedAt = 0)
        val id = dao.insert(entity)
        val retrieved = dao.getById(id)
        assertThat(retrieved?.ssid).isEqualTo("TestNet")
    }

    @Test
    fun deleteNetwork_removesFromDatabase() = runTest {
        val id = dao.insert(testEntity())
        dao.deleteById(id)
        assertThat(dao.getById(id)).isNull()
    }

    @Test
    fun getAllNetworks_returnsInPriorityOrder() = runTest {
        dao.insert(testEntity(priority = 30))
        dao.insert(testEntity(priority = 90))
        dao.insert(testEntity(priority = 50))
        val all = dao.getAllByPriority().first()
        assertThat(all.map { it.priority }).isInOrder(Comparator.reverseOrder<Int>())
    }
}
```

---

## ViewModel Test

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkListViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeWifiRepository()
    private lateinit var viewModel: NetworkListViewModel

    @Before
    fun setup() {
        viewModel = NetworkListViewModel(
            manageNetworksUseCase = ManageSavedNetworksUseCase(fakeRepository, FakeCredentialStore())
        )
    }

    @Test
    fun `adding a network updates state`() = runTest {
        viewModel.onAddNetwork(WifiNetwork(ssid = "NewNet", securityType = SecurityType.WPA2_PSK, priority = 50), "password")
        val state = viewModel.uiState.value
        assertThat(state.networks).hasSize(1)
        assertThat(state.networks.first().ssid).isEqualTo("NewNet")
    }

    @Test
    fun `toggling network updates enabled state`() = runTest {
        val id = fakeRepository.addNetwork(testNetwork(enabled = true))
        viewModel.onToggleNetwork(id, enabled = false)
        val state = viewModel.uiState.value
        assertThat(state.networks.first { it.id == id }.enabled).isFalse()
    }
}
```

---

## Compose UI Tests (Critical Flows)

```kotlin
@RunWith(AndroidJUnit4::class)
class AddNetworkFlowTest {

    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun canAddAndSeeNewNetwork() {
        composeRule.onNodeWithText("Networks").performClick()
        composeRule.onNodeWithContentDescription("Add network").performClick()
        composeRule.onNodeWithText("Network Name (SSID)").performTextInput("My Test Net")
        composeRule.onNodeWithText("Save Network").performClick()
        composeRule.onNodeWithText("My Test Net").assertIsDisplayed()
    }
}
```

---

## Test Coverage Requirements

| Component | Required Coverage |
|-----------|------------------|
| DecisionEngine | 90%+ (all scenarios) |
| ConditionEvaluator | 95%+ (all condition types) |
| RuleEvaluator | 90%+ (AND/OR/NOT combos) |
| AntiFlappingGuard | 95%+ |
| WifiRepository | 80%+ |
| ViewModels | 75%+ |
| UI critical flows | 5 flows minimum |

---

## Fake / Test Doubles

```kotlin
class FakeWifiRepository : WifiRepository {
    private val networks = mutableMapOf<Long, WifiNetwork>()
    private var nextId = 1L

    override suspend fun saveNetwork(network: WifiNetwork): Result<Long> {
        val id = if (network.id == 0L) nextId++ else network.id
        networks[id] = network.copy(id = id)
        return Result.success(id)
    }

    override fun observeAllNetworks(): Flow<List<WifiNetwork>> =
        flowOf(networks.values.toList())

    // ... other methods
}
```
