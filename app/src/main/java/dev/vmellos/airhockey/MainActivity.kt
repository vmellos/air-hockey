package dev.vmellos.airhockey

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.vmellos.airhockey.core.localization.LocaleHelper
import dev.vmellos.airhockey.core.localization.ProvideAppLocale
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.GameViewModel
import dev.vmellos.airhockey.feature.game.ui.GameScreen
import dev.vmellos.airhockey.feature.leaderboard.ui.LeaderboardScreen
import dev.vmellos.airhockey.feature.lobby.LobbyRole
import dev.vmellos.airhockey.feature.lobby.LobbyViewModel
import dev.vmellos.airhockey.feature.lobby.ui.BezelCalibrationScreen
import dev.vmellos.airhockey.feature.lobby.ui.LobbyScreen
import dev.vmellos.airhockey.feature.menu.ui.MainMenuScreen
import dev.vmellos.airhockey.feature.network.ControlMessages
import dev.vmellos.airhockey.feature.settings.ui.SettingsScreen
import dev.vmellos.airhockey.ui.theme.AirHockeyTheme

class MainActivity : ComponentActivity() {

    private var deepLinkUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.decorView.keepScreenOn = true

        AirHockeyApp.instance.adManager.initialize(this)

        deepLinkUri = intent?.dataString

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

    override fun onPause() {
        super.onPause()
        AirHockeyApp.instance.audioEngine.stopAll()
    }

    override fun onStop() {
        super.onStop()
        AirHockeyApp.instance.audioEngine.stopAll()
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
    val gameViewModel: GameViewModel = viewModel()
    val lobbyViewModel: LobbyViewModel = viewModel()

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
                    val p1 = userPrefs.playerName.ifBlank { "Você" }
                    val diff = when (userPrefs.aiDifficulty) {
                        dev.vmellos.airhockey.core.preferences.AiDifficulty.EASY -> "Fácil"
                        dev.vmellos.airhockey.core.preferences.AiDifficulty.MEDIUM -> "Médio"
                        dev.vmellos.airhockey.core.preferences.AiDifficulty.HARD -> "Difícil"
                    }
                    val puckPreset = when (userPrefs.aiDifficulty) {
                        dev.vmellos.airhockey.core.preferences.AiDifficulty.EASY -> dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.SLOW
                        dev.vmellos.airhockey.core.preferences.AiDifficulty.MEDIUM -> dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.NORMAL
                        dev.vmellos.airhockey.core.preferences.AiDifficulty.HARD -> dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.FAST
                    }
                    gameViewModel.initializeMatch(
                        mode = GameMode.SOLO,
                        bezelGapMm = userPrefs.bezelGapMm,
                        targetScore = userPrefs.targetScore,
                        matchTimeSeconds = userPrefs.matchTimeSeconds,
                        aiDifficulty = userPrefs.aiDifficulty,
                        puckMaxSpeed = puckPreset.maxSpeed,
                        airFriction = puckPreset.airFriction,
                        player1Name = p1,
                        player2Name = "IA ($diff)"
                    )
                    navController.navigate("game/SOLO")
                },
                onStartSplit = {
                    val puckPreset = dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.NORMAL
                    gameViewModel.initializeMatch(
                        mode = GameMode.SPLIT_SCREEN,
                        bezelGapMm = userPrefs.bezelGapMm,
                        targetScore = userPrefs.targetScore,
                        matchTimeSeconds = userPrefs.matchTimeSeconds,
                        puckMaxSpeed = puckPreset.maxSpeed,
                        airFriction = puckPreset.airFriction,
                        player1Name = "Jogador 1",
                        player2Name = "Jogador 2"
                    )
                    navController.navigate("game/SPLIT_SCREEN")
                },
                onStartConnected = {
                    lobbyViewModel.setTargetMode("CONNECTED")
                    navController.navigate("lobby")
                },
                onStartExtended = {
                    lobbyViewModel.setTargetMode("EXTENDED")
                    navController.navigate("lobby")
                },
                onOpenLeaderboard = {
                    navController.navigate("leaderboard")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("lobby") {
            val lobbyState by lobbyViewModel.state.collectAsState()
            val isHost = lobbyState.role == LobbyRole.HOST

            // Para Mesa Conectada: inicia a partida diretamente quando startMatchTrigger for acionado (sem passar por calibração)
            LaunchedEffect(lobbyState.startMatchTrigger) {
                if (lobbyState.startMatchTrigger > 0L && lobbyState.targetMode == "CONNECTED") {
                    val gameMode = if (isHost) GameMode.CONNECTED_HOST else GameMode.CONNECTED_GUEST
                    val effectivePuckSpeed = if (isHost) userPrefs.puckSpeed.maxSpeed else lobbyState.synchronizedPuckSpeed
                    val p1Name = if (isHost) {
                        lobbyState.selfPlayerName.ifBlank { "Jogador 1" }
                    } else {
                        lobbyState.peerPlayerName.ifBlank { "Jogador 1" }
                    }
                    val p2Name = if (isHost) {
                        lobbyState.peerPlayerName.ifBlank { "Jogador 2" }
                    } else {
                        lobbyState.selfPlayerName.ifBlank { "Jogador 2" }
                    }

                    lobbyViewModel.consumeStartMatchTrigger()

                    gameViewModel.initializeMatch(
                        mode = gameMode,
                        bezelGapMm = 0f,
                        targetScore = userPrefs.targetScore,
                        matchTimeSeconds = userPrefs.matchTimeSeconds,
                        puckMaxSpeed = effectivePuckSpeed,
                        airFriction = userPrefs.puckSpeed.airFriction,
                        player1Name = p1Name,
                        player2Name = p2Name
                    )

                    lobbyViewModel.coordinator?.let { gameViewModel.attachCoordinator(it) }

                    navController.navigate("game/${gameMode.name}") {
                        popUpTo("menu")
                    }
                }
            }

            // Para Mesa Estendida: navega o convidado automaticamente quando o anfitrião avança para a calibração
            LaunchedEffect(lobbyState.navigateToCalibrationTrigger) {
                if (lobbyState.navigateToCalibrationTrigger > 0L && lobbyState.targetMode == "EXTENDED") {
                    lobbyViewModel.consumeCalibrationTrigger()
                    navController.navigate("calibration")
                }
            }

            LobbyScreen(
                viewModel = lobbyViewModel,
                onNavigateToCalibration = {
                    navController.navigate("calibration")
                },
                onBackToMenu = {
                    lobbyViewModel.stopAll()
                    navController.popBackStack()
                }
            )
        }

        composable("calibration") {
            val lobbyState by lobbyViewModel.state.collectAsState()
            val isHost = lobbyState.role == LobbyRole.HOST
            val gameMode = if (isHost) GameMode.EXTENDED_HOST else GameMode.EXTENDED_GUEST

            // Largada Sincronizada: quando o gatilho de largada é acionado, ambos iniciam no mesmo instante
            LaunchedEffect(lobbyState.startMatchTrigger) {
                if (lobbyState.startMatchTrigger > 0L) {
                    val effectivePuckSpeed = if (isHost) userPrefs.puckSpeed.maxSpeed else lobbyState.synchronizedPuckSpeed
                    val p1Name = if (isHost) {
                        lobbyState.selfPlayerName.ifBlank { "Jogador 1" }
                    } else {
                        lobbyState.peerPlayerName.ifBlank { "Jogador 1" }
                    }
                    val p2Name = if (isHost) {
                        lobbyState.peerPlayerName.ifBlank { "Jogador 2" }
                    } else {
                        lobbyState.selfPlayerName.ifBlank { "Jogador 2" }
                    }

                    lobbyViewModel.consumeStartMatchTrigger()

                    gameViewModel.initializeMatch(
                        mode = gameMode,
                        bezelGapMm = lobbyState.calibratedGapMm,
                        targetScore = userPrefs.targetScore,
                        matchTimeSeconds = userPrefs.matchTimeSeconds,
                        puckMaxSpeed = effectivePuckSpeed,
                        airFriction = userPrefs.puckSpeed.airFriction,
                        player1Name = p1Name,
                        player2Name = p2Name
                    )

                    lobbyViewModel.coordinator?.let { gameViewModel.attachCoordinator(it) }

                    navController.navigate("game/${gameMode.name}") {
                        popUpTo("menu")
                    }
                }
            }

            BezelCalibrationScreen(
                role = lobbyState.role,
                initialGapMm = userPrefs.bezelGapMm,
                isPeerReady = lobbyState.isPeerReady,
                isSelfReady = lobbyState.isSelfReady,
                onReadyClick = { gapMm ->
                    if (isHost) {
                        lobbyViewModel.startHostMatch(gapMm, userPrefs.puckSpeed.maxSpeed)
                    } else {
                        lobbyViewModel.setGuestReady(gapMm)
                    }
                },
                onBackToLobby = {
                    lobbyViewModel.sendControlMessage(ControlMessages.EXIT)
                    lobbyViewModel.stopAll()
                    navController.popBackStack("lobby", inclusive = false)
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
    }
}
