package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.GameViewModel
import kotlin.math.min

// Paleta Neon Arcade
private val TableBackground = Color(0xFF0A0E1A)
private val TableRinkColor = Color(0xFF0F172A)
private val BorderCyan = Color(0xFF00E5FF)
private val BorderOrange = Color(0xFFFF5722)
private val CenterLineColor = Color(0x6600E5FF)
private val PuckBodyColor = Color(0xFFFFFFFF)
private val PuckGlowColor = Color(0x9900E5FF)
private val MalletP1Color = Color(0xFF00B0FF)
private val MalletP1RimColor = Color(0xFF80D8FF)
private val MalletP2Color = Color(0xFFFF3D00)
private val MalletP2RimColor = Color(0xFFFF9E80)
private val GoalSlotColor = Color(0xFF050810)

@Composable
fun AirHockeyArenaCanvas(
    viewModel: GameViewModel,
    mode: GameMode,
    modifier: Modifier = Modifier
) {
    val engine = viewModel.engine
    val dimensions = engine.dimensions
    val isExtendedGuest = mode == GameMode.EXTENDED_GUEST
    val isExtendedHost = mode == GameMode.EXTENDED_HOST
    val isSplitScreen = mode == GameMode.SPLIT_SCREEN

    // Frame pulse acoplado ao vsync do display sem alocação
    var framePulse by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                framePulse = time
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TableBackground)
            .pointerInput(mode) {
                awaitPointerEventScope {
                    val activePointers = mutableMapOf<PointerId, Offset>()

                    while (true) {
                        val event = awaitPointerEvent()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        val vWidth = 1000f
                        val vHeight = if (isExtendedHost || isExtendedGuest) 1000f else 2000f
                        val scale = min(w / vWidth, h / vHeight)
                        val offsetX = (w - vWidth * scale) / 2f
                        val offsetY = (h - vHeight * scale) / 2f

                        for (change in event.changes) {
                            if (change.pressed) {
                                val localX = (change.position.x - offsetX) / scale
                                val localY = (change.position.y - offsetY) / scale

                                if (isExtendedGuest) {
                                    viewModel.onGuestTouchLocal(localX, localY)
                                } else if (isExtendedHost) {
                                    viewModel.onTouchPlayer1(localX, localY)
                                } else if (isSplitScreen) {
                                    if (localY < 1000f) {
                                        viewModel.onTouchPlayer1(localX, localY)
                                    } else {
                                        viewModel.onTouchPlayer2(localX, localY)
                                    }
                                } else {
                                    // SOLO
                                    viewModel.onTouchPlayer1(localX, localY)
                                }
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Leitura acoplada ao framePulse para redraw suave a 120 FPS
            @Suppress("UNUSED_VARIABLE")
            val pulse = framePulse

            val screenW = size.width
            val screenH = size.height
            val vWidth = 1000f
            val vHeight = if (isExtendedHost || isExtendedGuest) 1000f else 2000f

            val scale = min(screenW / vWidth, screenH / vHeight)
            val offsetX = (screenW - vWidth * scale) / 2f
            val offsetY = (screenH - vHeight * scale) / 2f

            // Desenha arena da mesa
            drawTableRink(offsetX, offsetY, vWidth * scale, vHeight * scale, isExtendedHost, isExtendedGuest)

            // Converte coordenadas virtuais em pixels de tela
            fun toScreenX(vx: Float): Float {
                val effectiveX = if (isExtendedGuest) (1000f - vx) else vx
                return offsetX + effectiveX * scale
            }

            fun toScreenY(vy: Float): Float {
                val effectiveY = if (isExtendedGuest) {
                    (dimensions.totalHeight - vy)
                } else {
                    vy
                }
                return offsetY + effectiveY * scale
            }

            // Desenha metas
            drawGoals(offsetX, offsetY, scale, dimensions, isExtendedHost, isExtendedGuest)

            // Desenha Rebathedor 1
            val p1X = toScreenX(engine.player1Mallet.x)
            val p1Y = toScreenY(engine.player1Mallet.y)
            val p1R = engine.player1Mallet.radius * scale
            drawMallet(p1X, p1Y, p1R, MalletP1Color, MalletP1RimColor)

            // Desenha Rebathedor 2
            val p2X = toScreenX(engine.player2Mallet.x)
            val p2Y = toScreenY(engine.player2Mallet.y)
            val p2R = engine.player2Mallet.radius * scale
            drawMallet(p2X, p2Y, p2R, MalletP2Color, MalletP2RimColor)

            // Desenha Disco
            val puckX = toScreenX(engine.puck.x)
            val puckY = toScreenY(engine.puck.y)
            val puckR = engine.puck.radius * scale
            drawPuck(puckX, puckY, puckR)
        }
    }
}

private fun DrawScope.drawTableRink(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean
) {
    // Fundo do campo
    drawRoundRect(
        color = TableRinkColor,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(24f, 24f)
    )

    // Moldura externa neon
    val borderColor = if (isExtendedGuest) BorderOrange else BorderCyan
    drawRoundRect(
        color = borderColor,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(24f, 24f),
        style = Stroke(width = 6f)
    )

    // Linha de centro e círculo central
    if (!isExtendedHost && !isExtendedGuest) {
        val centerY = top + height / 2f
        drawLine(
            color = CenterLineColor,
            start = Offset(left, centerY),
            end = Offset(left + width, centerY),
            strokeWidth = 4f
        )

        drawCircle(
            color = CenterLineColor,
            radius = 120f * (width / 1000f),
            center = Offset(left + width / 2f, centerY),
            style = Stroke(width = 4f)
        )
    } else {
        // Na Mesa Estendida, a borda que encosta na outra mesa recebe uma demarcação luminosa
        val touchY = if (isExtendedHost) (top + height) else top
        drawLine(
            color = Color(0x88FFFFFF),
            start = Offset(left, touchY),
            end = Offset(left + width, touchY),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
        )
    }
}

private fun DrawScope.drawGoals(
    left: Float,
    top: Float,
    scale: Float,
    dimensions: dev.vmellos.airhockey.feature.physics.TableDimensions,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean
) {
    val goalW = dimensions.goalWidth * scale
    val goalLeft = left + dimensions.goalLeft * scale

    // Meta Player 1 (Base na tela do Anfitrião / Mesa única)
    if (!isExtendedGuest) {
        val goalTop = top
        drawRect(
            color = GoalSlotColor,
            topLeft = Offset(goalLeft, goalTop - 10f),
            size = Size(goalW, 16f)
        )
        drawLine(
            color = BorderCyan,
            start = Offset(goalLeft, goalTop),
            end = Offset(goalLeft + goalW, goalTop),
            strokeWidth = 8f
        )
    }

    // Meta Player 2 (Base na tela do Convidado / Topo da mesa única)
    if (!isExtendedHost) {
        val vHeight = if (isExtendedGuest) 1000f else 2000f
        val goalBottom = top + vHeight * scale
        drawRect(
            color = GoalSlotColor,
            topLeft = Offset(goalLeft, goalBottom - 6f),
            size = Size(goalW, 16f)
        )
        drawLine(
            color = BorderOrange,
            start = Offset(goalLeft, goalBottom),
            end = Offset(goalLeft + goalW, goalBottom),
            strokeWidth = 8f
        )
    }
}

private fun DrawScope.drawMallet(
    cx: Float,
    cy: Float,
    radius: Float,
    bodyColor: Color,
    rimColor: Color
) {
    // Sombra suave
    drawCircle(
        color = Color(0x40000000),
        radius = radius * 1.15f,
        center = Offset(cx, cy + radius * 0.15f)
    )

    // Base do rebathedor
    drawCircle(
        color = bodyColor,
        radius = radius,
        center = Offset(cx, cy)
    )

    // Borda luminosa externa
    drawCircle(
        color = rimColor,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = radius * 0.18f)
    )

    // Pega central (knob) com efeito de profundidade
    drawCircle(
        color = Color(0xFF1E293B),
        radius = radius * 0.45f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = rimColor,
        radius = radius * 0.45f,
        center = Offset(cx, cy),
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawPuck(
    cx: Float,
    cy: Float,
    radius: Float
) {
    // Brilho neon externo
    drawCircle(
        color = PuckGlowColor,
        radius = radius * 1.35f,
        center = Offset(cx, cy)
    )

    // Corpo do disco
    drawCircle(
        color = PuckBodyColor,
        radius = radius,
        center = Offset(cx, cy)
    )

    // Anel decorativo interno
    drawCircle(
        color = Color(0xFFCBD5E1),
        radius = radius * 0.65f,
        center = Offset(cx, cy),
        style = Stroke(width = 3f)
    )
}
