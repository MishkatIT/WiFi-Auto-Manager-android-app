package com.example.wifiautomanager.domain.rule

import com.example.wifiautomanager.domain.decision.EvaluationContext
import com.example.wifiautomanager.domain.model.Rule
import com.example.wifiautomanager.domain.model.ScannedNetwork
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.model.WifiState

class RuleEvaluator(
    private val conditionEvaluator: ConditionEvaluator = ConditionEvaluator()
) {
    fun evaluateRule(
        rule: Rule,
        context: EvaluationContext,
        currentState: WifiState,
        candidate: ScannedNetwork? = null,
        candidateSaved: WifiNetwork? = null,
        savedNetworks: List<WifiNetwork> = emptyList()
    ): Boolean {
        if (!rule.enabled) return false
        return evaluateGroup(rule.rootGroup, context, currentState, candidate, candidateSaved, savedNetworks)
    }

    fun evaluateNode(
        node: ConditionNode,
        context: EvaluationContext,
        currentState: WifiState,
        candidate: ScannedNetwork? = null,
        candidateSaved: WifiNetwork? = null,
        savedNetworks: List<WifiNetwork> = emptyList()
    ): Boolean = when (node) {
        is ConditionNode.Leaf ->
            conditionEvaluator.evaluate(
                node.condition, context, currentState, candidate, candidateSaved, savedNetworks
            )

        is ConditionNode.Group ->
            evaluateGroup(node.group, context, currentState, candidate, candidateSaved, savedNetworks)

        is ConditionNode.Not ->
            !evaluateNode(node.inner, context, currentState, candidate, candidateSaved, savedNetworks)
    }

    fun evaluateGroup(
        group: ConditionGroup,
        context: EvaluationContext,
        currentState: WifiState,
        candidate: ScannedNetwork? = null,
        candidateSaved: WifiNetwork? = null,
        savedNetworks: List<WifiNetwork> = emptyList()
    ): Boolean {
        if (group.conditions.isEmpty()) return true

        return when (group.operator) {
            LogicalOperator.AND ->
                group.conditions.all {
                    evaluateNode(it, context, currentState, candidate, candidateSaved, savedNetworks)
                }

            LogicalOperator.OR ->
                group.conditions.any {
                    evaluateNode(it, context, currentState, candidate, candidateSaved, savedNetworks)
                }
        }
    }
}
