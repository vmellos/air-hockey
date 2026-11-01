package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.MatchState
import dev.vmellos.airhockey.feature.game.MatchStatus

@Composable
fun ScoreBoardOverlay(
    matchState: MatchState,
    isSoundEnabled: Boolean = true,
    onToggleSound: () -> Unit = {},
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placar P1 (Cyan)
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = "P1",
                color = Color(0xFF00E5FF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${matchState.scorePlayer1}",
                color = Color(0xFF00E5FF),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        // Cronômetro central
        Surface(
            color = Color(0x33000000),
            shape = RoundedCornerShape(12.dp)
        ) {
            val minutes = matchState.remainingTimeSeconds / 60
            val seconds = matchState.remainingTimeSeconds % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                color = Color(0xFFE2E8F0),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Placar P2 (Orange) e Botão Pausa
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (matchState.mode == GameMode.SOLO) "IA" else "P2",
                    color = Color(0xFFFF5722),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${matchState.scorePlayer2}",
                    color = Color(0xFFFF5722),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = onToggleSound,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x22FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = if (isSoundEnabled) "Silenciar som" else "Ativar som",
                    tint = if (isSoundEnabled) Color.White else Color(0xFFFF5252)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x22FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = stringResource(R.string.game_paused),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun CountdownOverlay(
    countdownNumber: Int,
    modifier: Modifier = Modifier
) {
    val displayText = when (countdownNumber) {
        3 -> "3"
        2 -> "2"
        1 -> "1"
        0 -> stringResource(R.string.game_countdown_go)
        else -> ""
    }
    if (displayText.isEmpty()) return

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0x660A0E1A),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0x8800E5FF))
        ) {
            Text(
                text = displayText,
                color = if (countdownNumber == 0) Color(0xFF10B981) else Color(0xFF00E5FF),
                fontSize = if (countdownNumber == 0) 56.sp else 64.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
fun GoalBannerOverlay(
    scorer: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val color = if (scorer == 1) Color(0xFF00E5FF) else Color(0xFFFF5722)
        Surface(
            color = Color(0xDD0A0E1A),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, color)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 40.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.game_goal),
                    color = color,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )
                Text(
                    text = if (scorer == 1) "P1" else "P2",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PeerExitedDialog(
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Partida Encerrada",
                    color = Color(0xFFFF5722),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "O adversário saiu da partida.",
                    color = Color(0xFF94A3B8),
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.game_exit_to_menu), color = Color(0xFF0A0E1A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PauseDialog(
    isSoundEnabled: Boolean = true,
    onToggleSound: () -> Unit = {},
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.game_paused),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(text = "Efeitos Sonoros", color = Color.White, fontSize = 15.sp)
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x22FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isSoundEnabled) "Silenciar som" else "Ativar som",
                            tint = if (isSoundEnabled) Color(0xFF00E5FF) else Color(0xFFFF5252)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.game_resume), color = Color(0xFF0A0E1A), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                FilledTonalButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.game_restart))
                }

                Spacer(modifier = Modifier.height(12.dp))

                FilledTonalButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.game_exit_to_menu))
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(
    matchState: MatchState,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val winnerColor = if (matchState.winner == 1) Color(0xFF00E5FF) else Color(0xFFFF5722)
                val winnerTitle = if (matchState.mode == GameMode.SOLO) {
                    if (matchState.winner == 1) stringResource(R.string.game_winner_you) else stringResource(R.string.game_winner_opponent)
                } else {
                    if (matchState.winner == 1) stringResource(R.string.game_winner_player1) else stringResource(R.string.game_winner_player2)
                }

                Text(
                    text = winnerTitle,
                    color = winnerColor,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${matchState.scorePlayer1} - ${matchState.scorePlayer2}",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onPlayAgain,
                    colors = ButtonDefaults.buttonColors(containerColor = winnerColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.game_play_again), color = Color(0xFF0A0E1A), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                FilledTonalButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.game_exit_to_menu))
                }
            }
        }
    }
}
