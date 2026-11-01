package dev.vmellos.airhockey.feature.ai

import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.feature.physics.AirHockeyPhysicsEngine
import kotlin.math.abs
import kotlin.math.sqrt

class AirHockeyAiController(
    private val difficulty: AiDifficulty
) {
    private var bankShotTimer: Float = 0f
    private var bankShotDirection: Int = 1 // 1 = ricochete na parede direita, -1 = na parede esquerda
    private var isBankShotActive: Boolean = false

    // Variáveis de percepção com latência e erro de mira para humanizar o modo Fácil
    private var reactionTimer: Float = 0.3f
    private var perceivedPuckX: Float = 500f
    private var perceivedPuckY: Float = 1000f
    private var aimingErrorX: Float = 0f

    fun update(engine: AirHockeyPhysicsEngine, dt: Float) {
        val puck = engine.puck
        val mallet = engine.player2Mallet
        val dimensions = engine.dimensions

        val homeX = dimensions.width / 2f
        val homeY = 190f
        val goalLeft = dimensions.goalLeft
        val goalRight = dimensions.goalRight

        val isPuckInAiHalf = puck.y < dimensions.centerLineY
        val isPuckMovingTowardsAi = puck.vy < -15f

        // Configuração de dinâmica e velocidades
        val maxSpeed = when (difficulty) {
            AiDifficulty.EASY -> 900f
            AiDifficulty.MEDIUM -> 3200f
            AiDifficulty.HARD -> 5400f
        }

        // Para o modo Fácil: atualiza percepção com latência de reflexo humano (~280ms) e erro de mira
        if (difficulty == AiDifficulty.EASY) {
            reactionTimer += dt
            if (reactionTimer >= 0.28f) {
                reactionTimer = 0f
                perceivedPuckX = puck.x
                perceivedPuckY = puck.y
                aimingErrorX = ((Math.random().toFloat() * 120f) - 60f)
            }
        }

        // Atualiza temporizador de estratégias de Jogada Lateral
        bankShotTimer += dt
        if (bankShotTimer > 1.8f) {
            bankShotTimer = 0f
            bankShotDirection = if (puck.x > homeX) -1 else 1
            isBankShotActive = when (difficulty) {
                AiDifficulty.HARD -> true
                AiDifficulty.MEDIUM -> (Math.random() < 0.40)
                AiDifficulty.EASY -> false
            }
        }

        var desiredX = homeX
        var desiredY = homeY

        // -------------------------------------------------------------
        // SALVAGUARDA CONTRA AUTOGOL:
        // Se o disco estiver atrás do rebatedor da IA ou na zona crítica da meta
        // (Ativa apenas em MEDIUM e HARD; no EASY ela é desarmada para permitir gols acessíveis)
        // -------------------------------------------------------------
        val isPuckBehindMallet = puck.y <= mallet.y + 10f
        val isPuckInGoalMouthDanger = puck.y < 220f && puck.x in (goalLeft - 50f)..(goalRight + 50f)

        if (difficulty != AiDifficulty.EASY && (isPuckBehindMallet || isPuckInGoalMouthDanger)) {
            // Nunca acelere para trás contra o disco.
            // Posiciona-se como escudo centralizado na linha de meta sem empurrar o disco para trás.
            desiredY = 130f
            desiredX = puck.x.coerceIn(goalLeft + mallet.radius * 0.6f, goalRight - mallet.radius * 0.6f)
        } else {
            when (difficulty) {
                AiDifficulty.EASY -> {
                    if (isPuckBehindMallet) {
                        // Se o disco passou, apenas recua para a posição inicial sem interceptação milagrosa
                        desiredX = homeX
                        desiredY = homeY
                    } else if (perceivedPuckY < dimensions.centerLineY) {
                        desiredX = perceivedPuckX + aimingErrorX
                        // Avança moderadamente sem ultrapassar o disco
                        desiredY = if (perceivedPuckY > homeY + 20f) (perceivedPuckY - 20f).coerceAtMost(dimensions.centerLineY - 100f) else homeY
                    } else {
                        desiredX = homeX + (perceivedPuckX - homeX) * 0.25f
                        desiredY = homeY
                    }
                }

                AiDifficulty.MEDIUM -> {
                    if (isPuckInAiHalf || (isPuckMovingTowardsAi && puck.y < dimensions.totalHeight * 0.65f)) {
                        var targetX = puck.x
                        if (isPuckMovingTowardsAi && abs(puck.vy) > 30f) {
                            val interceptY = 250f
                            val timeToIntercept = (puck.y - interceptY) / abs(puck.vy)
                            targetX = predictXWithBounces(puck.x, puck.vx, timeToIntercept, dimensions.width, mallet.radius)
                        }

                        if (isPuckInAiHalf && puck.y > homeY + 40f && isBankShotActive) {
                            // Jogada lateral moderada
                            val lateralOffset = bankShotDirection * (mallet.radius * 0.45f)
                            desiredX = (targetX + lateralOffset).coerceIn(mallet.radius, dimensions.width - mallet.radius)
                            desiredY = (puck.y + 35f).coerceAtMost(dimensions.centerLineY - 80f)
                        } else {
                            desiredX = targetX.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                            desiredY = if (puck.y in (homeY + 30f)..(dimensions.centerLineY - 90f)) {
                                puck.y + 35f
                            } else {
                                homeY + 30f
                            }
                        }
                    } else {
                        desiredX = homeX + (puck.x - homeX) * 0.45f
                        desiredY = homeY
                    }
                }

                AiDifficulty.HARD -> {
                    if (isPuckInAiHalf || isPuckMovingTowardsAi) {
                        val interceptY = if (isPuckInAiHalf) puck.y else 260f
                        var predictedX = puck.x
                        if (isPuckMovingTowardsAi && abs(puck.vy) > 20f) {
                            val timeToIntercept = (puck.y - interceptY) / abs(puck.vy)
                            predictedX = predictXWithBounces(puck.x, puck.vx, timeToIntercept, dimensions.width, mallet.radius)
                        }

                        val canAttack = isPuckInAiHalf && puck.y > mallet.y + 25f && puck.y < dimensions.centerLineY - 60f

                        if (canAttack) {
                            if (isBankShotActive) {
                                // JOGADA LATERAL ESTRATÉGICA:
                                // Golpeia desfasado na lateral do disco para arremessá-lo de tabela na parede oposta
                                val lateralOffset = bankShotDirection * (mallet.radius * 0.60f)
                                desiredX = (puck.x + lateralOffset).coerceIn(mallet.radius + 20f, dimensions.width - mallet.radius - 20f)
                                desiredY = (puck.y + 65f).coerceAtMost(dimensions.centerLineY - 50f)
                            } else {
                                // GOLPE FRONTAL AGRESSIVO
                                desiredX = predictedX.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                                desiredY = (puck.y + 60f).coerceAtMost(dimensions.centerLineY - 50f)
                            }
                        } else {
                            // Posicionamento defensivo antecipado
                            desiredX = predictedX.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                            desiredY = (homeY + 40f).coerceAtMost(dimensions.centerLineY - 120f)
                        }
                    } else {
                        // Disco no campo adversário: cobre o ângulo mais perigoso
                        desiredX = (homeX + (puck.x - homeX) * 0.55f).coerceIn(goalLeft - 20f, goalRight + 20f)
                        desiredY = homeY
                    }
                }
            }
        }

        // Restringe estritamente o alvo ao meio campo da IA
        val clampedDesiredX = desiredX.coerceIn(mallet.radius, dimensions.width - mallet.radius)
        val clampedDesiredY = desiredY.coerceIn(mallet.radius, dimensions.centerLineY - mallet.radius)

        // Movimento cinemático contínuo respeitando maxSpeed
        val dx = clampedDesiredX - mallet.targetX
        val dy = clampedDesiredY - mallet.targetY
        val dist = sqrt(dx * dx + dy * dy)
        val step = maxSpeed * dt

        if (dist > step && dist > 0.001f) {
            mallet.targetX += (dx / dist) * step
            mallet.targetY += (dy / dist) * step
        } else {
            mallet.targetX = clampedDesiredX
            mallet.targetY = clampedDesiredY
        }
    }

    /**
     * Calcula o ricochete exato nas bordas laterais para prever com precisão onde o disco chegará.
     */
    private fun predictXWithBounces(startX: Float, vx: Float, time: Float, tableWidth: Float, radius: Float): Float {
        var x = startX + vx * time
        val minX = radius
        val maxX = tableWidth - radius
        val effectiveWidth = maxX - minX

        if (effectiveWidth <= 0f) return tableWidth / 2f

        // Normaliza em relação a minX
        var normX = x - minX
        val doubleWidth = 2f * effectiveWidth

        // Trata reflexões múltiplas
        normX %= doubleWidth
        if (normX < 0f) normX += doubleWidth

        val finalNormX = if (normX > effectiveWidth) {
            doubleWidth - normX
        } else {
            normX
        }

        return minX + finalNormX
    }
}
