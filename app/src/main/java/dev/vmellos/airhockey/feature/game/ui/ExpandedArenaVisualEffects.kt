package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

private fun buildChamferedArenaPath(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    chamfer: Float,
    isExtended: Boolean,
    closePath: Boolean = true
): Path = Path().apply {
    if (isExtended) {
        moveTo(left, top)
        lineTo(left, bottom - chamfer)
        lineTo(left + chamfer, bottom)
        lineTo(right - chamfer, bottom)
        lineTo(right, bottom - chamfer)
        lineTo(right, top)
        if (closePath) close()
    } else {
        moveTo(left, top + chamfer)
        lineTo(left + chamfer, top)
        lineTo(right - chamfer, top)
        lineTo(right, top + chamfer)
        lineTo(right, bottom - chamfer)
        lineTo(right - chamfer, bottom)
        lineTo(left + chamfer, bottom)
        lineTo(left, bottom - chamfer)
        if (closePath) close()
    }
}

private fun DrawScope.drawChamferedExtendedOrFullBorder(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    chamfer: Float,
    borderColor: Color,
    strokeWidth: Float,
    scale: Float,
    isExtended: Boolean
) {
    if (isExtended) {
        val openBorder = buildChamferedArenaPath(left, top, right, bottom, chamfer, isExtended = true, closePath = false)
        drawPath(
            path = openBorder,
            color = borderColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = borderColor.copy(alpha = 0.65f),
            start = Offset(left, top),
            end = Offset(right, top),
            strokeWidth = 3f * scale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * scale, 18f * scale), 0f)
        )
    } else {
        val fullBorder = buildChamferedArenaPath(left, top, right, bottom, chamfer, isExtended = false, closePath = true)
        drawPath(path = fullBorder, color = borderColor, style = Stroke(width = strokeWidth))
    }
}

// ============================================================================
// 1. PALETA DE CORES: GRIDIRON SCI-FI (FUTEBOL AMERICANO)
// ============================================================================
val GridironDark = Color(0xFF04120C)
val GridironRink = Color(0xFF072116)
val GridironGold = Color(0xFFFFB300)
val GridironGoldBright = Color(0xFFFFD54F)
val GridironGoldCore = Color(0xFFFFF8E1)
val GridironCyan = Color(0xFF00E5FF)
val GridironCyanBright = Color(0xFF80D8FF)
val GridironCyanCore = Color(0xFFE0F7FA)
val GridironYardLine = Color(0x2800E5FF)
val GridironHashMark = Color(0x3500E5FF)

// ============================================================================
// 2. PALETA DE CORES: CYBER DIAMOND (BEISEBOL CIBERNÉTICO)
// ============================================================================
val BaseballDark = Color(0xFF0E0B08)
val BaseballRink = Color(0xFF1C1510)
val BaseballRed = Color(0xFFFF1744)
val BaseballRedBright = Color(0xFFFF5252)
val BaseballRedCore = Color(0xFFFFEBEE)
val BaseballCyan = Color(0xFF00E5FF)
val BaseballCyanBright = Color(0xFF80D8FF)
val BaseballCyanCore = Color(0xFFE0F7FA)
val BaseballChalk = Color(0x35FFFFFF)

// ============================================================================
// 3. PALETA DE CORES: WASTELAND APOCALÍPTICO
// ============================================================================
val WastelandDark = Color(0xFF130B06)
val WastelandRink = Color(0xFF22130A)
val WastelandRust = Color(0xFF8D4E24)
val WastelandFlame = Color(0xFFFF3D00)
val WastelandFlameBright = Color(0xFFFF9100)
val WastelandFlameCore = Color(0xFFFFF3E0)
val WastelandHazardYellow = Color(0xFFFFD600)
val WastelandSteel = Color(0xFF607D8B)

// ============================================================================
// 4. PALETA DE CORES: RETRO SYNTHWAVE 80S (OUTRUN)
// ============================================================================
val SynthDark = Color(0xFF0F041D)
val SynthRink = Color(0xFF1E083A)
val SynthPink = Color(0xFFFF007F)
val SynthPinkBright = Color(0xFFFF4081)
val SynthPinkCore = Color(0xFFFCE4EC)
val SynthCyan = Color(0xFF00F0FF)
val SynthCyanBright = Color(0xFF80D8FF)
val SynthCyanCore = Color(0xFFE0F7FA)
val SynthSunYellow = Color(0xFFFFD600)
val SynthSunOrange = Color(0xFFFF6D00)
val SynthGridLine = Color(0x28FF007F)

// ============================================================================
// 5. PALETA DE CORES: REATOR NUCLEAR TOKAMAK
// ============================================================================
val TokamakDark = Color(0xFF030D13)
val TokamakRink = Color(0xFF061A26)
val TokamakRadiation = Color(0xFF76FF03)
val TokamakRadiationBright = Color(0xFFB2FF59)
val TokamakRadiationCore = Color(0xFFF1F8E9)
val TokamakPlasma = Color(0xFF00E5FF)
val TokamakPlasmaBright = Color(0xFF80D8FF)
val TokamakPlasmaCore = Color(0xFFE0F7FA)

// ============================================================================
// 6. PALETA DE CORES: CYBER PONG (RETRÔ CRT)
// ============================================================================
val PongDark = Color(0xFF020602)
val PongRink = Color(0xFF041004)
val PongGreen = Color(0xFF39FF14)
val PongGreenBright = Color(0xFFCCFF90)
val PongGreenCore = Color(0xFFF1F8E9)
val PongAmber = Color(0xFFFFB300)
val PongAmberBright = Color(0xFFFFD54F)
val PongAmberCore = Color(0xFFFFF8E1)

// ============================================================================
// 7. PALETA DE CORES: COLISEU CIBERNÉTICO
// ============================================================================
val ColosseumDark = Color(0xFF0D0A06)
val ColosseumRink = Color(0xFF1C150E)
val ColosseumGold = Color(0xFFFFD700)
val ColosseumGoldBright = Color(0xFFFFF176)
val ColosseumGoldCore = Color(0xFFFFFDE7)
val ColosseumCrimson = Color(0xFFD50000)
val ColosseumCrimsonBright = Color(0xFFFF5252)
val ColosseumCrimsonCore = Color(0xFFFFEBEE)
val ColosseumBronze = Color(0xFFB87333)

// ============================================================================
// RENDERIZAÇÃO: 1. GRIDIRON SCI-FI (FUTEBOL AMERICANO)
// ============================================================================

fun DrawScope.drawGridironTable(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean,
    isConnectedGuest: Boolean
) {
    val right = left + width
    val bottom = top + height
    val chamfer = 40f * scale
    val centerY = top + height / 2f
    val centerX = left + width / 2f
    val isExtended = isExtendedHost || isExtendedGuest

    // 1. Fundo do gramado escuro com cantos chanfrados
    val arenaPath = buildChamferedArenaPath(left, top, right, bottom, chamfer, isExtended = isExtended, closePath = true)

    drawPath(
        path = arenaPath,
        brush = Brush.verticalGradient(
            colors = listOf(GridironRink, GridironDark, GridironRink),
            startY = top,
            endY = bottom
        )
    )

    // Faixas de gramado de 10 jardas alternadas
    val yardStripes = 10
    val yardH = height / yardStripes
    for (i in 0 until yardStripes step 2) {
        drawRect(
            color = Color(0x0800E5FF),
            topLeft = Offset(left, top + i * yardH),
            size = Size(width, yardH)
        )
    }

    // Linhas de jardas a cada intervalo
    for (i in 1 until yardStripes) {
        val y = top + i * yardH
        val is50Yard = !isExtended && (i == yardStripes / 2)
        val lineColor = if (is50Yard) GridironGold.copy(alpha = 0.5f) else GridironYardLine
        val strokeW = if (is50Yard) 2.5f * scale else 1.2f * scale
        drawLine(
            color = lineColor,
            start = Offset(left + 20f * scale, y),
            end = Offset(right - 20f * scale, y),
            strokeWidth = strokeW
        )

        // Hash marks centrais (duas marquinhas no meio de cada jarda)
        val hashSpacing = 28f * scale
        drawLine(
            color = GridironHashMark,
            start = Offset(centerX - hashSpacing, y - 8f * scale),
            end = Offset(centerX - hashSpacing, y + 8f * scale),
            strokeWidth = 1.5f * scale
        )
        drawLine(
            color = GridironHashMark,
            start = Offset(centerX + hashSpacing, y - 8f * scale),
            end = Offset(centerX + hashSpacing, y + 8f * scale),
            strokeWidth = 1.5f * scale
        )
    }

    // Linha de 50 jardas central
    if (!isExtendedHost && !isExtendedGuest) {
        drawCircle(
            color = GridironGold,
            radius = 70f * scale,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.5f * scale)
        )
        drawCircle(
            color = GridironCyan,
            radius = 12f * scale,
            center = Offset(centerX, centerY)
        )
    }

    // Bordas Chanfradas em Dourado / Ciano
    val topColor = if (isConnectedGuest || isExtendedHost) GridironCyan else GridironGold
    val bottomColor = if (isExtendedGuest || isConnectedGuest) GridironGold else GridironCyan
    val borderColor = if (isExtended) bottomColor else topColor

    drawChamferedExtendedOrFullBorder(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        chamfer = chamfer,
        borderColor = borderColor,
        strokeWidth = 4f * scale,
        scale = scale,
        isExtended = isExtended
    )
}

fun DrawScope.drawGridironGoal(
    goalLeft: Float,
    goalWidth: Float,
    goalY: Float,
    scale: Float,
    color: Color,
    isTop: Boolean
) {
    val stemH = 26f * scale
    val uprightH = 34f * scale
    val centerX = goalLeft + goalWidth / 2f
    val crossbarY = if (isTop) goalY + stemH else goalY - stemH
    val tipY = if (isTop) crossbarY + uprightH else crossbarY - uprightH

    // Haste de sustentação central
    drawLine(
        color = color,
        start = Offset(centerX, goalY),
        end = Offset(centerX, crossbarY),
        strokeWidth = 4f * scale,
        cap = StrokeCap.Round
    )

    // Barra horizontal (Crossbar)
    drawLine(
        color = color,
        start = Offset(goalLeft + goalWidth * 0.15f, crossbarY),
        end = Offset(goalLeft + goalWidth * 0.85f, crossbarY),
        strokeWidth = 4f * scale,
        cap = StrokeCap.Round
    )

    // Traves verticais em Y (Uprights)
    drawLine(
        color = color,
        start = Offset(goalLeft + goalWidth * 0.15f, crossbarY),
        end = Offset(goalLeft + goalWidth * 0.15f, tipY),
        strokeWidth = 3f * scale,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(goalLeft + goalWidth * 0.85f, crossbarY),
        end = Offset(goalLeft + goalWidth * 0.85f, tipY),
        strokeWidth = 3f * scale,
        cap = StrokeCap.Round
    )
}

fun DrawScope.drawGridironPuck(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    scale: Float
) {
    // Sombra projetada
    drawCircle(
        color = Color(0x66000000),
        radius = radius * 1.05f,
        center = Offset(cx, cy + radius * 0.15f)
    )

    // Halo de brilho da bola
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.6f
        ),
        radius = radius * 1.6f,
        center = Offset(cx, cy)
    )

    // Corpo elipsoidal / esférico de couro composto escuro
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF6D3816), Color(0xFF381C09), Color(0xFF1E0E04)),
            center = Offset(cx - radius * 0.25f, cy - radius * 0.25f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // Anéis brancos de precisão em ambas as extremidades
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = radius * 0.76f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f * scale)
    )

    // Costuras brancas (Laces) no centro
    val laceW = radius * 0.5f
    drawLine(
        color = Color.White,
        start = Offset(cx, cy - laceW),
        end = Offset(cx, cy + laceW),
        strokeWidth = 2.5f * scale,
        cap = StrokeCap.Round
    )
    for (i in -2..2) {
        val ly = cy + i * (laceW * 0.38f)
        drawLine(
            color = Color.White,
            start = Offset(cx - radius * 0.22f, ly),
            end = Offset(cx + radius * 0.22f, ly),
            strokeWidth = 2.2f * scale,
            cap = StrokeCap.Round
        )
    }

    // Borda perimetral neon
    drawCircle(
        color = accentColor,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 3.5f * scale)
    )
}

fun DrawScope.drawGridironMallet(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    rimColor: Color,
    scale: Float
) {
    // Sombra
    drawCircle(color = Color(0x66000000), radius = radius * 1.1f, center = Offset(cx, cy + radius * 0.18f))

    // Halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.6f
        ),
        radius = radius * 1.6f,
        center = Offset(cx, cy)
    )

    // Base reforçada Linebacker (blindagem pesada)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(rimColor, accentColor, Color(0xFF0F1A12)),
            center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // 6 rebites de impacto hexagonais
    for (i in 0 until 6) {
        val angle = i * 60f * (Math.PI / 180f).toFloat()
        val rx = cx + cos(angle) * radius * 0.75f
        val ry = cy + sin(angle) * radius * 0.75f
        drawCircle(color = Color.White, radius = 2.5f * scale, center = Offset(rx, ry))
    }

    // Manopla central com anel amortecedor
    val knobR = radius * 0.45f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, rimColor, Color(0xFF0A120B)),
            center = Offset(cx - knobR * 0.2f, cy - knobR * 0.2f),
            radius = knobR * 1.1f
        ),
        radius = knobR,
        center = Offset(cx, cy)
    )
    drawCircle(color = rimColor, radius = knobR, center = Offset(cx, cy), style = Stroke(width = 2.5f * scale))
}

// ============================================================================
// RENDERIZAÇÃO: 2. CYBER DIAMOND (BEISEBOL CIBERNÉTICO)
// ============================================================================

fun DrawScope.drawCyberBaseballTable(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean,
    isConnectedGuest: Boolean
) {
    val right = left + width
    val bottom = top + height
    val chamfer = 40f * scale
    val centerY = top + height / 2f
    val centerX = left + width / 2f
    val isExtended = isExtendedHost || isExtendedGuest

    val arenaPath = buildChamferedArenaPath(left, top, right, bottom, chamfer, isExtended = isExtended, closePath = true)

    drawPath(
        path = arenaPath,
        brush = Brush.verticalGradient(
            colors = listOf(BaseballRink, BaseballDark, BaseballRink),
            startY = top,
            endY = bottom
        )
    )

    if (!isExtended) {
        // O Diamante de Beisebol (Losango conectando as 4 bases)
        val diamondPath = Path().apply {
            moveTo(centerX, bottom - 100f * scale) // Home Plate
            lineTo(right - 80f * scale, centerY)   // 1ª Base
            lineTo(centerX, top + 100f * scale)    // 2ª Base
            lineTo(left + 80f * scale, centerY)    // 3ª Base
            close()
        }
        drawPath(path = diamondPath, color = Color(0x18FF1744))
        drawPath(path = diamondPath, color = BaseballRed.copy(alpha = 0.5f), style = Stroke(width = 2f * scale))

        // Montinho central do arremessador
        drawCircle(
            color = BaseballRed.copy(alpha = 0.25f),
            radius = 55f * scale,
            center = Offset(centerX, centerY)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(centerX - 18f * scale, centerY - 5f * scale),
            size = Size(36f * scale, 10f * scale)
        )
    }

    // Bordas Chanfradas em Vermelho / Ciano
    drawChamferedExtendedOrFullBorder(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        chamfer = chamfer,
        borderColor = BaseballRed,
        strokeWidth = 4f * scale,
        scale = scale,
        isExtended = isExtended
    )
}

fun DrawScope.drawBaseballGoal(
    goalLeft: Float,
    goalWidth: Float,
    goalY: Float,
    scale: Float,
    color: Color,
    isTop: Boolean
) {
    val depth = 35f * scale
    val yTop = if (isTop) goalY else goalY - depth
    val yBottom = if (isTop) goalY + depth else goalY

    // Matriz de Strike Zone (3x3 retangular holográfica)
    drawRect(
        color = color.copy(alpha = 0.15f),
        topLeft = Offset(goalLeft, yTop),
        size = Size(goalWidth, depth)
    )
    drawRect(
        color = color,
        topLeft = Offset(goalLeft, yTop),
        size = Size(goalWidth, depth),
        style = Stroke(width = 3f * scale)
    )

    // Linhas internas da strike zone
    drawLine(
        color = color.copy(alpha = 0.6f),
        start = Offset(goalLeft + goalWidth / 3f, yTop),
        end = Offset(goalLeft + goalWidth / 3f, yBottom),
        strokeWidth = 1.5f * scale
    )
    drawLine(
        color = color.copy(alpha = 0.6f),
        start = Offset(goalLeft + (goalWidth * 2f) / 3f, yTop),
        end = Offset(goalLeft + (goalWidth * 2f) / 3f, yBottom),
        strokeWidth = 1.5f * scale
    )
}

fun DrawScope.drawCyberBaseballPuck(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    scale: Float
) {
    drawCircle(color = Color(0x66000000), radius = radius * 1.05f, center = Offset(cx, cy + radius * 0.15f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.4f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.5f
        ),
        radius = radius * 1.5f,
        center = Offset(cx, cy)
    )

    // Esfera branca de couro sintético
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, Color(0xFFE2E8F0), Color(0xFFCBD5E1)),
            center = Offset(cx - radius * 0.2f, cy - radius * 0.2f),
            radius = radius * 1.05f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // Costuras duplas curvas vermelhas (Twin Seams)
    for (dir in listOf(-1f, 1f)) {
        val seamPath = Path().apply {
            moveTo(cx + dir * radius * 0.45f, cy - radius * 0.85f)
            cubicTo(
                cx + dir * radius * 0.15f, cy - radius * 0.35f,
                cx + dir * radius * 0.15f, cy + radius * 0.35f,
                cx + dir * radius * 0.45f, cy + radius * 0.85f
            )
        }
        drawPath(path = seamPath, color = BaseballRed, style = Stroke(width = 2.5f * scale, cap = StrokeCap.Round))

        // Pontos de costura (Stitches)
        for (i in -4..4) {
            val sy = cy + i * (radius * 0.18f)
            val sx = cx + dir * (radius * 0.28f)
            drawLine(
                color = BaseballRed,
                start = Offset(sx - 4f * scale, sy - 3f * scale),
                end = Offset(sx + 4f * scale, sy + 3f * scale),
                strokeWidth = 1.8f * scale
            )
        }
    }

    drawCircle(color = accentColor, radius = radius, center = Offset(cx, cy), style = Stroke(width = 3.5f * scale))
}

fun DrawScope.drawSluggerMallet(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    rimColor: Color,
    scale: Float
) {
    drawCircle(color = Color(0x66000000), radius = radius * 1.1f, center = Offset(cx, cy + radius * 0.18f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.6f
        ),
        radius = radius * 1.6f,
        center = Offset(cx, cy)
    )

    // Platô de alumínio escuro aeronáutico
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(rimColor, accentColor, Color(0xFF1E140F)),
            center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // Ranhuras concêntricas de sweet-spot
    for (i in 1..3) {
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = radius * (0.28f + i * 0.18f),
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f * scale)
        )
    }

    // Manopla central gravada
    val knobR = radius * 0.42f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, rimColor, Color(0xFF140D09)),
            center = Offset(cx - knobR * 0.2f, cy - knobR * 0.2f),
            radius = knobR * 1.1f
        ),
        radius = knobR,
        center = Offset(cx, cy)
    )
    drawCircle(color = rimColor, radius = knobR, center = Offset(cx, cy), style = Stroke(width = 2.5f * scale))
}

// ============================================================================
// RENDERIZAÇÃO: 3. WASTELAND APOCALÍPTICO (MAD MAX / INDUSTRIAL)
// ============================================================================

fun DrawScope.drawWastelandTable(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean,
    isConnectedGuest: Boolean
) {
    val right = left + width
    val bottom = top + height
    val chamfer = 40f * scale
    val centerY = top + height / 2f
    val centerX = left + width / 2f
    val isExtended = isExtendedHost || isExtendedGuest

    val arenaPath = buildChamferedArenaPath(left, top, right, bottom, chamfer, isExtended = isExtended, closePath = true)

    drawPath(
        path = arenaPath,
        brush = Brush.verticalGradient(
            colors = listOf(WastelandRink, WastelandDark, WastelandRink),
            startY = top,
            endY = bottom
        )
    )

    // Chapas de aço sobrepostas (linhas industriais de solda)
    val plates = 4
    val plateH = height / plates
    for (i in 1 until plates) {
        val y = top + i * plateH
        drawLine(
            color = WastelandSteel.copy(alpha = 0.4f),
            start = Offset(left, y),
            end = Offset(right, y),
            strokeWidth = 2.5f * scale
        )
        // Rebites na solda
        var rx = left + 30f * scale
        while (rx < right) {
            drawCircle(color = WastelandRust, radius = 2.5f * scale, center = Offset(rx, y))
            rx += 40f * scale
        }
    }

    // Listras de advertência (Hazard Stripes amarela e preta) nos 4 cantos
    for (offset in listOf(top + 80f * scale, bottom - 80f * scale)) {
        drawLine(
            color = WastelandHazardYellow.copy(alpha = 0.45f),
            start = Offset(left + 20f * scale, offset),
            end = Offset(left + 80f * scale, offset + 40f * scale),
            strokeWidth = 6f * scale,
            cap = StrokeCap.Square
        )
        drawLine(
            color = WastelandHazardYellow.copy(alpha = 0.45f),
            start = Offset(right - 80f * scale, offset),
            end = Offset(right - 20f * scale, offset + 40f * scale),
            strokeWidth = 6f * scale,
            cap = StrokeCap.Square
        )
    }

    if (!isExtended) {
        // Círculo central com fissuras de magma incandescente
        drawCircle(
            color = WastelandFlame.copy(alpha = 0.6f),
            radius = 75f * scale,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3f * scale)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(WastelandFlameBright.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = 65f * scale
            ),
            radius = 65f * scale,
            center = Offset(centerX, centerY)
        )
    }

    drawChamferedExtendedOrFullBorder(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        chamfer = chamfer,
        borderColor = WastelandFlame,
        strokeWidth = 4.5f * scale,
        scale = scale,
        isExtended = isExtended
    )
}

fun DrawScope.drawWastelandGoal(
    goalLeft: Float,
    goalWidth: Float,
    goalY: Float,
    scale: Float,
    color: Color,
    isTop: Boolean
) {
    val h = 32f * scale
    val yTip = if (isTop) goalY + h else goalY - h

    // Grades de ferro batido e spikes de sucata
    drawLine(
        color = color,
        start = Offset(goalLeft, goalY),
        end = Offset(goalLeft + goalWidth, goalY),
        strokeWidth = 5f * scale,
        cap = StrokeCap.Square
    )

    // 5 Espetos / Dentes de sucata
    val spikeCount = 5
    val step = goalWidth / spikeCount
    for (i in 0..spikeCount) {
        val sx = goalLeft + i * step
        drawLine(
            color = WastelandFlameBright,
            start = Offset(sx, goalY),
            end = Offset(sx, yTip),
            strokeWidth = 3f * scale,
            cap = StrokeCap.Round
        )
    }
}

fun DrawScope.drawSawbladePuck(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    scale: Float
) {
    drawCircle(color = Color(0x66000000), radius = radius * 1.05f, center = Offset(cx, cy + radius * 0.15f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(WastelandFlame.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.6f
        ),
        radius = radius * 1.6f,
        center = Offset(cx, cy)
    )

    // Disco metálico escuro
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(WastelandSteel, Color(0xFF263238), Color(0xFF10171A)),
            center = Offset(cx - radius * 0.2f, cy - radius * 0.2f),
            radius = radius
        ),
        radius = radius * 0.88f,
        center = Offset(cx, cy)
    )

    // 12 dentes de serra circular giratória
    val toothCount = 12
    for (i in 0 until toothCount) {
        val angle = i * (360f / toothCount)
        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            val toothPath = Path().apply {
                moveTo(cx, cy - radius * 0.82f)
                lineTo(cx + radius * 0.22f, cy - radius)
                lineTo(cx, cy - radius * 0.95f)
                close()
            }
            drawPath(path = toothPath, color = WastelandFlameBright)
        }
    }

    // Núcleo incandescente e furos de arrefecimento
    drawCircle(color = WastelandFlame, radius = radius * 0.32f, center = Offset(cx, cy))
    drawCircle(color = Color.White, radius = radius * 0.14f, center = Offset(cx, cy))
}

fun DrawScope.drawRipperMallet(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    rimColor: Color,
    scale: Float
) {
    drawCircle(color = Color(0x66000000), radius = radius * 1.1f, center = Offset(cx, cy + radius * 0.18f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.6f
        ),
        radius = radius * 1.6f,
        center = Offset(cx, cy)
    )

    // Batedor com blindagem de placas de ferro
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(rimColor, accentColor, Color(0xFF1E0E04)),
            center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // 4 Spikes de canto agressivos
    for (i in 0 until 4) {
        val angle = i * 90f + 45f
        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            drawLine(
                color = WastelandFlameBright,
                start = Offset(cx, cy - radius * 0.75f),
                end = Offset(cx, cy - radius * 1.12f),
                strokeWidth = 3.5f * scale,
                cap = StrokeCap.Round
            )
        }
    }

    val knobR = radius * 0.42f
    drawCircle(color = Color(0xFF140802), radius = knobR, center = Offset(cx, cy))
    drawCircle(color = rimColor, radius = knobR, center = Offset(cx, cy), style = Stroke(width = 2.5f * scale))
    drawCircle(color = Color.White, radius = knobR * 0.28f, center = Offset(cx, cy))
}

// ============================================================================
// RENDERIZAÇÃO: 4. RETRO SYNTHWAVE 80S (OUTRUN / SUNSET)
// ============================================================================

fun DrawScope.drawRetroSynthwaveTable(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean,
    isConnectedGuest: Boolean
) {
    val right = left + width
    val bottom = top + height
    val chamfer = 40f * scale
    val centerY = top + height / 2f
    val centerX = left + width / 2f
    val isExtended = isExtendedHost || isExtendedGuest

    val arenaPath = buildChamferedArenaPath(left, top, right, bottom, chamfer, isExtended = isExtended, closePath = true)

    drawPath(
        path = arenaPath,
        brush = Brush.verticalGradient(
            colors = listOf(SynthRink, SynthDark, SynthRink),
            startY = top,
            endY = bottom
        )
    )

    // Grade Outrun em perspectiva magenta
    val gridLinesCount = 12
    for (i in 1 until gridLinesCount) {
        val ratio = i / gridLinesCount.toFloat()
        val y = top + ratio * height
        drawLine(
            color = SynthGridLine,
            start = Offset(left, y),
            end = Offset(right, y),
            strokeWidth = 1.2f * scale
        )
    }

    // Linhas verticais convergentes
    for (i in 0..8) {
        val x = left + i * (width / 8f)
        drawLine(
            color = SynthGridLine,
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = 1.2f * scale
        )
    }

    if (!isExtended) {
        // Sol Neon Fatiado (Retro Sun) no centro
        val sunRadius = 70f * scale
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(SynthSunYellow, SynthSunOrange, SynthPink),
                startY = centerY - sunRadius,
                endY = centerY + sunRadius
            ),
            radius = sunRadius,
            center = Offset(centerX, centerY)
        )

        // Fatias horizontais do sol clássico dos anos 80
        for (i in 1..4) {
            val sliceY = centerY + (i * 12f * scale)
            val sliceH = 4f * scale * (i * 0.8f)
            drawRect(
                color = SynthDark,
                topLeft = Offset(centerX - sunRadius, sliceY),
                size = Size(sunRadius * 2f, sliceH)
            )
        }
    }

    drawChamferedExtendedOrFullBorder(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        chamfer = chamfer,
        borderColor = SynthPink,
        strokeWidth = 4f * scale,
        scale = scale,
        isExtended = isExtended
    )
}

fun DrawScope.drawSynthwaveGoal(
    goalLeft: Float,
    goalWidth: Float,
    goalY: Float,
    scale: Float,
    color: Color,
    isTop: Boolean
) {
    val barCount = 10
    val barW = (goalWidth / barCount) * 0.75f
    val gap = (goalWidth / barCount) * 0.25f
    val maxH = 34f * scale

    // Barras de Equalizador de Áudio (VU Meter)
    for (i in 0 until barCount) {
        val bx = goalLeft + i * (barW + gap)
        val h = maxH * (0.35f + 0.65f * (sin(i * 0.7f).coerceIn(0.1f, 1f)))
        val by = if (isTop) goalY else goalY - h

        drawRect(
            color = if (i % 2 == 0) SynthPink else SynthCyan,
            topLeft = Offset(bx, by),
            size = Size(barW, h)
        )
    }
}

fun DrawScope.drawSynthwavePuck(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    scale: Float
) {
    drawCircle(color = Color(0x66000000), radius = radius * 1.05f, center = Offset(cx, cy + radius * 0.15f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SynthPink.copy(alpha = 0.5f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.6f
        ),
        radius = radius * 1.6f,
        center = Offset(cx, cy)
    )

    // Disco cromado Synthwave
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, SynthPinkBright, SynthDark),
            center = Offset(cx - radius * 0.2f, cy - radius * 0.2f),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // Estrela de 4 pontas no centro
    drawLine(
        color = SynthCyanBright,
        start = Offset(cx - radius * 0.6f, cy),
        end = Offset(cx + radius * 0.6f, cy),
        strokeWidth = 2.5f * scale,
        cap = StrokeCap.Round
    )
    drawLine(
        color = SynthCyanBright,
        start = Offset(cx, cy - radius * 0.6f),
        end = Offset(cx, cy + radius * 0.6f),
        strokeWidth = 2.5f * scale,
        cap = StrokeCap.Round
    )

    drawCircle(color = SynthPink, radius = radius, center = Offset(cx, cy), style = Stroke(width = 3.5f * scale))
}

fun DrawScope.drawOutrunStrikerMallet(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    rimColor: Color,
    scale: Float
) {
    drawCircle(color = Color(0x66000000), radius = radius * 1.1f, center = Offset(cx, cy + radius * 0.18f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SynthPink.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.6f
        ),
        radius = radius * 1.6f,
        center = Offset(cx, cy)
    )

    // Base com gradiente Sunset
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SynthSunYellow, SynthPink, SynthDark),
            center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // Anéis de neon concêntricos
    drawCircle(color = SynthCyan, radius = radius * 0.72f, center = Offset(cx, cy), style = Stroke(width = 2f * scale))

    val knobR = radius * 0.42f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, SynthPink, SynthDark),
            center = Offset(cx - knobR * 0.2f, cy - knobR * 0.2f),
            radius = knobR * 1.1f
        ),
        radius = knobR,
        center = Offset(cx, cy)
    )
    drawCircle(color = Color.White, radius = knobR * 0.25f, center = Offset(cx, cy))
}

// ============================================================================
// RENDERIZAÇÃO: 5. REATOR NUCLEAR TOKAMAK (FUSÃO MAGNÉTICA)
// ============================================================================

fun DrawScope.drawTokamakTable(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean,
    isConnectedGuest: Boolean
) {
    val right = left + width
    val bottom = top + height
    val chamfer = 40f * scale
    val centerY = top + height / 2f
    val centerX = left + width / 2f
    val isExtended = isExtendedHost || isExtendedGuest

    val arenaPath = buildChamferedArenaPath(left, top, right, bottom, chamfer, isExtended = isExtended, closePath = true)

    drawPath(
        path = arenaPath,
        brush = Brush.verticalGradient(
            colors = listOf(TokamakRink, TokamakDark, TokamakRink),
            startY = top,
            endY = bottom
        )
    )

    // Bobinas magnéticas toroidais perimetrais
    val coilStep = 55f * scale
    var cyc = top + 60f * scale
    while (cyc < bottom - 60f * scale) {
        drawLine(
            color = TokamakRadiation.copy(alpha = 0.22f),
            start = Offset(left + 15f * scale, cyc),
            end = Offset(right - 15f * scale, cyc),
            strokeWidth = 1.2f * scale
        )
        cyc += coilStep
    }

    if (!isExtended) {
        // Símbolo sutil de Radiação Trefoil no centro
        drawCircle(
            color = TokamakRadiation.copy(alpha = 0.65f),
            radius = 70f * scale,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.5f * scale)
        )
        for (i in 0 until 3) {
            val angle = i * 120f
            rotate(degrees = angle, pivot = Offset(centerX, centerY)) {
                val bladePath = Path().apply {
                    moveTo(centerX, centerY - 15f * scale)
                    lineTo(centerX - 35f * scale, centerY - 55f * scale)
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(centerX - 55f * scale, centerY - 55f * scale, centerX + 55f * scale, centerY + 55f * scale),
                        startAngleDegrees = -120f,
                        sweepAngleDegrees = 60f,
                        forceMoveTo = false
                    )
                    close()
                }
                drawPath(path = bladePath, color = TokamakRadiation.copy(alpha = 0.4f))
            }
        }
        drawCircle(color = TokamakRadiationBright, radius = 12f * scale, center = Offset(centerX, centerY))
    }

    drawChamferedExtendedOrFullBorder(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        chamfer = chamfer,
        borderColor = TokamakRadiation,
        strokeWidth = 4f * scale,
        scale = scale,
        isExtended = isExtended
    )
}

fun DrawScope.drawTokamakGoal(
    goalLeft: Float,
    goalWidth: Float,
    goalY: Float,
    scale: Float,
    color: Color,
    isTop: Boolean
) {
    val h = 28f * scale
    val yPinch = if (isTop) goalY + h else goalY - h

    // Polos de confinamento magnético
    drawCircle(color = color, radius = 8f * scale, center = Offset(goalLeft, goalY))
    drawCircle(color = color, radius = 8f * scale, center = Offset(goalLeft + goalWidth, goalY))

    // Feixe de plasma de alta voltagem
    drawLine(
        color = TokamakPlasmaBright,
        start = Offset(goalLeft, goalY),
        end = Offset(goalLeft + goalWidth / 2f, yPinch),
        strokeWidth = 3f * scale
    )
    drawLine(
        color = TokamakPlasmaBright,
        start = Offset(goalLeft + goalWidth / 2f, yPinch),
        end = Offset(goalLeft + goalWidth, goalY),
        strokeWidth = 3f * scale
    )
}

fun DrawScope.drawAtomicCorePuck(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    scale: Float
) {
    drawCircle(color = Color(0x66000000), radius = radius * 1.05f, center = Offset(cx, cy + radius * 0.15f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(TokamakRadiation.copy(alpha = 0.5f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.7f
        ),
        radius = radius * 1.7f,
        center = Offset(cx, cy)
    )

    // Núcleo denso
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, TokamakRadiationBright, Color(0xFF0F3304)),
            center = Offset(cx, cy),
            radius = radius * 0.6f
        ),
        radius = radius * 0.5f,
        center = Offset(cx, cy)
    )

    // 3 Órbitas eletrônicas elípticas rotacionadas em 60 graus
    for (i in 0 until 3) {
        val angle = i * 60f
        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            drawOval(
                color = TokamakPlasma,
                topLeft = Offset(cx - radius, cy - radius * 0.45f),
                size = Size(radius * 2f, radius * 0.9f),
                style = Stroke(width = 2f * scale)
            )
            // Nó de elétron orbitando
            drawCircle(color = Color.White, radius = 3.5f * scale, center = Offset(cx + radius, cy))
        }
    }
}

fun DrawScope.drawMagneticCoilMallet(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    rimColor: Color,
    scale: Float
) {
    drawCircle(color = Color(0x66000000), radius = radius * 1.1f, center = Offset(cx, cy + radius * 0.18f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.6f
        ),
        radius = radius * 1.6f,
        center = Offset(cx, cy)
    )

    // Base em câmara toroidal
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(rimColor, accentColor, Color(0xFF041706)),
            center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // 8 filamentos de bobina magnética
    for (i in 0 until 8) {
        val angle = i * 45f
        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            drawLine(
                color = TokamakRadiationBright,
                start = Offset(cx, cy - radius * 0.6f),
                end = Offset(cx, cy - radius * 0.9f),
                strokeWidth = 2.5f * scale
            )
        }
    }

    val knobR = radius * 0.42f
    drawCircle(color = Color(0xFF030D05), radius = knobR, center = Offset(cx, cy))
    drawCircle(color = TokamakRadiationBright, radius = knobR, center = Offset(cx, cy), style = Stroke(width = 2.5f * scale))
    drawCircle(color = Color.White, radius = knobR * 0.3f, center = Offset(cx, cy))
}

// ============================================================================
// RENDERIZAÇÃO: 6. CYBER PONG (RETRÔ CRT MINIMALISTA)
// ============================================================================

fun DrawScope.drawCyberPongTable(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean,
    isConnectedGuest: Boolean
) {
    val right = left + width
    val bottom = top + height
    val centerY = top + height / 2f
    val isExtended = isExtendedHost || isExtendedGuest

    // Fundo preto absoluto de tubo CRT
    drawRect(
        color = PongDark,
        topLeft = Offset(left, top),
        size = Size(width, height)
    )

    // Scanlines sutis de fósforo
    val scanStep = 10f * scale
    var sy = top
    while (sy < bottom) {
        drawLine(
            color = Color(0x0A39FF14),
            start = Offset(left, sy),
            end = Offset(right, sy),
            strokeWidth = 1f
        )
        sy += scanStep
    }

    // Linha tracejada clássica do Pong (no vão do topo quando Estendida, ou no centro em Mesa Completa)
    val dividerY = if (isExtended) top else centerY
    val dashH = 20f * scale
    val dashGap = 16f * scale
    var dx = left + 10f * scale
    while (dx < right - 10f * scale) {
        drawRect(
            color = PongGreen.copy(alpha = 0.8f),
            topLeft = Offset(dx, dividerY - 2.5f * scale),
            size = Size(18f * scale, 5f * scale)
        )
        dx += dashH + dashGap
    }

    // Borda fosfórica limpa de vetor (em U aberta no topo quando Estendida)
    if (isExtended) {
        val openBorder = Path().apply {
            moveTo(left, top)
            lineTo(left, bottom)
            lineTo(right, bottom)
            lineTo(right, top)
        }
        drawPath(
            path = openBorder,
            color = PongGreen,
            style = Stroke(width = 4f * scale, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )
    } else {
        drawRect(
            color = PongGreen,
            topLeft = Offset(left, top),
            size = Size(width, height),
            style = Stroke(width = 4f * scale)
        )
    }
}

fun DrawScope.drawPongGoal(
    goalLeft: Float,
    goalWidth: Float,
    goalY: Float,
    scale: Float,
    color: Color,
    isTop: Boolean
) {
    // Barra de barreira clássica do Pong
    drawRect(
        color = color,
        topLeft = Offset(goalLeft, if (isTop) goalY else goalY - 8f * scale),
        size = Size(goalWidth, 8f * scale)
    )
}

fun DrawScope.drawPongPuck(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    scale: Float
) {
    // O clássico quadrado de luz analógico do Pong
    val size = radius * 1.6f
    drawRect(
        color = PongGreen.copy(alpha = 0.3f),
        topLeft = Offset(cx - size * 0.65f, cy - size * 0.65f),
        size = Size(size * 1.3f, size * 1.3f)
    )
    drawRect(
        color = Color.White,
        topLeft = Offset(cx - size / 2f, cy - size / 2f),
        size = Size(size, size)
    )
}

fun DrawScope.drawPongMallet(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    rimColor: Color,
    scale: Float
) {
    // Manopla circular com anel segmentado de fósforo
    drawCircle(color = Color(0x66000000), radius = radius * 1.1f, center = Offset(cx, cy + radius * 0.18f))
    drawCircle(
        color = PongGreen.copy(alpha = 0.25f),
        radius = radius * 1.5f,
        center = Offset(cx, cy)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(PongGreenBright, PongGreen, PongDark),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    drawCircle(color = PongGreen, radius = radius, center = Offset(cx, cy), style = Stroke(width = 3f * scale))

    val knobR = radius * 0.42f
    drawCircle(color = PongDark, radius = knobR, center = Offset(cx, cy))
    drawCircle(color = Color.White, radius = knobR * 0.4f, center = Offset(cx, cy))
}

// ============================================================================
// RENDERIZAÇÃO: 7. COLISEU CIBERNÉTICO (GLADIADOR / SCUTUM)
// ============================================================================

fun DrawScope.drawColosseumTable(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean,
    isConnectedGuest: Boolean
) {
    val right = left + width
    val bottom = top + height
    val chamfer = 40f * scale
    val centerY = top + height / 2f
    val centerX = left + width / 2f
    val isExtended = isExtendedHost || isExtendedGuest

    val arenaPath = buildChamferedArenaPath(left, top, right, bottom, chamfer, isExtended = isExtended, closePath = true)

    drawPath(
        path = arenaPath,
        brush = Brush.verticalGradient(
            colors = listOf(ColosseumRink, ColosseumDark, ColosseumRink),
            startY = top,
            endY = bottom
        )
    )

    // Meandro Grego / Borda Geométrica Romana ao longo das laterais
    val meanderH = 24f * scale
    var my = top + 60f * scale
    while (my < bottom - 60f * scale) {
        drawRect(
            color = ColosseumGold.copy(alpha = 0.25f),
            topLeft = Offset(left + 15f * scale, my),
            size = Size(10f * scale, 12f * scale),
            style = Stroke(width = 1.5f * scale)
        )
        drawRect(
            color = ColosseumGold.copy(alpha = 0.25f),
            topLeft = Offset(right - 25f * scale, my),
            size = Size(10f * scale, 12f * scale),
            style = Stroke(width = 1.5f * scale)
        )
        my += meanderH
    }

    if (!isExtended) {
        // Coroa de Louros Imperial no círculo central
        drawCircle(
            color = ColosseumGold,
            radius = 70f * scale,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.5f * scale)
        )
        for (i in 0 until 12) {
            val angle = i * 30f
            rotate(degrees = angle, pivot = Offset(centerX, centerY)) {
                drawOval(
                    color = ColosseumGold.copy(alpha = 0.5f),
                    topLeft = Offset(centerX - 8f * scale, centerY - 70f * scale),
                    size = Size(16f * scale, 10f * scale)
                )
            }
        }
        drawCircle(color = ColosseumCrimson, radius = 14f * scale, center = Offset(centerX, centerY))
    }

    drawChamferedExtendedOrFullBorder(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        chamfer = chamfer,
        borderColor = ColosseumGold,
        strokeWidth = 4.5f * scale,
        scale = scale,
        isExtended = isExtended
    )
}

fun DrawScope.drawColosseumGoal(
    goalLeft: Float,
    goalWidth: Float,
    goalY: Float,
    scale: Float,
    color: Color,
    isTop: Boolean
) {
    val h = 30f * scale
    val yBase = if (isTop) goalY else goalY - h
    val yTip = if (isTop) goalY + h else goalY

    // Pórtico de Colunas Romanas
    drawRect(
        color = color.copy(alpha = 0.2f),
        topLeft = Offset(goalLeft, yBase),
        size = Size(goalWidth, h)
    )

    // Pilares esquerdo e direito
    drawRect(color = color, topLeft = Offset(goalLeft, yBase), size = Size(12f * scale, h))
    drawRect(color = color, topLeft = Offset(goalLeft + goalWidth - 12f * scale, yBase), size = Size(12f * scale, h))

    // Frontão superior
    drawLine(
        color = ColosseumGold,
        start = Offset(goalLeft, yTip),
        end = Offset(goalLeft + goalWidth, yTip),
        strokeWidth = 3f * scale
    )
}

fun DrawScope.drawGladiatorShieldPuck(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    scale: Float
) {
    drawCircle(color = Color(0x66000000), radius = radius * 1.05f, center = Offset(cx, cy + radius * 0.15f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ColosseumGold.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.6f
        ),
        radius = radius * 1.6f,
        center = Offset(cx, cy)
    )

    // Escudo Scutum / Clipeus de bronze e couro carmesim
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ColosseumCrimson, Color(0xFF800000), Color(0xFF2B0000)),
            center = Offset(cx - radius * 0.2f, cy - radius * 0.2f),
            radius = radius * 1.05f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // Umbo central de ouro reforçado
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, ColosseumGold, ColosseumBronze),
            center = Offset(cx - radius * 0.1f, cy - radius * 0.1f),
            radius = radius * 0.5f
        ),
        radius = radius * 0.38f,
        center = Offset(cx, cy)
    )

    // 8 cravos / rebites no perímetro
    for (i in 0 until 8) {
        val angle = i * 45f * (Math.PI / 180f).toFloat()
        val rx = cx + cos(angle) * radius * 0.78f
        val ry = cy + sin(angle) * radius * 0.78f
        drawCircle(color = ColosseumGold, radius = 2.5f * scale, center = Offset(rx, ry))
    }

    drawCircle(color = ColosseumGold, radius = radius, center = Offset(cx, cy), style = Stroke(width = 3.5f * scale))
}

fun DrawScope.drawSpartanAegisMallet(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    rimColor: Color,
    scale: Float
) {
    drawCircle(color = Color(0x66000000), radius = radius * 1.1f, center = Offset(cx, cy + radius * 0.18f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ColosseumGold.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.6f
        ),
        radius = radius * 1.6f,
        center = Offset(cx, cy)
    )

    // Base em bronze e ouro espartano
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(rimColor, accentColor, Color(0xFF261808)),
            center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // Lâminas perimetrais defensivas
    for (i in 0 until 6) {
        val angle = i * 60f
        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            drawLine(
                color = ColosseumGoldBright,
                start = Offset(cx, cy - radius * 0.75f),
                end = Offset(cx, cy - radius * 1.05f),
                strokeWidth = 3f * scale,
                cap = StrokeCap.Round
            )
        }
    }

    val knobR = radius * 0.42f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, ColosseumCrimson, Color(0xFF1E0404)),
            center = Offset(cx - knobR * 0.2f, cy - knobR * 0.2f),
            radius = knobR * 1.1f
        ),
        radius = knobR,
        center = Offset(cx, cy)
    )
    drawCircle(color = ColosseumGold, radius = knobR, center = Offset(cx, cy), style = Stroke(width = 2.5f * scale))
}
