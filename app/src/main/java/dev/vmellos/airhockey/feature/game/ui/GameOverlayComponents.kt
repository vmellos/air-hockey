package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.platform.LocalDensity
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
    onPauseClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isExtended = matchState.mode == GameMode.EXTENDED_HOST ||
            matchState.mode == GameMode.EXTENDED_GUEST ||
            matchState.mode == GameMode.AI_VS_AI_EXTENDED_HOST ||
            matchState.mode == GameMode.AI_VS_AI_EXTENDED_GUEST

    val isExtendedGuest = matchState.mode == GameMode.EXTENDED_GUEST ||
            matchState.mode == GameMode.AI_VS_AI_EXTENDED_GUEST

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        // Projeção virtual padrão da arena (largura 1000f, altura 2000f)
        val scale = kotlin.math.min(screenW / 1000f, screenH / 2000f)
        val tableWidthDp = with(LocalDensity.current) { (1000f * scale).toDp() }

        Box(
            modifier = Modifier
                .width(tableWidthDp)
                .fillMaxHeight()
                .align(Alignment.Center)
        ) {
            // Placar Lateral Numérico na lateral direita da Mesa
            Surface(
                color = Color(0x33000000),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    if (isExtended) {
                        // Na Mesa Estendida, cada aparelho exibe o dígito de sua respectiva metade
                        if (isExtendedGuest) {
                            Text(
                                text = "${matchState.scorePlayer2}",
                                color = Color(0xFFFF5722),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = "${matchState.scorePlayer1}",
                                color = Color(0xFF00E5FF),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        // Modos de 1 Tela ou Mesa Conectada: P2 no topo (Laranja), Divisor, P1 na base (Ciano)
                        Text(
                            text = "${matchState.scorePlayer2}",
                            color = Color(0xFFFF5722),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(1.5.dp)
                                .background(Color(0x44FFFFFF), RoundedCornerShape(1.dp))
                        )
                        Text(
                            text = "${matchState.scorePlayer1}",
                            color = Color(0xFF00E5FF),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
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
                    text = stringResource(R.string.game_goal_point_banner),
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
                    text = stringResource(R.string.game_peer_exited_title),
                    color = Color(0xFFFF5722),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.game_peer_exited_desc),
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

@Composable
fun StageResultDialog(
    stageNumber: Int,
    starsEarned: Int,
    isWon: Boolean,
    scorePlayer: Int,
    scoreAi: Int,
    onSelectStages: () -> Unit,
    onRetry: () -> Unit,
    onNextStage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6070B14)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {
            // Level Title
            Text(
                text = "LEVEL $stageNumber",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Neon Glowing Card
            val accentColor = if (isWon) Color(0xFF00E5FF) else Color(0xFFFF2A6D)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(24.dp))
                    .border(3.dp, accentColor, RoundedCornerShape(24.dp))
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF070B14),
                        border = BorderStroke(2.dp, accentColor),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isWon) "YOU WIN" else "FASE NAO CONCLUIDA",
                            color = accentColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3 Stars Container
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..3) {
                                val isEarned = isWon && i <= starsEarned
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isEarned) Color(0xFFFFB800) else Color(0xFF475569),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Final Score
                    Text(
                        text = "$scorePlayer - $scoreAi",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons Row (Square Rounded Neon Buttons)
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Stages List (Neon Green)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0x1F00E676), RoundedCornerShape(18.dp))
                        .border(2.dp, Color(0xFF00E676), RoundedCornerShape(18.dp))
                        .clickable(onClick = onSelectStages),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.content_desc_stage_list),
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Button 2: Retry (Neon Magenta)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0x1FE040FB), RoundedCornerShape(18.dp))
                        .border(2.dp, Color(0xFFE040FB), RoundedCornerShape(18.dp))
                        .clickable(onClick = onRetry),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.content_desc_restart),
                        tint = Color(0xFFE040FB),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Button 3: Next Stage (Neon Orange) - displayed if won and stageNumber < 100
                if (isWon && stageNumber < 100) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0x1FFF9100), RoundedCornerShape(18.dp))
                            .border(2.dp, Color(0xFFFF9100), RoundedCornerShape(18.dp))
                            .clickable(onClick = onNextStage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = stringResource(R.string.content_desc_next_stage),
                            tint = Color(0xFFFF9100),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
