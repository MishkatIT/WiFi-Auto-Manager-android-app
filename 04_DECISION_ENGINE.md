# 04 — Decision Engine

## Overview

The decision engine is the core of the application.

It is:
- **Pure Kotlin** — zero Android imports
- **Deterministic** — same input always produces same output
- **Explainable** — every decision includes a human-readable reason
- **Extensible** — new Condition types can be added without rewriting the engine

---

## Engine API

```kotlin
interface DecisionEngine {
    fun evaluate(
        currentState: WifiState,
        availableNetworks: List<ScannedNetwork>,
        savedNetworks: List<WifiNetwork>,
        rules: List<Rule>,
        settings: AppSettings,
        context: EvaluationContext,
    ): Decision
}

data class EvaluationContext(
    val currentTimeMs: Long,
    val lastSwitchTimeMs: Long?,
    val internetUnavailableSinceMs: Long?,
    val connectedSinceMs: Long?,
)
```

---

## Evaluation Pipeline

```
1. Guard checks
   ├── Auto manager enabled?
   ├── Cooldown active?
   └── Current state valid?

2. Build candidate list
   ├── Filter: only enabled saved networks
   ├── Filter: only networks visible in scan results
   └── Map: attach scan result to saved network

3. For each candidate, evaluate eligibility:
   ├── Minimum signal check
   ├── Internet requirement check
   ├── Minimum improvement check (vs current)
   └── Rule evaluation

4. Rank eligible candidates
   └── By: priority → signal → internet status

5. Select top candidate

6. Build Decision with explanation

7. Persist to decision log
```

---

## Condition Evaluator

```kotlin
class ConditionEvaluator {

    fun evaluate(
        condition: Condition,
        context: EvaluationContext,
        currentState: WifiState,
        candidate: ScannedNetwork?,
        candidateSaved: WifiNetwork?,
    ): Boolean = when (condition) {

        is Condition.CurrentSignalBelow -> {
            val rssi = (currentState as? WifiState.Connected)?.rssi ?: return false
            rssi < condition.thresholdDbm
        }

        is Condition.InternetUnavailableForAtLeast -> {
            val since = context.internetUnavailableSinceMs ?: return false
            val durationMs = context.currentTimeMs - since
            durationMs >= condition.seconds * 1_000L
        }

        is Condition.TimeBetween -> {
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = context.currentTimeMs
            }
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val min  = cal.get(java.util.Calendar.MINUTE)
            val current = hour * 60 + min
            val start   = condition.startHour * 60 + condition.startMin
            val end     = condition.endHour * 60 + condition.endMin
            if (start <= end) current in start..end
            else current >= start || current <= end    // crosses midnight
        }

        is Condition.SwitchCooldownExpired -> {
            val last = context.lastSwitchTimeMs ?: return true
            (context.currentTimeMs - last) >= condition.seconds * 1_000L
        }

        // ... all other conditions
    }
}
```

---

## Rule Evaluator

```kotlin
class RuleEvaluator(private val conditionEvaluator: ConditionEvaluator) {

    fun evaluate(
        node: ConditionNode,
        context: EvaluationContext,
        currentState: WifiState,
        candidate: ScannedNetwork?,
        candidateSaved: WifiNetwork?,
    ): Boolean = when (node) {

        is ConditionNode.Leaf ->
            conditionEvaluator.evaluate(
                node.condition, context, currentState, candidate, candidateSaved
            )

        is ConditionNode.Group -> when (node.group.operator) {
            LogicalOperator.AND ->
                node.group.conditions.all { evaluate(it, context, currentState, candidate, candidateSaved) }
            LogicalOperator.OR  ->
                node.group.conditions.any { evaluate(it, context, currentState, candidate, candidateSaved) }
        }

        is ConditionNode.Not ->
            !evaluate(node.inner, context, currentState, candidate, candidateSaved)
    }
}
```

---

## Anti-Flapping Guard

```kotlin
class AntiFlappingGuard {

    fun shouldAllowSwitch(
        currentRssi: Int,
        candidateRssi: Int,
        requiredImprovementDbm: Int,
        lastSwitchTimeMs: Long?,
        cooldownSeconds: Int,
        currentTimeMs: Long,
    ): AntiFlappingResult {

        // Cooldown check
        if (lastSwitchTimeMs != null) {
            val elapsed = currentTimeMs - lastSwitchTimeMs
            if (elapsed < cooldownSeconds * 1_000L) {
                val remaining = (cooldownSeconds * 1_000L - elapsed) / 1_000L
                return AntiFlappingResult.Blocked(
                    reason = "Cooldown active: ${remaining}s remaining"
                )
            }
        }

        // Improvement check
        val improvement = candidateRssi - currentRssi   // positive = candidate stronger
        if (improvement < requiredImprovementDbm) {
            return AntiFlappingResult.Blocked(
                reason = "Improvement ${improvement} dBm < required ${requiredImprovementDbm} dBm"
            )
        }

        return AntiFlappingResult.Allowed(improvementDbm = improvement)
    }
}

sealed class AntiFlappingResult {
    data class  Allowed(val improvementDbm: Int) : AntiFlappingResult()
    data class  Blocked(val reason: String) : AntiFlappingResult()
}
```

---

## Decision Explainer

Every decision must produce a human-readable reason string.

```kotlin
class DecisionExplainer {

    fun explain(
        action: DecisionAction,
        currentState: WifiState,
        candidates: List<CandidateResult>,
        settings: AppSettings,
    ): String = buildString {

        when (action) {

            is DecisionAction.StayOnCurrent -> {
                appendLine("Staying on current network.")
                candidates.filter { !it.qualified }.forEach { c ->
                    appendLine("• ${c.network.ssid} rejected: ${c.rejectionReason}")
                }
            }

            is DecisionAction.SuggestNetwork -> {
                val c = candidates.first { it.qualified && it.network.id == action.network.id }
                appendLine("Suggesting: ${action.network.ssid}")
                appendLine("Signal improvement: ${c.signalImprovement} dBm")
                appendLine("Required: ${action.network.minimumImprovementDbm} dBm")
            }

            is DecisionAction.NoCandidates ->
                appendLine("No eligible candidate networks found.")

            is DecisionAction.CooldownActive ->
                appendLine("Switch cooldown is active. No switch attempted.")

            is DecisionAction.RulesDisabled ->
                appendLine("Auto manager is disabled.")
        }
    }
}
```

---

## Scenario Verification

The engine must correctly handle the following cases (all unit-tested):

| Scenario | Current | Candidate | Result |
|----------|---------|-----------|--------|
| A | -76 dBm, no internet | -55 dBm, internet | Suggest candidate |
| B | -68 dBm, internet | -66 dBm, internet (only 2 dBm better) | Stay |
| C | -45 dBm, no internet (5s) | -60 dBm, internet | Wait — timeout not reached |
| D | -75 dBm | -73 dBm (only 2 dBm better) | Stay |
| E | Cooldown 30s remaining | Any | Stay, report cooldown |
| F | No candidates visible | — | Stay, report no candidates |
| G | Two candidates, both qualify | -52 dBm pri=2, -60 dBm pri=5 | Prefer priority 5 |

---

## Decision Cycle (Background)

The decision cycle is triggered by:

1. `ConnectivityManager.NetworkCallback` — network changes
2. `WifiManager.SCAN_RESULTS_AVAILABLE_ACTION` — new scan results
3. WorkManager periodic task — configurable interval fallback

```
Network event received
        ↓
Collect current state snapshot
        ↓
Run DecisionEngine.evaluate()
        ↓
Record in DecisionLogEntity
        ↓
If action = SuggestNetwork:
    → Register WifiNetworkSuggestion
    → Post notification (if enabled)
        ↓
Emit result to UI via StateFlow
```
