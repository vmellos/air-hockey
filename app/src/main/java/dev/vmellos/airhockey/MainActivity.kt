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
import dev.vmellos.airhockey.feature.lobby.LobbyRole
import dev.vmellos.airhockey.feature.lobby.LobbyViewModel
import dev.vmellos.airhockey.feature.lobby.ui.BezelCalibrationScreen
import dev.vmellos.airhockey.feature.lobby.ui.LobbyScreen
import dev.vmellos.airhockey.feature.menu.ui.MainMenuScreen
import dev.vmellos.airhockey.feature.settings.ui.SettingsScreen
import dev.vmellos.airhockey.ui.theme.AirHockeyTheme

class MainActivity : ComponentActivity() {

    private var deepLinkUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

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
                    gameViewModel.initializeMatch(
                        mode = GameMode.SOLO,
                        bezelGapMm = userPrefs.bezelGapMm,
                        targetScore = userPrefs.targetScore,
                        matchTimeSeconds = userPrefs.matchTimeSeconds,
                        aiDifficulty = userPrefs.aiDifficulty
                    )
                    navController.navigate("game/SOLO")
                },
                onStartSplit = {
                    gameViewModel.initializeMatch(
                        mode = GameMode.SPLIT_SCREEN,
                        bezelGapMm = userPrefs.bezelGapMm,
                        targetScore = userPrefs.targetScore,
                        matchTimeSeconds = userPrefs.matchTimeSeconds
                    )
                    navController.navigate("game/SPLIT_SCREEN")
                },
                onStartExtended = {
                    navController.navigate("lobby")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("lobby") {
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
            BezelCalibrationScreen(
                role = lobbyState.role,
                initialGapMm = userPrefs.bezelGapMm,
                onConfirmGap = { gapMm ->
                    val isHost = lobbyState.role == LobbyRole.HOST
                    val gameMode = if (isHost) GameMode.EXTENDED_HOST else GameMode.EXTENDED_GUEST

                    // Conecta canais UDP e TCP do lobby com o GameViewModel
                    if (isHost) {
                        gameViewModel.onHostBroadcastTelemetry = { px, py, pvx, pvy, hmx, hmy ->
                            lobbyViewModel.udpSocket.sendHostTelemetry(px, py, pvx, pvy, hmx, hmy)
                        }
                        lobbyViewModel.udpSocket.onGuestInputReceived = { input ->
                            gameViewModel.onHostReceiveGuestInput(input.guestTargetX, input.guestTargetY)
                        }
                        gameViewModel.onHostGoalScored = { scorer, p1, p2 ->
                            lobbyViewModel.tcpServer.sendMessage("GOAL:$scorer,$p1,$p2")
                        }
                    } else {
                        lobbyViewModel.udpSocket.onHostTelemetryReceived = { t ->
                            gameViewModel.onGuestReceiveTelemetry(
                                t.puckX, t.puckY,
                                t.puckVx, t.puckVy,
                                t.hostMalletX, t.hostMalletY
                            )
                        }
                        gameViewModel.onGuestSendInput = { gx, gy ->
                            lobbyViewModel.udpSocket.sendGuestInput(gx, gy)
                        }
                    }

                    gameViewModel.initializeMatch(
                        mode = gameMode,
                        bezelGapMm = gapMm,
                        targetScore = userPrefs.targetScore,
                        matchTimeSeconds = userPrefs.matchTimeSeconds
                    )

                    navController.navigate("game/${gameMode.name}") {
                        popUpTo("menu")
                    }
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
                    lobbyViewModel.stopAll()
                    navController.navigate("menu") {
                        popUpTo("menu") { inclusive = true }
                    }
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
