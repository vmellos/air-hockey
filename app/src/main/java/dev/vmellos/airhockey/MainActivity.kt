package dev.vmellos.airhockey

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.vmellos.airhockey.core.update.InAppUpdateEngine
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.vmellos.airhockey.core.localization.LocaleHelper
import dev.vmellos.airhockey.core.localization.ProvideAppLocale
import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.GameViewModel
import dev.vmellos.airhockey.feature.game.ui.GameScreen
import dev.vmellos.airhockey.feature.leaderboard.ui.LeaderboardScreen
import dev.vmellos.airhockey.feature.lobby.LobbyRole
import dev.vmellos.airhockey.feature.lobby.LobbyViewModel
import dev.vmellos.airhockey.feature.lobby.ui.LobbyScreen
import dev.vmellos.airhockey.feature.menu.ui.MainMenuScreen
import dev.vmellos.airhockey.feature.stage.ui.StageSelectionScreen
import dev.vmellos.airhockey.feature.network.ControlMessages
import dev.vmellos.airhockey.feature.settings.ui.SettingsScreen
import dev.vmellos.airhockey.feature.customization.ui.ArenaCustomizationScreen
import dev.vmellos.airhockey.feature.customization.ui.PreMatchArenaSelectionScreen
import dev.vmellos.airhockey.ui.theme.AirHockeyTheme

class MainActivity : ComponentActivity() {

    private var deepLinkUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.decorView.keepScreenOn = true

        AirHockeyApp.instance.adManager.initialize(this)

        deepLinkUri = intent?.dataString

        // Autenticação silenciosa e conexão do Google Play Games Services v2
        AirHockeyApp.instance.playGamesManager.authenticateSilently(this)
        AirHockeyApp.instance.stageRepository.setOnProgressUpdatedListener {
            AirHockeyApp.instance.playGamesSaveManager.syncProgressAsync(this)
        }

        lifecycleScope.launch {
            delay(500)
            AirHockeyApp.instance.inAppUpdateEngine.checkForUpdate(this@MainActivity)
        }

        setContent {
            val preferencesRepo = AirHockeyApp.instance.preferencesRepository
            val userPrefs by preferencesRepo.userPreferencesFlow.collectAsState(initial = UserPreferences())

            // Sincroniza idioma reativo na UI via ProvideAppLocale do iptv_app
            ProvideAppLocale(languageCode = userPrefs.appLanguage) {
                AirHockeyTheme {
                    AirHockeyAppNavigation(
                        userPrefs = userPrefs,
                        deepLinkUri = deepLinkUri
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AirHockeyApp.instance.playGamesManager.attachActivity(this)
        AirHockeyApp.instance.inAppUpdateEngine.checkDownloadedStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        AirHockeyApp.instance.audioEngine.stopAll()
        AirHockeyApp.instance.playGamesManager.detachActivity(this)
    }

    override fun onPause() {
        super.onPause()
        AirHockeyApp.instance.audioEngine.stopAll()
    }

    override fun onStop() {
        super.onStop()
        AirHockeyApp.instance.audioEngine.stopAll()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == InAppUpdateEngine.REQUEST_CODE_FLEXIBLE_UPDATE) {
            AirHockeyApp.instance.inAppUpdateEngine.onUpdateFlowResult(resultCode)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkUri = intent.dataString
    }
}

@Composable
fun AirHockeyAppNavigation(
    userPrefs: UserPreferences,
    deepLinkUri: String? = null,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val gameViewModel: GameViewModel = viewModel()
    val lobbyViewModel: LobbyViewModel = viewModel()
    var aiExtendedDiff1 by rememberSaveable { mutableStateOf(dev.vmellos.airhockey.core.preferences.AiDifficulty.MEDIUM) }
    var aiExtendedDiff2 by rememberSaveable { mutableStateOf(dev.vmellos.airhockey.core.preferences.AiDifficulty.MEDIUM) }

    fun getAiDiffLabel(diff: dev.vmellos.airhockey.core.preferences.AiDifficulty): String {
        return when (diff) {
            dev.vmellos.airhockey.core.preferences.AiDifficulty.EASY -> context.getString(R.string.settings_ai_easy)
            dev.vmellos.airhockey.core.preferences.AiDifficulty.MEDIUM -> context.getString(R.string.settings_ai_medium)
            dev.vmellos.airhockey.core.preferences.AiDifficulty.HARD -> context.getString(R.string.settings_ai_hard)
        }
    }

    // Processa Deep Link (ex: câmera nativa lendo QR code airhockey://connect)
    LaunchedEffect(deepLinkUri) {
        val uri = deepLinkUri
        if (!uri.isNullOrBlank() && uri.startsWith("airhockey://connect")) {
            lobbyViewModel.handleQrScanResult(uri)
            navController.navigate("lobby") {
                popUpTo("menu")
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "menu",
        modifier = modifier.fillMaxSize()
    ) {
        composable("menu") {
            MainMenuScreen(
                onStartSolo = {
                    navController.navigate("stages")
                },
                onStartSplit = {
                    navController.navigate("pre-match/SPLIT_SCREEN")
                },
                onStartNetwork = {
                    lobbyViewModel.resetToRoleSelection()
                    navController.navigate("lobby")
                },
                onOpenLeaderboard = {
                    navController.navigate("leaderboard")
                },
                onOpenCustomization = {
                    navController.navigate("customization")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("lobby") {
            val lobbyState by lobbyViewModel.state.collectAsState()
            val isHost = lobbyState.role == LobbyRole.HOST

            // Início direto da partida para todos os modos em rede (Mesa Conectada, Mesa Estendida e IA x IA Estendida)
            LaunchedEffect(lobbyState.startMatchTrigger) {
                if (lobbyState.startMatchTrigger > 0L) {
                    val isAiExtended = lobbyState.targetMode == "AI_EXTENDED" || lobbyState.targetMode == "AI_CONNECTED"
                    val p1Label = if (isAiExtended) {
                        val d1 = if (isHost) (AiDifficulty.entries.find { it.name.equals(lobbyState.aiDifficulty1, ignoreCase = true) } ?: aiExtendedDiff1)
                                 else (AiDifficulty.entries.find { it.name.equals(lobbyState.aiDifficulty1, ignoreCase = true) } ?: AiDifficulty.MEDIUM)
                        context.getString(R.string.bot_name_with_difficulty, context.getString(R.string.bot_1_label), getAiDiffLabel(d1))
                    } else null

                    val p2Label = if (isAiExtended) {
                        val d2 = if (isHost) (AiDifficulty.entries.find { it.name.equals(lobbyState.aiDifficulty2, ignoreCase = true) } ?: aiExtendedDiff2)
                                 else (AiDifficulty.entries.find { it.name.equals(lobbyState.aiDifficulty2, ignoreCase = true) } ?: AiDifficulty.MEDIUM)
                        context.getString(R.string.bot_name_with_difficulty, context.getString(R.string.bot_2_label), getAiDiffLabel(d2))
                    } else null

                    val spec = lobbyViewModel.buildMatchSpec(
                        userPrefs = userPrefs,
                        aiExtendedDiff1 = aiExtendedDiff1,
                        aiExtendedDiff2 = aiExtendedDiff2,
                        p1CustomLabel = p1Label,
                        p2CustomLabel = p2Label
                    )

                    lobbyViewModel.consumeStartMatchTrigger()
                    gameViewModel.initializeMatch(spec)
                    lobbyViewModel.coordinator?.let { gameViewModel.attachCoordinator(it) }

                    navController.navigate("game/${spec.mode.name}") {
                        popUpTo("menu")
                    }
                }
            }

            LobbyScreen(
                viewModel = lobbyViewModel,
                onStartHostMatch = {
                    val gap = if (lobbyState.targetMode == "CONNECTED" || lobbyState.targetMode == "AI_CONNECTED") 0f else userPrefs.bezelGapMm
                    val d1 = if (lobbyState.targetMode == "AI_EXTENDED" || lobbyState.targetMode == "AI_CONNECTED") {
                        if (lobbyState.aiDifficulty1.isNotBlank()) lobbyState.aiDifficulty1 else aiExtendedDiff1.name
                    } else ""
                    val d2 = if (lobbyState.targetMode == "AI_EXTENDED" || lobbyState.targetMode == "AI_CONNECTED") {
                        if (lobbyState.aiDifficulty2.isNotBlank()) lobbyState.aiDifficulty2 else aiExtendedDiff2.name
                    } else ""
                    val dim = lobbyState.hostArenaDimension ?: userPrefs.arenaDimension.name
                    lobbyViewModel.startHostMatch(gap, userPrefs.puckSpeed.maxSpeed, d1, d2, userPrefs.tableTheme.name, dim)
                },
                onBackToMenu = {
                    lobbyViewModel.stopAll()
                    navController.popBackStack()
                },
                onNavigateToStages = {
                    navController.navigate("stages") {
                        popUpTo("menu")
                    }
                },
                onStartAiVsAiLocal = { diff1, diff2, chosenTheme, chosenDimension ->
                    val puckPreset = when (diff1) {
                        dev.vmellos.airhockey.core.preferences.AiDifficulty.EASY -> dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.SLOW
                        dev.vmellos.airhockey.core.preferences.AiDifficulty.MEDIUM -> dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.NORMAL
                        dev.vmellos.airhockey.core.preferences.AiDifficulty.HARD -> dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.FAST
                    }
                    val d1Str = getAiDiffLabel(diff1)
                    val d2Str = getAiDiffLabel(diff2)
                    val is3d = chosenDimension == dev.vmellos.airhockey.core.preferences.ArenaDimension.THREE_DIMENSIONAL
                    val hasIntro = is3d && userPrefs.arena3dIntroEnabled
                    gameViewModel.initializeMatch(
                        mode = GameMode.AI_VS_AI_LOCAL,
                        bezelGapMm = userPrefs.bezelGapMm,
                        targetScore = userPrefs.targetScore,
                        matchTimeSeconds = userPrefs.matchTimeSeconds,
                        aiDifficulty = diff1,
                        aiDifficultyPlayer1 = diff1,
                        aiDifficultyPlayer2 = diff2,
                        puckMaxSpeed = puckPreset.maxSpeed,
                        airFriction = puckPreset.airFriction,
                        player1Name = context.getString(R.string.bot_name_with_difficulty, context.getString(R.string.bot_1_label), d1Str),
                        player2Name = context.getString(R.string.bot_name_with_difficulty, context.getString(R.string.bot_2_label), d2Str),
                        tableTheme = chosenTheme,
                        hasIntroPresentation = hasIntro,
                        arenaDimension = chosenDimension
                    )
                    navController.navigate("game/AI_VS_AI_LOCAL") {
                        popUpTo("menu")
                    }
                }
            )
        }

        composable("pre-match/{mode}") { backStackEntry ->
            val modeStr = backStackEntry.arguments?.getString("mode") ?: "SPLIT_SCREEN"
            val isSplit = modeStr == "SPLIT_SCREEN"

            PreMatchArenaSelectionScreen(
                confirmButtonText = stringResource(R.string.pre_match_start_game),
                isSplitScreen = isSplit,
                onBack = { navController.popBackStack() },
                onNavigateToStages = {
                    navController.navigate("stages") {
                        popUpTo("menu")
                    }
                },
                onConfirm = { chosenTheme, chosenDimension ->
                    if (isSplit) {
                        val puckPreset = dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.NORMAL
                        val is3d = chosenDimension == dev.vmellos.airhockey.core.preferences.ArenaDimension.THREE_DIMENSIONAL
                        val hasIntro = is3d && userPrefs.arena3dIntroEnabled
                        gameViewModel.initializeMatch(
                            mode = GameMode.SPLIT_SCREEN,
                            bezelGapMm = userPrefs.bezelGapMm,
                            targetScore = userPrefs.targetScore,
                            matchTimeSeconds = userPrefs.matchTimeSeconds,
                            puckMaxSpeed = puckPreset.maxSpeed,
                            airFriction = puckPreset.airFriction,
                            player1Name = context.getString(R.string.player_1_default),
                            player2Name = context.getString(R.string.player_2_default),
                            tableTheme = chosenTheme,
                            arenaDimension = chosenDimension,
                            hasIntroPresentation = hasIntro
                        )
                        navController.navigate("game/SPLIT_SCREEN") {
                            popUpTo("menu")
                        }
                    }
                }
            )
        }

        composable("stages") {
            StageSelectionScreen(
                onSelectStage = { stageNum ->
                    val p1 = userPrefs.playerName.ifBlank { context.getString(R.string.player_you) }
                    val is3d = userPrefs.arenaDimension == dev.vmellos.airhockey.core.preferences.ArenaDimension.THREE_DIMENSIONAL
                    val hasIntro = is3d && userPrefs.arena3dIntroEnabled
                    gameViewModel.initializeMatch(
                        mode = GameMode.SOLO,
                        bezelGapMm = userPrefs.bezelGapMm,
                        player1Name = p1,
                        player2Name = context.getString(R.string.player_ai),
                        stageNumber = stageNum,
                        hasIntroPresentation = hasIntro,
                        arenaDimension = userPrefs.arenaDimension
                    )
                    navController.navigate("game/SOLO")
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "game/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val modeStr = backStackEntry.arguments?.getString("mode") ?: GameMode.SOLO.name
            val mode = try {
                GameMode.valueOf(modeStr)
            } catch (_: Exception) {
                GameMode.SOLO
            }

            GameScreen(
                viewModel = gameViewModel,
                mode = mode,
                onExitToMenu = {
                    gameViewModel.stopMatch()
                    lobbyViewModel.sendControlMessage(ControlMessages.EXIT)
                    lobbyViewModel.stopAll()
                    navController.navigate("menu") {
                        popUpTo("menu") { inclusive = true }
                    }
                },
                onExitToStages = {
                    gameViewModel.stopMatch()
                    navController.navigate("stages") {
                        popUpTo("stages") { inclusive = true }
                    }
                },
                onStartNextStage = { nextStage ->
                    val p1 = userPrefs.playerName.ifBlank { context.getString(R.string.player_you) }
                    val is3d = userPrefs.arenaDimension == dev.vmellos.airhockey.core.preferences.ArenaDimension.THREE_DIMENSIONAL
                    val hasIntro = is3d && userPrefs.arena3dIntroEnabled
                    gameViewModel.initializeMatch(
                        mode = GameMode.SOLO,
                        bezelGapMm = userPrefs.bezelGapMm,
                        player1Name = p1,
                        player2Name = context.getString(R.string.player_ai),
                        stageNumber = nextStage,
                        hasIntroPresentation = hasIntro,
                        arenaDimension = userPrefs.arenaDimension
                    )
                }
            )
        }

        composable("leaderboard") {
            LeaderboardScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("customization") {
            ArenaCustomizationScreen(
                onBack = { navController.popBackStack() },
                onNavigateToStages = {
                    navController.navigate("stages") {
                        popUpTo("menu")
                    }
                }
            )
        }
    }
}
