package dev.vmellos.airhockey.feature.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.preferences.CameraZoom3D
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.core.preferences.VisualEffectsProfile
import dev.vmellos.airhockey.core.util.findActivity
import dev.vmellos.airhockey.feature.game.ArenaRuntime
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.MatchStatus
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    runtime: ArenaRuntime,
    mode: GameMode,
    onExitToMenu: () -> Unit,
    onExitToStages: () -> Unit = onExitToMenu,
    onStartNextStage: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val matchState by runtime.matchState.collectAsState()
    val isSoundEnabled by runtime.soundEnabled.collectAsState()
    val isMusicEnabled by runtime.musicEnabled.collectAsState()
    val prefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val effectiveTheme = matchState.tableTheme ?: prefs.tableTheme
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
    LaunchedEffect(matchState.status, isMusicEnabled, effectiveTheme) {
        when (matchState.status) {
            MatchStatus.PRESENTATION, MatchStatus.COUNTDOWN, MatchStatus.PLAYING, MatchStatus.GOAL_PAUSE -> {
                if (isMusicEnabled) {
                    AirHockeyApp.instance.audioEngine.playBgm(effectiveTheme)
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
                AirHockeyApp.instance.audioEngine.stopBgm()
            }
        }
    }

    // O BackHandler nunca fecha o jogo sem confirmação:
    // Ele pausa o jogo e exibe o diálogo de opções (Continuar, Reiniciar, Sair para o Menu).
    BackHandler(enabled = true) {
        when {
            matchState.peerExited || matchState.status == MatchStatus.GAME_OVER -> {
                runtime.exitMatch()
                onExitToMenu()
            }
            matchState.status == MatchStatus.PAUSED -> {
                // Já está no diálogo de pausa: permanece aguardando a seleção do usuário
            }
            else -> {
                // Em partida (PLAYING, COUNTDOWN, GOAL_PAUSE): pausa o jogo e espera ação
                runtime.requestPause()
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                runtime.requestPause()
                AirHockeyApp.instance.audioEngine.stopAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runtime.exitMatch()
            AirHockeyApp.instance.audioEngine.stopAll()
        }
    }

    val isExtended = matchState.isExtendedMode
    val is3dActive = matchState.arenaDimension == dev.vmellos.airhockey.core.preferences.ArenaDimension.THREE_DIMENSIONAL
    val shouldPresent = is3dActive && prefs.arena3dIntroEnabled && !isExtended
    var is3dPresenting by remember(matchState.status) {
        mutableStateOf(matchState.status == MatchStatus.PRESENTATION)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Mesa de Jogo (Modo 3D Sceneview quando Dimensão 3D estiver ativa, ou 2D Canvas)
        if (is3dActive) {
            AirHockeyArena3DScene(
                runtime = runtime,
                mode = mode,
                zoom = prefs.cameraZoom3d,
                tableTheme = effectiveTheme,
                onPresentationFinished = {
                    is3dPresenting = false
                    runtime.startPresentationCompleted()
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AirHockeyArenaCanvas(
                runtime = runtime,
                mode = mode,
                tableTheme = effectiveTheme,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Placar e Controles estilizados pelo tema ativo
        ScoreBoardOverlay(
            matchState = matchState,
            tableTheme = effectiveTheme,
            isSoundEnabled = isSoundEnabled,
            onToggleSound = { runtime.toggleSound() },
            onPauseClick = { runtime.requestPause() }
        )

        // Overlay de Contagem Regressiva estilizado por tema (iniciado após a apresentação 3D)
        if (matchState.status == MatchStatus.COUNTDOWN && !is3dPresenting) {
            CountdownOverlay(
                countdownNumber = matchState.countdownNumber,
                tableTheme = effectiveTheme
            )
        }

        // Overlay de Gol/Ponto estilizado por tema
        if (matchState.status == MatchStatus.GOAL_PAUSE) {
            val scorerName = if (matchState.lastScorer == 1) matchState.player1Name else matchState.player2Name
            GoalBannerOverlay(
                scorer = matchState.lastScorer,
                scorerName = scorerName,
                tableTheme = effectiveTheme
            )
        }

        // Diálogo de Pausa
        if (matchState.status == MatchStatus.PAUSED && !matchState.peerExited) {
            PauseDialog(
                isSoundEnabled = isSoundEnabled,
                onToggleSound = { runtime.toggleSound() },
                isMusicEnabled = isMusicEnabled,
                onToggleMusic = { runtime.toggleMusic() },
                is3dActive = is3dActive,
                cameraZoom3d = prefs.cameraZoom3d,
                onCameraZoom3dChange = { newZoom ->
                    scope.launch {
                        AirHockeyApp.instance.preferencesRepository.updateCameraZoom3d(newZoom)
                    }
                },
                neonBloomIntensity = prefs.neonBloomIntensity,
                visualEffectsEnabled = prefs.visualEffectsProfile == VisualEffectsProfile.FULL,
                onNeonBloomIntensityChange = { newVal ->
                    scope.launch {
                        AirHockeyApp.instance.preferencesRepository.updateNeonBloomIntensity(newVal)
                    }
                },
                onResume = { runtime.requestResume() },
                onRestart = { runtime.requestRestart() },
                onExit = {
                    runtime.exitMatch()
                    onExitToMenu()
                }
            )
        }

        // Diálogo quando o adversário sai na Mesa Estendida
        if (matchState.peerExited) {
            PeerExitedDialog(
                onExit = {
                    runtime.exitMatch()
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
                    conqueredTheme = matchState.newlyConqueredTheme,
                    onSelectStages = {
                        if (activity != null) {
                            AirHockeyApp.instance.adManager.showInterstitialIfAllowed(activity) {
                                runtime.exitMatch()
                                onExitToStages()
                            }
                        } else {
                            runtime.exitMatch()
                            onExitToStages()
                        }
                    },
                    onRetry = {
                        // Não exibe anúncio intersticial na revanche/retry imediato para preservar a retenção do jogador
                        runtime.requestRestart()
                    },
                    onNextStage = {
                        if (activity != null) {
                            AirHockeyApp.instance.adManager.showInterstitialIfAllowed(activity) {
                                runtime.exitMatch()
                                onStartNextStage(matchState.currentStage + 1)
                            }
                        } else {
                            runtime.exitMatch()
                            onStartNextStage(matchState.currentStage + 1)
                        }
                    }
                )
            } else {
                GameOverDialog(
                    matchState = matchState,
                    onPlayAgain = {
                        // Revanche imediata sem anúncio para fluidez contínua de jogo
                        runtime.requestRestart()
                    },
                    onExit = {
                        if (activity != null) {
                            AirHockeyApp.instance.adManager.showInterstitialIfAllowed(activity) {
                                runtime.exitMatch()
                                onExitToMenu()
                            }
                        } else {
                            runtime.exitMatch()
                            onExitToMenu()
                        }
                    }
                )
            }
        }
    }
}
