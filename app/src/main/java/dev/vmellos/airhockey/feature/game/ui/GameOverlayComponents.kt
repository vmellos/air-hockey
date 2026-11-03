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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
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
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placar P1 (Cyan)
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.widthIn(min = 60.dp, max = 110.dp)
        ) {
            Text(
                text = matchState.player1Name,
                color = Color(0xFF00E5FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = "${matchState.scorePlayer1}",
                color = Color(0xFF00E5FF),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        // Cronômetro central
        Surface(
            color = Color(0x44000000),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            val minutes = matchState.remainingTimeSeconds / 60
            val seconds = matchState.remainingTimeSeconds % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                color = Color(0xFFE2E8F0),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // Placar P2 (Orange) e Botão Pausa isolado à direita
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(min = 60.dp, max = 110.dp)
            ) {
                Text(
                    text = matchState.player2Name,
                    color = Color(0xFFFF5722),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${matchState.scorePlayer2}",
                    color = Color(0xFFFF5722),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x33FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = stringResource(R.string.game_paused),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
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
        Text(
            text = displayText,
            color = if (countdownNumber == 0) Color(0xFF10B981) else Color(0xFF00E5FF),
            fontSize = if (countdownNumber == 0) 64.sp else 80.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun GoalBannerOverlay(
    scorer: Int,
    scorerName: String = "",
    modifier: Modifier = Modifier
) {
    val color = if (scorer == 1) Color(0xFF00E5FF) else Color(0xFFFF5722)
    val glowColor = if (scorer == 1) Color(0xFF80D8FF) else Color(0xFFFF9E80)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.game_goal),
                color = color,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 10.sp,
                fontFamily = FontFamily.SansSerif,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = glowColor.copy(alpha = 0.95f),
                        blurRadius = 36f
                    )
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = color.copy(alpha = 0.22f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, glowColor.copy(alpha = 0.80f))
            ) {
                Text(
                    text = "+1 PONTO",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
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
    isMusicEnabled: Boolean = true,
    onToggleMusic: () -> Unit = {},
    visualEffectsProfile: dev.vmellos.airhockey.core.preferences.VisualEffectsProfile = dev.vmellos.airhockey.core.preferences.VisualEffectsProfile.FULL,
    onChangeVisualEffects: (dev.vmellos.airhockey.core.preferences.VisualEffectsProfile) -> Unit = {},
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(text = stringResource(R.string.settings_sound_effects), color = Color.White, fontSize = 15.sp)
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isSoundEnabled) Color(0x3300E5FF) else Color(0x33EF4444), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isSoundEnabled) "Silenciar som" else "Ativar som",
                            tint = if (isSoundEnabled) Color(0xFF00E5FF) else Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(text = stringResource(R.string.settings_music), color = Color.White, fontSize = 15.sp)
                    IconButton(
                        onClick = onToggleMusic,
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isMusicEnabled) Color(0x3300E5FF) else Color(0x33EF4444), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMusicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = if (isMusicEnabled) "Silenciar música" else "Tocar música",
                            tint = if (isMusicEnabled) Color(0xFF00E5FF) else Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Seletor Rápido de Efeitos Visuais
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_visual_effects),
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val full = visualEffectsProfile == dev.vmellos.airhockey.core.preferences.VisualEffectsProfile.FULL
                        val balanced = visualEffectsProfile == dev.vmellos.airhockey.core.preferences.VisualEffectsProfile.BALANCED
                        val off = visualEffectsProfile == dev.vmellos.airhockey.core.preferences.VisualEffectsProfile.OFF

                        Surface(
                            onClick = { onChangeVisualEffects(dev.vmellos.airhockey.core.preferences.VisualEffectsProfile.FULL) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (full) Color(0x3300E5FF) else Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (full) Color(0xFF00E5FF) else Color(0x33FFFFFF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Completo",
                                color = if (full) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = if (full) FontWeight.Bold else FontWeight.Normal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            onClick = { onChangeVisualEffects(dev.vmellos.airhockey.core.preferences.VisualEffectsProfile.BALANCED) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (balanced) Color(0x3300E5FF) else Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (balanced) Color(0xFF00E5FF) else Color(0x33FFFFFF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Econômico",
                                color = if (balanced) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = if (balanced) FontWeight.Bold else FontWeight.Normal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            onClick = { onChangeVisualEffects(dev.vmellos.airhockey.core.preferences.VisualEffectsProfile.OFF) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (off) Color(0x3300E5FF) else Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (off) Color(0xFF00E5FF) else Color(0x33FFFFFF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Mínimo",
                                color = if (off) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = if (off) FontWeight.Bold else FontWeight.Normal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.game_resume), color = Color(0xFF0A0E1A), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.game_restart))
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                val isDraw = matchState.winner == 0
                val winnerColor = if (isDraw) Color(0xFFE2E8F0) else if (matchState.winner == 1) Color(0xFF00E5FF) else Color(0xFFFF5722)
                val winnerTitle = if (isDraw) {
                    stringResource(R.string.game_draw)
                } else if (matchState.mode == GameMode.SOLO) {
                    if (matchState.winner == 1) stringResource(R.string.game_winner_you) else stringResource(R.string.game_winner_opponent)
                } else {
                    val winnerName = if (matchState.winner == 1) matchState.player1Name else matchState.player2Name
                    "$winnerName Venceu!"
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
