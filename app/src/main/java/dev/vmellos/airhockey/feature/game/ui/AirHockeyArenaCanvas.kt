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
import androidx.compose.runtime.collectAsState
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.GameViewModel
import dev.vmellos.airhockey.feature.physics.TableProjection
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
    val projection = remember(dimensions, mode) { TableProjection(dimensions, mode) }
    val isExtendedGuest = mode == GameMode.EXTENDED_GUEST
    val isExtendedHost = mode == GameMode.EXTENDED_HOST
    val isConnectedGuest = mode == GameMode.CONNECTED_GUEST
    val isConnectedHost = mode == GameMode.CONNECTED_HOST
    val isSplitScreen = mode == GameMode.SPLIT_SCREEN

    val prefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val touchOffset = if (prefs.touchOffsetEnabled) 110f else 0f

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
            .pointerInput(mode, projection, touchOffset) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        for (change in event.changes) {
                            if (change.pressed) {
                                val tablePt = projection.screenToTable(change.position.x, change.position.y, w, h)

                                when (mode) {
                                    GameMode.EXTENDED_GUEST, GameMode.CONNECTED_GUEST -> {
                                        viewModel.onTouchGuestGlobal(tablePt.x, tablePt.y + touchOffset)
                                    }
                                    GameMode.EXTENDED_HOST, GameMode.CONNECTED_HOST -> {
                                        viewModel.onTouchPlayer1(tablePt.x, tablePt.y - touchOffset)
                                    }
                                    GameMode.SPLIT_SCREEN -> {
                                        if (tablePt.y < dimensions.centerLineY) {
                                            viewModel.onTouchPlayer2(tablePt.x, tablePt.y + touchOffset)
                                        } else {
                                            viewModel.onTouchPlayer1(tablePt.x, tablePt.y - touchOffset)
                                        }
                                    }
                                    GameMode.SOLO -> {
                                        viewModel.onTouchPlayer1(tablePt.x, tablePt.y - touchOffset)
                                    }
                                }
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val pulse = framePulse

            val screenW = size.width
            val screenH = size.height
            val scale = projection.calculateScale(screenW, screenH)
            val offsetX = projection.calculateOffsetX(screenW, screenH)
            val offsetY = projection.calculateOffsetY(screenW, screenH)
            val vWidth = projection.viewportVirtualWidth
            val vHeight = projection.viewportVirtualHeight

            // Desenha arena da mesa
            drawTableRink(offsetX, offsetY, vWidth * scale, vHeight * scale, isExtendedHost, isExtendedGuest, isConnectedGuest)

            // Desenha metas
            drawGoals(offsetX, offsetY, scale, dimensions, isExtendedHost, isExtendedGuest, isConnectedGuest)

            // Desenha Rebatedor 1
            val p1Screen = projection.tableToScreen(engine.player1Mallet.x, engine.player1Mallet.y, screenW, screenH)
            drawMallet(p1Screen.x, p1Screen.y, engine.player1Mallet.radius * scale, MalletP1Color, MalletP1RimColor)

            // Desenha Rebatedor 2
            val p2Screen = projection.tableToScreen(engine.player2Mallet.x, engine.player2Mallet.y, screenW, screenH)
            drawMallet(p2Screen.x, p2Screen.y, engine.player2Mallet.radius * scale, MalletP2Color, MalletP2RimColor)

            // Desenha Disco somente se visível no viewport deste dispositivo
            if (projection.isVisibleInViewport(engine.puck.x, engine.puck.y, engine.puck.radius)) {
                val puckScreen = projection.tableToScreen(engine.puck.x, engine.puck.y, screenW, screenH)
                drawPuck(puckScreen.x, puckScreen.y, engine.puck.radius * scale)
            }
        }
    }
}

private fun DrawScope.drawTableRink(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean,
    isConnectedGuest: Boolean
) {
    // Fundo do campo
    drawRoundRect(
        color = TableRinkColor,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(24f, 24f)
    )

    // Moldura externa neon
    val borderColor = if (isExtendedGuest || isConnectedGuest) BorderOrange else BorderCyan
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
        // Na Mesa Estendida (Disposição Topo-a-Topo), a borda do TOPO encosta na outra mesa e recebe a demarcação luminosa
        val touchY = top
        drawLine(
            color = Color(0x88FFFFFF),
            start = Offset(left, touchY),
            end = Offset(left + width, touchY),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
        )

        // Semicírculo central neon no topo: ao juntar os dois celulares topo-a-topo, completa o círculo central
        val circleRadius = 140f * (width / 1000f)
        drawArc(
            color = CenterLineColor,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(left + width / 2f - circleRadius, top - circleRadius),
            size = Size(circleRadius * 2f, circleRadius * 2f),
            style = Stroke(width = 4f)
        )
    }
}

private fun DrawScope.drawGoals(
    left: Float,
    top: Float,
    scale: Float,
    dimensions: dev.vmellos.airhockey.feature.physics.TableDimensions,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean,
    isConnectedGuest: Boolean
) {
    val goalW = dimensions.goalWidth * scale
    val goalLeft = left + dimensions.goalLeft * scale
    val goalCenterX = goalLeft + goalW / 2f
    val creaseRadius = (goalW / 2f) * 1.35f
    val vHeight = 2000f
    val screenBottom = top + vHeight * scale

    if (isConnectedGuest) {
        // Área da Meta Player 2 (Laranja - Base)
        drawGoalCrease(goalCenterX, screenBottom, creaseRadius, BorderOrange, isTop = false)
        // Área da Meta Player 1 (Ciano - Topo)
        drawGoalCrease(goalCenterX, top, creaseRadius, BorderCyan, isTop = true)

        // Meta do Player 2 (Laranja) fica na BASE da tela (defendida pelo Convidado)
        drawRect(
            color = GoalSlotColor,
            topLeft = Offset(goalLeft, screenBottom - 6f),
            size = Size(goalW, 16f)
        )
        drawLine(
            color = BorderOrange,
            start = Offset(goalLeft, screenBottom),
            end = Offset(goalLeft + goalW, screenBottom),
            strokeWidth = 8f
        )

        // Meta do Player 1 (Ciano) fica no TOPO da tela (defendida pelo Anfitrião)
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
    } else {
        // Meta Player 1 (Azul / Cyan) - Base da tela para Anfitrião, Solo e Split
        if (!isExtendedGuest) {
            drawGoalCrease(goalCenterX, screenBottom, creaseRadius, BorderCyan, isTop = false)

            drawRect(
                color = GoalSlotColor,
                topLeft = Offset(goalLeft, screenBottom - 6f),
                size = Size(goalW, 16f)
            )
            drawLine(
                color = BorderCyan,
                start = Offset(goalLeft, screenBottom),
                end = Offset(goalLeft + goalW, screenBottom),
                strokeWidth = 8f
            )
        }

        // Meta Player 2 (Laranja)
        // No modo Estendido (Convidado): base da tela do Convidado
        // No modo Solo e Split: topo da tela
        if (isExtendedGuest) {
            drawGoalCrease(goalCenterX, screenBottom, creaseRadius, BorderOrange, isTop = false)

            drawRect(
                color = GoalSlotColor,
                topLeft = Offset(goalLeft, screenBottom - 6f),
                size = Size(goalW, 16f)
            )
            drawLine(
                color = BorderOrange,
                start = Offset(goalLeft, screenBottom),
                end = Offset(goalLeft + goalW, screenBottom),
                strokeWidth = 8f
            )
        } else if (!isExtendedHost) {
            drawGoalCrease(goalCenterX, top, creaseRadius, BorderOrange, isTop = true)

            val goalTop = top
            drawRect(
                color = GoalSlotColor,
                topLeft = Offset(goalLeft, goalTop - 10f),
                size = Size(goalW, 16f)
            )
            drawLine(
                color = BorderOrange,
                start = Offset(goalLeft, goalTop),
                end = Offset(goalLeft + goalW, goalTop),
                strokeWidth = 8f
            )
        }
    }
}

private fun DrawScope.drawGoalCrease(
    centerX: Float,
    baseY: Float,
    radius: Float,
    color: Color,
    isTop: Boolean
) {
    val startAngle = if (isTop) 0f else 180f
    val arcTop = if (isTop) baseY - radius else baseY - radius

    // Preenchimento suave translúcido
    drawArc(
        color = color.copy(alpha = 0.08f),
        startAngle = startAngle,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(centerX - radius, arcTop),
        size = Size(radius * 2f, radius * 2f)
    )

    // Contorno neon luminoso
    drawArc(
        color = color.copy(alpha = 0.85f),
        startAngle = startAngle,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(centerX - radius, arcTop),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 3.5f)
    )

    // Detalhe tracejado interno de precisão
    val innerRadius = radius * 0.6f
    val innerTop = if (isTop) baseY - innerRadius else baseY - innerRadius
    drawArc(
        color = color.copy(alpha = 0.35f),
        startAngle = startAngle,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(centerX - innerRadius, innerTop),
        size = Size(innerRadius * 2f, innerRadius * 2f),
        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    )
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
