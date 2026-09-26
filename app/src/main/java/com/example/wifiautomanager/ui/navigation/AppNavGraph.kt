package com.example.wifiautomanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.wifiautomanager.ui.dashboard.DashboardScreen
import com.example.wifiautomanager.ui.dashboard.DashboardViewModel
import com.example.wifiautomanager.ui.diagnostics.DiagnosticsScreen
import com.example.wifiautomanager.ui.diagnostics.DiagnosticsViewModel
import com.example.wifiautomanager.ui.more.MoreScreen
import com.example.wifiautomanager.ui.nearby.NearbyNetworksScreen
import com.example.wifiautomanager.ui.nearby.NearbyNetworksViewModel
import com.example.wifiautomanager.ui.networks.NetworkListScreen
import com.example.wifiautomanager.ui.networks.NetworkListViewModel
import com.example.wifiautomanager.ui.rules.RuleListScreen
import com.example.wifiautomanager.ui.rules.RuleListViewModel
import com.example.wifiautomanager.ui.settings.SettingsScreen
import com.example.wifiautomanager.ui.settings.SettingsViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            val viewModel = hiltViewModel<DashboardViewModel>()
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToNetworks = { navController.navigate(Screen.Networks.route) },
                onNavigateToRules = { navController.navigate(Screen.Rules.route) }
            )
        }
        composable(Screen.Networks.route) {
            val viewModel = hiltViewModel<NetworkListViewModel>()
            NetworkListScreen(
                viewModel = viewModel,
                onAddNetworkClick = { navController.navigate(Screen.AddNetwork.route) },
                onEditNetworkClick = { id -> navController.navigate(Screen.EditNetwork(id).route) }
            )
        }
        composable(Screen.AddNetwork.route) {
            val viewModel = hiltViewModel<com.example.wifiautomanager.ui.networks.AddEditNetworkViewModel>()
            com.example.wifiautomanager.ui.networks.AddEditNetworkScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditNetwork.ROUTE_PATTERN,
            arguments = listOf(
                androidx.navigation.navArgument("id") {
                    type = androidx.navigation.NavType.LongType
                }
            )
        ) {
            val viewModel = hiltViewModel<com.example.wifiautomanager.ui.networks.AddEditNetworkViewModel>()
            com.example.wifiautomanager.ui.networks.AddEditNetworkScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.More.route) {
            MoreScreen(
                onNavigateToNearby = { navController.navigate(Screen.Nearby.route) },
                onNavigateToRules = { navController.navigate(Screen.Rules.route) },
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Rules.route) {
            val viewModel = hiltViewModel<RuleListViewModel>()
            RuleListScreen(
                viewModel = viewModel,
                onAddRuleClick = { navController.navigate(Screen.RuleBuilder.route) },
                onEditRuleClick = { id -> navController.navigate(Screen.EditRule(id).route) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Nearby.route) {
            val viewModel = hiltViewModel<NearbyNetworksViewModel>()
            NearbyNetworksScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onEditNetworkClick = { id -> navController.navigate(Screen.EditNetwork(id).route) },
                onAddNetworkWithSsid = { _ -> navController.navigate(Screen.AddNetwork.route) }
            )
        }
        composable(Screen.Diagnostics.route) {
            val viewModel = hiltViewModel<DiagnosticsViewModel>()
            DiagnosticsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            val viewModel = hiltViewModel<SettingsViewModel>()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.RuleBuilder.route) {
            val viewModel = hiltViewModel<com.example.wifiautomanager.ui.rules.RuleBuilderViewModel>()
            com.example.wifiautomanager.ui.rules.RuleBuilderScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditRule.ROUTE_PATTERN,
            arguments = listOf(
                androidx.navigation.navArgument("id") {
                    type = androidx.navigation.NavType.LongType
                }
            )
        ) {
            val viewModel = hiltViewModel<com.example.wifiautomanager.ui.rules.RuleBuilderViewModel>()
            com.example.wifiautomanager.ui.rules.RuleBuilderScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
