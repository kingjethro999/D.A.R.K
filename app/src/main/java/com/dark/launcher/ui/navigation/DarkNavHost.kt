package com.dark.launcher.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dark.launcher.ui.components.DarkBottomBar
import com.dark.launcher.ui.hidden.HiddenAppsScreen
import com.dark.launcher.ui.hidden.HiddenAppsViewModel
import com.dark.launcher.ui.home.HomeScreen
import com.dark.launcher.ui.home.HomeViewModel
import com.dark.launcher.ui.onboarding.OnboardingScreen
import com.dark.launcher.ui.onboarding.OnboardingViewModel
import com.dark.launcher.ui.recorder.RecorderScreen
import com.dark.launcher.ui.recorder.RecorderViewModel
import com.dark.launcher.ui.settings.SettingsScreen
import com.dark.launcher.ui.settings.SettingsViewModel
import com.dark.launcher.ui.terminal.TerminalScreen
import com.dark.launcher.ui.terminal.TerminalViewModel
import com.dark.launcher.ui.theme.Black

object DarkRoutes {
    const val ONBOARDING = "onboarding"
    const val ONBOARDING_ROUTE = "onboarding?first={first}"
    const val ONBOARDING_FROM_APP = "onboarding?first=false"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HIDDEN = "hidden"
    const val TERMINAL = "terminal"
    const val RECORDER = "recorder"
}

@Composable
fun DarkNavHost(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val onboardingVm: OnboardingViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        onboardingVm.refreshState()
    }

    val showBar = currentRoute == DarkRoutes.HOME ||
        currentRoute == DarkRoutes.SETTINGS ||
        currentRoute == DarkRoutes.RECORDER

    val barViewModel: BottomBarViewModel = hiltViewModel()
    val steps by barViewModel.todaySteps.collectAsStateWithLifecycle()
    val nowPlaying by barViewModel.nowPlaying.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Black,
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                DarkBottomBar(
                    currentRoute = currentRoute,
                    steps = steps,
                    nowPlaying = nowPlaying,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = DarkRoutes.ONBOARDING,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            composable(
                DarkRoutes.ONBOARDING_ROUTE,
                arguments = listOf(
                    androidx.navigation.navArgument("first") {
                        type = androidx.navigation.NavType.BoolType
                        defaultValue = true
                    }
                )
            ) {
                val firstLaunch = it.arguments?.getBoolean("first") ?: true
                OnboardingScreen(
                    viewModel = onboardingVm,
                    firstLaunch = firstLaunch,
                    onComplete = {
                        navController.navigate(DarkRoutes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    },
                    onExit = { navController.popBackStack() }
                )
            }
            composable(DarkRoutes.HOME) {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = viewModel,
                    onOpenSettings = { navController.navigate(DarkRoutes.SETTINGS) },
                    onOpenTerminal = { navController.navigate(DarkRoutes.TERMINAL) },
                    onOpenHidden = { navController.navigate(DarkRoutes.HIDDEN) },
                    onOpenSetup = { navController.navigate(DarkRoutes.ONBOARDING_FROM_APP) }
                )
            }
            composable(DarkRoutes.SETTINGS) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenHidden = { navController.navigate(DarkRoutes.HIDDEN) },
                    onOpenSetup = { navController.navigate(DarkRoutes.ONBOARDING_FROM_APP) }
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
                    onBack = { navController.popBackStack() },
                    onOpenSetup = { navController.navigate(DarkRoutes.ONBOARDING_FROM_APP) }
                )
            }
            composable(DarkRoutes.RECORDER) {
                val viewModel: RecorderViewModel = hiltViewModel()
                RecorderScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
