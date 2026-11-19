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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.AppLanguage
import com.example.core.attemptAutoSignIn
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.onGoogleSignInClicked
import com.example.core.signOutWithCleanup
import com.example.core.stringsFor
import com.example.data.local.AppDatabase
import com.example.data.local.GamePreferencesRepository
import com.example.data.local.MiningHistoryRepository
import com.example.data.local.TradeHistoryRepository
import com.example.data.remote.CloudSyncRepository
import com.example.ui.components.FixedSimulationTopBar
import com.example.ui.components.HaigenxBottomBar
import com.example.ui.components.MainTab
import com.example.ui.components.ROUTE_SETTINGS
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MarketScreen
import com.example.ui.screens.MiningScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.HaigenxTheme
import com.example.ui.theme.ObsidianBlack
import com.example.viewmodel.GameViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase ops
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(ObsidianBlack.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(ObsidianBlack.toArgb())
        )

        setContent {
            HaigenxTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val prefsRepo = remember(context) { GamePreferencesRepository(context.applicationContext) }
    val savedPrefs by prefsRepo.gameDataFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.local.PersistedGameData()
    )

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var authChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = { authChecked = true },
                onUnauthenticated = { authChecked = true }
            )
        } else {
            authChecked = true
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            language = savedPrefs.language,
            onSelectLanguage = { newLang ->
                scope.launch { prefsRepo.saveLanguage(newLang) }
            },
            onAuthSuccess = { authChecked = true }
        )
    } else {
        // Key by uid so signing out or switching Google accounts cleanly resets ViewModel & Firestore listeners
        key(user.uid) {
            HaigenxAuthenticatedApp(
                firebaseUser = user,
                credentialManager = credentialManager
            )
        }
    }
}

@Composable
fun HaigenxAuthenticatedApp(
    firebaseUser: FirebaseUser,
    credentialManager: CredentialManager
) {
    val uid = firebaseUser.uid
    val displayName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
        ?: firebaseUser.email?.substringBefore("@")
        ?: "HAIGENX1 Miner"

    val gameViewModel: GameViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val roomDb = AppDatabase.getInstance(app)
                GameViewModel(
                    currentUserId = uid,
                    displayName = displayName,
                    prefsRepo = GamePreferencesRepository(app),
                    miningHistoryRepo = MiningHistoryRepository(roomDb.miningRecordDao()),
                    tradeHistoryRepo = TradeHistoryRepository(roomDb.tradeRecordDao()),
                    cloudSyncRepo = CloudSyncRepository(db)
                )
            }
        }
    )

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: MainTab.MINING.route

    val context = LocalContext.current
    val language by gameViewModel.language.collectAsStateWithLifecycle()
    val miningState by gameViewModel.miningState.collectAsStateWithLifecycle()
    val marketState by gameViewModel.marketState.collectAsStateWithLifecycle()
    val tradingState by gameViewModel.tradingState.collectAsStateWithLifecycle()
    val adBoostDialogState by gameViewModel.adBoostDialogState.collectAsStateWithLifecycle()
    val chatMessages by gameViewModel.chatMessages.collectAsStateWithLifecycle()
    val aiUiState by gameViewModel.aiUiState.collectAsStateWithLifecycle()
    val miningHistory by gameViewModel.miningHistory.collectAsStateWithLifecycle()
    val tradeHistory by gameViewModel.tradeHistory.collectAsStateWithLifecycle()
    val participantCount by gameViewModel.participantCount.collectAsStateWithLifecycle()
    val accountDisplayName by gameViewModel.accountDisplayName.collectAsStateWithLifecycle()

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
                    participantCount = participantCount,
                    language = language,
                    strings = strings,
                    onMineTap = { gameViewModel.onMineTap() },
                    onOpenShopTab = {
                        navController.navigate(MainTab.SHOP.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(MainTab.SHOP.route) {
                ShopScreen(
                    miningState = miningState,
                    upgradeItems = gameViewModel.getUpgradeItems(strings),
                    language = language,
                    strings = strings,
                    onPurchaseUpgrade = { type -> gameViewModel.purchaseUpgrade(type) }
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
                    tradingState = tradingState,
                    tradeHistory = tradeHistory,
                    minedBalanceHx1 = miningState.balanceHx1,
                    participantCount = participantCount,
                    language = language,
                    strings = strings,
                    onSelectLeverage = { lev -> gameViewModel.selectLeverage(lev) },
                    onOpenTrade = { dir -> gameViewModel.openTrade(dir) }
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
                    accountDisplayName = accountDisplayName,
                    accountEmail = firebaseUser.email,
                    currentLanguage = language,
                    strings = strings,
                    onSelectLanguage = { newLang -> gameViewModel.setLanguage(newLang) },
                    onSaveDisplayName = { newName -> gameViewModel.updateDisplayName(newName) },
                    onSwitchGoogleAccount = {
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {},
                            onAuthError = {},
                            scope = scope
                        )
                    },
                    onSignOut = {
                        signOutWithCleanup(
                            credentialManager = credentialManager,
                            scope = scope,
                            onBeforeFirebaseSignOut = {
                                gameViewModel.flushStateBeforeSignOut()
                            }
                        )
                    },
                    onNavigateBack = { navController.popBackStack() },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    }
}
