package dev.vmellos.airhockey.feature.ai

import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.feature.physics.AirHockeyPhysicsEngine
import kotlin.math.abs

class AirHockeyAiController(
    private val difficulty: AiDifficulty
) {
    fun update(engine: AirHockeyPhysicsEngine, dt: Float) {
        val puck = engine.puck
        val mallet = engine.player2Mallet
        val dimensions = engine.dimensions

        val homeX = dimensions.width / 2f
        val homeY = dimensions.totalHeight - 220f

        val isPuckInAiHalf = puck.y > dimensions.centerLineY
        val isPuckMovingTowardsAi = puck.vy > 0

        val maxSpeed = when (difficulty) {
            AiDifficulty.EASY -> 450f
            AiDifficulty.MEDIUM -> 900f
            AiDifficulty.HARD -> 1750f
        }

        var desiredX = homeX
        var desiredY = homeY

        when (difficulty) {
            AiDifficulty.EASY -> {
                if (isPuckInAiHalf) {
                    desiredX = puck.x
                    desiredY = homeY - 50f
                } else {
                    desiredX = homeX
                    desiredY = homeY
                }
            }

            AiDifficulty.MEDIUM -> {
                if (isPuckInAiHalf) {
                    desiredX = puck.x
                    if (puck.y < homeY && puck.y > dimensions.centerLineY + 100f) {
                        desiredY = puck.y
                    } else {
                        desiredY = homeY - 70f
                    }
                } else {
                    desiredX = homeX + (puck.x - homeX) * 0.35f
                    desiredY = homeY
                }
            }

            AiDifficulty.HARD -> {
                if (isPuckInAiHalf || (isPuckMovingTowardsAi && puck.y > dimensions.totalHeight * 0.3f)) {
                    var predictedX = puck.x
                    if (isPuckMovingTowardsAi && abs(puck.vy) > 10f) {
                        val timeToReach = (homeY - puck.y) / abs(puck.vy)
                        predictedX = puck.x + puck.vx * timeToReach
                        if (predictedX < 0) predictedX = -predictedX
                        if (predictedX > dimensions.width) predictedX = 2 * dimensions.width - predictedX
                    }
                    desiredX = predictedX.coerceIn(mallet.radius, dimensions.width - mallet.radius)

                    if (puck.y in (dimensions.centerLineY + 80f)..(homeY + 20f)) {
                        desiredY = puck.y - 40f
                    } else {
                        desiredY = homeY - 60f
                    }
                } else {
                    desiredX = (homeX + (puck.x - homeX) * 0.5f).coerceIn(dimensions.goalLeft, dimensions.goalRight)
                    desiredY = homeY
                }
            }
        }

        // Move suavemente até a posição desejada respeitando a velocidade máxima da IA
        val dx = desiredX - mallet.targetX
        val dy = desiredY - mallet.targetY
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val step = maxSpeed * dt

        if (dist > step && dist > 0.001f) {
            mallet.targetX += (dx / dist) * step
            mallet.targetY += (dy / dist) * step
        } else {
            mallet.targetX = desiredX
            mallet.targetY = desiredY
        }
    }
}
