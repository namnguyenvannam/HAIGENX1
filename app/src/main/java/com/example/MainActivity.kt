package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.configureAppCheck
import com.example.core.stringsFor
import com.example.ui.components.FixedSimulationTopBar
import com.example.ui.components.HaigenxBottomBar
import com.example.ui.components.MainTab
import com.example.ui.components.ROUTE_SETTINGS
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.MarketScreen
import com.example.ui.screens.MiningScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.HaigenxTheme
import com.example.ui.theme.ObsidianBlack
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(ObsidianBlack.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(ObsidianBlack.toArgb())
        )

        setContent {
            HaigenxTheme {
                HaigenxMinerApp()
            }
        }
    }
}

@Composable
fun HaigenxMinerApp(
    gameViewModel: GameViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: MainTab.MINING.route

    val language by gameViewModel.language.collectAsStateWithLifecycle()
    val miningState by gameViewModel.miningState.collectAsStateWithLifecycle()
    val marketState by gameViewModel.marketState.collectAsStateWithLifecycle()
    val adBoostDialogState by gameViewModel.adBoostDialogState.collectAsStateWithLifecycle()
    val chatMessages by gameViewModel.chatMessages.collectAsStateWithLifecycle()
    val aiUiState by gameViewModel.aiUiState.collectAsStateWithLifecycle()
    val miningHistory by gameViewModel.miningHistory.collectAsStateWithLifecycle()

    val strings = stringsFor(language)
    val isSettings = currentRoute == ROUTE_SETTINGS

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            FixedSimulationTopBar(
                strings = strings,
                isBoostActive = miningState.isBoostActive,
                boostRemainingSeconds = miningState.boostRemainingSeconds,
                isSettingsScreen = isSettings,
                onOpenSettings = {
                    if (!isSettings) {
                        navController.navigate(ROUTE_SETTINGS) {
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        },
        bottomBar = {
            if (!isSettings) {
                HaigenxBottomBar(
                    currentRoute = currentRoute,
                    strings = strings,
                    onSelectTab = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.MINING.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = {
                val fromIdx = MainTab.entries.indexOfFirst { it.route == initialState.destination.route }
                val toIdx = MainTab.entries.indexOfFirst { it.route == targetState.destination.route }
                val direction = if (fromIdx >= 0 && toIdx >= 0 && toIdx < fromIdx) {
                    AnimatedContentTransitionScope.SlideDirection.Right
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Left
                }
                slideIntoContainer(towards = direction, animationSpec = tween(240)) +
                    fadeIn(animationSpec = tween(220))
            },
            exitTransition = {
                val fromIdx = MainTab.entries.indexOfFirst { it.route == initialState.destination.route }
                val toIdx = MainTab.entries.indexOfFirst { it.route == targetState.destination.route }
                val direction = if (fromIdx >= 0 && toIdx >= 0 && toIdx < fromIdx) {
                    AnimatedContentTransitionScope.SlideDirection.Right
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Left
                }
                slideOutOfContainer(towards = direction, animationSpec = tween(220)) +
                    fadeOut(animationSpec = tween(180))
            }
        ) {
            composable(MainTab.MINING.route) {
                MiningScreen(
                    state = miningState,
                    language = language,
                    strings = strings,
                    onMineTap = { gameViewModel.onMineTap() }
                )
            }

            composable(MainTab.AI.route) {
                AiAssistantScreen(
                    miningState = miningState,
                    chatMessages = chatMessages,
                    aiUiState = aiUiState,
                    adBoostDialogState = adBoostDialogState,
                    strings = strings,
                    onStartAiBoost = { gameViewModel.startSimulatedAdBoostWait() },
                    onCancelAdBoost = { gameViewModel.cancelSimulatedAdWait() },
                    onSendMessage = { prompt -> gameViewModel.sendAiMessage(prompt) },
                    onRetryLast = { gameViewModel.retryLastAiMessage() }
                )
            }

            composable(MainTab.MARKET.route) {
                MarketScreen(
                    state = marketState,
                    minedBalanceHx1 = miningState.balanceHx1,
                    language = language,
                    strings = strings
                )
            }

            composable(MainTab.WALLET.route) {
                WalletScreen(
                    balanceHx1 = miningState.balanceHx1,
                    currentPriceUsdt = marketState.currentPriceUsdt,
                    isPriceUp = marketState.isPriceUp,
                    tickCounter = marketState.tickCounter,
                    historyRecords = miningHistory,
                    language = language,
                    strings = strings
                )
            }

            composable(
                route = ROUTE_SETTINGS,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(240)
                    ) + fadeIn(animationSpec = tween(220))
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(220)
                    ) + fadeOut(animationSpec = tween(180))
                }
            ) {
                SettingsScreen(
                    currentLanguage = language,
                    strings = strings,
                    onSelectLanguage = { newLang -> gameViewModel.setLanguage(newLang) },
                    onNavigateBack = { navController.popBackStack() },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    }
}
