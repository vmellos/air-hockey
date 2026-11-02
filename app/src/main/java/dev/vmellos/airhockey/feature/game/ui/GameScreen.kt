package dev.vmellos.airhockey.feature.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.core.util.findActivity
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.GameViewModel
import dev.vmellos.airhockey.feature.game.MatchStatus

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    mode: GameMode,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matchState by viewModel.matchState.collectAsState()
    val isSoundEnabled by viewModel.soundEnabled.collectAsState()

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

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopMatch()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Canvas da Mesa de Jogo
        AirHockeyArenaCanvas(
            viewModel = viewModel,
            mode = mode,
            modifier = Modifier.fillMaxSize()
        )

        // Placar e Controles
        ScoreBoardOverlay(
            matchState = matchState,
            isSoundEnabled = isSoundEnabled,
            onToggleSound = { viewModel.toggleSound() },
            onPauseClick = { viewModel.pauseMatch() }
        )

        // Overlay de Contagem Regressiva estilizado
        if (matchState.status == MatchStatus.COUNTDOWN) {
            CountdownOverlay(countdownNumber = matchState.countdownNumber)
        }

        // Overlay de Gol/Ponto
        if (matchState.status == MatchStatus.GOAL_PAUSE) {
            GoalBannerOverlay(scorer = matchState.lastScorer)
        }

        // Diálogo de Pausa
        if (matchState.status == MatchStatus.PAUSED && !matchState.peerExited) {
            PauseDialog(
                isSoundEnabled = isSoundEnabled,
                onToggleSound = { viewModel.toggleSound() },
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
            GameOverDialog(
                matchState = matchState,
                onPlayAgain = {
                    if (activity != null) {
                        AirHockeyApp.instance.adManager.showInterstitialIfAllowed(activity) {
                            viewModel.restartMatch()
                        }
                    } else {
                        viewModel.restartMatch()
                    }
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
