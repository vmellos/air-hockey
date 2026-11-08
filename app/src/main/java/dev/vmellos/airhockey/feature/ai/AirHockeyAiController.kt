package dev.vmellos.airhockey.feature.ai

import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.feature.physics.AirHockeyPhysicsEngine
import dev.vmellos.airhockey.feature.physics.TableDimensions
import kotlin.math.abs
import kotlin.math.sqrt

class AirHockeyAiController(
    val difficulty: AiDifficulty,
    val isPlayer1: Boolean = false,
    val customMaxSpeed: Float? = null,
    val customReactionDelay: Float? = null,
    val customAimingErrorRange: Float? = null,
    val customBankShotChance: Float? = null
) {
    private var bankShotTimer: Float = 0f
    private var bankShotDirection: Int = 1 // 1 = ricochete na parede direita, -1 = na parede esquerda
    private var isBankShotActive: Boolean = false
    private var idleTimer: Float = 0f

    // Variáveis de percepção com latência e erro de mira para humanizar o modo Fácil e a curva gradual
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
        val maxSpeed = customMaxSpeed ?: when (difficulty) {
            AiDifficulty.EASY -> 900f
            AiDifficulty.MEDIUM -> 3200f
            AiDifficulty.HARD -> 5400f
        }

        // Percepção com latência de reflexo humano e dispersão de mira (calibração gradual ou modo Fácil)
        val effectiveReactionDelay = customReactionDelay ?: if (difficulty == AiDifficulty.EASY) 0.28f else 0.0f
        val effectiveAimingError = customAimingErrorRange ?: if (difficulty == AiDifficulty.EASY) 60f else 0.0f

        if (effectiveReactionDelay > 0f) {
            reactionTimer += dt
            if (reactionTimer >= effectiveReactionDelay) {
                reactionTimer = 0f
                perceivedPuckX = puck.x
                perceivedPuckY = puckY
                aimingErrorX = if (effectiveAimingError > 0f) {
                    ((Math.random().toFloat() * (effectiveAimingError * 2f)) - effectiveAimingError)
                } else 0f
            }
        } else {
            perceivedPuckX = puck.x
            perceivedPuckY = puckY
            aimingErrorX = 0f
        }

        // Atualiza temporizador de estratégias de Jogada Lateral
        val effectiveBankShotChance = customBankShotChance ?: when (difficulty) {
            AiDifficulty.HARD -> 1.0f
            AiDifficulty.MEDIUM -> 0.45f
            AiDifficulty.EASY -> 0.0f
        }

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

            isBankShotActive = (Math.random().toFloat() < effectiveBankShotChance)
        }

        var desiredX = homeX
        var desiredY = homeY

        // SALVAGUARDAS DE SEGURANÇA E RESGATE DE CANTO:
        val isPuckInCorner = puckY <= (homeY + 70f) && (puck.x < goalLeft - 20f || puck.x > goalRight + 20f)
        val isPuckInGoalMouthDanger = puckY < 220f && puck.x in (goalLeft - 40f)..(goalRight + 40f)
        val isPuckBehindMallet = puckY <= malletY + 10f

        val effectivePuckX = if (effectiveAimingError > 0f) (perceivedPuckX + aimingErrorX).coerceIn(puck.radius, dimensions.width - puck.radius) else puck.x
        val effectivePuckY = if (effectiveReactionDelay > 0f) perceivedPuckY else puckY

        var isAutogoalRisk = false

        if (isPuckInCorner && (isStationaryPuck || idleTimer > 0.4f)) {
            // MANOBRA DE RESGATE DE CANTO (GLOSSARY.md):
            // O disco está preso no escanteio. A IA não deve ficar travada na boca do gol!
            if (puckY < mallet.radius + 20f) {
                // Disco colado na parede de fundo do canto: aproxima-se lateralmente pelo lado interno
                val lateralOffset = if (puck.x > homeX) -mallet.radius * 0.75f else mallet.radius * 0.75f
                desiredX = (puck.x + lateralOffset).coerceIn(mallet.radius, dimensions.width - mallet.radius)
                desiredY = (puckY + mallet.radius * 0.45f).coerceAtMost(aiHalfBoundaryY - mallet.radius)
            } else {
                // Disco no canto com espaço suficiente: contorna por trás e rebate para a frente
                desiredX = puck.x.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                desiredY = (puckY - mallet.radius * 0.6f).coerceIn(mallet.radius, homeY + 40f)
            }
        } else if (isPuckInGoalMouthDanger || (isPuckBehindMallet && puck.x in (goalLeft - 30f)..(goalRight + 30f))) {
            // SALVAGUARDA CONTRA AUTOGOL (GLOSSARY.md):
            // Disco no funil crítico de perigo imediato à meta: cobre o vão do gol
            isAutogoalRisk = true
            desiredY = 130f
            desiredX = effectivePuckX.coerceIn(goalLeft + mallet.radius * 0.6f, goalRight - mallet.radius * 0.6f)
        } else {
            val useSimpleLogic = (difficulty == AiDifficulty.EASY && customMaxSpeed == null)

            if (useSimpleLogic) {
                val isDeadlockBreak = idleTimer > 1.2f && puckY in (homeY + 20f)..(aiHalfBoundaryY + 20f)
                val canStrikeServe = isStationaryPuck && isPuckInAiHalf && puckY > malletY + 15f

                if (isPuckBehindMallet) {
                    desiredX = homeX
                    desiredY = homeY
                } else if (canStrikeServe || isDeadlockBreak) {
                    desiredX = effectivePuckX.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                    desiredY = (effectivePuckY + 45f).coerceAtMost(aiHalfBoundaryY - mallet.radius * 0.3f)
                } else if (effectivePuckY <= aiHalfBoundaryY + 20f) {
                    desiredX = effectivePuckX
                    desiredY = if (effectivePuckY > homeY + 20f) {
                        (effectivePuckY - 35f).coerceIn(homeY, aiHalfBoundaryY - mallet.radius)
                    } else {
                        homeY
                    }
                } else {
                    desiredX = homeX + (effectivePuckX - homeX) * 0.25f
                    desiredY = homeY
                }
            } else {
                val isHardTactics = (customMaxSpeed != null && customMaxSpeed >= 3800f) || (customMaxSpeed == null && difficulty == AiDifficulty.HARD)
                val deadlockYThreshold = if (isHardTactics) 280f else 250f
                val isDeadlockBreak = idleTimer > 0.8f && puckY in (aiHalfBoundaryY - deadlockYThreshold)..(aiHalfBoundaryY + 70f)
                val canStrikeServe = isStationaryPuck && isPuckInAiHalf && puckY > malletY + 15f

                if (canStrikeServe || isDeadlockBreak) {
                    val tactical = calculateTacticalAttackTarget(
                        puckX = effectivePuckX,
                        puckY = effectivePuckY,
                        opponentX = opponentX,
                        opponentY = opponentY,
                        dimensions = dimensions,
                        malletRadius = mallet.radius,
                        isHard = isHardTactics
                    )
                    desiredX = tactical.first.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                    val advanceOffset = if (isHardTactics) 70f else 55f
                    desiredY = (effectivePuckY + advanceOffset).coerceAtMost(aiHalfBoundaryY - mallet.radius * 0.25f)
                } else if (isPuckInAiHalf || (isPuckMovingTowardsAi && puckY < dimensions.totalHeight * 0.70f)) {
                    val defenseBaselineY = homeY + 40f
                    var targetX = effectivePuckX
                    if (isPuckMovingTowardsAi && abs(puckVy) > 20f && puckY > defenseBaselineY) {
                        val timeToIntercept = (puckY - defenseBaselineY) / abs(puckVy)
                        targetX = predictXWithBounces(effectivePuckX, puck.vx, timeToIntercept, dimensions.width, mallet.radius)
                    }

                    val canAttack = isPuckInAiHalf && puckY > malletY + 20f && puckY <= aiHalfBoundaryY

                    if (canAttack) {
                        if (isBankShotActive) {
                            val lateralIntensity = if (isHardTactics) 0.60f else 0.45f
                            val lateralOffset = bankShotDirection * (mallet.radius * lateralIntensity)
                            desiredX = (targetX + lateralOffset).coerceIn(mallet.radius + 15f, dimensions.width - mallet.radius - 15f)
                            val pushOffset = if (isHardTactics) 65f else 45f
                            desiredY = (effectivePuckY + pushOffset).coerceAtMost(aiHalfBoundaryY - mallet.radius * 0.25f)
                        } else {
                            val tactical = calculateTacticalAttackTarget(
                                puckX = targetX,
                                puckY = effectivePuckY,
                                opponentX = opponentX,
                                opponentY = opponentY,
                                dimensions = dimensions,
                                malletRadius = mallet.radius,
                                isHard = isHardTactics
                            )
                            desiredX = tactical.first.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                            val pushOffset = if (isHardTactics) 65f else 45f
                            desiredY = (effectivePuckY + pushOffset).coerceAtMost(aiHalfBoundaryY - mallet.radius * 0.25f)
                        }
                    } else {
                        // Posicionamento de contenção defensiva
                        desiredX = targetX.coerceIn(mallet.radius, dimensions.width - mallet.radius)
                        desiredY = if (effectivePuckY in (homeY + 30f)..(aiHalfBoundaryY - mallet.radius)) {
                            effectivePuckY + 35f
                        } else {
                            homeY + 30f
                        }
                    }
                } else {
                    val trackingFactor = if (isHardTactics) 0.55f else 0.45f
                    desiredX = (homeX + (effectivePuckX - homeX) * trackingFactor).coerceIn(goalLeft - 20f, goalRight + 20f)
                    desiredY = homeY
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

        // Atenua a aceleração em 50% sob risco iminente de autogol para evitar pancadas na própria rede
        val speedMultiplier = when {
            isAutogoalRisk -> 0.50f
            isEasyDeadlock -> 0.70f
            else -> 1.0f
        }
        val effectiveMaxSpeed = maxSpeed * speedMultiplier
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
