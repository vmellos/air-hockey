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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Layers
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.MatchState
import dev.vmellos.airhockey.feature.game.MatchStatus

@Composable
fun ScoreBoardOverlay(
    matchState: MatchState,
    tableTheme: TableTheme = TableTheme.CLASSIC_NEON,
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

    val (boardShape, boardBg, boardBorder, p1Color, p2Color, boardFont) = when (tableTheme) {
        TableTheme.CYBER_PONG -> SixTuple(
            CutCornerShape(0.dp),
            Color(0xFF000000),
            BorderStroke(1.5.dp, PongGreen),
            PongGreen,
            PongGreen,
            FontFamily.Monospace
        )
        TableTheme.CYBER_COLOSSEUM -> SixTuple(
            RoundedCornerShape(6.dp),
            Color(0xDD1C1408),
            BorderStroke(1.2.dp, ColosseumGold.copy(alpha = 0.7f)),
            ColosseumGold,
            ColosseumCrimson,
            FontFamily.Serif
        )
        TableTheme.CYBER_SOCCER -> SixTuple(
            RoundedCornerShape(12.dp),
            Color(0xCC05180D),
            BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f)),
            NeonEmerald,
            NeonRuby,
            FontFamily.SansSerif
        )
        TableTheme.NEO_BASKETBALL -> SixTuple(
            CutCornerShape(8.dp),
            Color(0xDD140E08),
            BorderStroke(1.2.dp, NeonAmber.copy(alpha = 0.7f)),
            NeonAmber,
            NeonCyan,
            FontFamily.SansSerif
        )
        TableTheme.GRAVITY_VOLLEY -> SixTuple(
            RoundedCornerShape(16.dp),
            Color(0xCC0E0A1E),
            BorderStroke(1.dp, NeonViolet.copy(alpha = 0.6f)),
            NeonViolet,
            NeonMagenta,
            FontFamily.SansSerif
        )
        TableTheme.GRIDIRON_FOOTBALL -> SixTuple(
            CutCornerShape(6.dp),
            Color(0xDD081420),
            BorderStroke(1.5.dp, GridironCyan.copy(alpha = 0.7f)),
            GridironCyan,
            GridironGold,
            FontFamily.SansSerif
        )
        TableTheme.CYBER_BASEBALL -> SixTuple(
            CutCornerShape(12.dp),
            Color(0xCC18080C),
            BorderStroke(1.dp, BaseballRed.copy(alpha = 0.7f)),
            BaseballRed,
            BaseballCyan,
            FontFamily.SansSerif
        )
        TableTheme.WASTELAND_APOCALYPSE -> SixTuple(
            CutCornerShape(4.dp),
            Color(0xEE160C06),
            BorderStroke(1.5.dp, WastelandFlame.copy(alpha = 0.8f)),
            WastelandFlame,
            WastelandFlameBright,
            FontFamily.SansSerif
        )
        TableTheme.RETRO_SYNTHWAVE -> SixTuple(
            RoundedCornerShape(10.dp),
            Color(0xDD1A0A1C),
            BorderStroke(1.2.dp, SynthPink.copy(alpha = 0.7f)),
            SynthPink,
            SynthCyan,
            FontFamily.SansSerif
        )
        TableTheme.TOKAMAK_REACTOR -> SixTuple(
            CutCornerShape(10.dp),
            Color(0xDD081608),
            BorderStroke(1.2.dp, TokamakRadiation.copy(alpha = 0.7f)),
            TokamakRadiation,
            TokamakPlasma,
            FontFamily.SansSerif
        )
        TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D, TableTheme.REALIDADE_AUMENTADA_AR -> SixTuple(
            RoundedCornerShape(12.dp),
            Color(0x33000000),
            BorderStroke(1.dp, Color(0x33FFFFFF)),
            NeonCyan,
            NeonOrange,
            FontFamily.Monospace
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val scale = kotlin.math.min(screenW / 1000f, screenH / 2000f)
        val tableWidthDp = with(LocalDensity.current) { (1000f * scale).toDp() }

        Box(
            modifier = Modifier
                .width(tableWidthDp)
                .fillMaxHeight()
                .align(Alignment.Center)
        ) {
            // Placar Lateral Numérico estilizado por tema na lateral direita da Mesa
            Surface(
                color = boardBg,
                shape = boardShape,
                border = boardBorder,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${matchState.scorePlayer2}",
                        color = p2Color,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = boardFont
                    )
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(1.5.dp)
                            .background(boardBorder.brush ?: Brush.horizontalGradient(listOf(p2Color.copy(alpha = 0.5f), p1Color.copy(alpha = 0.5f))), RoundedCornerShape(1.dp))
                    )
                    Text(
                        text = "${matchState.scorePlayer1}",
                        color = p1Color,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = boardFont
                    )
                    if (matchState.targetScore > 0) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(1.dp)
                                .background(Color(0x22FFFFFF), RoundedCornerShape(1.dp))
                        )
                        Text(
                            text = stringResource(R.string.scoreboard_target, matchState.targetScore),
                            color = Color(0x99FFFFFF),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    if (matchState.mode == GameMode.SOLO && matchState.currentStage > 0) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(1.dp)
                                .background(Color(0x22FFFFFF), RoundedCornerShape(1.dp))
                        )
                        Text(
                            text = stringResource(R.string.stage_level_header, matchState.currentStage).uppercase(),
                            color = Color(0xCC00E5FF),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
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
    tableTheme: TableTheme = TableTheme.CLASSIC_NEON,
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

    val (numberColor, goColor, font) = when (tableTheme) {
        TableTheme.CYBER_PONG -> Triple(PongGreen, PongGreenBright, FontFamily.Monospace)
        TableTheme.CYBER_COLOSSEUM -> Triple(ColosseumGold, ColosseumGoldBright, FontFamily.Serif)
        TableTheme.CYBER_SOCCER -> Triple(NeonEmerald, NeonEmeraldBright, FontFamily.SansSerif)
        TableTheme.NEO_BASKETBALL -> Triple(NeonAmber, NeonAmberBright, FontFamily.SansSerif)
        TableTheme.GRAVITY_VOLLEY -> Triple(NeonViolet, NeonMagentaBright, FontFamily.SansSerif)
        TableTheme.GRIDIRON_FOOTBALL -> Triple(GridironCyan, GridironGold, FontFamily.SansSerif)
        TableTheme.CYBER_BASEBALL -> Triple(BaseballRed, BaseballCyanBright, FontFamily.SansSerif)
        TableTheme.WASTELAND_APOCALYPSE -> Triple(WastelandFlame, WastelandFlameBright, FontFamily.SansSerif)
        TableTheme.RETRO_SYNTHWAVE -> Triple(SynthPink, SynthCyanBright, FontFamily.SansSerif)
        TableTheme.TOKAMAK_REACTOR -> Triple(TokamakRadiation, TokamakPlasmaBright, FontFamily.SansSerif)
        TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D, TableTheme.REALIDADE_AUMENTADA_AR -> Triple(NeonCyan, Color(0xFF10B981), FontFamily.SansSerif)
    }

    val textColor = if (countdownNumber == 0) goColor else numberColor

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            color = textColor,
            fontSize = if (countdownNumber == 0) 64.sp else 80.sp,
            fontWeight = FontWeight.Black,
            fontFamily = font,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = textColor.copy(alpha = 0.85f),
                    blurRadius = 30f
                )
            )
        )
    }
}

@Composable
fun GoalBannerOverlay(
    scorer: Int,
    scorerName: String = "",
    tableTheme: TableTheme = TableTheme.CLASSIC_NEON,
    modifier: Modifier = Modifier
) {
    val (primaryColor, secondaryColor) = when (tableTheme) {
        TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D, TableTheme.REALIDADE_AUMENTADA_AR -> Pair(NeonCyan, NeonOrange)
        TableTheme.CYBER_SOCCER -> Pair(NeonEmerald, NeonRuby)
        TableTheme.NEO_BASKETBALL -> Pair(NeonAmber, NeonCyan)
        TableTheme.GRAVITY_VOLLEY -> Pair(NeonViolet, NeonMagenta)
        TableTheme.GRIDIRON_FOOTBALL -> Pair(GridironCyan, GridironGold)
        TableTheme.CYBER_BASEBALL -> Pair(BaseballRed, BaseballCyan)
        TableTheme.WASTELAND_APOCALYPSE -> Pair(WastelandFlame, WastelandFlameBright)
        TableTheme.RETRO_SYNTHWAVE -> Pair(SynthPink, SynthCyan)
        TableTheme.TOKAMAK_REACTOR -> Pair(TokamakRadiation, TokamakPlasma)
        TableTheme.CYBER_PONG -> Pair(PongGreen, PongGreen)
        TableTheme.CYBER_COLOSSEUM -> Pair(ColosseumGold, ColosseumCrimson)
    }

    val color = if (scorer == 1) primaryColor else secondaryColor
    val glowColor = when (tableTheme) {
        TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D, TableTheme.REALIDADE_AUMENTADA_AR -> if (scorer == 1) NeonCyanBright else NeonOrangeBright
        TableTheme.CYBER_SOCCER -> if (scorer == 1) NeonEmeraldBright else NeonRubyBright
        TableTheme.NEO_BASKETBALL -> if (scorer == 1) NeonAmberBright else NeonCyanBright
        TableTheme.GRAVITY_VOLLEY -> if (scorer == 1) NeonVioletBright else NeonMagentaBright
        TableTheme.GRIDIRON_FOOTBALL -> if (scorer == 1) GridironCyan else GridironGold
        TableTheme.CYBER_BASEBALL -> if (scorer == 1) BaseballRedBright else BaseballCyanBright
        TableTheme.WASTELAND_APOCALYPSE -> WastelandFlameBright
        TableTheme.RETRO_SYNTHWAVE -> if (scorer == 1) SynthPinkBright else SynthCyanBright
        TableTheme.TOKAMAK_REACTOR -> if (scorer == 1) TokamakRadiationBright else TokamakPlasmaBright
        TableTheme.CYBER_PONG -> PongGreenBright
        TableTheme.CYBER_COLOSSEUM -> if (scorer == 1) ColosseumGoldBright else ColosseumCrimson
    }

    val bannerShape = when (tableTheme) {
        TableTheme.CYBER_PONG -> CutCornerShape(0.dp)
        TableTheme.WASTELAND_APOCALYPSE -> CutCornerShape(4.dp)
        TableTheme.CYBER_COLOSSEUM -> RoundedCornerShape(8.dp)
        TableTheme.GRAVITY_VOLLEY -> RoundedCornerShape(22.dp)
        TableTheme.CYBER_SOCCER -> RoundedCornerShape(14.dp)
        TableTheme.RETRO_SYNTHWAVE -> RoundedCornerShape(16.dp)
        TableTheme.GRIDIRON_FOOTBALL -> CutCornerShape(8.dp)
        TableTheme.NEO_BASKETBALL -> CutCornerShape(12.dp)
        TableTheme.TOKAMAK_REACTOR -> CutCornerShape(12.dp)
        TableTheme.CYBER_BASEBALL -> CutCornerShape(14.dp)
        TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D, TableTheme.REALIDADE_AUMENTADA_AR -> CutCornerShape(16.dp)
    }

    val bannerBg = when (tableTheme) {
        TableTheme.CYBER_PONG -> Color(0xFF000000)
        TableTheme.CYBER_SOCCER -> Color(0xE6051A0E)
        TableTheme.NEO_BASKETBALL -> Color(0xE6140F08)
        TableTheme.GRAVITY_VOLLEY -> Color(0xE6120822)
        TableTheme.GRIDIRON_FOOTBALL -> Color(0xE6081422)
        TableTheme.CYBER_BASEBALL -> Color(0xE61A0A0E)
        TableTheme.WASTELAND_APOCALYPSE -> Color(0xF0180D07)
        TableTheme.RETRO_SYNTHWAVE -> Color(0xE61A081C)
        TableTheme.TOKAMAK_REACTOR -> Color(0xE607180A)
        TableTheme.CYBER_COLOSSEUM -> Color(0xE61C1408)
        TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D, TableTheme.REALIDADE_AUMENTADA_AR -> Color(0xD9080E1A)
    }

    val bannerFont = when (tableTheme) {
        TableTheme.CYBER_PONG -> FontFamily.Monospace
        TableTheme.CYBER_COLOSSEUM -> FontFamily.Serif
        else -> FontFamily.SansSerif
    }

    val subtitleText = scorerName.ifEmpty {
        if (scorer == 1) stringResource(R.string.player_1_default) else stringResource(R.string.player_2_default)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = bannerBg,
            shape = bannerShape,
            border = BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        color.copy(alpha = 0.25f),
                        glowColor,
                        color.copy(alpha = 0.25f)
                    )
                )
            ),
            shadowElevation = 20.dp,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.game_goal),
                    color = color,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = if (tableTheme == TableTheme.CYBER_PONG) 4.sp else 8.sp,
                    fontFamily = bannerFont,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = glowColor.copy(alpha = 0.95f),
                            blurRadius = 32f
                        )
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(1.5.dp)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, glowColor)))
                    )
                    Text(
                        text = subtitleText.uppercase(),
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = bannerFont,
                        letterSpacing = 2.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(1.5.dp)
                            .background(Brush.horizontalGradient(listOf(glowColor, Color.Transparent)))
                    )
                }
            }
        }
    }
}

// Estrutura de conveniência interna para estilização de placar
private data class SixTuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)

@Composable
private fun ArcadePrimaryButton(
    text: String,
    containerColor: Color,
    textColor: Color = Color(0xFF070B14),
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CutCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.2.dp, containerColor.copy(alpha = 0.90f)),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
private fun ArcadeSecondaryButton(
    text: String,
    accentColor: Color = Color(0xFF00E5FF),
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CutCornerShape(10.dp),
        color = Color(0xD9080E1A),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.40f)),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color(0xFFE2E8F0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
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
            .background(Color(0xD9000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xF20B0F1C),
            shape = CutCornerShape(20.dp),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFF5722),
                        Color(0xFFFF3D00)
                    )
                )
            ),
            shadowElevation = 24.dp,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.90f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.game_peer_exited_title).uppercase(),
                    color = Color(0xFFFF5722),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.game_peer_exited_desc),
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                ArcadePrimaryButton(
                    text = stringResource(R.string.game_exit_to_menu).uppercase(),
                    containerColor = Color(0xFF00E5FF),
                    onClick = onExit
                )
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
    onExit: () -> Unit,
    onSwitchTo3D: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xF20B0F1C),
            shape = CutCornerShape(20.dp),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF38BDF8),
                        Color(0xFF00E5FF)
                    )
                )
            ),
            shadowElevation = 24.dp,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.90f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Título Arcade
                Text(
                    text = stringResource(R.string.game_paused).uppercase(),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Controles de Áudio HUD Arcade
                Surface(
                    color = Color(0xD9080E1A),
                    shape = CutCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0x3300E5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sound Effects
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.settings_sound_effects),
                                color = Color(0xFFE2E8F0),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                onClick = onToggleSound,
                                shape = CutCornerShape(8.dp),
                                color = if (isSoundEnabled) Color(0x2600E5FF) else Color(0x26EF4444),
                                border = BorderStroke(1.dp, if (isSoundEnabled) Color(0xFF00E5FF) else Color(0xFFEF4444)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                        contentDescription = if (isSoundEnabled) "Silenciar som" else "Ativar som",
                                        tint = if (isSoundEnabled) Color(0xFF00E5FF) else Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Divisor sutil
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0x1AFFFFFF))
                        )

                        // Game Music
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.settings_music),
                                color = Color(0xFFE2E8F0),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                onClick = onToggleMusic,
                                shape = CutCornerShape(8.dp),
                                color = if (isMusicEnabled) Color(0x2600E5FF) else Color(0x26EF4444),
                                border = BorderStroke(1.dp, if (isMusicEnabled) Color(0xFF00E5FF) else Color(0xFFEF4444)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isMusicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                        contentDescription = if (isMusicEnabled) "Silenciar música" else "Tocar música",
                                        tint = if (isMusicEnabled) Color(0xFF00E5FF) else Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botão Primário: Retomar (Ciano Neon)
                ArcadePrimaryButton(
                    text = stringResource(R.string.game_resume).uppercase(),
                    containerColor = Color(0xFF00E5FF),
                    icon = Icons.Default.PlayArrow,
                    onClick = onResume
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Botão Secundário: Reiniciar
                ArcadeSecondaryButton(
                    text = stringResource(R.string.game_restart).uppercase(),
                    accentColor = Color(0xFF00E5FF),
                    icon = Icons.Default.Refresh,
                    onClick = onRestart
                )

                if (onSwitchTo3D != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ArcadeSecondaryButton(
                        text = stringResource(R.string.ar_switch_to_3d).uppercase(),
                        accentColor = Color(0xFF10B981),
                        icon = Icons.Default.Layers,
                        onClick = onSwitchTo3D
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botão Terciário: Sair para o Menu
                ArcadeSecondaryButton(
                    text = stringResource(R.string.game_exit_to_menu).uppercase(),
                    accentColor = Color(0xFF94A3B8),
                    onClick = onExit
                )
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
    val isDraw = matchState.winner == 0
    val winnerColor = if (isDraw) Color(0xFFE2E8F0) else if (matchState.winner == 1) Color(0xFF00E5FF) else Color(0xFFFF5722)
    val winnerTitle = if (isDraw) {
        stringResource(R.string.game_draw)
    } else if (matchState.mode == GameMode.SOLO) {
        if (matchState.winner == 1) stringResource(R.string.game_winner_you) else stringResource(R.string.game_winner_opponent)
    } else {
        val winnerName = if (matchState.winner == 1) matchState.player1Name else matchState.player2Name
        stringResource(R.string.game_winner_named, winnerName)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xF20B0F1C),
            shape = CutCornerShape(20.dp),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        winnerColor,
                        winnerColor.copy(alpha = 0.45f)
                    )
                )
            ),
            shadowElevation = 24.dp,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.90f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = winnerTitle.uppercase(),
                    color = winnerColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Placar Final Arcade
                Surface(
                    color = Color(0xD9080E1A),
                    shape = CutCornerShape(10.dp),
                    border = BorderStroke(1.dp, winnerColor.copy(alpha = 0.35f)),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "${matchState.scorePlayer1} - ${matchState.scorePlayer2}",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                ArcadePrimaryButton(
                    text = stringResource(R.string.game_play_again).uppercase(),
                    containerColor = winnerColor,
                    icon = Icons.Default.Refresh,
                    onClick = onPlayAgain
                )

                Spacer(modifier = Modifier.height(10.dp))

                ArcadeSecondaryButton(
                    text = stringResource(R.string.game_exit_to_menu).uppercase(),
                    accentColor = Color(0xFF94A3B8),
                    onClick = onExit
                )
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
    val accentColor = if (isWon) Color(0xFF00E5FF) else Color(0xFFFF5722)
    val glowColor = if (isWon) Color(0xFFFFB800) else Color(0xFFFF6D00)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xF20B0F1C),
            shape = CutCornerShape(20.dp),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        accentColor,
                        glowColor.copy(alpha = 0.70f),
                        accentColor
                    )
                )
            ),
            shadowElevation = 24.dp,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.90f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Level Number
                Text(
                    text = stringResource(R.string.stage_level_header, stageNumber).uppercase(),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 2.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Badge de Status (Vitória / Derrota)
                Surface(
                    shape = CutCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.2.dp, accentColor),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = stringResource(if (isWon) R.string.stage_win else R.string.stage_failed).uppercase(),
                        color = accentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Cápsula das 3 Estrelas de Desempenho
                Surface(
                    shape = CutCornerShape(10.dp),
                    color = Color(0xD9080E1A),
                    border = BorderStroke(1.dp, if (isWon) Color(0x66FFB800) else Color(0x33475569)),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..3) {
                            val isEarned = isWon && i <= starsEarned
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isEarned) Color(0xFFFFB800) else Color(0xFF334155),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Placar Final
                Text(
                    text = "$scorePlayer - $scoreAi",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Botões de Ação Integrados ao Card
                if (isWon && stageNumber < 100) {
                    // Vitória: Próxima Fase (Primário Ciano)
                    ArcadePrimaryButton(
                        text = stringResource(R.string.stage_next_btn).uppercase(),
                        containerColor = Color(0xFF00E5FF),
                        icon = Icons.Default.FastForward,
                        onClick = onNextStage
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ArcadeSecondaryButton(
                            text = stringResource(R.string.stage_list_btn).uppercase(),
                            icon = Icons.AutoMirrored.Filled.List,
                            accentColor = Color(0xFF94A3B8),
                            onClick = onSelectStages,
                            modifier = Modifier.weight(1f)
                        )
                        ArcadeSecondaryButton(
                            text = stringResource(R.string.stage_retry_btn).uppercase(),
                            icon = Icons.Default.Refresh,
                            accentColor = Color(0xFF00E5FF),
                            onClick = onRetry,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else if (isWon) {
                    // Fase 100 Concluída (Campanha Finalizada)
                    ArcadePrimaryButton(
                        text = stringResource(R.string.stage_list_btn).uppercase(),
                        containerColor = Color(0xFFFFB800),
                        icon = Icons.AutoMirrored.Filled.List,
                        onClick = onSelectStages
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ArcadeSecondaryButton(
                        text = stringResource(R.string.stage_retry_btn).uppercase(),
                        icon = Icons.Default.Refresh,
                        accentColor = Color(0xFF00E5FF),
                        onClick = onRetry
                    )
                } else {
                    // Derrota: Tentar Novamente (Primário Laranja Neon)
                    ArcadePrimaryButton(
                        text = stringResource(R.string.stage_retry_btn).uppercase(),
                        containerColor = Color(0xFFFF5722),
                        textColor = Color.White,
                        icon = Icons.Default.Refresh,
                        onClick = onRetry
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ArcadeSecondaryButton(
                        text = stringResource(R.string.stage_list_btn).uppercase(),
                        icon = Icons.AutoMirrored.Filled.List,
                        accentColor = Color(0xFF94A3B8),
                        onClick = onSelectStages
                    )
                }
            }
        }
    }
}
