package com.example.wifiautomanager.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Networks : Screen("networks")
    data object More : Screen("more")
    data object AddNetwork : Screen("networks/add")
    data class EditNetwork(val id: Long) : Screen("networks/edit/$id") {
        companion object {
            const val ROUTE_PATTERN = "networks/edit/{id}"
        }
    }
    data object Rules : Screen("rules")
    data object RuleBuilder : Screen("rules/builder")
    data class EditRule(val id: Long) : Screen("rules/edit/$id") {
        companion object {
            const val ROUTE_PATTERN = "rules/edit/{id}"
        }
    }
    data object Nearby : Screen("nearby")
    data object Diagnostics : Screen("diagnostics")
    data class DecisionDetail(val id: Long) : Screen("diagnostics/decision/$id") {
        companion object {
            const val ROUTE_PATTERN = "diagnostics/decision/{id}"
        }
    }
    data object Settings : Screen("settings")
}
