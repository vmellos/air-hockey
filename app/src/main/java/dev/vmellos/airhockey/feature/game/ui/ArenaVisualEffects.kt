package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.ui.geometry.CornerRadius
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
import dev.vmellos.airhockey.core.preferences.VisualEffectsProfile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Cores Oficiais Neon Arcade
val NeonCyan = Color(0xFF00E5FF)
val NeonCyanBright = Color(0xFF80D8FF)
val NeonCyanCore = Color(0xFFE0F7FA)

val NeonOrange = Color(0xFFFF5722)
val NeonOrangeBright = Color(0xFFFF9E80)
val NeonOrangeCore = Color(0xFFFFF3E0)

val TableBgDark = Color(0xFF070B14)
val TableRinkDark = Color(0xFF0B1120)
val TableGridLine = Color(0x1200E5FF)
val GoldSpark = Color(0xFFFFD54F)
val WhiteHot = Color(0xFFFFFFFF)

// Cores Oficiais Cyber Futebol
val CyberTurfDark = Color(0xFF030D07)
val CyberTurfRink = Color(0xFF061A0E)
val CyberTurfGrid = Color(0x1800E676)

val NeonEmerald = Color(0xFF00E676)
val NeonEmeraldBright = Color(0xFF69F0AE)
val NeonEmeraldCore = Color(0xFFE8F5E9)

val CyberSoccerGold = Color(0xFFFFD54F)
val CyberSoccerGoldBright = Color(0xFFFFF59D)

val NeonRuby = Color(0xFFFF1744)
val NeonRubyBright = Color(0xFFFF8A80)
val NeonRubyCore = Color(0xFFFFEBEE)

// Cores Oficiais Neo-Tokyo Basquete
val CourtDark = Color(0xFF100A05)
val CourtRink = Color(0xFF1F1208)
val CourtWoodSlats = Color(0x14FF9100)

val NeonAmber = Color(0xFFFF9100)
val NeonAmberBright = Color(0xFFFFB74D)
val NeonAmberCore = Color(0xFFFFF8E1)

val BasketballLine = Color(0xFFFFAB00)
val BasketballKeyGlow = Color(0x22FF6D00)

// Cores Oficiais Vôlei Gravitacional (Zero-G Volley)
val VolleySpaceDark = Color(0xFF070817)
val VolleySpaceRink = Color(0xFF100E2C)
val VolleyGridLine = Color(0x187C4DFF)

val NeonViolet = Color(0xFF7C4DFF)
val NeonVioletBright = Color(0xFFB388FF)
val NeonVioletCore = Color(0xFFEDE7F6)

val NeonMagenta = Color(0xFFFF007F)
val NeonMagentaBright = Color(0xFFFF4081)
val NeonMagentaCore = Color(0xFFFCE4EC)

val VolleyYellow = Color(0xFFFFEA00)
val VolleyBlue = Color(0xFF2979FF)

/**
 * Calcula o fator de amortecimento exponencial independente da taxa de quadros
 * equivalente a [dampingPer60HzFrame] aplicado a 60 FPS.
 */
internal fun frameRateIndependentDamping(dtSec: Float, dampingPer60HzFrame: Float): Float {
    val clampedDt = dtSec.coerceIn(0.001f, 0.10f)
    return Math.pow(dampingPer60HzFrame.toDouble(), (clampedDt * 60f).toDouble()).toFloat()
}

class SparkParticle {
    var x: Float = 0f
    var y: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var life: Float = 0f
    var maxLife: Float = 0.8f
    var color: Color = GoldSpark
    var size: Float = 3f
    var active: Boolean = false

    fun init(startX: Float, startY: Float, velX: Float, velY: Float, sparkColor: Color, pSize: Float, duration: Float) {
        x = startX
        y = startY
        vx = velX
        vy = velY
        color = sparkColor
        size = pSize
        life = 0f
        maxLife = duration
        active = true
    }

    fun update(dt: Float) {
        if (!active) return
        x += vx * dt
        y += vy * dt
        val damping = frameRateIndependentDamping(dt, 0.94f)
        vx *= damping
        vy *= damping
        life += dt
        if (life >= maxLife) {
            active = false
        }
    }
}

class VisualShockwave {
    var x: Float = 0f
    var y: Float = 0f
    var color: Color = NeonCyan
    var maxRadius: Float = 160f
    var progress: Float = 0f
    var durationSec: Float = 0.42f
    var active: Boolean = false
    var isRadiating: Boolean = false

    fun init(startX: Float, startY: Float, waveColor: Color, radius: Float, radiating: Boolean = false) {
        x = startX
        y = startY
        color = waveColor
        maxRadius = radius
        progress = 0f
        durationSec = if (radiating) 0.70f else 0.42f
        active = true
        isRadiating = radiating
    }

    fun update(dt: Float) {
        if (!active) return
        val clampedDt = dt.coerceIn(0.001f, 0.10f)
        progress += clampedDt / durationSec
        if (progress >= 1f) {
            active = false
        }
    }
}

class ParticlePool(
    private val maxSparks: Int = 50,
    private val maxShockwaves: Int = 8
) {
    private val sparks = Array(maxSparks) { SparkParticle() }
    private val shockwaves = Array(maxShockwaves) { VisualShockwave() }
    private val rng = Random(42)

    fun spawnSparks(
        x: Float,
        y: Float,
        count: Int,
        baseColor: Color,
        spreadSpeed: Float = 600f,
        duration: Float = 0.7f
    ) {
        var spawned = 0
        for (p in sparks) {
            if (!p.active) {
                val angle = rng.nextFloat() * 2f * PI.toFloat()
                val speed = (0.3f + rng.nextFloat() * 0.7f) * spreadSpeed
                val vx = cos(angle) * speed
                val vy = sin(angle) * speed
                val size = 2.5f + rng.nextFloat() * 3.5f
                val color = if (rng.nextFloat() > 0.4f) baseColor else GoldSpark
                p.init(x, y, vx, vy, color, size, duration * (0.6f + rng.nextFloat() * 0.5f))
                spawned++
                if (spawned >= count) break
            }
        }
    }

    fun spawnGoalBurst(goalCenterX: Float, goalY: Float, isTop: Boolean, color: Color) {
        // Dispara onda de choque radial expansiva com raios irradiantes (estilo arcade neon)
        spawnShockwave(goalCenterX, goalY, color, maxRadius = 520f, radiating = true)

        // Dispara leque de faíscas incandescentes em direção ao interior da arena
        var spawned = 0
        val targetAngleBase = if (isTop) (PI / 2.0).toFloat() else (-PI / 2.0).toFloat()
        for (p in sparks) {
            if (!p.active) {
                val angleSpread = (rng.nextFloat() - 0.5f) * (PI * 0.75f).toFloat()
                val angle = targetAngleBase + angleSpread
                val speed = 400f + rng.nextFloat() * 750f
                val vx = cos(angle) * speed
                val vy = sin(angle) * speed
                val size = 3f + rng.nextFloat() * 4.5f
                val sparkColor = if (rng.nextBoolean()) color else GoldSpark
                p.init(goalCenterX, goalY, vx, vy, sparkColor, size, duration = 0.95f + rng.nextFloat() * 0.4f)
                spawned++
                if (spawned >= 32) break
            }
        }
    }

    fun spawnShockwave(x: Float, y: Float, color: Color, maxRadius: Float = 160f, radiating: Boolean = false) {
        for (sw in shockwaves) {
            if (!sw.active) {
                sw.init(x, y, color, maxRadius, radiating)
                break
            }
        }
    }

    fun update(dt: Float) {
        for (p in sparks) {
            if (p.active) p.update(dt)
        }
        for (sw in shockwaves) {
            if (sw.active) sw.update(dt)
        }
    }

    fun drawShockwaves(drawScope: DrawScope, scale: Float, timeSec: Float = 0f) {
        for (sw in shockwaves) {
            if (!sw.active) continue
            val currentRadius = sw.maxRadius * scale * sw.progress
            val currentAlpha = (1f - sw.progress).coerceIn(0f, 1f)

            if (sw.isRadiating) {
                drawScope.drawRadiatingEnergyWave(
                    centerX = sw.x,
                    centerY = sw.y,
                    radius = currentRadius,
                    color = sw.color,
                    timeSec = timeSec,
                    rayCount = 48,
                    scale = scale,
                    alpha = currentAlpha
                )
            } else {
                drawScope.drawCircle(
                    color = sw.color.copy(alpha = currentAlpha * 0.75f),
                    radius = currentRadius,
                    center = Offset(sw.x, sw.y),
                    style = Stroke(width = 4.5f * currentAlpha * scale)
                )
            }
        }
    }

    fun drawSparks(drawScope: DrawScope, scale: Float) {
        for (p in sparks) {
            if (!p.active) continue
            val alpha = (1f - (p.life / p.maxLife)).coerceIn(0f, 1f)
            val currentSize = p.size * scale * (0.5f + 0.5f * alpha)
            drawScope.drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = currentSize,
                center = Offset(p.x, p.y)
            )
            // Núcleo branco incandescente para partículas grandes
            if (currentSize > 3f) {
                drawScope.drawCircle(
                    color = WhiteHot.copy(alpha = alpha * 0.85f),
                    radius = currentSize * 0.45f,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}

/**
 * Desenha a arena da mesa com Bordas Chanfradas Neon em 45 graus, malha cibernética e demarcação tática.
 */
fun DrawScope.drawChamferedTable(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean,
    isConnectedGuest: Boolean
) {
    val chamfer = 48f * scale
    val right = left + width
    val bottom = top + height
    val centerY = top + height / 2f

    // 1. Fundo do campo escuro profundo
    val tablePath = Path().apply {
        moveTo(left + chamfer, top)
        lineTo(right - chamfer, top)
        lineTo(right, top + chamfer)
        lineTo(right, bottom - chamfer)
        lineTo(right - chamfer, bottom)
        lineTo(left + chamfer, bottom)
        lineTo(left, bottom - chamfer)
        lineTo(left, top + chamfer)
        close()
    }
    drawPath(path = tablePath, color = TableRinkDark)

    // 2. Sutil grade cibernética translúcida (cyber grid)
    val gridStep = 70f * scale
    var gx = left + gridStep
    while (gx < right) {
        drawLine(
            color = TableGridLine,
            start = Offset(gx, top),
            end = Offset(gx, bottom),
            strokeWidth = 1f
        )
        gx += gridStep
    }
    var gy = top + gridStep
    while (gy < bottom) {
        drawLine(
            color = TableGridLine,
            start = Offset(left, gy),
            end = Offset(right, gy),
            strokeWidth = 1f
        )
        gy += gridStep
    }

    // 3. Bordas Chanfradas Neon em 45 graus
    val isSinglePlayerHalfOrange = !isExtendedHost && !isExtendedGuest
    val topHalfColor = if (isConnectedGuest || isExtendedHost) NeonCyan else NeonOrange
    val bottomHalfColor = if (isExtendedGuest || isConnectedGuest) NeonOrange else NeonCyan

    if (isExtendedHost || isExtendedGuest) {
        // Mesa Estendida: Topo aberto com linha tracejada de passagem
        val singleColor = if (isExtendedGuest) NeonOrange else NeonCyan
        val singleBorder = Path().apply {
            moveTo(left, top)
            lineTo(left, bottom - chamfer)
            lineTo(left + chamfer, bottom)
            lineTo(right - chamfer, bottom)
            lineTo(right, bottom - chamfer)
            lineTo(right, top)
        }
        // Glow sutil da borda
        drawPath(path = singleBorder, color = singleColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        // Linha neon principal
        drawPath(path = singleBorder, color = singleColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Linha tracejada no vão do topo
        drawLine(
            color = Color(0x99FFFFFF),
            start = Offset(left, top),
            end = Offset(right, top),
            strokeWidth = 3f * scale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * scale, 18f * scale), 0f)
        )

        // Semicírculo central neon
        val circleRadius = 130f * scale
        drawArc(
            color = singleColor.copy(alpha = 0.85f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(left + width / 2f - circleRadius, top - circleRadius),
            size = Size(circleRadius * 2f, circleRadius * 2f),
            style = Stroke(width = 4f * scale)
        )
    } else {
        // Modo Solo ou Tela Dividida: Metade Superior Laranja e Metade Inferior Ciano
        // Metade Superior (Laranja)
        val topHalfPath = Path().apply {
            moveTo(left, centerY)
            lineTo(left, top + chamfer)
            lineTo(left + chamfer, top)
            lineTo(right - chamfer, top)
            lineTo(right, top + chamfer)
            lineTo(right, centerY)
        }
        drawPath(path = topHalfPath, color = topHalfColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        drawPath(path = topHalfPath, color = topHalfColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Metade Inferior (Ciano)
        val bottomHalfPath = Path().apply {
            moveTo(left, centerY)
            lineTo(left, bottom - chamfer)
            lineTo(left + chamfer, bottom)
            lineTo(right - chamfer, bottom)
            lineTo(right, bottom - chamfer)
            lineTo(right, centerY)
        }
        drawPath(path = bottomHalfPath, color = bottomHalfColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        drawPath(path = bottomHalfPath, color = bottomHalfColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Linha central com círculo central neon
        val centerRadius = 125f * scale
        val centerX = left + width / 2f

        // Linha divisória com quebra para o círculo
        drawLine(
            color = Color(0x88FFFFFF),
            start = Offset(left, centerY),
            end = Offset(centerX - centerRadius, centerY),
            strokeWidth = 3.5f * scale
        )
        drawLine(
            color = Color(0x88FFFFFF),
            start = Offset(centerX + centerRadius, centerY),
            end = Offset(right, centerY),
            strokeWidth = 3.5f * scale
        )

        // Círculo central com brilho
        drawCircle(
            color = Color(0x22FFFFFF),
            radius = centerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 8f * scale)
        )
        drawCircle(
            color = Color(0xAAFFFFFF),
            radius = centerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3.5f * scale)
        )
        drawCircle(
            color = Color(0xEEFFFFFF),
            radius = 7f * scale,
            center = Offset(centerX, centerY)
        )
    }

    // 4. Círculos de Face-off Táticos nas duas metades (estilo arcade profissional)
    drawTacticalCircles(left, top, width, height, scale, isExtendedHost, isExtendedGuest)
}

private fun DrawScope.drawTacticalCircles(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean
) {
    val r = 55f * scale
    val xOffset = width * 0.28f
    val yOffset = height * 0.22f
    val centerX = left + width / 2f
    val centerY = top + height / 2f

    if (!isExtendedHost) {
        // Círculos Táticos da Metade Superior (Laranja)
        val topY = if (isExtendedGuest) top + yOffset else centerY - yOffset
        val colTop = if (isExtendedGuest) NeonOrange else NeonOrange
        drawFaceoffSpot(centerX - xOffset, topY, r, colTop, scale)
        drawFaceoffSpot(centerX + xOffset, topY, r, colTop, scale)
    }

    if (!isExtendedGuest) {
        // Círculos Táticos da Metade Inferior (Ciano)
        val botY = if (isExtendedHost) top + yOffset else centerY + yOffset
        val colBot = NeonCyan
        drawFaceoffSpot(centerX - xOffset, botY, r, colBot, scale)
        drawFaceoffSpot(centerX + xOffset, botY, r, colBot, scale)
    }
}

private fun DrawScope.drawFaceoffSpot(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    scale: Float
) {
    // Anel externo sutil
    drawCircle(
        color = color.copy(alpha = 0.40f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f * scale)
    )
    // Ponto central
    drawCircle(
        color = color.copy(alpha = 0.85f),
        radius = 5.5f * scale,
        center = Offset(cx, cy)
    )
}

/**
 * Desenha a Meta Trapezoidal Angulada com contorno luminoso e área de perigo demarcada.
 */
fun DrawScope.drawAngledGoal(
    goalLeft: Float,
    goalW: Float,
    baseY: Float,
    scale: Float,
    color: Color,
    isTop: Boolean
) {
    val depth = 38f * scale * (if (isTop) 1f else -1f)
    val chamfer = 24f * scale
    val goalRight = goalLeft + goalW
    val goalCenterX = goalLeft + goalW / 2f

    // 1. Área da Meta Trapezoidal (Goal Crease)
    val creaseDepth = 150f * scale * (if (isTop) 1f else -1f)
    val creaseSpread = 45f * scale
    val creasePath = Path().apply {
        moveTo(goalLeft - creaseSpread + chamfer, baseY)
        lineTo(goalRight + creaseSpread - chamfer, baseY)
        lineTo(goalRight + creaseSpread, baseY + creaseDepth * 0.4f)
        lineTo(goalRight + creaseSpread * 0.5f, baseY + creaseDepth)
        lineTo(goalLeft - creaseSpread * 0.5f, baseY + creaseDepth)
        lineTo(goalLeft - creaseSpread, baseY + creaseDepth * 0.4f)
        close()
    }

    drawPath(path = creasePath, color = color.copy(alpha = 0.08f))
    drawPath(path = creasePath, color = color.copy(alpha = 0.70f), style = Stroke(width = 3f * scale))

    // 2. Nicho interno da Meta (Goal Slot)
    val slotPath = Path().apply {
        moveTo(goalLeft, baseY)
        lineTo(goalLeft + chamfer, baseY - depth)
        lineTo(goalRight - chamfer, baseY - depth)
        lineTo(goalRight, baseY)
        close()
    }
    drawPath(path = slotPath, color = Color(0xFF030712))

    // 3. Linha de Meta Neon Luminosa
    drawLine(
        color = color.copy(alpha = 0.35f),
        start = Offset(goalLeft, baseY),
        end = Offset(goalRight, baseY),
        strokeWidth = 14f * scale,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(goalLeft, baseY),
        end = Offset(goalRight, baseY),
        strokeWidth = 7f * scale,
        cap = StrokeCap.Round
    )
}

/**
 * Desenha o Rebatedor com relevo pseudo-3D, manopla central ergonômica e iluminação arcade.
 */
fun DrawScope.drawPseudo3DMallet(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    rimColor: Color,
    scale: Float
) {
    // 1. Sombra direcional de contato com a mesa
    drawCircle(
        color = Color(0x66000000),
        radius = radius * 1.08f,
        center = Offset(cx, cy + radius * 0.16f)
    )

    // 2. Halo de Brilho Neon Radial
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.45f), accentColor.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.65f
        ),
        radius = radius * 1.65f,
        center = Offset(cx, cy)
    )

    // 3. Base do Rebatedor (Platô exterior)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(rimColor, accentColor, Color(0xFF0F172A)),
            center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // 4. Anel de destaque neon biselado
    drawCircle(
        color = rimColor,
        radius = radius * 0.94f,
        center = Offset(cx, cy),
        style = Stroke(width = radius * 0.12f)
    )

    // 5. Degrau intermediário rebaixado
    drawCircle(
        color = Color(0xFF0F172A),
        radius = radius * 0.62f,
        center = Offset(cx, cy)
    )

    // 6. Manopla Central Pseudo-3D (Pega do jogador com reflexo especular de luz)
    val knobRadius = radius * 0.42f
    val lightOffsetX = cx - knobRadius * 0.28f
    val lightOffsetY = cy - knobRadius * 0.32f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(WhiteHot, rimColor, accentColor, Color(0xFF1E293B)),
            center = Offset(lightOffsetX, lightOffsetY),
            radius = knobRadius * 1.15f
        ),
        radius = knobRadius,
        center = Offset(cx, cy)
    )

    // Contorno fino de precisão na manopla
    drawCircle(
        color = rimColor.copy(alpha = 0.90f),
        radius = knobRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f * scale)
    )
}

/**
 * Desenha o Disco em formato de Anel Toroidal de Energia com centro escuro de alto contraste.
 */
fun DrawScope.drawEnergyRingPuck(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    scale: Float
) {
    // 1. Brilho Neon Volumétrico
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.50f), accentColor.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.8f
        ),
        radius = radius * 1.8f,
        center = Offset(cx, cy)
    )

    // 2. Anel Externo de Energia
    drawCircle(
        color = accentColor,
        radius = radius,
        center = Offset(cx, cy)
    )

    // 3. Borda Incandescente
    drawCircle(
        color = WhiteHot,
        radius = radius * 0.88f,
        center = Offset(cx, cy),
        style = Stroke(width = radius * 0.20f)
    )

    // 4. Centro Escuro Translúcido (Formato Anelar Toroidal)
    drawCircle(
        color = Color(0xFF060913),
        radius = radius * 0.52f,
        center = Offset(cx, cy)
    )

    // 5. Anel interno de contenção de plasma
    drawCircle(
        color = accentColor.copy(alpha = 0.90f),
        radius = radius * 0.52f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.2f * scale)
    )

    // 6. Ponto de energia no núcleo
    drawCircle(
        color = WhiteHot,
        radius = radius * 0.16f,
        center = Offset(cx, cy)
    )
}

/**
 * Desenha o Rastro de Fogo de Plasma Fluido e Ondulado atrás do disco em movimento.
 */
fun DrawScope.drawDynamicEnergyTrail(
    trailPoints: List<Offset>,
    puckRadius: Float,
    color: Color,
    coreColor: Color,
    framePulse: Long,
    profile: VisualEffectsProfile,
    scale: Float
) {
    if (profile == VisualEffectsProfile.OFF || trailPoints.size < 2) return

    val pointCount = trailPoints.size
    val pulseSec = (framePulse / 1_000_000_000.0).toFloat()

    // Desenha fita de plasma conectando os pontos históricos com oscilação ondulada
    val outerPath = Path()
    val corePath = Path()

    for (i in 0 until pointCount - 1) {
        val p1 = trailPoints[i]
        val p2 = trailPoints[i + 1]

        val fraction = (i.toFloat() / pointCount.toFloat())
        val alpha = (1f - fraction).coerceIn(0f, 1f) * 0.70f
        val segRadius = (puckRadius * (1f - fraction * 0.65f))

        // Adiciona ondulação transversal de chama de plasma
        val waveOffset = sin(pulseSec * 18f + i * 1.1f) * (8f * scale * (1f - fraction))

        // Círculo externo de plasma
        drawCircle(
            color = color.copy(alpha = alpha * 0.65f),
            radius = segRadius,
            center = Offset(p1.x + waveOffset, p1.y)
        )

        // Núcleo incandescente de chama
        drawCircle(
            color = coreColor.copy(alpha = alpha * 0.95f),
            radius = segRadius * 0.45f,
            center = Offset(p1.x + waveOffset * 0.5f, p1.y)
        )
    }
}

/**
 * Desenha a Onda de Choque Radial Arcada com feixes densos e filamentos de energia
 * que se expandem e irradiam a partir do centro (idêntico à referência visual do print).
 */
fun DrawScope.drawRadiatingEnergyWave(
    centerX: Float,
    centerY: Float,
    radius: Float,
    color: Color,
    timeSec: Float,
    rayCount: Int = 48,
    scale: Float = 1f,
    alpha: Float = 1f
) {
    if (radius <= 0f || alpha <= 0.01f) return

    val coreColor = if (color == NeonOrange) NeonOrangeCore else NeonCyanCore
    val brightColor = if (color == NeonOrange) NeonOrangeBright else NeonCyanBright

    // 1. Halo volumétrico de energia difusa
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                coreColor.copy(alpha = 0.50f * alpha),
                color.copy(alpha = 0.30f * alpha),
                color.copy(alpha = 0.08f * alpha),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = radius * 1.35f
        ),
        radius = radius * 1.35f,
        center = Offset(centerX, centerY)
    )

    // 2. Anel interno de contenção
    val innerRadius = radius * 0.42f
    drawCircle(
        color = coreColor.copy(alpha = 0.90f * alpha),
        radius = innerRadius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 3.5f * scale)
    )

    // 3. Feixes de plasma densos irradiando do anel interno até a periferia (efeito raio de sol / íris de choque do print)
    val angleStep = (2f * PI / rayCount).toFloat()
    for (i in 0 until rayCount) {
        val angle = i * angleStep + timeSec * 0.35f
        // Variação de comprimento harmônico para gerar textura pulsante de energia
        val lengthMod = 0.72f + 0.28f * sin(i * 3.7f + timeSec * 10f)
        val outerRayRadius = innerRadius + (radius - innerRadius) * lengthMod

        val cosA = cos(angle)
        val sinA = sin(angle)

        val startX = centerX + cosA * innerRadius
        val startY = centerY + sinA * innerRadius
        val endX = centerX + cosA * outerRayRadius
        val endY = centerY + sinA * outerRayRadius

        val rayWidth = if (i % 2 == 0) 2.5f * scale else 1.5f * scale
        val rayAlpha = (0.50f + 0.45f * lengthMod) * alpha

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(coreColor.copy(alpha = rayAlpha), color.copy(alpha = rayAlpha * 0.35f)),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = rayWidth,
            cap = StrokeCap.Round
        )
    }

    // 4. Anel externo perimetral brilhante
    drawCircle(
        color = brightColor.copy(alpha = 0.90f * alpha),
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 4.2f * scale)
    )
}

/**
 * Desenha o Vórtice de Contagem Regressiva central no início e retomada da partida.
 * Composto por anéis concêntricos de plasma neon pulsantes e fluidos, conforme especificado no GLOSSARY.md.
 */
fun DrawScope.drawCountdownVortex(
    centerX: Float,
    centerY: Float,
    countdownNumber: Int,
    framePulse: Long,
    scale: Float
) {
    val timeSec = (framePulse / 1_000_000_000.0).toFloat()
    val vortexColor = if (countdownNumber == 0) Color(0xFF10B981) else NeonCyan
    val coreColor = if (countdownNumber == 0) Color(0xFFA7F3D0) else Color(0xFFE0F7FA)

    val pulseFast = (sin(timeSec * 7f) * 0.5f + 0.5f)
    val pulseSlow = (sin(timeSec * 3.5f) * 0.5f + 0.5f)

    val baseRadius = (130f + pulseFast * 12f) * scale
    val innerRadius = baseRadius * 0.55f
    val outerRadius = baseRadius * 1.25f

    // 1. Halo suave de fundo em plasma neon
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                coreColor.copy(alpha = 0.22f),
                vortexColor.copy(alpha = 0.12f),
                vortexColor.copy(alpha = 0.03f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = outerRadius * 1.2f
        ),
        radius = outerRadius * 1.2f,
        center = Offset(centerX, centerY)
    )

    // 2. Anel interno de contenção pulsante
    drawCircle(
        color = coreColor.copy(alpha = 0.85f),
        radius = innerRadius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2.5f * scale)
    )

    // 3. Anéis concêntricos intermediários com arcos neon rotativos
    val rotationDeg = (timeSec * 45f) % 360f
    val arcRadius = baseRadius * 0.88f
    val arcRect = androidx.compose.ui.geometry.Rect(
        centerX - arcRadius,
        centerY - arcRadius,
        centerX + arcRadius,
        centerY + arcRadius
    )

    for (i in 0 until 4) {
        val startAngle = rotationDeg + i * 90f + 10f
        drawArc(
            color = vortexColor.copy(alpha = 0.75f),
            startAngle = startAngle,
            sweepAngle = 65f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = 3.2f * scale, cap = StrokeCap.Round)
        )
    }

    // 4. Segundo anel orbital com rotação contrária
    val counterRotationDeg = (-timeSec * 30f) % 360f
    val outerArcRadius = outerRadius * 0.95f
    val outerArcRect = androidx.compose.ui.geometry.Rect(
        centerX - outerArcRadius,
        centerY - outerArcRadius,
        centerX + outerArcRadius,
        centerY + outerArcRadius
    )

    for (i in 0 until 3) {
        val startAngle = counterRotationDeg + i * 120f + 15f
        drawArc(
            color = vortexColor.copy(alpha = 0.45f),
            startAngle = startAngle,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = outerArcRect.topLeft,
            size = outerArcRect.size,
            style = Stroke(width = 2.0f * scale, cap = StrokeCap.Round)
        )
    }

    // 5. Anel externo perimetral fluido com expansão pulsante
    drawCircle(
        color = vortexColor.copy(alpha = 0.70f + 0.25f * pulseSlow),
        radius = outerRadius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2.8f * scale)
    )

    // 6. Onda expansiva concêntrica periódica que viaja para fora
    val waveProgress = (timeSec * 1.5f) % 1.0f
    val expandingRadius = innerRadius + (outerRadius * 1.3f - innerRadius) * waveProgress
    val waveAlpha = (1f - waveProgress).coerceIn(0f, 1f) * 0.5f
    drawCircle(
        color = vortexColor.copy(alpha = waveAlpha),
        radius = expandingRadius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 1.8f * scale)
    )
}

/**
 * Desenha a arena no tema Cyber Futebol com gramado sintético cibernético,
 * demarcações de campo completas (círculo central, grande área, pequena área,
 * marca do pênalti e arcos de escanteio) e cantos chanfrados neon.
 */
fun DrawScope.drawCyberSoccerTable(
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

    // 1. Fundo do gramado sintético cibernético escuro com cantos chanfrados
    val arenaPath = Path().apply {
        if (isExtendedHost || isExtendedGuest) {
            moveTo(left, top)
            lineTo(left, bottom - chamfer)
            lineTo(left + chamfer, bottom)
            lineTo(right - chamfer, bottom)
            lineTo(right, bottom - chamfer)
            lineTo(right, top)
            close()
        } else {
            moveTo(left, top + chamfer)
            lineTo(left + chamfer, top)
            lineTo(right - chamfer, top)
            lineTo(right, top + chamfer)
            lineTo(right, bottom - chamfer)
            lineTo(right - chamfer, bottom)
            lineTo(left + chamfer, bottom)
            lineTo(left, bottom - chamfer)
            close()
        }
    }

    // Gradiente do gramado cibernético escuro
    drawPath(
        path = arenaPath,
        brush = Brush.verticalGradient(
            colors = listOf(CyberTurfRink, CyberTurfDark, CyberTurfRink),
            startY = top,
            endY = bottom
        )
    )

    // Faixas de grama táticas horizontais sutis (alternadas)
    val stripeCount = 14
    val stripeH = height / stripeCount
    for (i in 0 until stripeCount step 2) {
        val sTop = top + i * stripeH
        drawRect(
            color = Color(0x0600E676),
            topLeft = Offset(left, sTop),
            size = Size(width, stripeH)
        )
    }

    // 2. Grade cibernética holográfica sobre o gramado
    val gridStep = 42f * scale
    var gx = left + gridStep
    while (gx < right) {
        drawLine(
            color = CyberTurfGrid,
            start = Offset(gx, top),
            end = Offset(gx, bottom),
            strokeWidth = 1f
        )
        gx += gridStep
    }

    // 3. Bordas Chanfradas em Neon Esmeralda / Rubi
    val topColor = if (isConnectedGuest || isExtendedHost) NeonEmerald else NeonRuby
    val bottomColor = if (isExtendedGuest || isConnectedGuest) NeonRuby else NeonEmerald

    if (isExtendedHost || isExtendedGuest) {
        val singleColor = if (isExtendedGuest) NeonRuby else NeonEmerald
        val singleBorder = Path().apply {
            moveTo(left, top)
            lineTo(left, bottom - chamfer)
            lineTo(left + chamfer, bottom)
            lineTo(right - chamfer, bottom)
            lineTo(right, bottom - chamfer)
            lineTo(right, top)
        }
        drawPath(path = singleBorder, color = singleColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        drawPath(path = singleBorder, color = singleColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Linha tracejada do topo (passagem entre aparelhos)
        drawLine(
            color = Color(0x99FFFFFF),
            start = Offset(left, top),
            end = Offset(right, top),
            strokeWidth = 3f * scale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * scale, 18f * scale), 0f)
        )

        // Semicírculo central
        val circleRadius = 130f * scale
        drawArc(
            color = singleColor.copy(alpha = 0.85f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - circleRadius, top - circleRadius),
            size = Size(circleRadius * 2f, circleRadius * 2f),
            style = Stroke(width = 4f * scale)
        )
        drawCircle(color = CyberSoccerGold, radius = 6f * scale, center = Offset(centerX, top))
    } else {
        // Metade Superior (Adversário / P2 - Rubi Neon)
        val topHalfPath = Path().apply {
            moveTo(left, centerY)
            lineTo(left, top + chamfer)
            lineTo(left + chamfer, top)
            lineTo(right - chamfer, top)
            lineTo(right, top + chamfer)
            lineTo(right, centerY)
        }
        drawPath(path = topHalfPath, color = topColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        drawPath(path = topHalfPath, color = topColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Metade Inferior (Jogador 1 - Esmeralda Neon)
        val bottomHalfPath = Path().apply {
            moveTo(left, centerY)
            lineTo(left, bottom - chamfer)
            lineTo(left + chamfer, bottom)
            lineTo(right - chamfer, bottom)
            lineTo(right, bottom - chamfer)
            lineTo(right, centerY)
        }
        drawPath(path = bottomHalfPath, color = bottomColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        drawPath(path = bottomHalfPath, color = bottomColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Linha central divisória de meio-campo
        val centerRadius = 125f * scale
        drawLine(
            color = Color(0x99FFFFFF),
            start = Offset(left, centerY),
            end = Offset(centerX - centerRadius, centerY),
            strokeWidth = 3.5f * scale
        )
        drawLine(
            color = Color(0x99FFFFFF),
            start = Offset(centerX + centerRadius, centerY),
            end = Offset(right, centerY),
            strokeWidth = 3.5f * scale
        )

        // Círculo Central de Futebol com anel neon duplo
        drawCircle(
            color = Color(0x2800E676),
            radius = centerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 8f * scale)
        )
        drawCircle(
            color = Color(0xEEFFFFFF),
            radius = centerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3.5f * scale)
        )
        // Ponto central de saída de bola (dourado troféu)
        drawCircle(
            color = CyberSoccerGold,
            radius = 7.5f * scale,
            center = Offset(centerX, centerY)
        )
    }

    // 4. Demarcações Táticas de Futebol: Grande Área e Pequena Área
    drawSoccerAreas(left, top, width, height, scale, isExtendedHost, isExtendedGuest)

    // 5. Arcos de Escanteio (Corner Arcs)
    drawSoccerCornerArcs(left, top, width, height, scale, chamfer, isExtendedHost, isExtendedGuest)
}

/**
 * Desenha Grande Área, Pequena Área e Marca do Pênalti no tema Cyber Futebol.
 */
private fun DrawScope.drawSoccerAreas(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean
) {
    val centerX = left + width / 2f
    val bottom = top + height

    val penaltyBoxW = width * 0.62f
    val penaltyBoxH = 175f * scale
    val goalBoxW = width * 0.36f
    val goalBoxH = 80f * scale
    val penaltySpotDist = 205f * scale
    val penaltyArcR = 55f * scale

    // Metade Superior (Adversário / P2)
    if (!isExtendedHost) {
        val pLeft = centerX - penaltyBoxW / 2f
        val pRight = centerX + penaltyBoxW / 2f
        val pBottom = top + penaltyBoxH
        val gLeft = centerX - goalBoxW / 2f
        val gRight = centerX + goalBoxW / 2f
        val gBottom = top + goalBoxH
        val spotY = top + penaltySpotDist

        // Grande Área
        val penaltyPathTop = Path().apply {
            moveTo(pLeft, top)
            lineTo(pLeft, pBottom)
            lineTo(pRight, pBottom)
            lineTo(pRight, top)
        }
        drawPath(path = penaltyPathTop, color = NeonRuby.copy(alpha = 0.55f), style = Stroke(width = 2.5f * scale))

        // Meia-lua da Grande Área
        drawArc(
            color = NeonRuby.copy(alpha = 0.65f),
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(centerX - penaltyArcR, spotY - penaltyArcR),
            size = Size(penaltyArcR * 2f, penaltyArcR * 2f),
            style = Stroke(width = 2.5f * scale)
        )

        // Pequena Área
        val goalPathTop = Path().apply {
            moveTo(gLeft, top)
            lineTo(gLeft, gBottom)
            lineTo(gRight, gBottom)
            lineTo(gRight, top)
        }
        drawPath(path = goalPathTop, color = NeonRuby.copy(alpha = 0.40f), style = Stroke(width = 2f * scale))

        // Marca do Pênalti
        drawCircle(color = CyberSoccerGold, radius = 5f * scale, center = Offset(centerX, spotY))
    }

    // Metade Inferior (Jogador 1 / P1)
    if (!isExtendedGuest) {
        val pLeft = centerX - penaltyBoxW / 2f
        val pRight = centerX + penaltyBoxW / 2f
        val pTop = bottom - penaltyBoxH
        val gLeft = centerX - goalBoxW / 2f
        val gRight = centerX + goalBoxW / 2f
        val gTop = bottom - goalBoxH
        val spotY = bottom - penaltySpotDist

        // Grande Área
        val penaltyPathBot = Path().apply {
            moveTo(pLeft, bottom)
            lineTo(pLeft, pTop)
            lineTo(pRight, pTop)
            lineTo(pRight, bottom)
        }
        drawPath(path = penaltyPathBot, color = NeonEmerald.copy(alpha = 0.55f), style = Stroke(width = 2.5f * scale))

        // Meia-lua da Grande Área
        drawArc(
            color = NeonEmerald.copy(alpha = 0.65f),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(centerX - penaltyArcR, spotY - penaltyArcR),
            size = Size(penaltyArcR * 2f, penaltyArcR * 2f),
            style = Stroke(width = 2.5f * scale)
        )

        // Pequena Área
        val goalPathBot = Path().apply {
            moveTo(gLeft, bottom)
            lineTo(gLeft, gTop)
            lineTo(gRight, gTop)
            lineTo(gRight, bottom)
        }
        drawPath(path = goalPathBot, color = NeonEmerald.copy(alpha = 0.40f), style = Stroke(width = 2f * scale))

        // Marca do Pênalti
        drawCircle(color = CyberSoccerGold, radius = 5f * scale, center = Offset(centerX, spotY))
    }
}

/**
 * Desenha arcos de escanteio nos 4 cantos chanfrados da arena.
 */
private fun DrawScope.drawSoccerCornerArcs(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    chamfer: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean
) {
    val arcR = 32f * scale
    val right = left + width
    val bottom = top + height

    if (!isExtendedHost) {
        // Canto Superior Esquerdo
        drawArc(
            color = NeonRuby.copy(alpha = 0.60f),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left + chamfer * 0.4f - arcR, top + chamfer * 0.4f - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(width = 2.2f * scale)
        )
        // Canto Superior Direito
        drawArc(
            color = NeonRuby.copy(alpha = 0.60f),
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(right - chamfer * 0.4f - arcR, top + chamfer * 0.4f - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(width = 2.2f * scale)
        )
    }

    if (!isExtendedGuest) {
        // Canto Inferior Esquerdo
        drawArc(
            color = NeonEmerald.copy(alpha = 0.60f),
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left + chamfer * 0.4f - arcR, bottom - chamfer * 0.4f - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(width = 2.2f * scale)
        )
        // Canto Inferior Direito
        drawArc(
            color = NeonEmerald.copy(alpha = 0.60f),
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(right - chamfer * 0.4f - arcR, bottom - chamfer * 0.4f - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(width = 2.2f * scale)
        )
    }
}

/**
 * Desenha a Meta Trapezoidal com padrão de rede de futebol holográfica.
 */
fun DrawScope.drawCyberSoccerGoal(
    goalLeft: Float,
    goalW: Float,
    baseY: Float,
    scale: Float,
    color: Color,
    isTop: Boolean
) {
    val depth = 38f * scale * (if (isTop) 1f else -1f)
    val chamfer = 24f * scale
    val goalRight = goalLeft + goalW

    // 1. Nicho interno da Meta (Goal Slot)
    val slotPath = Path().apply {
        moveTo(goalLeft, baseY)
        lineTo(goalLeft + chamfer, baseY - depth)
        lineTo(goalRight - chamfer, baseY - depth)
        lineTo(goalRight, baseY)
        close()
    }
    drawPath(path = slotPath, color = Color(0xFF020704))

    // 2. Rede de futebol holográfica (Mesh diagonal de neon)
    val netStep = 18f * scale
    var nx = goalLeft + 10f * scale
    while (nx < goalRight - 10f * scale) {
        drawLine(
            color = color.copy(alpha = 0.22f),
            start = Offset(nx, baseY),
            end = Offset(nx + 14f * scale, baseY - depth),
            strokeWidth = 1.2f * scale
        )
        drawLine(
            color = color.copy(alpha = 0.22f),
            start = Offset(nx, baseY - depth),
            end = Offset(nx + 14f * scale, baseY),
            strokeWidth = 1.2f * scale
        )
        nx += netStep
    }

    // 3. Linha de Meta Neon Luminosa
    drawLine(
        color = color.copy(alpha = 0.35f),
        start = Offset(goalLeft, baseY),
        end = Offset(goalRight, baseY),
        strokeWidth = 14f * scale,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(goalLeft, baseY),
        end = Offset(goalRight, baseY),
        strokeWidth = 7f * scale,
        cap = StrokeCap.Round
    )
    // Traves laterais brilhantes
    drawCircle(color = WhiteHot, radius = 5f * scale, center = Offset(goalLeft, baseY))
    drawCircle(color = WhiteHot, radius = 5f * scale, center = Offset(goalRight, baseY))
}

/**
 * Desenha o Disco com a Aparência de Esfera Cyber Futebol:
 * Gomos pentagonais e hexagonais estilizados com costuras de neon e núcleo de alta energia.
 */
fun DrawScope.drawCyberSoccerPuck(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    scale: Float
) {
    // 1. Halo de Brilho Neon Volumétrico
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.50f), accentColor.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.8f
        ),
        radius = radius * 1.8f,
        center = Offset(cx, cy)
    )

    // 2. Base da Esfera Cyber Futebol (Metálica escura com gradiente esférico)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF2A3D32), Color(0xFF101C15), Color(0xFF060B08)),
            center = Offset(cx - radius * 0.20f, cy - radius * 0.20f),
            radius = radius * 1.05f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // 3. Anel perimetral de contenção magnética
    drawCircle(
        color = accentColor,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 3.5f * scale)
    )

    // 4. Desenha gomos de futebol cibernéticos (Pentágono central e linhas radiais para hexágonos)
    val pentagonRadius = radius * 0.40f
    val pentagonPath = Path().apply {
        for (i in 0 until 5) {
            val angle = (i * 72f - 90f) * (PI.toFloat() / 180f)
            val px = cx + pentagonRadius * cos(angle)
            val py = cy + pentagonRadius * sin(angle)
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(path = pentagonPath, color = Color(0xFF08140D))
    drawPath(path = pentagonPath, color = accentColor, style = Stroke(width = 2.2f * scale))

    // Linhas radiais dos vértices do pentágono até a borda
    for (i in 0 until 5) {
        val angle = (i * 72f - 90f) * (PI.toFloat() / 180f)
        val startX = cx + pentagonRadius * cos(angle)
        val startY = cy + pentagonRadius * sin(angle)
        val endX = cx + radius * 0.90f * cos(angle)
        val endY = cy + radius * 0.90f * sin(angle)
        drawLine(
            color = accentColor.copy(alpha = 0.90f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2f * scale
        )
    }

    // 5. Núcleo incandescente de energia no centro da bola
    drawCircle(
        color = WhiteHot,
        radius = radius * 0.14f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = CyberSoccerGold,
        radius = radius * 0.08f,
        center = Offset(cx, cy)
    )
}

/**
 * Desenha o Rebatedor com a Aparência Chute Magnético (Cyber Striker):
 * Manopla pseudo-3D com travas radiais de chuteira futurista e emissor de arco magnético.
 */
fun DrawScope.drawCyberStrikerMallet(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    rimColor: Color,
    scale: Float
) {
    // 1. Sombra direcional de contato com a mesa
    drawCircle(
        color = Color(0x66000000),
        radius = radius * 1.08f,
        center = Offset(cx, cy + radius * 0.16f)
    )

    // 2. Halo de Brilho Neon Radial
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.45f), accentColor.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.65f
        ),
        radius = radius * 1.65f,
        center = Offset(cx, cy)
    )

    // 3. Base do Rebatedor (Platô com acabamento de titânio escuro)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(rimColor, accentColor, Color(0xFF0C1812)),
            center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // 4. Anel dentado de travas magnéticas
    val cleatCount = 8
    val cleatDist = radius * 0.82f
    for (i in 0 until cleatCount) {
        val angle = (i * 360f / cleatCount) * (PI.toFloat() / 180f)
        val kx = cx + cleatDist * cos(angle)
        val ky = cy + cleatDist * sin(angle)
        drawCircle(
            color = rimColor,
            radius = 4.5f * scale,
            center = Offset(kx, ky)
        )
    }

    // 5. Anel biselado intermediário
    drawCircle(
        color = rimColor,
        radius = radius * 0.94f,
        center = Offset(cx, cy),
        style = Stroke(width = radius * 0.10f)
    )
    drawCircle(
        color = Color(0xFF09140D),
        radius = radius * 0.62f,
        center = Offset(cx, cy)
    )

    // 6. Manopla Central Pseudo-3D com núcleo de energia esférico
    val knobRadius = radius * 0.42f
    val lightOffsetX = cx - knobRadius * 0.28f
    val lightOffsetY = cy - knobRadius * 0.32f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(WhiteHot, rimColor, accentColor, Color(0xFF0F261B)),
            center = Offset(lightOffsetX, lightOffsetY),
            radius = knobRadius * 1.15f
        ),
        radius = knobRadius,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = rimColor.copy(alpha = 0.90f),
        radius = knobRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f * scale)
    )

    // Arco de impulso magnético na manopla
    drawCircle(
        color = WhiteHot,
        radius = knobRadius * 0.35f,
        center = Offset(cx, cy)
    )
}

/**
 * Desenha a arena no tema Neo-Tokyo Basquete com parquet de madeira/asfalto sintético,
 * linha de 3 pontos, garrafão, círculo de lance livre e cantos chanfrados neon.
 */
fun DrawScope.drawNeoBasketballTable(
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

    // 1. Fundo da quadra com cantos chanfrados
    val arenaPath = Path().apply {
        if (isExtendedHost || isExtendedGuest) {
            moveTo(left, top)
            lineTo(left, bottom - chamfer)
            lineTo(left + chamfer, bottom)
            lineTo(right - chamfer, bottom)
            lineTo(right, bottom - chamfer)
            lineTo(right, top)
            close()
        } else {
            moveTo(left, top + chamfer)
            lineTo(left + chamfer, top)
            lineTo(right - chamfer, top)
            lineTo(right, top + chamfer)
            lineTo(right, bottom - chamfer)
            lineTo(right - chamfer, bottom)
            lineTo(left + chamfer, bottom)
            lineTo(left, bottom - chamfer)
            close()
        }
    }

    // Gradiente do piso da quadra (madeira escura sintética futurista)
    drawPath(
        path = arenaPath,
        brush = Brush.verticalGradient(
            colors = listOf(CourtRink, CourtDark, CourtRink),
            startY = top,
            endY = bottom
        )
    )

    // Tábuas de parquet estilizadas (linhas verticais sutis de tábuas de basquete)
    val slatStep = 30f * scale
    var sx = left + slatStep
    while (sx < right) {
        drawLine(
            color = CourtWoodSlats,
            start = Offset(sx, top),
            end = Offset(sx, bottom),
            strokeWidth = 1f
        )
        sx += slatStep
    }

    // 2. Bordas Chanfradas Neon (Metade superior: Ciano / Metade inferior: Âmbar)
    val topColor = if (isConnectedGuest || isExtendedHost) NeonAmber else NeonCyan
    val bottomColor = if (isExtendedGuest || isConnectedGuest) NeonCyan else NeonAmber

    if (isExtendedHost || isExtendedGuest) {
        val singleColor = if (isExtendedGuest) NeonCyan else NeonAmber
        val singleBorder = Path().apply {
            moveTo(left, top)
            lineTo(left, bottom - chamfer)
            lineTo(left + chamfer, bottom)
            lineTo(right - chamfer, bottom)
            lineTo(right, bottom - chamfer)
            lineTo(right, top)
        }
        drawPath(path = singleBorder, color = singleColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        drawPath(path = singleBorder, color = singleColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Linha tracejada do topo
        drawLine(
            color = Color(0x99FFFFFF),
            start = Offset(left, top),
            end = Offset(right, top),
            strokeWidth = 3f * scale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * scale, 18f * scale), 0f)
        )

        // Círculo central da quadra
        val circleRadius = 125f * scale
        drawArc(
            color = singleColor.copy(alpha = 0.85f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - circleRadius, top - circleRadius),
            size = Size(circleRadius * 2f, circleRadius * 2f),
            style = Stroke(width = 3.5f * scale)
        )
        drawCircle(color = NeonAmber, radius = 6f * scale, center = Offset(centerX, top))
    } else {
        // Metade Superior (P2 / Adversário - Ciano Neon)
        val topHalfPath = Path().apply {
            moveTo(left, centerY)
            lineTo(left, top + chamfer)
            lineTo(left + chamfer, top)
            lineTo(right - chamfer, top)
            lineTo(right, top + chamfer)
            lineTo(right, centerY)
        }
        drawPath(path = topHalfPath, color = topColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        drawPath(path = topHalfPath, color = topColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Metade Inferior (P1 - Âmbar Neon)
        val bottomHalfPath = Path().apply {
            moveTo(left, centerY)
            lineTo(left, bottom - chamfer)
            lineTo(left + chamfer, bottom)
            lineTo(right - chamfer, bottom)
            lineTo(right, bottom - chamfer)
            lineTo(right, centerY)
        }
        drawPath(path = bottomHalfPath, color = bottomColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        drawPath(path = bottomHalfPath, color = bottomColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Linha central divisória de meio-campo
        val centerRadius = 125f * scale
        val jumpCircleRadius = 50f * scale
        drawLine(
            color = Color(0x99FFFFFF),
            start = Offset(left, centerY),
            end = Offset(centerX - centerRadius, centerY),
            strokeWidth = 3.5f * scale
        )
        drawLine(
            color = Color(0x99FFFFFF),
            start = Offset(centerX + centerRadius, centerY),
            end = Offset(right, centerY),
            strokeWidth = 3.5f * scale
        )

        // Grande Círculo de Meio-Campo (Basquete)
        drawCircle(
            color = Color(0x22FF9100),
            radius = centerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 7f * scale)
        )
        drawCircle(
            color = Color(0xEEFFFFFF),
            radius = centerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3.5f * scale)
        )

        // Círculo interno de Jump Ball
        drawCircle(
            color = BasketballLine,
            radius = jumpCircleRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.5f * scale)
        )
        drawCircle(
            color = NeonAmber,
            radius = 7.5f * scale,
            center = Offset(centerX, centerY)
        )
    }

    // 3. Demarcações Táticas de Basquete: Linhas de 3 Pontos, Garrafão e Lance Livre
    drawBasketballCourtMarkings(left, top, width, height, scale, isExtendedHost, isExtendedGuest)
}

/**
 * Desenha Linhas de 3 pontos, Garrafão e Lance Livre para o tema Neo-Tokyo Basquete.
 */
private fun DrawScope.drawBasketballCourtMarkings(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean
) {
    val centerX = left + width / 2f
    val bottom = top + height

    val keyW = width * 0.44f
    val keyH = 220f * scale
    val ftCircleR = keyW / 2f
    val threePtR = width * 0.42f
    val restrictedArcR = 45f * scale

    // Metade Superior (P2 - Ciano)
    if (!isExtendedHost) {
        val kLeft = centerX - keyW / 2f
        val kRight = centerX + keyW / 2f
        val kBottom = top + keyH

        // Preenchimento brilhante do garrafão
        drawRect(
            color = Color(0x1800E5FF),
            topLeft = Offset(kLeft, top),
            size = Size(keyW, keyH)
        )
        // Borda do Garrafão
        val keyPathTop = Path().apply {
            moveTo(kLeft, top)
            lineTo(kLeft, kBottom)
            lineTo(kRight, kBottom)
            lineTo(kRight, top)
        }
        drawPath(path = keyPathTop, color = NeonCyan.copy(alpha = 0.65f), style = Stroke(width = 2.8f * scale))

        // Círculo de Lance Livre (Metade inferior sólida, metade superior tracejada)
        drawArc(
            color = NeonCyan.copy(alpha = 0.85f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - ftCircleR, kBottom - ftCircleR),
            size = Size(ftCircleR * 2f, ftCircleR * 2f),
            style = Stroke(width = 2.5f * scale)
        )
        drawArc(
            color = NeonCyan.copy(alpha = 0.40f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - ftCircleR, kBottom - ftCircleR),
            size = Size(ftCircleR * 2f, ftCircleR * 2f),
            style = Stroke(width = 2.2f * scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f * scale, 10f * scale), 0f))
        )

        // Arco da Linha de 3 Pontos
        val threePtPathTop = Path().apply {
            val cornerDist = 38f * scale
            moveTo(left + cornerDist, top)
            lineTo(left + cornerDist, top + 90f * scale)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(centerX - threePtR, top - 30f * scale, centerX + threePtR, top - 30f * scale + threePtR * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(left + width - cornerDist, top)
        }
        drawPath(path = threePtPathTop, color = NeonCyan.copy(alpha = 0.50f), style = Stroke(width = 2.5f * scale))

        // Semicírculo de área restrita
        drawArc(
            color = NeonCyan.copy(alpha = 0.60f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - restrictedArcR, top - restrictedArcR * 0.3f),
            size = Size(restrictedArcR * 2f, restrictedArcR * 2f),
            style = Stroke(width = 2.2f * scale)
        )
    }

    // Metade Inferior (P1 - Âmbar)
    if (!isExtendedGuest) {
        val kLeft = centerX - keyW / 2f
        val kRight = centerX + keyW / 2f
        val kTop = bottom - keyH

        // Preenchimento brilhante do garrafão
        drawRect(
            color = Color(0x22FF9100),
            topLeft = Offset(kLeft, kTop),
            size = Size(keyW, keyH)
        )
        // Borda do Garrafão
        val keyPathBot = Path().apply {
            moveTo(kLeft, bottom)
            lineTo(kLeft, kTop)
            lineTo(kRight, kTop)
            lineTo(kRight, bottom)
        }
        drawPath(path = keyPathBot, color = NeonAmber.copy(alpha = 0.65f), style = Stroke(width = 2.8f * scale))

        // Círculo de Lance Livre (Metade superior sólida, metade inferior tracejada)
        drawArc(
            color = NeonAmber.copy(alpha = 0.85f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - ftCircleR, kTop - ftCircleR),
            size = Size(ftCircleR * 2f, ftCircleR * 2f),
            style = Stroke(width = 2.5f * scale)
        )
        drawArc(
            color = NeonAmber.copy(alpha = 0.40f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - ftCircleR, kTop - ftCircleR),
            size = Size(ftCircleR * 2f, ftCircleR * 2f),
            style = Stroke(width = 2.2f * scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f * scale, 10f * scale), 0f))
        )

        // Arco da Linha de 3 Pontos
        val threePtPathBot = Path().apply {
            val cornerDist = 38f * scale
            moveTo(left + cornerDist, bottom)
            lineTo(left + cornerDist, bottom - 90f * scale)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(centerX - threePtR, bottom + 30f * scale - threePtR * 2f, centerX + threePtR, bottom + 30f * scale),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(left + width - cornerDist, bottom)
        }
        drawPath(path = threePtPathBot, color = NeonAmber.copy(alpha = 0.50f), style = Stroke(width = 2.5f * scale))

        // Semicírculo de área restrita
        drawArc(
            color = NeonAmber.copy(alpha = 0.60f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - restrictedArcR, bottom - restrictedArcR * 1.7f),
            size = Size(restrictedArcR * 2f, restrictedArcR * 2f),
            style = Stroke(width = 2.2f * scale)
        )
    }
}

/**
 * Desenha a Meta Trapezoidal no tema Basquete com aro holográfico e rede de energia.
 */
fun DrawScope.drawBasketballGoal(
    goalLeft: Float,
    goalW: Float,
    baseY: Float,
    scale: Float,
    color: Color,
    isTop: Boolean
) {
    val depth = 38f * scale * (if (isTop) 1f else -1f)
    val chamfer = 24f * scale
    val goalRight = goalLeft + goalW
    val goalCenterX = goalLeft + goalW / 2f

    // 1. Nicho interno da Meta
    val slotPath = Path().apply {
        moveTo(goalLeft, baseY)
        lineTo(goalLeft + chamfer, baseY - depth)
        lineTo(goalRight - chamfer, baseY - depth)
        lineTo(goalRight, baseY)
        close()
    }
    drawPath(path = slotPath, color = Color(0xFF0F0A06))

    // 2. Rede holográfica cônica de basquete
    val rimW = goalW * 0.65f
    val rimLeft = goalCenterX - rimW / 2f
    val rimH = 26f * scale * (if (isTop) 1f else -1f)

    // Fios da rede convergindo
    val netStrands = 7
    val strandStep = rimW / (netStrands - 1)
    for (i in 0 until netStrands) {
        val nx = rimLeft + i * strandStep
        val targetX = goalCenterX + (nx - goalCenterX) * 0.45f
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(nx, baseY),
            end = Offset(targetX, baseY - rimH),
            strokeWidth = 1.3f * scale
        )
    }

    // 3. Aro de Basquete Neon (Semicírculo brilhante)
    drawArc(
        color = NeonAmber,
        startAngle = if (isTop) 0f else 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(rimLeft, baseY - 12f * scale),
        size = Size(rimW, 24f * scale),
        style = Stroke(width = 4f * scale)
    )

    // 4. Linha de Meta Neon Luminosa
    drawLine(
        color = color.copy(alpha = 0.35f),
        start = Offset(goalLeft, baseY),
        end = Offset(goalRight, baseY),
        strokeWidth = 14f * scale,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(goalLeft, baseY),
        end = Offset(goalRight, baseY),
        strokeWidth = 7f * scale,
        cap = StrokeCap.Round
    )
    drawCircle(color = WhiteHot, radius = 5f * scale, center = Offset(goalLeft, baseY))
    drawCircle(color = WhiteHot, radius = 5f * scale, center = Offset(goalRight, baseY))
}

/**
 * Desenha o Disco com a Aparência de Bola de Basquete Cibernética (Neo Basketball):
 * Esfera âmbar incandescente com as clássicas linhas caneladas curvas de basquete e núcleo de fusão.
 */
fun DrawScope.drawNeoBasketballPuck(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    scale: Float
) {
    // 1. Halo de Brilho Neon Volumétrico
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.50f), accentColor.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.8f
        ),
        radius = radius * 1.8f,
        center = Offset(cx, cy)
    )

    // 2. Base da Esfera de Basquete com gradiente esférico profundo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF9100), Color(0xFFD84315), Color(0xFF260E04)),
            center = Offset(cx - radius * 0.22f, cy - radius * 0.22f),
            radius = radius * 1.05f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // 3. Anel perimetral de contenção metálica
    drawCircle(
        color = accentColor,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 3.2f * scale)
    )

    // 4. Linhas Características da Bola de Basquete (Seams)
    // Linha horizontal
    drawLine(
        color = Color(0xFF140803),
        start = Offset(cx - radius * 0.95f, cy),
        end = Offset(cx + radius * 0.95f, cy),
        strokeWidth = 3f * scale
    )
    drawLine(
        color = accentColor.copy(alpha = 0.70f),
        start = Offset(cx - radius * 0.95f, cy),
        end = Offset(cx + radius * 0.95f, cy),
        strokeWidth = 1.2f * scale
    )

    // Linha vertical
    drawLine(
        color = Color(0xFF140803),
        start = Offset(cx, cy - radius * 0.95f),
        end = Offset(cx, cy + radius * 0.95f),
        strokeWidth = 3f * scale
    )
    drawLine(
        color = accentColor.copy(alpha = 0.70f),
        start = Offset(cx, cy - radius * 0.95f),
        end = Offset(cx, cy + radius * 0.95f),
        strokeWidth = 1.2f * scale
    )

    // Arcos curvos de basquete laterais
    val arcCurveR = radius * 0.90f
    // Arco esquerdo
    drawArc(
        color = Color(0xFF140803),
        startAngle = 45f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(cx - arcCurveR * 1.35f, cy - arcCurveR),
        size = Size(arcCurveR * 2f, arcCurveR * 2f),
        style = Stroke(width = 2.8f * scale)
    )
    drawArc(
        color = accentColor.copy(alpha = 0.65f),
        startAngle = 45f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(cx - arcCurveR * 1.35f, cy - arcCurveR),
        size = Size(arcCurveR * 2f, arcCurveR * 2f),
        style = Stroke(width = 1.2f * scale)
    )

    // Arco direito
    drawArc(
        color = Color(0xFF140803),
        startAngle = 225f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(cx - arcCurveR * 0.65f, cy - arcCurveR),
        size = Size(arcCurveR * 2f, arcCurveR * 2f),
        style = Stroke(width = 2.8f * scale)
    )
    drawArc(
        color = accentColor.copy(alpha = 0.65f),
        startAngle = 225f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(cx - arcCurveR * 0.65f, cy - arcCurveR),
        size = Size(arcCurveR * 2f, arcCurveR * 2f),
        style = Stroke(width = 1.2f * scale)
    )

    // 5. Núcleo de energia incandescente
    drawCircle(
        color = WhiteHot,
        radius = radius * 0.16f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = NeonAmber,
        radius = radius * 0.09f,
        center = Offset(cx, cy)
    )
}

/**
 * Desenha o Rebatedor com a Aparência Dunk Striker:
 * Inspirado em aros de basquete reforçados com amortecedores hidráulicos e centro texturizado.
 */
fun DrawScope.drawDunkStrikerMallet(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    rimColor: Color,
    scale: Float
) {
    // 1. Sombra direcional
    drawCircle(
        color = Color(0x66000000),
        radius = radius * 1.08f,
        center = Offset(cx, cy + radius * 0.16f)
    )

    // 2. Halo de Brilho Neon Radial
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.45f), accentColor.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.65f
        ),
        radius = radius * 1.65f,
        center = Offset(cx, cy)
    )

    // 3. Base do Rebatedor (Aro de ferro pesado estilo Dunk)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(rimColor, accentColor, Color(0xFF1E1005)),
            center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // 4. Entalhes de amortecedor hidráulico (6 suportes radiais)
    val notchCount = 6
    val notchDist = radius * 0.82f
    for (i in 0 until notchCount) {
        val angle = (i * 360f / notchCount) * (PI.toFloat() / 180f)
        val kx = cx + notchDist * cos(angle)
        val ky = cy + notchDist * sin(angle)
        drawCircle(
            color = rimColor,
            radius = 4f * scale,
            center = Offset(kx, ky)
        )
    }

    // 5. Anel intermediário chanfrado
    drawCircle(
        color = rimColor,
        radius = radius * 0.94f,
        center = Offset(cx, cy),
        style = Stroke(width = radius * 0.10f)
    )
    drawCircle(
        color = Color(0xFF180A03),
        radius = radius * 0.62f,
        center = Offset(cx, cy)
    )

    // 6. Manopla Central com domo de energia e reflexo especular
    val knobRadius = radius * 0.42f
    val lightOffsetX = cx - knobRadius * 0.28f
    val lightOffsetY = cy - knobRadius * 0.32f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(WhiteHot, rimColor, accentColor, Color(0xFF261205)),
            center = Offset(lightOffsetX, lightOffsetY),
            radius = knobRadius * 1.15f
        ),
        radius = knobRadius,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = rimColor.copy(alpha = 0.90f),
        radius = knobRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f * scale)
    )

    // Anel central de choque no centro da manopla
    drawCircle(
        color = WhiteHot,
        radius = knobRadius * 0.32f,
        center = Offset(cx, cy)
    )
}

/**
 * Desenha a arena no tema Vôlei Gravitacional com quadra espacial cósmica,
 * rede de feixe laser central, linha dos 3 metros de ataque e cantos chanfrados neon.
 */
fun DrawScope.drawGravityVolleyTable(
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

    // 1. Fundo da quadra espacial cósmica com cantos chanfrados
    val arenaPath = Path().apply {
        if (isExtendedHost || isExtendedGuest) {
            moveTo(left, top)
            lineTo(left, bottom - chamfer)
            lineTo(left + chamfer, bottom)
            lineTo(right - chamfer, bottom)
            lineTo(right, bottom - chamfer)
            lineTo(right, top)
            close()
        } else {
            moveTo(left, top + chamfer)
            lineTo(left + chamfer, top)
            lineTo(right - chamfer, top)
            lineTo(right, top + chamfer)
            lineTo(right, bottom - chamfer)
            lineTo(right - chamfer, bottom)
            lineTo(left + chamfer, bottom)
            lineTo(left, bottom - chamfer)
            close()
        }
    }

    // Gradiente nebulosa espacial
    drawPath(
        path = arenaPath,
        brush = Brush.verticalGradient(
            colors = listOf(VolleySpaceRink, VolleySpaceDark, VolleySpaceRink),
            startY = top,
            endY = bottom
        )
    )

    // Grade de partículas gravitacionais
    val gridStep = 45f * scale
    var gx = left + gridStep
    while (gx < right) {
        drawLine(
            color = VolleyGridLine,
            start = Offset(gx, top),
            end = Offset(gx, bottom),
            strokeWidth = 1f
        )
        gx += gridStep
    }

    // 2. Bordas Chanfradas Neon (Metade superior: Magenta / Metade inferior: Violeta)
    val topColor = if (isConnectedGuest || isExtendedHost) NeonViolet else NeonMagenta
    val bottomColor = if (isExtendedGuest || isConnectedGuest) NeonMagenta else NeonViolet

    if (isExtendedHost || isExtendedGuest) {
        val singleColor = if (isExtendedGuest) NeonMagenta else NeonViolet
        val singleBorder = Path().apply {
            moveTo(left, top)
            lineTo(left, bottom - chamfer)
            lineTo(left + chamfer, bottom)
            lineTo(right - chamfer, bottom)
            lineTo(right, bottom - chamfer)
            lineTo(right, top)
        }
        drawPath(path = singleBorder, color = singleColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        drawPath(path = singleBorder, color = singleColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Linha tracejada do topo
        drawLine(
            color = Color(0x99FFFFFF),
            start = Offset(left, top),
            end = Offset(right, top),
            strokeWidth = 3f * scale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * scale, 18f * scale), 0f)
        )

        // Círculo central com anéis orbitais
        val circleRadius = 120f * scale
        drawArc(
            color = singleColor.copy(alpha = 0.85f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - circleRadius, top - circleRadius),
            size = Size(circleRadius * 2f, circleRadius * 2f),
            style = Stroke(width = 3.5f * scale)
        )
        drawCircle(color = NeonVioletBright, radius = 6f * scale, center = Offset(centerX, top))
    } else {
        // Metade Superior (P2 - Neon Magenta)
        val topHalfPath = Path().apply {
            moveTo(left, centerY)
            lineTo(left, top + chamfer)
            lineTo(left + chamfer, top)
            lineTo(right - chamfer, top)
            lineTo(right, top + chamfer)
            lineTo(right, centerY)
        }
        drawPath(path = topHalfPath, color = topColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        drawPath(path = topHalfPath, color = topColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Metade Inferior (P1 - Neon Violeta)
        val bottomHalfPath = Path().apply {
            moveTo(left, centerY)
            lineTo(left, bottom - chamfer)
            lineTo(left + chamfer, bottom)
            lineTo(right - chamfer, bottom)
            lineTo(right, bottom - chamfer)
            lineTo(right, centerY)
        }
        drawPath(path = bottomHalfPath, color = bottomColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        drawPath(path = bottomHalfPath, color = bottomColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Rede Laser Central de Vôlei (Linha dupla com antena nos trilhos)
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(left, centerY - 3f * scale),
            end = Offset(right, centerY - 3f * scale),
            strokeWidth = 2f * scale
        )
        drawLine(
            color = WhiteHot,
            start = Offset(left, centerY),
            end = Offset(right, centerY),
            strokeWidth = 3.5f * scale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f * scale, 8f * scale), 0f)
        )
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(left, centerY + 3f * scale),
            end = Offset(right, centerY + 3f * scale),
            strokeWidth = 2f * scale
        )

        // Postes antenas laterais da rede
        drawCircle(color = NeonMagentaBright, radius = 6f * scale, center = Offset(left, centerY))
        drawCircle(color = NeonVioletBright, radius = 6f * scale, center = Offset(right, centerY))
    }

    // 3. Demarcações Táticas de Vôlei: Linha de Ataque dos 3m e Zonas de Saque
    drawVolleyCourtMarkings(left, top, width, height, scale, isExtendedHost, isExtendedGuest)
}

/**
 * Desenha as Linhas de Ataque (3 metros) e Limites de Saque para o Vôlei Gravitacional.
 */
private fun DrawScope.drawVolleyCourtMarkings(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean
) {
    val centerY = top + height / 2f
    val attackDist = height * 0.16f
    val cornerIndent = 32f * scale

    // Linha de Ataque Metade Superior (P2)
    if (!isExtendedHost) {
        val attackY = if (isExtendedGuest) top + attackDist else centerY - attackDist
        drawLine(
            color = NeonMagenta.copy(alpha = 0.55f),
            start = Offset(left + cornerIndent, attackY),
            end = Offset(left + width - cornerIndent, attackY),
            strokeWidth = 2.5f * scale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * scale, 12f * scale), 0f)
        )
    }

    // Linha de Ataque Metade Inferior (P1)
    if (!isExtendedGuest) {
        val attackY = if (isExtendedHost) top + attackDist else centerY + attackDist
        drawLine(
            color = NeonViolet.copy(alpha = 0.55f),
            start = Offset(left + cornerIndent, attackY),
            end = Offset(left + width - cornerIndent, attackY),
            strokeWidth = 2.5f * scale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * scale, 12f * scale), 0f)
        )
    }
}

/**
 * Desenha a Meta Trapezoidal no tema Vôlei Gravitacional com vórtice de ressonância.
 */
fun DrawScope.drawVolleyGoal(
    goalLeft: Float,
    goalW: Float,
    baseY: Float,
    scale: Float,
    color: Color,
    isTop: Boolean
) {
    val depth = 38f * scale * (if (isTop) 1f else -1f)
    val chamfer = 24f * scale
    val goalRight = goalLeft + goalW
    val goalCenterX = goalLeft + goalW / 2f

    val slotPath = Path().apply {
        moveTo(goalLeft, baseY)
        lineTo(goalLeft + chamfer, baseY - depth)
        lineTo(goalRight - chamfer, baseY - depth)
        lineTo(goalRight, baseY)
        close()
    }
    drawPath(path = slotPath, color = Color(0xFF070617))

    // Arcos de ressonância no nicho da meta
    val ringR = goalW * 0.32f
    drawArc(
        color = color.copy(alpha = 0.40f),
        startAngle = if (isTop) 0f else 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(goalCenterX - ringR, baseY - depth * 0.5f - ringR),
        size = Size(ringR * 2f, ringR * 2f),
        style = Stroke(width = 2.5f * scale)
    )

    // Linha de Meta Neon Luminosa
    drawLine(
        color = color.copy(alpha = 0.35f),
        start = Offset(goalLeft, baseY),
        end = Offset(goalRight, baseY),
        strokeWidth = 14f * scale,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(goalLeft, baseY),
        end = Offset(goalRight, baseY),
        strokeWidth = 7f * scale,
        cap = StrokeCap.Round
    )
    drawCircle(color = WhiteHot, radius = 5f * scale, center = Offset(goalLeft, baseY))
    drawCircle(color = WhiteHot, radius = 5f * scale, center = Offset(goalRight, baseY))
}

/**
 * Desenha o Disco com a Aparência de Vôlei Gravitacional (Zero-G Volley):
 * Padrão espiral de 3 cores (amarelo, azul elétrico e branco) com vórtice aerodinâmico Mikasa e núcleo estelar.
 */
fun DrawScope.drawGravityVolleyPuck(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    scale: Float
) {
    // 1. Halo de Brilho Neon Volumétrico
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.50f), accentColor.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.8f
        ),
        radius = radius * 1.8f,
        center = Offset(cx, cy)
    )

    // 2. Base da Esfera com gradiente esférico
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(VolleyBlue, Color(0xFF0D47A1), Color(0xFF060D20)),
            center = Offset(cx - radius * 0.20f, cy - radius * 0.20f),
            radius = radius * 1.05f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // 3. Painéis espirais curvos de vôlei (amarelo e branco sobre azul)
    for (i in 0 until 3) {
        val angleDeg = i * 120f
        rotate(degrees = angleDeg, pivot = Offset(cx, cy)) {
            val panelPath = Path().apply {
                moveTo(cx, cy)
                cubicTo(
                    cx + radius * 0.35f, cy - radius * 0.70f,
                    cx + radius * 0.85f, cy - radius * 0.50f,
                    cx + radius, cy
                )
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(cx - radius, cy - radius, cx + radius, cy + radius),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 60f,
                    forceMoveTo = false
                )
                close()
            }
            val panelColor = if (i % 2 == 0) VolleyYellow else WhiteHot
            drawPath(path = panelPath, color = panelColor.copy(alpha = 0.88f))
            drawPath(path = panelPath, color = Color(0xFF0A1838), style = Stroke(width = 2.2f * scale))
        }
    }

    // 4. Anel perimetral de contenção
    drawCircle(
        color = accentColor,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 3.5f * scale)
    )

    // 5. Núcleo incandescente de energia estelar
    drawCircle(
        color = WhiteHot,
        radius = radius * 0.15f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = NeonVioletBright,
        radius = radius * 0.09f,
        center = Offset(cx, cy)
    )
}

/**
 * Desenha o Rebatedor com a Aparência Spike Emitter:
 * Perfil côncavo de pulso cinético com emissores de arco sônico e núcleo de alta densidade.
 */
fun DrawScope.drawSpikeEmitterMallet(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    rimColor: Color,
    scale: Float
) {
    // 1. Sombra direcional
    drawCircle(
        color = Color(0x66000000),
        radius = radius * 1.08f,
        center = Offset(cx, cy + radius * 0.16f)
    )

    // 2. Halo de Brilho Neon Radial
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.45f), accentColor.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.65f
        ),
        radius = radius * 1.65f,
        center = Offset(cx, cy)
    )

    // 3. Base do Rebatedor (Platô côncavo em titânio violeta)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(rimColor, accentColor, Color(0xFF140826)),
            center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // 4. Arcos emissores de pulso cinético (4 defletores côncavos)
    for (i in 0 until 4) {
        val angle = i * 90f + 45f
        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            drawArc(
                color = rimColor,
                startAngle = -25f,
                sweepAngle = 50f,
                useCenter = false,
                topLeft = Offset(cx - radius * 0.85f, cy - radius * 0.85f),
                size = Size(radius * 1.7f, radius * 1.7f),
                style = Stroke(width = 3.5f * scale, cap = StrokeCap.Round)
            )
        }
    }

    // 5. Anel interno chanfrado
    drawCircle(
        color = Color(0xFF100720),
        radius = radius * 0.62f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = rimColor.copy(alpha = 0.80f),
        radius = radius * 0.62f,
        center = Offset(cx, cy),
        style = Stroke(width = 2f * scale)
    )

    // 6. Manopla Central com núcleo de ressonância sônica
    val knobRadius = radius * 0.42f
    val lightOffsetX = cx - knobRadius * 0.28f
    val lightOffsetY = cy - knobRadius * 0.32f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(WhiteHot, rimColor, accentColor, Color(0xFF1E0A3A)),
            center = Offset(lightOffsetX, lightOffsetY),
            radius = knobRadius * 1.15f
        ),
        radius = knobRadius,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = rimColor.copy(alpha = 0.90f),
        radius = knobRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f * scale)
    )

    // Ponto incandescente de impacto
    drawCircle(
        color = WhiteHot,
        radius = knobRadius * 0.32f,
        center = Offset(cx, cy)
    )
}

// ============================================================================
// ARENA KIARA CAT 2D (TEMA FELINO ARCADE)
// ============================================================================

fun DrawScope.drawKiaraCatTable(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    isExtendedHost: Boolean,
    isExtendedGuest: Boolean,
    isConnectedGuest: Boolean
) {
    val chamfer = 48f * scale
    val right = left + width
    val bottom = top + height
    val centerY = top + height / 2f

    // 1. Fundo do campo escuro profundo violeta
    val tablePath = Path().apply {
        moveTo(left + chamfer, top)
        lineTo(right - chamfer, top)
        lineTo(right, top + chamfer)
        lineTo(right, bottom - chamfer)
        lineTo(right - chamfer, bottom)
        lineTo(left + chamfer, bottom)
        lineTo(left, bottom - chamfer)
        lineTo(left, top + chamfer)
        close()
    }
    drawPath(path = tablePath, color = Color(0xFF0C0816))

    // 2. Grade cibernética neon magenta sutil
    val gridStep = 70f * scale
    var gx = left + gridStep
    while (gx < right) {
        drawLine(
            color = Color(0x16FF1493),
            start = Offset(gx, top),
            end = Offset(gx, bottom),
            strokeWidth = 1f
        )
        gx += gridStep
    }
    var gy = top + gridStep
    while (gy < bottom) {
        drawLine(
            color = Color(0x16FF1493),
            start = Offset(left, gy),
            end = Offset(right, gy),
            strokeWidth = 1f
        )
        gy += gridStep
    }

    // 3. Patinhas sutis no piso da mesa (marcas d'água neon)
    fun drawPawPrint(cx: Float, cy: Float, pawScale: Float, pawColor: Color) {
        drawOval(
            color = pawColor,
            topLeft = Offset(cx - 18f * pawScale, cy - 12f * pawScale),
            size = Size(36f * pawScale, 26f * pawScale)
        )
        val toeOffsets = listOf(
            Offset(-22f * pawScale, -20f * pawScale) to 5.5f * pawScale,
            Offset(-8f * pawScale, -28f * pawScale) to 6f * pawScale,
            Offset(8f * pawScale, -28f * pawScale) to 6f * pawScale,
            Offset(22f * pawScale, -20f * pawScale) to 5.5f * pawScale
        )
        for ((off, rad) in toeOffsets) {
            drawCircle(
                color = pawColor,
                radius = rad,
                center = Offset(cx + off.x, cy + off.y)
            )
        }
    }

    val pawAlphaColor = Color(0x1CFF1493)
    val pawGoldAlpha = Color(0x18FFC107)
    drawPawPrint(left + width * 0.28f, top + height * 0.22f, scale * 1.1f, pawGoldAlpha)
    drawPawPrint(left + width * 0.72f, top + height * 0.32f, scale * 1.0f, pawAlphaColor)
    drawPawPrint(left + width * 0.28f, top + height * 0.68f, scale * 1.0f, pawAlphaColor)
    drawPawPrint(left + width * 0.72f, top + height * 0.78f, scale * 1.1f, pawGoldAlpha)

    // 4. Bordas Chanfradas Neon
    if (isExtendedHost || isExtendedGuest) {
        val singleColor = if (isExtendedGuest) NeonCyan else Color(0xFFFF1493)
        val singleBorder = Path().apply {
            moveTo(left, top)
            lineTo(left, bottom - chamfer)
            lineTo(left + chamfer, bottom)
            lineTo(right - chamfer, bottom)
            lineTo(right, bottom - chamfer)
            lineTo(right, top)
        }
        drawPath(path = singleBorder, color = singleColor.copy(alpha = 0.25f), style = Stroke(width = 12f * scale))
        drawPath(path = singleBorder, color = singleColor, style = Stroke(width = 5.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawLine(
            color = Color(0x99FFFFFF),
            start = Offset(left, top),
            end = Offset(right, top),
            strokeWidth = 3f * scale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * scale, 18f * scale), 0f)
        )

        val circleRadius = 130f * scale
        drawArc(
            color = singleColor.copy(alpha = 0.85f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(left + width / 2f - circleRadius, top - circleRadius),
            size = Size(circleRadius * 2f, circleRadius * 2f),
            style = Stroke(width = 4f * scale)
        )
    } else {
        val topHalfColor = if (isConnectedGuest) Color(0xFFFF1493) else NeonCyan
        val bottomHalfColor = if (isConnectedGuest) NeonCyan else Color(0xFFFF1493)

        // Metade Superior
        val topHalfPath = Path().apply {
            moveTo(left, centerY)
            lineTo(left, top + chamfer)
            lineTo(left + chamfer, top)
            lineTo(right - chamfer, top)
            lineTo(right, top + chamfer)
            lineTo(right, centerY)
        }
        drawPath(path = topHalfPath, color = topHalfColor.copy(alpha = 0.20f), style = Stroke(width = 11f * scale))
        drawPath(path = topHalfPath, color = topHalfColor, style = Stroke(width = 5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Metade Inferior
        val bottomHalfPath = Path().apply {
            moveTo(right, centerY)
            lineTo(right, bottom - chamfer)
            lineTo(right - chamfer, bottom)
            lineTo(left + chamfer, bottom)
            lineTo(left, bottom - chamfer)
            lineTo(left, centerY)
        }
        drawPath(path = bottomHalfPath, color = bottomHalfColor.copy(alpha = 0.20f), style = Stroke(width = 11f * scale))
        drawPath(path = bottomHalfPath, color = bottomHalfColor, style = Stroke(width = 5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Linha divisória central dourada
        drawLine(
            color = Color(0xFFFFC107).copy(alpha = 0.85f),
            start = Offset(left, centerY),
            end = Offset(right, centerY),
            strokeWidth = 3.5f * scale
        )

        // Círculo central com contorno de orelhas de gato
        val circleRadius = 110f * scale
        val cx = left + width / 2f
        drawCircle(
            color = Color(0xFFFFC107).copy(alpha = 0.80f),
            radius = circleRadius,
            center = Offset(cx, centerY),
            style = Stroke(width = 3.5f * scale)
        )

        // Orelhas de gato no topo e na base do círculo central
        val earW = 28f * scale
        val earH = 34f * scale
        val earLeftPath = Path().apply {
            moveTo(cx - circleRadius * 0.65f, centerY - circleRadius * 0.76f)
            lineTo(cx - circleRadius * 0.45f - earW, centerY - circleRadius * 0.76f - earH)
            lineTo(cx - circleRadius * 0.25f, centerY - circleRadius * 0.96f)

            moveTo(cx + circleRadius * 0.25f, centerY - circleRadius * 0.96f)
            lineTo(cx + circleRadius * 0.45f + earW, centerY - circleRadius * 0.76f - earH)
            lineTo(cx + circleRadius * 0.65f, centerY - circleRadius * 0.76f)

            moveTo(cx - circleRadius * 0.65f, centerY + circleRadius * 0.76f)
            lineTo(cx - circleRadius * 0.45f - earW, centerY + circleRadius * 0.76f + earH)
            lineTo(cx - circleRadius * 0.25f, centerY + circleRadius * 0.96f)

            moveTo(cx + circleRadius * 0.25f, centerY + circleRadius * 0.96f)
            lineTo(cx + circleRadius * 0.45f + earW, centerY + circleRadius * 0.76f + earH)
            lineTo(cx + circleRadius * 0.65f, centerY + circleRadius * 0.76f)
        }
        drawPath(path = earLeftPath, color = Color(0xFFFFC107), style = Stroke(width = 3.5f * scale, cap = StrokeCap.Round))
    }
}

fun DrawScope.drawKiaraCatGoal(
    goalLeft: Float,
    goalWidth: Float,
    y: Float,
    scale: Float,
    goalColor: Color,
    isTop: Boolean
) {
    val goalRight = goalLeft + goalWidth
    val depth = 38f * scale * (if (isTop) 1f else -1f)

    val goalPath = Path().apply {
        moveTo(goalLeft, y)
        lineTo(goalLeft + 16f * scale, y + depth)
        lineTo(goalRight - 16f * scale, y + depth)
        lineTo(goalRight, y)
    }

    drawPath(path = goalPath, color = goalColor.copy(alpha = 0.22f), style = Stroke(width = 8f * scale))
    drawPath(path = goalPath, color = goalColor, style = Stroke(width = 4.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))

    val arcHeight = 18f * scale * (if (isTop) 1f else -1f)
    val laserBar = Path().apply {
        moveTo(goalLeft + 25f * scale, y)
        quadraticBezierTo(goalLeft + goalWidth / 2f, y + arcHeight, goalRight - 25f * scale, y)
    }
    drawPath(path = laserBar, color = Color(0xFFFFC107), style = Stroke(width = 3f * scale, cap = StrokeCap.Round))
}

fun DrawScope.drawNekoStriker(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    rimColor: Color,
    scale: Float
) {
    drawCircle(
        color = Color(0xFF0F0B18),
        radius = radius,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = rimColor.copy(alpha = 0.35f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 7f * scale)
    )
    drawCircle(
        color = rimColor,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 3.5f * scale)
    )

    val earH = radius * 0.45f
    val earW = radius * 0.35f
    val earsPath = Path().apply {
        moveTo(cx - radius * 0.65f, cy - radius * 0.4f)
        lineTo(cx - radius * 0.55f, cy - radius - earH * 0.5f)
        lineTo(cx - radius * 0.15f, cy - radius * 0.85f)

        moveTo(cx + radius * 0.15f, cy - radius * 0.85f)
        lineTo(cx + radius * 0.55f, cy - radius - earH * 0.5f)
        lineTo(cx + radius * 0.65f, cy - radius * 0.4f)
    }
    drawPath(path = earsPath, color = rimColor, style = Stroke(width = 3.5f * scale, cap = StrokeCap.Round))

    val knobRadius = radius * 0.52f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(WhiteHot, accentColor, Color(0xFF1E0A3A)),
            center = Offset(cx - knobRadius * 0.2f, cy - knobRadius * 0.2f),
            radius = knobRadius * 1.1f
        ),
        radius = knobRadius,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = rimColor,
        radius = knobRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f * scale)
    )

    drawCircle(
        color = Color(0xFFFFC107),
        radius = knobRadius * 0.28f,
        center = Offset(cx, cy + knobRadius * 0.15f)
    )
    for (ang in listOf(-45.0, -15.0, 15.0, 45.0)) {
        val rad = Math.toRadians(ang)
        val tx = cx + (radius * 0.55f * sin(rad)).toFloat()
        val ty = cy - (radius * 0.55f * cos(rad)).toFloat() + knobRadius * 0.15f
        drawCircle(
            color = Color(0xFFFFC107),
            radius = knobRadius * 0.12f,
            center = Offset(tx, ty)
        )
    }
}

fun DrawScope.drawKiara2DGutterMascot(
    centerX: Float,
    centerY: Float,
    scale: Float
) {
    val r = 26f * scale
    drawCircle(
        color = Color(0xFFFF1493).copy(alpha = 0.20f),
        radius = r * 1.5f,
        center = Offset(centerX, centerY)
    )

    drawCircle(
        color = Color(0xFF140D22),
        radius = r,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = Color(0xFFFF1493),
        radius = r,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2.5f * scale)
    )

    val earH = r * 0.75f
    val ears = Path().apply {
        moveTo(centerX - r * 0.8f, centerY - r * 0.2f)
        lineTo(centerX - r * 0.55f, centerY - r - earH * 0.6f)
        lineTo(centerX - r * 0.15f, centerY - r * 0.85f)

        moveTo(centerX + r * 0.15f, centerY - r * 0.85f)
        lineTo(centerX + r * 0.55f, centerY - r - earH * 0.6f)
        lineTo(centerX + r * 0.8f, centerY - r * 0.2f)
    }
    drawPath(path = ears, color = Color(0xFFFF1493), style = Stroke(width = 2.5f * scale, cap = StrokeCap.Round))

    drawOval(
        color = Color(0xFFFFC107),
        topLeft = Offset(centerX - r * 0.55f, centerY - r * 0.25f),
        size = Size(r * 0.35f, r * 0.5f)
    )
    drawOval(
        color = Color(0xFFFFC107),
        topLeft = Offset(centerX + r * 0.20f, centerY - r * 0.25f),
        size = Size(r * 0.35f, r * 0.5f)
    )

    drawCircle(
        color = Color(0xFF0F0B18),
        radius = r * 0.12f,
        center = Offset(centerX - r * 0.37f, centerY)
    )
    drawCircle(
        color = Color(0xFF0F0B18),
        radius = r * 0.12f,
        center = Offset(centerX + r * 0.37f, centerY)
    )

    drawCircle(
        color = Color(0xFFFF69B4),
        radius = r * 0.10f,
        center = Offset(centerX, centerY + r * 0.25f)
    )
    for (dir in listOf(-1, 1)) {
        for (idx in listOf(-1, 1)) {
            val sx = centerX + dir * r * 0.25f
            val sy = centerY + r * 0.28f + idx * 4f * scale
            val ex = centerX + dir * r * 0.95f
            val ey = sy + idx * 3f * scale
            drawLine(
                color = Color(0xCCFFFFFF),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = 1.2f * scale
            )
        }
    }
}

/**
 * Desenha pegada efêmera da Mascote Kiara 2D no Canvas durante o circuito de patrulha (ADR 0042).
 */
fun DrawScope.drawKiaraPawTrailPrint(
    cx: Float,
    cy: Float,
    scale: Float,
    alpha: Float
) {
    val pawColor = Color(0xFFFF1493).copy(alpha = 0.55f * alpha)
    drawOval(
        color = pawColor,
        topLeft = Offset(cx - 10f * scale, cy - 7f * scale),
        size = Size(20f * scale, 14f * scale)
    )
    val toeOffsets = listOf(
        Offset(-11f * scale, -10f * scale) to 3.0f * scale,
        Offset(-4f * scale, -14f * scale) to 3.5f * scale,
        Offset(4f * scale, -14f * scale) to 3.5f * scale,
        Offset(11f * scale, -10f * scale) to 3.0f * scale
    )
    for ((off, rad) in toeOffsets) {
        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = 0.65f * alpha),
            radius = rad,
            center = Offset(cx + off.x, cy + off.y)
        )
    }
}

fun DrawScope.drawKiaraPawPuck(
    cx: Float,
    cy: Float,
    radius: Float,
    accentColor: Color,
    scale: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.50f), accentColor.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.8f
        ),
        radius = radius * 1.8f,
        center = Offset(cx, cy)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF2A1C35), Color(0xFF140D22), Color(0xFF090510)),
            center = Offset(cx - radius * 0.20f, cy - radius * 0.20f),
            radius = radius * 1.05f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = accentColor,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 3.5f * scale)
    )

    val padRadius = radius * 0.32f
    drawOval(
        color = Color(0xFFFFC107),
        topLeft = Offset(cx - padRadius * 1.1f, cy - padRadius * 0.35f),
        size = Size(padRadius * 2.2f, padRadius * 1.6f)
    )
    for (ang in listOf(-45.0, -15.0, 15.0, 45.0)) {
        val rad = Math.toRadians(ang)
        val tx = cx + (radius * 0.55f * sin(rad)).toFloat()
        val ty = cy - (radius * 0.55f * cos(rad)).toFloat() + padRadius * 0.2f
        drawCircle(
            color = Color(0xFFFFC107),
            radius = padRadius * 0.38f,
            center = Offset(tx, ty)
        )
    }
}

