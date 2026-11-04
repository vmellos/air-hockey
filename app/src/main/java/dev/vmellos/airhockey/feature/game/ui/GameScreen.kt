package dev.vmellos.airhockey.feature.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.core.util.findActivity
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.GameViewModel
import dev.vmellos.airhockey.feature.game.MatchStatus
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    mode: GameMode,
    onExitToMenu: () -> Unit,
    onExitToStages: () -> Unit = onExitToMenu,
    onStartNextStage: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val matchState by viewModel.matchState.collectAsState()
    val isSoundEnabled by viewModel.soundEnabled.collectAsState()
    val isMusicEnabled by viewModel.musicEnabled.collectAsState()
    val prefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val window = context.findActivity()?.window

    // Oculta barras do sistema (controles físicos/virtuais e barra de status) durante a partida (Modo Imersivo)
    DisposableEffect(window) {
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Controle dinâmico da música de fundo (BGM) conforme o ciclo de vida, tema e estado do jogo
    LaunchedEffect(matchState.status, isMusicEnabled, prefs.tableTheme) {
        when (matchState.status) {
            MatchStatus.COUNTDOWN, MatchStatus.PLAYING, MatchStatus.GOAL_PAUSE -> {
                if (isMusicEnabled) {
                    AirHockeyApp.instance.audioEngine.playBgm(prefs.tableTheme)
                } else {
                    AirHockeyApp.instance.audioEngine.pauseBgm()
                }
            }
            MatchStatus.PAUSED -> {
                AirHockeyApp.instance.audioEngine.pauseBgm()
            }
            MatchStatus.GAME_OVER -> {
                AirHockeyApp.instance.audioEngine.stopBgm()
            }
            else -> {
                AirHockeyApp.instance.audioEngine.pauseBgm()
            }
        }
    }

    // O BackHandler nunca fecha o jogo sem confirmação:
    // Ele pausa o jogo e exibe o diálogo de opções (Continuar, Reiniciar, Sair para o Menu).
    BackHandler(enabled = true) {
        when {
            matchState.peerExited || matchState.status == MatchStatus.GAME_OVER -> {
                viewModel.stopMatch()
                onExitToMenu()
            }
            matchState.status == MatchStatus.PAUSED -> {
                // Já está no diálogo de pausa: permanece aguardando a seleção do usuário
            }
            else -> {
                // Em partida (PLAYING, COUNTDOWN, GOAL_PAUSE): pausa o jogo e espera ação
                viewModel.pauseMatch()
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.pauseMatch()
                AirHockeyApp.instance.audioEngine.stopAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopMatch()
            AirHockeyApp.instance.audioEngine.stopAll()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Canvas da Mesa de Jogo
        AirHockeyArenaCanvas(
            viewModel = viewModel,
            mode = mode,
            modifier = Modifier.fillMaxSize()
        )

        // Placar e Controles estilizados pelo tema ativo
        ScoreBoardOverlay(
            matchState = matchState,
            tableTheme = prefs.tableTheme,
            isSoundEnabled = isSoundEnabled,
            onToggleSound = { viewModel.toggleSound() },
            onPauseClick = { viewModel.pauseMatch() }
        )

        // Overlay de Contagem Regressiva estilizado por tema
        if (matchState.status == MatchStatus.COUNTDOWN) {
            CountdownOverlay(
                countdownNumber = matchState.countdownNumber,
                tableTheme = prefs.tableTheme
            )
        }

        // Overlay de Gol/Ponto estilizado por tema
        if (matchState.status == MatchStatus.GOAL_PAUSE) {
            val scorerName = if (matchState.lastScorer == 1) matchState.player1Name else matchState.player2Name
            GoalBannerOverlay(
                scorer = matchState.lastScorer,
                scorerName = scorerName,
                tableTheme = prefs.tableTheme
            )
        }

        // Diálogo de Pausa
        if (matchState.status == MatchStatus.PAUSED && !matchState.peerExited) {
            PauseDialog(
                isSoundEnabled = isSoundEnabled,
                onToggleSound = { viewModel.toggleSound() },
                isMusicEnabled = isMusicEnabled,
                onToggleMusic = { viewModel.toggleMusic() },
                onResume = { viewModel.resumeMatch() },
                onRestart = { viewModel.restartMatch() },
                onExit = {
                    viewModel.stopMatch()
                    onExitToMenu()
                }
            )
        }

        // Diálogo quando o adversário sai na Mesa Estendida
        if (matchState.peerExited) {
            PeerExitedDialog(
                onExit = {
                    viewModel.stopMatch()
                    onExitToMenu()
                }
            )
        }

        // Diálogo de Fim de Jogo
        if (matchState.status == MatchStatus.GAME_OVER) {
            val activity = LocalContext.current.findActivity()
            if (matchState.mode == GameMode.SOLO) {
                StageResultDialog(
                    stageNumber = matchState.currentStage,
                    starsEarned = matchState.starsEarned,
                    isWon = matchState.winner == 1,
                    scorePlayer = matchState.scorePlayer1,
                    scoreAi = matchState.scorePlayer2,
                    onSelectStages = {
                        if (activity != null) {
                            AirHockeyApp.instance.adManager.showInterstitialIfAllowed(activity) {
                                viewModel.stopMatch()
                                onExitToStages()
                            }
                        } else {
                            viewModel.stopMatch()
                            onExitToStages()
                        }
                    },
                    onRetry = {
                        // Não exibe anúncio intersticial na revanche/retry imediato para preservar a retenção do jogador
                        viewModel.restartMatch()
                    },
                    onNextStage = {
                        if (activity != null) {
                            AirHockeyApp.instance.adManager.showInterstitialIfAllowed(activity) {
                                viewModel.stopMatch()
                                onStartNextStage(matchState.currentStage + 1)
                            }
                        } else {
                            viewModel.stopMatch()
                            onStartNextStage(matchState.currentStage + 1)
                        }
                    }
                )
            } else {
                GameOverDialog(
                    matchState = matchState,
                    onPlayAgain = {
                        // Revanche imediata sem anúncio para fluidez contínua de jogo
                        viewModel.restartMatch()
                    },
                    onExit = {
                        if (activity != null) {
                            AirHockeyApp.instance.adManager.showInterstitialIfAllowed(activity) {
                                viewModel.stopMatch()
                                onExitToMenu()
                            }
                        } else {
                            viewModel.stopMatch()
                            onExitToMenu()
                        }
                    }
                )
            }
        }
    }
}
