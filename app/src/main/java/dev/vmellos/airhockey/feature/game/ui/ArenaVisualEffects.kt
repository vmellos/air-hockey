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
        vx *= 0.94f
        vy *= 0.94f
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
    var active: Boolean = false

    fun init(startX: Float, startY: Float, waveColor: Color, radius: Float) {
        x = startX
        y = startY
        color = waveColor
        maxRadius = radius
        progress = 0f
        active = true
    }

    fun update(speed: Float) {
        if (!active) return
        progress += speed
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
        // Dispara onda de choque expansiva
        spawnShockwave(goalCenterX, goalY, color, maxRadius = 380f)

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

    fun spawnShockwave(x: Float, y: Float, color: Color, maxRadius: Float = 160f) {
        for (sw in shockwaves) {
            if (!sw.active) {
                sw.init(x, y, color, maxRadius)
                break
            }
        }
    }

    fun update(dt: Float) {
        for (p in sparks) {
            if (p.active) p.update(dt)
        }
        for (sw in shockwaves) {
            if (sw.active) sw.update(0.040f)
        }
    }

    fun drawShockwaves(drawScope: DrawScope, scale: Float) {
        for (sw in shockwaves) {
            if (!sw.active) continue
            val currentRadius = sw.maxRadius * scale * sw.progress
            val currentAlpha = (1f - sw.progress).coerceIn(0f, 1f) * 0.75f
            drawScope.drawCircle(
                color = sw.color.copy(alpha = currentAlpha),
                radius = currentRadius,
                center = Offset(sw.x, sw.y),
                style = Stroke(width = 4.5f * (1f - sw.progress) * scale)
            )
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
    val topHalfColor = if (isExtendedGuest || isConnectedGuest) NeonOrange else if (isExtendedHost) NeonCyan else NeonOrange
    val bottomHalfColor = if (isExtendedGuest) NeonOrange else NeonCyan

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
    val isBalanced = profile == VisualEffectsProfile.BALANCED
    val outerPath = Path()
    val corePath = Path()

    for (i in 0 until pointCount - 1) {
        val p1 = trailPoints[i]
        val p2 = trailPoints[i + 1]

        val fraction = (i.toFloat() / pointCount.toFloat())
        val alpha = (1f - fraction).coerceIn(0f, 1f) * 0.70f
        val segRadius = (puckRadius * (1f - fraction * 0.65f))

        // Adiciona ondulação transversal de chama de plasma
        val waveOffset = if (!isBalanced) {
            sin(pulseSec * 18f + i * 1.1f) * (8f * scale * (1f - fraction))
        } else {
            0f
        }

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
 * Desenha o Vórtice de Contagem Regressiva central radiante no início e retomada.
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
    val baseRadius = 145f * scale

    // 1. Anéis concêntricos pulsantes
    val pulse = (sin(timeSec * 8f) * 0.5f + 0.5f)
    val ring1Radius = baseRadius * (1f + pulse * 0.14f)
    val ring2Radius = baseRadius * (0.75f - pulse * 0.08f)

    drawCircle(
        color = vortexColor.copy(alpha = 0.20f),
        radius = ring1Radius * 1.3f,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = vortexColor.copy(alpha = 0.50f),
        radius = ring1Radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 4f * scale)
    )
    drawCircle(
        color = vortexColor.copy(alpha = 0.85f),
        radius = ring2Radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 3.5f * scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f * scale, 16f * scale), timeSec * 60f))
    )

    // 2. Lâminas de energia em rotação ao redor do centro (estilo turbina/vórtice como no Print 1)
    rotate(degrees = timeSec * 90f, pivot = Offset(centerX, centerY)) {
        val bladeCount = 12
        val bladeLength = 40f * scale
        for (i in 0 until bladeCount) {
            val angle = (i.toFloat() / bladeCount.toFloat()) * (2f * PI).toFloat()
            val startX = centerX + cos(angle) * (ring2Radius + 8f * scale)
            val startY = centerY + sin(angle) * (ring2Radius + 8f * scale)
            val endX = centerX + cos(angle + 0.18f) * (ring2Radius + 8f * scale + bladeLength)
            val endY = centerY + sin(angle + 0.18f) * (ring2Radius + 8f * scale + bladeLength)

            drawLine(
                color = vortexColor.copy(alpha = 0.75f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3f * scale,
                cap = StrokeCap.Round
            )
        }
    }
}

