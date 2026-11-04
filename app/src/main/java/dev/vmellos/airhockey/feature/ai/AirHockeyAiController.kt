package dev.vmellos.airhockey.feature.ai

import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.feature.physics.AirHockeyPhysicsEngine
import dev.vmellos.airhockey.feature.physics.TableDimensions
import kotlin.math.abs
import kotlin.math.sqrt

class AirHockeyAiController(
    val difficulty: AiDifficulty,
    val isPlayer1: Boolean = false
) {
    private var bankShotTimer: Float = 0f
    private var bankShotDirection: Int = 1 // 1 = ricochete na parede direita, -1 = na parede esquerda
    private var isBankShotActive: Boolean = false
    private var idleTimer: Float = 0f

    // Variáveis de percepção com latência e erro de mira para humanizar o modo Fácil
    private var reactionTimer: Float = 0.3f
    private var perceivedPuckX: Float = 500f
    private var perceivedPuckY: Float = 1000f
    private var aimingErrorX: Float = 0f

    fun update(engine: AirHockeyPhysicsEngine, dt: Float) {
        val puck = engine.puck
        val mallet = if (isPlayer1) engine.player1Mallet else engine.player2Mallet
        val opponentMallet = if (isPlayer1) engine.player2Mallet else engine.player1Mallet
        val dimensions = engine.dimensions

        // Coordenadas normalizadas para simetria (se for Player 1, projeta como se estivesse no topo)
        val puckY = if (isPlayer1) dimensions.totalHeight - puck.y else puck.y
        val puckVy = if (isPlayer1) -puck.vy else puck.vy
        val malletY = if (isPlayer1) dimensions.totalHeight - mallet.y else mallet.y
        val opponentX = opponentMallet.x
        val opponentY = if (isPlayer1) dimensions.totalHeight - opponentMallet.y else opponentMallet.y

        val homeX = dimensions.width / 2f
        val homeY = 190f
        val goalLeft = dimensions.goalLeft
        val goalRight = dimensions.goalRight

        val puckSpeed = puck.speed()
        if (puckSpeed < 30f) {
            idleTimer += dt
        } else {
            idleTimer = 0f
        }
        val isStationaryPuck = puckSpeed < 35f

        // Limite da metade de campo da IA (na Mesa Estendida, cada aparelho tem profundidade de viewportHeight)
        val aiHalfBoundaryY = if (dimensions.isExtendedMode) dimensions.viewportHeight else dimensions.centerLineY

        // Se o disco estiver parado ou quase parado próximo ao meio de campo, expande a área de domínio para desempacar
        val isPuckInAiHalf = puckY <= (aiHalfBoundaryY + if (isStationaryPuck) 40f else 0f)
        val isPuckMovingTowardsAi = puckVy < -15f

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
                perceivedPuckY = puckY
                aimingErrorX = ((Math.random().toFloat() * 120f) - 60f)
            }
        }

        // Atualiza temporizador de estratégias de Jogada Lateral
        bankShotTimer += dt
        if (bankShotTimer > 1.8f) {
            bankShotTimer = 0f
            // Se oponente adiantado, favorece o ricochete no flanco oposto ao oponente
            val isOpponentAdvanced = opponentY < dimensions.totalHeight - 380f
            bankShotDirection = if (isOpponentAdvanced) {
                if (opponentX < homeX) 1 else -1
            } else {
                if (puck.x > homeX) -1 else 1
            }

            isBankShotActive = when (difficulty) {
                AiDifficulty.HARD -> true
                AiDifficulty.MEDIUM -> (Math.random() < 0.45)
                AiDifficulty.EASY -> false
            }
        }

        var desiredX = homeX
        var desiredY = homeY

        // SALVAGUARDA CONTRA AUTOGOL:
        val isPuckBehindMallet = puckY <= malletY + 10f
        val isPuckInGoalMouthDanger = puckY < 220f && puck.x in (goalLeft - 50f)..(goalRight + 50f)

        if (difficulty != AiDifficulty.EASY && (isPuckBehindMallet || isPuckInGoalMouthDanger)) {
            desiredY = 130f
            desiredX = puck.x.coerceIn(goalLeft + mallet.radius * 0.6f, goalRight - mallet.radius * 0.6f)
        } else {
            when (difficulty) {
                AiDifficulty.EASY -> {
                    val isDeadlockBreak = idleTimer > 1.2f && puckY in (homeY + 20f)..(aiHalfBoundaryY + 20f)
                    val canStrikeServe = isStationaryPuck && isPuckInAiHalf && puckY > malletY + 15f

                    if (isPuckBehindMallet) {
                        desiredX = homeX
                        desiredY = homeY
                    } else if (canStrikeServe || isDeadlockBreak) {
                        // Saque de Início da IA ou Desempace suave no nível Fácil
                        desiredX = (puck.x + aimingErrorX).coerceIn(mallet.radius, dimensions.width - mallet.radius)
                        desiredY = (puckY + 45f).coerceAtMost(aiHalfBoundaryY - mallet.radius * 0.3f)
                    } else if (perceivedPuckY <= aiHalfBoundaryY + 20f) {
                        desiredX = perceivedPuckX + aimingErrorX
                        desiredY = if (perceivedPuckY > homeY + 20f) {
                            (perceivedPuckY - 35f).coerceIn(homeY, aiHalfBoundaryY - mallet.radius)
                        } else {
                            homeY
                        }
                    } else {
                        desiredX = homeX + (perceivedPuckX - homeX) * 0.25f
                        desiredY = homeY
                    }
                }

                AiDifficulty.MEDIUM -> {
                    val isDeadlockBreak = idleTimer > 1.0f && puckY in (aiHalfBoundaryY - 250f)..(aiHalfBoundaryY + 60f)
                    val canStrikeServe = isStationaryPuck && isPuckInAiHalf && puckY > malletY + 15f

                    if (canStrikeServe || isDeadlockBreak) {
                        // Saque de Início ou Desempate: avança com determinação e golpeia rumo ao gol adversário
                        val tactical = calculateTacticalAttackTarget(
                            puckX = puck.x,
                            puckY = puckY,
                            opponentX = opponentX,
                            opponentY = opponentY,
                            dimensions = dimensions,
                            malletRadius = mallet.radius,
                            isHard = false
                        )
                        desiredX = tactical.first.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                        desiredY = (puckY + 55f).coerceAtMost(aiHalfBoundaryY - mallet.radius * 0.3f)
                    } else if (isPuckInAiHalf || (isPuckMovingTowardsAi && puckY < dimensions.totalHeight * 0.65f)) {
                        val defenseBaselineY = homeY + 40f
                        var targetX = puck.x
                        if (isPuckMovingTowardsAi && abs(puckVy) > 30f && puckY > defenseBaselineY) {
                            val timeToIntercept = (puckY - defenseBaselineY) / abs(puckVy)
                            targetX = predictXWithBounces(puck.x, puck.vx, timeToIntercept, dimensions.width, mallet.radius)
                        }

                        val canAttack = isPuckInAiHalf && puckY > malletY + 25f && puckY <= aiHalfBoundaryY

                        if (canAttack) {
                            if (isBankShotActive) {
                                val lateralOffset = bankShotDirection * (mallet.radius * 0.45f)
                                desiredX = (targetX + lateralOffset).coerceIn(mallet.radius, dimensions.width - mallet.radius)
                                desiredY = (puckY + 45f).coerceAtMost(aiHalfBoundaryY - mallet.radius * 0.3f)
                            } else {
                                val tactical = calculateTacticalAttackTarget(
                                    puckX = targetX,
                                    puckY = puckY,
                                    opponentX = opponentX,
                                    opponentY = opponentY,
                                    dimensions = dimensions,
                                    malletRadius = mallet.radius,
                                    isHard = false
                                )
                                desiredX = tactical.first.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                                desiredY = (puckY + 45f).coerceAtMost(aiHalfBoundaryY - mallet.radius * 0.3f)
                            }
                        } else {
                            desiredX = targetX.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                            desiredY = if (puckY in (homeY + 30f)..(aiHalfBoundaryY - mallet.radius)) {
                                puckY + 35f
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
                    val isDeadlockBreak = idleTimer > 0.8f && puckY in (aiHalfBoundaryY - 280f)..(aiHalfBoundaryY + 80f)
                    val canStrikeServe = isStationaryPuck && isPuckInAiHalf && puckY > malletY + 15f

                    if (canStrikeServe || isDeadlockBreak) {
                        // Saque de Início ou Desempate no nível Difícil: explora o canto desprotegido do oponente
                        val tactical = calculateTacticalAttackTarget(
                            puckX = puck.x,
                            puckY = puckY,
                            opponentX = opponentX,
                            opponentY = opponentY,
                            dimensions = dimensions,
                            malletRadius = mallet.radius,
                            isHard = true
                        )
                        desiredX = tactical.first.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                        desiredY = (puckY + 70f).coerceAtMost(aiHalfBoundaryY - mallet.radius * 0.2f)
                    } else if (isPuckInAiHalf || isPuckMovingTowardsAi) {
                        val defenseBaselineY = homeY + 40f
                        var predictedX = puck.x
                        if (isPuckMovingTowardsAi && abs(puckVy) > 20f && puckY > defenseBaselineY) {
                            val timeToIntercept = (puckY - defenseBaselineY) / abs(puckVy)
                            predictedX = predictXWithBounces(puck.x, puck.vx, timeToIntercept, dimensions.width, mallet.radius)
                        }

                        val canAttack = isPuckInAiHalf && puckY > malletY + 20f && puckY <= aiHalfBoundaryY

                        if (canAttack) {
                            if (isBankShotActive) {
                                val lateralOffset = bankShotDirection * (mallet.radius * 0.60f)
                                desiredX = (puck.x + lateralOffset).coerceIn(mallet.radius + 20f, dimensions.width - mallet.radius - 20f)
                                desiredY = (puckY + 65f).coerceAtMost(aiHalfBoundaryY - mallet.radius * 0.2f)
                            } else {
                                // Mira Tática Anti-Oponente de precisão máxima
                                val tactical = calculateTacticalAttackTarget(
                                    puckX = predictedX,
                                    puckY = puckY,
                                    opponentX = opponentX,
                                    opponentY = opponentY,
                                    dimensions = dimensions,
                                    malletRadius = mallet.radius,
                                    isHard = true
                                )
                                desiredX = tactical.first.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                                desiredY = (puckY + 65f).coerceAtMost(aiHalfBoundaryY - mallet.radius * 0.2f)
                            }
                        } else {
                            desiredX = predictedX.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                            desiredY = (homeY + 40f).coerceAtMost(aiHalfBoundaryY - 120f)
                        }
                    } else {
                        // Posicionamento defensivo com leitura da linha de passe adversária
                        desiredX = (homeX + (puck.x - homeX) * 0.55f).coerceIn(goalLeft - 20f, goalRight + 20f)
                        desiredY = homeY
                    }
                }
            }
        }

        // Restringe estritamente o alvo ao meio campo correspondente
        val clampedDesiredX = desiredX.coerceIn(mallet.radius, dimensions.width - mallet.radius)
        val clampedDesiredY = if (isPlayer1) {
            val minY = if (dimensions.isExtendedMode) dimensions.guestHalfTopY + mallet.radius else (dimensions.centerLineY + mallet.radius)
            (dimensions.totalHeight - desiredY).coerceIn(minY, dimensions.totalHeight - mallet.radius)
        } else {
            val maxY = if (dimensions.isExtendedMode) dimensions.hostHalfBottomY - mallet.radius else (dimensions.centerLineY - mallet.radius)
            desiredY.coerceIn(mallet.radius, maxY)
        }

        val dx = clampedDesiredX - mallet.targetX
        val dy = clampedDesiredY - mallet.targetY
        val dist = sqrt(dx * dx + dy * dy)
        val isEasyDeadlock = difficulty == AiDifficulty.EASY && (idleTimer > 1.2f && puckY in (homeY + 20f)..(dimensions.centerLineY + 20f))
        val effectiveMaxSpeed = if (isEasyDeadlock) 650f else maxSpeed
        val step = effectiveMaxSpeed * dt

        if (dist > step && dist > 0.001f) {
            mallet.targetX += (dx / dist) * step
            mallet.targetY += (dy / dist) * step
        } else {
            mallet.targetX = clampedDesiredX
            mallet.targetY = clampedDesiredY
        }
    }

    /**
     * Calcula o alvo de ataque tático explorando o posicionamento do rebatedor oponente.
     */
    private fun calculateTacticalAttackTarget(
        puckX: Float,
        puckY: Float,
        opponentX: Float,
        opponentY: Float,
        dimensions: TableDimensions,
        malletRadius: Float,
        isHard: Boolean
    ): Pair<Float, Float> {
        val goalLeft = dimensions.goalLeft
        val goalRight = dimensions.goalRight
        val tableCenter = dimensions.width / 2f

        // Detecta se o adversário avançou além da sua zona de defesa de base
        val isOpponentAdvanced = opponentY < dimensions.totalHeight - 380f

        val targetGoalX = if (isOpponentAdvanced) {
            // Se adiantado, explora o flanco oposto onde o oponente deixou espaço vazio
            if (opponentX < tableCenter) goalRight - 40f else goalLeft + 40f
        } else {
            // Oponente posicionado na retaguarda: mira no canto livre
            if (opponentX < tableCenter - 30f) {
                goalRight - 45f // Oponente na esquerda -> ataca o poste direito
            } else if (opponentX > tableCenter + 30f) {
                goalLeft + 45f  // Oponente na direita -> ataca o poste esquerdo
            } else {
                // Centralizado: escolhe o canto mais vantajoso baseado na posição do disco
                if (puckX < tableCenter) goalRight - 45f else goalLeft + 45f
            }
        }

        val dx = targetGoalX - puckX
        val dy = (dimensions.totalHeight - puckY).coerceAtLeast(10f)
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val dirX = dx / dist

        // Para projetar o disco em direção a dirX, o rebatedor atinge o disco ligeiramente no lado oposto
        val lateralIntensity = if (isHard) (malletRadius * 0.65f) else (malletRadius * 0.40f)
        val desiredX = puckX - (dirX * lateralIntensity)
        val desiredY = puckY + (if (isHard) 65f else 45f)

        return Pair(desiredX, desiredY)
    }

    private fun predictXWithBounces(startX: Float, vx: Float, time: Float, tableWidth: Float, radius: Float): Float {
        var x = startX + vx * time
        val minX = radius
        val maxX = tableWidth - radius
        val effectiveWidth = maxX - minX

        if (effectiveWidth <= 0f) return tableWidth / 2f

        var normX = x - minX
        val doubleWidth = 2f * effectiveWidth

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
