package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
            onPauseClick = { viewModel.togglePause() }
        )

        // Overlay de Contagem Regressiva
        if (matchState.status == MatchStatus.COUNTDOWN) {
            CountdownOverlay(countdownNumber = matchState.countdownNumber)
        }

        // Overlay de Gol/Ponto
        if (matchState.status == MatchStatus.GOAL_PAUSE) {
            GoalBannerOverlay(scorer = matchState.lastScorer)
        }

        // Diálogo de Pausa
        if (matchState.status == MatchStatus.PAUSED) {
            PauseDialog(
                onResume = { viewModel.togglePause() },
                onRestart = { viewModel.restartMatch() },
                onExit = onExitToMenu
            )
        }

        // Diálogo de Fim de Jogo
        if (matchState.status == MatchStatus.GAME_OVER) {
            GameOverDialog(
                matchState = matchState,
                onPlayAgain = { viewModel.restartMatch() },
                onExit = onExitToMenu
            )
        }
    }
}
