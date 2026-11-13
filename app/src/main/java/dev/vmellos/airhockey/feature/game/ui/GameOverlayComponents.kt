package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import dev.vmellos.airhockey.core.preferences.CameraZoom3D
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.remember
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
    val isExtended = matchState.capabilities.isExtended
    val isExtendedGuest = matchState.capabilities.isExtendedGuest

    val overlayStyle = remember(tableTheme) { tableTheme.kit().overlayStyle }
    val boardShape = overlayStyle.shape
    val boardBg = overlayStyle.background
    val boardBorder = overlayStyle.border
    val p1Color = overlayStyle.p1Color
    val p2Color = overlayStyle.p2Color
    val boardFont = overlayStyle.fontFamily

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val isLandscape = screenW > screenH
        val scale = kotlin.math.min(screenW / 1000f, screenH / 2000f)
        val tableWidthDp = with(LocalDensity.current) { (1000f * scale).toDp() }

        // Placar Lateral Numérico estilizado por tema na lateral da Mesa e Botão de Pausa
        val scoreboardAndControls = @Composable {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = if (isLandscape) 14.dp else 8.dp)
            ) {
                Surface(
                    color = boardBg,
                    shape = boardShape,
                    border = boardBorder
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
                    }
                }

                Surface(
                    onClick = onPauseClick,
                    shape = CircleShape,
                    color = boardBg,
                    border = boardBorder,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = stringResource(R.string.game_paused),
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (isLandscape) {
            // Em modo paisagem: ancoragem na Calha Lateral Direita externa à mesa (ADR 0037)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                scoreboardAndControls()
            }
        } else {
            // Em modo retrato: ancoragem adjacente ao trilho direito da arena virtual
            Box(
                modifier = Modifier
                    .width(tableWidthDp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    scoreboardAndControls()
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
        TableTheme.KIARA_CAT -> Triple(Color(0xFFFF1493), Color(0xFFFFC107), FontFamily.SansSerif)
        TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D -> Triple(NeonCyan, Color(0xFF10B981), FontFamily.SansSerif)
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
        TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D -> Pair(NeonCyan, NeonOrange)
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
        TableTheme.KIARA_CAT -> Pair(Color(0xFFFF1493), NeonCyan)
    }

    val color = if (scorer == 1) primaryColor else secondaryColor
    val glowColor = when (tableTheme) {
        TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D -> if (scorer == 1) NeonCyanBright else NeonOrangeBright
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
        TableTheme.KIARA_CAT -> if (scorer == 1) Color(0xFFFF69B4) else NeonCyanBright
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
        TableTheme.KIARA_CAT -> RoundedCornerShape(14.dp)
        TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D -> CutCornerShape(16.dp)
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
        TableTheme.KIARA_CAT -> Color(0xE614081E)
        TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D -> Color(0xD9080E1A)
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
                        Color(0xFFFFB800),
                        Color(0xFFFF8F00)
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
                    color = Color(0xFFFFB800),
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
fun NeonBloomIntensityControl(
    neonBloomIntensity: Float,
    effectsEnabledInContext: Boolean = true,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    hintText: String? = null,
    titleFontSize: TextUnit = 14.sp,
    badgeFontSize: TextUnit = 12.sp,
    sliderHeight: Dp = 30.dp,
    leadingIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val display = resolveNeonBloomDisplayState(
        savedIntensity = neonBloomIntensity,
        effectsEnabledInContext = effectsEnabledInContext
    )
    val intensityLabel = when (display.band) {
        NeonBloomBand.OFF -> stringResource(R.string.neon_bloom_off)
        NeonBloomBand.SOFT -> stringResource(R.string.neon_bloom_soft)
        NeonBloomBand.NORMAL -> stringResource(R.string.neon_bloom_normal)
        NeonBloomBand.VIBRANT -> stringResource(R.string.neon_bloom_vibrant)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .alpha(if (display.isControlEnabled) 1f else 0.58f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = stringResource(R.string.neon_bloom_intensity_title),
                    color = Color(0xFFE2E8F0),
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "${display.percent}% • $intensityLabel",
                color = if (display.isControlEnabled) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                fontSize = badgeFontSize,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = display.sliderValue,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = display.isControlEnabled,
            valueRange = 0.0f..0.10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF),
                inactiveTrackColor = Color(0xFF1E293B),
                disabledThumbColor = Color(0xFF475569),
                disabledActiveTrackColor = Color(0xFF334155),
                disabledInactiveTrackColor = Color(0xFF1E293B)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(sliderHeight)
        )
        if (!hintText.isNullOrBlank()) {
            Text(
                text = hintText,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun AudioAndVisualControlsSurface(
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    isMusicEnabled: Boolean,
    onToggleMusic: () -> Unit,
    is3dActive: Boolean,
    cameraZoom3d: CameraZoom3D,
    onCameraZoom3dChange: ((CameraZoom3D) -> Unit)?,
    neonBloomIntensity: Float,
    visualEffectsEnabled: Boolean,
    onNeonBloomIntensityChange: ((Float) -> Unit)?
) {
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

            // Controle de Brilho do Neon (quando o 3D estiver em execução)
            if (is3dActive && onNeonBloomIntensityChange != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0x1AFFFFFF))
                )

                NeonBloomIntensityControl(
                    neonBloomIntensity = neonBloomIntensity,
                    effectsEnabledInContext = visualEffectsEnabled,
                    onValueChange = onNeonBloomIntensityChange,
                    hintText = if (!visualEffectsEnabled) {
                        stringResource(R.string.neon_bloom_disabled_in_settings_hint)
                    } else {
                        null
                    },
                    titleFontSize = 14.sp,
                    badgeFontSize = 12.sp,
                    sliderHeight = 30.dp
                )
            }

            // Controle de Zoom da Câmera 3D (quando a Arena 3D estiver ativa)
            if (is3dActive && onCameraZoom3dChange != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0x1AFFFFFF))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.camera_zoom_title),
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(CameraZoom3D.CLOSE, CameraZoom3D.STANDARD, CameraZoom3D.WIDE).forEach { zoomOption ->
                            val isSelected = zoomOption == cameraZoom3d
                            val label = when (zoomOption) {
                                CameraZoom3D.CLOSE -> stringResource(R.string.camera_zoom_close)
                                CameraZoom3D.STANDARD -> stringResource(R.string.camera_zoom_standard)
                                CameraZoom3D.WIDE -> stringResource(R.string.camera_zoom_wide)
                            }
                            Surface(
                                onClick = { onCameraZoom3dChange(zoomOption) },
                                shape = CutCornerShape(6.dp),
                                color = if (isSelected) Color(0x3300E5FF) else Color(0x1A0F172A),
                                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x2EFFFFFF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 2,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
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
    is3dActive: Boolean = false,
    cameraZoom3d: CameraZoom3D = CameraZoom3D.STANDARD,
    onCameraZoom3dChange: ((CameraZoom3D) -> Unit)? = null,
    neonBloomIntensity: Float = 0.05f,
    visualEffectsEnabled: Boolean = true,
    onNeonBloomIntensityChange: ((Float) -> Unit)? = null,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000))
            .safeDrawingPadding(),
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
                .widthIn(max = if (isLandscape) 640.dp else 420.dp)
                .fillMaxWidth(0.92f)
                .padding(if (isLandscape) 8.dp else 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(if (isLandscape) 16.dp else 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLandscape) {
                    // Layout Bicolunar no Menu de Pausa em Paisagem (ADR 0037)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coluna Esquerda: Controles de Áudio, Bloom e Câmera 3D
                        Column(
                            modifier = Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AudioAndVisualControlsSurface(
                                isSoundEnabled = isSoundEnabled,
                                onToggleSound = onToggleSound,
                                isMusicEnabled = isMusicEnabled,
                                onToggleMusic = onToggleMusic,
                                is3dActive = is3dActive,
                                cameraZoom3d = cameraZoom3d,
                                onCameraZoom3dChange = onCameraZoom3dChange,
                                neonBloomIntensity = neonBloomIntensity,
                                visualEffectsEnabled = visualEffectsEnabled,
                                onNeonBloomIntensityChange = onNeonBloomIntensityChange
                            )
                        }

                        // Coluna Direita: Cabeçalho PAUSADO e Ações Arcade
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.game_paused).uppercase(),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            ArcadePrimaryButton(
                                text = stringResource(R.string.game_resume).uppercase(),
                                containerColor = Color(0xFF00E5FF),
                                icon = Icons.Default.PlayArrow,
                                onClick = onResume
                            )

                            ArcadeSecondaryButton(
                                text = stringResource(R.string.game_restart).uppercase(),
                                accentColor = Color(0xFF00E5FF),
                                icon = Icons.Default.Refresh,
                                onClick = onRestart
                            )

                            ArcadeSecondaryButton(
                                text = stringResource(R.string.game_exit_to_menu).uppercase(),
                                accentColor = Color(0xFF94A3B8),
                                onClick = onExit
                            )
                        }
                    }
                } else {
                    // Modo Retrato: Coluna Única com rolagem vertical segura
                    Text(
                        text = stringResource(R.string.game_paused).uppercase(),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    AudioAndVisualControlsSurface(
                        isSoundEnabled = isSoundEnabled,
                        onToggleSound = onToggleSound,
                        isMusicEnabled = isMusicEnabled,
                        onToggleMusic = onToggleMusic,
                        is3dActive = is3dActive,
                        cameraZoom3d = cameraZoom3d,
                        onCameraZoom3dChange = onCameraZoom3dChange,
                        neonBloomIntensity = neonBloomIntensity,
                        visualEffectsEnabled = visualEffectsEnabled,
                        onNeonBloomIntensityChange = onNeonBloomIntensityChange
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    ArcadePrimaryButton(
                        text = stringResource(R.string.game_resume).uppercase(),
                        containerColor = Color(0xFF00E5FF),
                        icon = Icons.Default.PlayArrow,
                        onClick = onResume
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ArcadeSecondaryButton(
                        text = stringResource(R.string.game_restart).uppercase(),
                        accentColor = Color(0xFF00E5FF),
                        icon = Icons.Default.Refresh,
                        onClick = onRestart
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
}

@Composable
fun GameOverDialog(
    matchState: MatchState,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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
            .background(Color(0xD9000000))
            .safeDrawingPadding(),
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
                .widthIn(max = if (isLandscape) 520.dp else 420.dp)
                .fillMaxWidth(0.90f)
                .padding(if (isLandscape) 8.dp else 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(if (isLandscape) 14.dp else 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = winnerTitle.uppercase(),
                    color = winnerColor,
                    fontSize = if (isLandscape) 20.sp else 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

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
                        fontSize = if (isLandscape) 30.sp else 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = if (isLandscape) 6.dp else 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 24.dp))

                ArcadePrimaryButton(
                    text = stringResource(R.string.game_play_again).uppercase(),
                    containerColor = winnerColor,
                    icon = Icons.Default.Refresh,
                    onClick = onPlayAgain
                )

                Spacer(modifier = Modifier.height(8.dp))

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
    conqueredTheme: TableTheme? = null,
    onSelectStages: () -> Unit,
    onRetry: () -> Unit,
    onNextStage: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val accentColor = if (isWon) Color(0xFF00E5FF) else Color(0xFFF43F5E)
    val glowColor = if (isWon) Color(0xFFFFB800) else Color(0xFFE11D48)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000))
            .safeDrawingPadding(),
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
                .widthIn(max = if (isLandscape) 640.dp else 420.dp)
                .fillMaxWidth(0.92f)
                .padding(if (isLandscape) 8.dp else 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(if (isLandscape) 14.dp else 22.dp)
                    .verticalScroll(rememberScrollState()),
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

                // Celebração de Conquista de Arena (sem emojis, com ícone vetorial e tipografia arcade)
                val conqueredKit = if (isWon && conqueredTheme != null) {
                    remember(conqueredTheme) { conqueredTheme.kit() }
                } else null

                if (conqueredKit != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = CutCornerShape(12.dp),
                        color = Color(0x3300E5FF),
                        border = BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(conqueredKit.visuals.primaryColor, conqueredKit.visuals.secondaryColor)
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = conqueredKit.icon,
                                    contentDescription = null,
                                    tint = conqueredKit.visuals.primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = stringResource(R.string.arena_conquered_badge),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(conqueredKit.titleRes).uppercase(),
                                color = conqueredKit.visuals.primaryColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.arena_conquered_message),
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

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
                    // Derrota: Tentar Novamente (Botão Âmbar Arcade)
                    ArcadePrimaryButton(
                        text = stringResource(R.string.stage_retry_btn).uppercase(),
                        containerColor = Color(0xFFFFB800),
                        textColor = Color(0xFF070B14),
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
