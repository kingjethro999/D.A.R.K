package com.dark.launcher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dark.launcher.ui.hidden.HiddenAppsScreen
import com.dark.launcher.ui.hidden.HiddenAppsViewModel
import com.dark.launcher.ui.home.HomeScreen
import com.dark.launcher.ui.home.HomeViewModel
import com.dark.launcher.ui.settings.SettingsScreen
import com.dark.launcher.ui.settings.SettingsViewModel
import com.dark.launcher.ui.terminal.TerminalScreen
import com.dark.launcher.ui.terminal.TerminalViewModel

object DarkRoutes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HIDDEN = "hidden"
    const val TERMINAL = "terminal"
}

@Composable
fun DarkNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = DarkRoutes.HOME
    ) {
        composable(DarkRoutes.HOME) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate(DarkRoutes.SETTINGS) },
                onOpenTerminal = { navController.navigate(DarkRoutes.TERMINAL) },
                onOpenHidden = { navController.navigate(DarkRoutes.HIDDEN) }
            )
        }
        composable(DarkRoutes.SETTINGS) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenHidden = { navController.navigate(DarkRoutes.HIDDEN) }
            )
        }
        composable(DarkRoutes.HIDDEN) {
            val viewModel: HiddenAppsViewModel = hiltViewModel()
            HiddenAppsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(DarkRoutes.TERMINAL) {
            val viewModel: TerminalViewModel = hiltViewModel()
            TerminalScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
