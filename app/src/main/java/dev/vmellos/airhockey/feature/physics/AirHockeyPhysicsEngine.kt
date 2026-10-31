package dev.vmellos.airhockey.feature.physics

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class CollisionEvent {
    NONE,
    PADDLE_HIT_P1,
    PADDLE_HIT_P2,
    WALL_HIT,
    GOAL_PLAYER1,
    GOAL_PLAYER2
}

class AirHockeyPhysicsEngine(
    val dimensions: TableDimensions
) {
    val puck = Puck(
        x = dimensions.width / 2f,
        y = dimensions.centerLineY,
        radius = dimensions.puckRadius
    )

    val player1Mallet = Mallet(
        x = dimensions.width / 2f,
        y = dimensions.totalHeight - 250f,
        radius = dimensions.malletRadius
    )

    val player2Mallet = Mallet(
        x = dimensions.width / 2f,
        y = 250f,
        radius = dimensions.malletRadius
    )

    private val wallRestitution = 0.96f
    private val paddleRestitution = 1.0f
    private val maxSpeed = 12000f
    private val maxSpeedSq = maxSpeed * maxSpeed
    private val airFrictionPerSec = 0.35f

    var onCollisionListener: ((CollisionEvent, Float) -> Unit)? = null

    fun resetPositions(scorer: Int = 0) {
        val puckY = if (dimensions.isExtendedMode) {
            when (scorer) {
                1 -> dimensions.hostHalfBottomY - 220f // P1 marcou, P2 saca
                2 -> dimensions.guestHalfTopY + 220f   // P2 marcou, P1 saca
                else -> dimensions.guestHalfTopY + 220f // Início da partida: P1 saca
            }
        } else {
            when (scorer) {
                1 -> dimensions.centerLineY - 200f
                2 -> dimensions.centerLineY + 200f
                else -> dimensions.centerLineY
            }
        }
        puck.reset(dimensions.width / 2f, puckY)
        player1Mallet.reset(dimensions.width / 2f, dimensions.totalHeight - 250f)
        player2Mallet.reset(dimensions.width / 2f, 250f)
    }

    /**
     * Executa um tick físico determinístico com substepping dinâmico para prevenir tunneling.
     */
    fun tick(dt: Float) {
        val speed = puck.speed()
        val displacement = speed * dt
        val maxSafeStep = puck.radius * 0.45f
        val steps = if (displacement > maxSafeStep && maxSafeStep > 0f) {
            min(ceil(displacement / maxSafeStep).toInt(), 6)
        } else {
            1
        }

        val subDt = dt / steps
        for (i in 0 until steps) {
            subTick(subDt)
        }
    }

    private fun subTick(dt: Float) {
        updateMalletPositions(dt)

        // Aplica velocidade no disco
        puck.x += puck.vx * dt
        puck.y += puck.vy * dt

        // Resolução de colisões
        resolvePaddleCollision(player1Mallet, isPlayer1 = true)
        resolvePaddleCollision(player2Mallet, isPlayer1 = false)
        resolveWallCollisions()

        // Aplica atrito do ar contínuo
        val frictionFactor = (1f - airFrictionPerSec * dt).coerceIn(0f, 1f)
        puck.vx *= frictionFactor
        puck.vy *= frictionFactor

        // Teto de velocidade terminal
        val spd2 = puck.speedSquared()
        if (spd2 > maxSpeedSq) {
            val scale = maxSpeed / sqrt(spd2)
            puck.vx *= scale
            puck.vy *= scale
        } else if (spd2 < 0.05f) {
            // Evita números subnormais na FPU
            puck.vx = 0f
            puck.vy = 0f
        }
    }

    private fun updateMalletPositions(dt: Float) {
        // Interpola suavemente a posição do rebathedor até o toque do jogador
        val smoothing = (30f * dt).coerceIn(0f, 1f)

        val oldP1X = player1Mallet.x
        val oldP1Y = player1Mallet.y
        player1Mallet.x += (player1Mallet.targetX - player1Mallet.x) * smoothing
        player1Mallet.y += (player1Mallet.targetY - player1Mallet.y) * smoothing
        clampMalletToHalf(player1Mallet, isPlayer1 = true)
        player1Mallet.vx = if (dt > 0) (player1Mallet.x - oldP1X) / dt else 0f
        player1Mallet.vy = if (dt > 0) (player1Mallet.y - oldP1Y) / dt else 0f

        val oldP2X = player2Mallet.x
        val oldP2Y = player2Mallet.y
        player2Mallet.x += (player2Mallet.targetX - player2Mallet.x) * smoothing
        player2Mallet.y += (player2Mallet.targetY - player2Mallet.y) * smoothing
        clampMalletToHalf(player2Mallet, isPlayer1 = false)
        player2Mallet.vx = if (dt > 0) (player2Mallet.x - oldP2X) / dt else 0f
        player2Mallet.vy = if (dt > 0) (player2Mallet.y - oldP2Y) / dt else 0f
    }

    private fun clampMalletToHalf(mallet: Mallet, isPlayer1: Boolean) {
        val r = mallet.radius
        mallet.x = mallet.x.coerceIn(r, dimensions.width - r)

        if (isPlayer1) {
            // Player 1 (Humano / Anfitrião) joga na metade inferior [centerLineY + r, totalHeight - r]
            val minY = if (dimensions.isExtendedMode) dimensions.guestHalfTopY + r else (dimensions.centerLineY + r)
            mallet.y = mallet.y.coerceIn(minY, dimensions.totalHeight - r)
        } else {
            // Player 2 (IA / Convidado) joga na metade superior [r, centerLineY - r]
            val maxY = if (dimensions.isExtendedMode) dimensions.hostHalfBottomY - r else (dimensions.centerLineY - r)
            mallet.y = mallet.y.coerceIn(r, maxY)
        }
    }

    private fun resolvePaddleCollision(mallet: Mallet, isPlayer1: Boolean) {
        val dx = puck.x - mallet.x
        val dy = puck.y - mallet.y
        val distSq = dx * dx + dy * dy
        val minDist = puck.radius + mallet.radius

        if (distSq < minDist * minDist) {
            val dist = if (distSq > 0.0001f) sqrt(distSq) else 0.01f
            val nx = dx / dist
            val ny = dy / dist

            // Separa os corpos para evitar sobreposição
            puck.x = mallet.x + nx * minDist
            puck.y = mallet.y + ny * minDist

            // Velocidade relativa
            val rvx = puck.vx - mallet.vx
            val rvy = puck.vy - mallet.vy
            val velAlongNormal = rvx * nx + rvy * ny

            if (velAlongNormal < 0f) {
                val impulseMag = -(1f + paddleRestitution) * velAlongNormal
                puck.vx += (impulseMag * nx) + mallet.vx * 0.40f
                puck.vy += (impulseMag * ny) + mallet.vy * 0.40f

                val impactIntensity = (sqrt(puck.speedSquared()) / 3000f).coerceIn(0.2f, 1.0f)
                val event = if (isPlayer1) CollisionEvent.PADDLE_HIT_P1 else CollisionEvent.PADDLE_HIT_P2
                onCollisionListener?.invoke(event, impactIntensity)
            }
        }
    }

    private fun resolveWallCollisions() {
        val r = puck.radius

        // Parede Esquerda
        if (puck.x - r < 0f) {
            puck.x = r
            puck.vx = -puck.vx * wallRestitution
            onCollisionListener?.invoke(CollisionEvent.WALL_HIT, 0.7f)
        }

        // Parede Direita
        if (puck.x + r > dimensions.width) {
            puck.x = dimensions.width - r
            puck.vx = -puck.vx * wallRestitution
            onCollisionListener?.invoke(CollisionEvent.WALL_HIT, 0.7f)
        }

        // Meta em Y <= 0 (Defendida pelo Player 2 / IA no Topo -> Ponto para Player 1)
        if (puck.y - r < 0f) {
            val isInGoal = puck.x in dimensions.goalLeft..dimensions.goalRight
            if (isInGoal) {
                if (puck.y < -r * 0.5f) {
                    onCollisionListener?.invoke(CollisionEvent.GOAL_PLAYER1, 1.0f)
                }
            } else {
                puck.y = r
                puck.vy = -puck.vy * wallRestitution
                onCollisionListener?.invoke(CollisionEvent.WALL_HIT, 0.7f)
            }
        }

        // Meta em Y >= totalHeight (Defendida pelo Player 1 / Humano na Base -> Ponto para Player 2)
        if (puck.y + r > dimensions.totalHeight) {
            val isInGoal = puck.x in dimensions.goalLeft..dimensions.goalRight
            if (isInGoal) {
                if (puck.y > dimensions.totalHeight + r * 0.5f) {
                    onCollisionListener?.invoke(CollisionEvent.GOAL_PLAYER2, 1.0f)
                }
            } else {
                puck.y = dimensions.totalHeight - r
                puck.vy = -puck.vy * wallRestitution
                onCollisionListener?.invoke(CollisionEvent.WALL_HIT, 0.7f)
            }
        }
    }
}
