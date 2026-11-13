package dev.vmellos.airhockey.feature.physics

import dev.vmellos.airhockey.feature.ai.AirHockeyAiController
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.MatchSpec
import dev.vmellos.airhockey.feature.game.MatchState
import dev.vmellos.airhockey.feature.game.MatchStatus
import dev.vmellos.airhockey.feature.network.ControlMessages
import dev.vmellos.airhockey.feature.network.HostTelemetryPacket
import dev.vmellos.airhockey.core.preferences.TableTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Snapshot imutável do estado e da física da partida a cada instante.
 */
data class MatchFrameSnapshot(
    val puckX: Float,
    val puckY: Float,
    val puckVx: Float,
    val puckVy: Float,
    val player1MalletX: Float,
    val player1MalletY: Float,
    val player2MalletX: Float,
    val player2MalletY: Float,
    val scorePlayer1: Int = 0,
    val scorePlayer2: Int = 0,
    val targetScore: Int = 3,
    val remainingTimeSeconds: Int = 180,
    val countdownNumber: Int = 3,
    val matchStatus: MatchStatus = MatchStatus.COUNTDOWN,
    val winner: Int = 0,
    val lastScorer: Int = 0,
    val puckLastHitBy: Int = 0,
    val timestampNano: Long = System.nanoTime()
) {
    fun puckSpeed(): Float = kotlin.math.hypot(puckVx, puckVy)

    fun toTelemetryPacket(sequenceNumber: Long): HostTelemetryPacket {
        return HostTelemetryPacket(
            sequenceNumber = sequenceNumber,
            timestampNano = timestampNano,
            puckX = puckX,
            puckY = puckY,
            puckVx = puckVx,
            puckVy = puckVy,
            hostMalletX = player1MalletX,
            hostMalletY = player1MalletY,
            guestMalletX = player2MalletX,
            guestMalletY = player2MalletY,
            puckLastHitBy = puckLastHitBy.toByte()
        )
    }

    fun toRemoteMatchState(): ControlMessages.RemoteMatchState {
        return ControlMessages.RemoteMatchState(
            statusName = matchStatus.name,
            scorePlayer1 = scorePlayer1,
            scorePlayer2 = scorePlayer2,
            remainingTimeSeconds = remainingTimeSeconds,
            countdownNumber = countdownNumber,
            winner = winner,
            lastScorer = lastScorer
        )
    }
}

/**
 * Porta de áudio no seam do Árbitro Virtual.
 */
interface AudioOutputPort {
    fun playPaddleHit(intensity: Float, isPlayer1: Boolean = true) {}
    fun playWallHit() {}
    fun playGoalScored(score: Int, target: Int) {}
    fun playGoalConceded(score: Int, target: Int) {}
    fun playGoal() { playGoalScored(1, 1) }
    fun playCountdownBeep(number: Int) {}
    fun playCountdownBeep(isFinal: Boolean) { playCountdownBeep(if (isFinal) 0 else 1) }
    fun stopAll() {}
}

object NoOpAudioOutputPort : AudioOutputPort {
    override fun playPaddleHit(intensity: Float, isPlayer1: Boolean) {}
    override fun playWallHit() {}
    override fun playGoalScored(score: Int, target: Int) {}
    override fun playGoalConceded(score: Int, target: Int) {}
    override fun playCountdownBeep(number: Int) {}
    override fun stopAll() {}
}

/**
 * Módulo profundo do Árbitro Virtual (GLOSSARY.md).
 *
 * Unifica a autoridade de regras esportivas da partida:
 * - Ciclo de vida e regras da partida (Contagem regressiva, Em jogo, Gol, Fim de Jogo, Pausa)
 * - Integração determinística a 120 Hz do motor de física [AirHockeyPhysicsEngine]
 * - Atualização tática das inteligências artificiais [AirHockeyAiController]
 * - Pré-posicionamento e conciliação de Previsão no Cliente para jogos em rede
 * - Registro e emissão do estado imutável [MatchState]
 */
class ArbitroVirtual(
    val dimensions: TableDimensions,
    val spec: MatchSpec,
    val audioPort: AudioOutputPort = NoOpAudioOutputPort,
    val onGoalScoredCallback: ((scorer: Int, isGameOver: Boolean) -> Unit)? = null
) {
    constructor(
        dimensions: TableDimensions,
        mode: GameMode = GameMode.SOLO,
        targetScore: Int = 3,
        matchTimeSeconds: Int = 180,
        audioPort: AudioOutputPort = NoOpAudioOutputPort
    ) : this(
        dimensions = dimensions,
        spec = MatchSpec(
            mode = mode,
            targetScore = targetScore,
            matchTimeSeconds = matchTimeSeconds
        ),
        audioPort = audioPort
    )

    val mode: GameMode = spec.mode
    val isGuest: Boolean = mode == GameMode.EXTENDED_GUEST || mode == GameMode.CONNECTED_GUEST || mode == GameMode.AI_VS_AI_EXTENDED_GUEST || mode == GameMode.AI_VS_AI_CONNECTED_GUEST
    val isHost: Boolean = !isGuest

    val engine: AirHockeyPhysicsEngine = AirHockeyPhysicsEngine(
        dimensions = dimensions,
        maxSpeed = spec.puckMaxSpeed,
        airFrictionPerSec = spec.airFriction
    )

    val aiControllerP1: AirHockeyAiController? = when (mode) {
        GameMode.AI_VS_AI_LOCAL, GameMode.AI_VS_AI_CONNECTED_HOST -> AirHockeyAiController(
            difficulty = spec.aiDifficultyPlayer1,
            isPlayer1 = true,
            customMaxSpeed = spec.customAiMaxSpeed,
            customReactionDelay = spec.customAiReactionDelay,
            customAimingErrorRange = spec.customAiAimingErrorRange,
            customBankShotChance = spec.customAiBankShotChance
        )
        GameMode.AI_VS_AI_EXTENDED, GameMode.AI_VS_AI_EXTENDED_HOST -> if (isHost) AirHockeyAiController(
            difficulty = spec.aiDifficultyPlayer1,
            isPlayer1 = true,
            customMaxSpeed = spec.customAiMaxSpeed,
            customReactionDelay = spec.customAiReactionDelay,
            customAimingErrorRange = spec.customAiAimingErrorRange,
            customBankShotChance = spec.customAiBankShotChance
        ) else null
        else -> null
    }

    val aiControllerP2: AirHockeyAiController? = when (mode) {
        GameMode.SOLO -> AirHockeyAiController(
            difficulty = spec.aiDifficulty,
            isPlayer1 = false,
            customMaxSpeed = spec.customAiMaxSpeed,
            customReactionDelay = spec.customAiReactionDelay,
            customAimingErrorRange = spec.customAiAimingErrorRange,
            customBankShotChance = spec.customAiBankShotChance
        )
        GameMode.AI_VS_AI_LOCAL, GameMode.AI_VS_AI_CONNECTED_HOST -> AirHockeyAiController(
            difficulty = spec.aiDifficultyPlayer2,
            isPlayer1 = false,
            customMaxSpeed = spec.customAiMaxSpeed,
            customReactionDelay = spec.customAiReactionDelay,
            customAimingErrorRange = spec.customAiAimingErrorRange,
            customBankShotChance = spec.customAiBankShotChance
        )
        GameMode.AI_VS_AI_EXTENDED, GameMode.AI_VS_AI_EXTENDED_HOST -> if (isHost) AirHockeyAiController(
            difficulty = spec.aiDifficultyPlayer2,
            isPlayer1 = false,
            customMaxSpeed = spec.customAiMaxSpeed,
            customReactionDelay = spec.customAiReactionDelay,
            customAimingErrorRange = spec.customAiAimingErrorRange,
            customBankShotChance = spec.customAiBankShotChance
        ) else null
        else -> null
    }

    private val _matchState = MutableStateFlow(
        MatchState(
            mode = mode,
            status = if (spec.hasIntroPresentation) MatchStatus.PRESENTATION else MatchStatus.COUNTDOWN,
            scorePlayer1 = 0,
            scorePlayer2 = 0,
            targetScore = spec.targetScore,
            countdownNumber = 3,
            remainingTimeSeconds = 0,
            isHost = isHost,
            peerExited = false,
            player1Name = spec.player1Name,
            player2Name = spec.player2Name,
            currentStage = spec.stageNumber,
            starsEarned = 0,
            tableTheme = spec.tableTheme,
            arenaDimension = spec.arenaDimension
        )
    )
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val _frames = MutableStateFlow(buildSnapshot())
    val frames: StateFlow<MatchFrameSnapshot> = _frames.asStateFlow()

    var pendingCollisionEvent: Byte = 0
        private set
    var pendingCollisionIntensity: Float = 0f
        private set

    private val simulationLock = Any()

    // Acumuladores de tempo determinísticos desacoplados de corrotinas externas
    private var realTimeAccumulator: Float = 0f
    private var goalPauseAccumulator: Float = 0f
    private var countdownStepAccumulator: Float = 0f
    private var secondTimerAccumulator: Float = 0f
    private var deadPuckAccumulator: Float = 0f

    companion object {
        const val FIXED_DT_SEC: Float = 1f / 120f
        const val MAX_FRAME_ELAPSED_SEC: Float = 0.10f
        const val MAX_CATCHUP_STEPS: Int = 12
        const val GOAL_PAUSE_DURATION_SEC: Float = 1.20f
        const val COUNTDOWN_STEP_DURATION_SEC: Float = 0.90f
        const val DEAD_PUCK_TIMEOUT_SEC: Float = 3.50f
    }

    init {
        setupCollisionListeners()
        engine.resetPositions(0)
        publishSnapshot()
    }

    private fun buildSnapshot(): MatchFrameSnapshot {
        val s = _matchState.value
        return MatchFrameSnapshot(
            puckX = engine.puck.x,
            puckY = engine.puck.y,
            puckVx = engine.puck.vx,
            puckVy = engine.puck.vy,
            player1MalletX = engine.player1Mallet.x,
            player1MalletY = engine.player1Mallet.y,
            player2MalletX = engine.player2Mallet.x,
            player2MalletY = engine.player2Mallet.y,
            scorePlayer1 = s.scorePlayer1,
            scorePlayer2 = s.scorePlayer2,
            targetScore = s.targetScore,
            remainingTimeSeconds = s.remainingTimeSeconds,
            countdownNumber = s.countdownNumber,
            matchStatus = s.status,
            winner = s.winner,
            lastScorer = s.lastScorer,
            puckLastHitBy = engine.puck.lastHitBy
        )
    }

    fun currentSnapshot(): MatchFrameSnapshot = _frames.value

    fun getPlayer2Target(): Pair<Float, Float> = synchronized(simulationLock) {
        Pair(engine.player2Mallet.targetX, engine.player2Mallet.targetY)
    }

    private fun publishSnapshot() {
        _frames.value = buildSnapshot()
    }

    private fun recordPendingCollision(eventCode: Byte, intensity: Float) {
        val prevIsPaddle = pendingCollisionEvent.toInt() == 1 || pendingCollisionEvent.toInt() == 2
        if (eventCode.toInt() == 3 && prevIsPaddle) {
            return
        }
        val nextIsPaddle = eventCode.toInt() == 1 || eventCode.toInt() == 2
        pendingCollisionEvent = eventCode
        pendingCollisionIntensity = if (prevIsPaddle && nextIsPaddle) {
            maxOf(pendingCollisionIntensity, intensity)
        } else {
            intensity
        }
    }

    private fun setupCollisionListeners() {
        engine.onCollisionListener = { event, intensity ->
            when (event) {
                CollisionEvent.PADDLE_HIT_P1 -> {
                    recordPendingCollision(1, intensity)
                    audioPort.playPaddleHit(intensity, isPlayer1 = true)
                }
                CollisionEvent.PADDLE_HIT_P2 -> {
                    recordPendingCollision(2, intensity)
                    audioPort.playPaddleHit(intensity, isPlayer1 = false)
                }
                CollisionEvent.WALL_HIT -> {
                    recordPendingCollision(3, intensity)
                    audioPort.playWallHit()
                }
                CollisionEvent.GOAL_PLAYER1 -> {
                    handleGoalInternal(scorer = 1)
                }
                CollisionEvent.GOAL_PLAYER2 -> {
                    handleGoalInternal(scorer = 2)
                }
                CollisionEvent.NONE -> {}
            }
        }
    }

    fun handleGoal(scorer: Int) = synchronized(simulationLock) {
        handleGoalInternal(scorer)
    }

    private fun handleGoalInternal(scorer: Int) {
        if (_matchState.value.status != MatchStatus.PLAYING) return

        val p1 = if (scorer == 1) _matchState.value.scorePlayer1 + 1 else _matchState.value.scorePlayer1
        val p2 = if (scorer == 2) _matchState.value.scorePlayer2 + 1 else _matchState.value.scorePlayer2
        val target = _matchState.value.targetScore

        val isPlayer1Scored = (scorer == 1)
        val currentScorerScore = if (isPlayer1Scored) p1 else p2

        if (isPlayer1Scored) {
            audioPort.playGoalScored(currentScorerScore, target)
        } else {
            audioPort.playGoalConceded(currentScorerScore, target)
        }

        val isGameOver = (p1 >= target || p2 >= target)
        if (isGameOver) {
            _matchState.update {
                it.copy(
                    scorePlayer1 = p1,
                    scorePlayer2 = p2,
                    lastScorer = scorer,
                    winner = if (p1 >= target) 1 else 2,
                    status = MatchStatus.GAME_OVER
                )
            }
        } else {
            goalPauseAccumulator = 0f
            _matchState.update {
                it.copy(
                    scorePlayer1 = p1,
                    scorePlayer2 = p2,
                    lastScorer = scorer,
                    status = MatchStatus.GOAL_PAUSE
                )
            }
        }

        publishSnapshot()
        onGoalScoredCallback?.invoke(scorer, isGameOver)
    }

    /**
     * Avança a simulação usando acumulador de passo fixo (120 Hz) desacoplado do FPS da tela.
     * Retorna o número de passos fixos executados neste ciclo.
     */
    fun advanceRealTime(elapsedSec: Float): Int = synchronized(simulationLock) {
        if (elapsedSec <= 0f || !elapsedSec.isFinite()) return 0
        if (!isActiveLoopStatus(_matchState.value.status)) {
            realTimeAccumulator = 0f
            return 0
        }

        realTimeAccumulator += elapsedSec.coerceAtMost(MAX_FRAME_ELAPSED_SEC)
        var steps = 0
        while (realTimeAccumulator >= FIXED_DT_SEC && steps < MAX_CATCHUP_STEPS) {
            if (!isActiveLoopStatus(_matchState.value.status)) {
                realTimeAccumulator = 0f
                break
            }
            tickInternal(FIXED_DT_SEC)
            realTimeAccumulator -= FIXED_DT_SEC
            steps++
        }
        if (steps >= MAX_CATCHUP_STEPS && realTimeAccumulator >= FIXED_DT_SEC) {
            realTimeAccumulator = 0f
        }
        steps
    }

    private fun isActiveLoopStatus(status: MatchStatus): Boolean {
        return status == MatchStatus.COUNTDOWN ||
            status == MatchStatus.PLAYING ||
            status == MatchStatus.GOAL_PAUSE
    }

    fun tickPhysics(dt: Float) = synchronized(simulationLock) {
        tickPhysicsInternal(dt)
    }

    private fun tickPhysicsInternal(dt: Float) {
        if (_matchState.value.status != MatchStatus.PLAYING) return

        // REGRA DO DISCO PRESO (GLOSSARY.md / ADR 0039):
        // Se o disco permanecer praticamente imóvel (< 30f) por mais de 3,5 segundos,
        // o Árbitro Virtual intervém, apita e reinicia no centro neutro da mesa.
        val puckSpeed = engine.puck.speed()
        if (puckSpeed < 30f) {
            deadPuckAccumulator += dt
            if (deadPuckAccumulator >= DEAD_PUCK_TIMEOUT_SEC) {
                deadPuckAccumulator = 0f
                engine.puck.reset(dimensions.width / 2f, dimensions.centerLineY)
                audioPort.playCountdownBeep(true)
                publishSnapshot()
                return
            }
        } else {
            deadPuckAccumulator = 0f
        }

        if (isHost) {
            aiControllerP1?.update(engine, dt)
            aiControllerP2?.update(engine, dt)
            engine.tick(dt)
        } else if (aiControllerP2 != null) {
            aiControllerP2.update(engine, dt)
        }
        publishSnapshot()
    }

    /**
     * Avança o tempo de forma estritamente determinística no seam do Árbitro Virtual.
     *
     * Unifica a simulação física a 120 Hz, contagem regressiva de início/retomada,
     * pausa pós-gol e acumulador de segundos da partida em uma única linha do tempo sem corrotinas soltas.
     */
    fun tick(dt: Float) = synchronized(simulationLock) {
        tickInternal(dt)
    }

    private fun tickInternal(dt: Float) {
        when (_matchState.value.status) {
            MatchStatus.PLAYING -> {
                tickPhysicsInternal(dt)
                if (isHost) {
                    secondTimerAccumulator += dt
                    if (secondTimerAccumulator >= 1.0f) {
                        val elapsedSec = secondTimerAccumulator.toInt()
                        secondTimerAccumulator -= elapsedSec
                        repeat(elapsedSec) { tickTimerInternal() }
                    }
                }
            }
            MatchStatus.COUNTDOWN -> {
                if (isHost) {
                    tickMalletsOnlyInternal(dt)
                    countdownStepAccumulator += dt
                    if (countdownStepAccumulator >= COUNTDOWN_STEP_DURATION_SEC) {
                        countdownStepAccumulator -= COUNTDOWN_STEP_DURATION_SEC
                        val next = _matchState.value.countdownNumber - 1
                        setCountdownInternal(next)
                    }
                }
            }
            MatchStatus.GOAL_PAUSE -> {
                if (isHost) {
                    goalPauseAccumulator += dt
                    if (goalPauseAccumulator >= GOAL_PAUSE_DURATION_SEC) {
                        goalPauseAccumulator = 0f
                        resetPositionsInternal(_matchState.value.lastScorer)
                        startCountdownInternal()
                    }
                }
            }
            else -> {}
        }
    }

    fun tickMalletsOnly(dt: Float) = synchronized(simulationLock) {
        tickMalletsOnlyInternal(dt)
    }

    private fun tickMalletsOnlyInternal(dt: Float) {
        if (_matchState.value.status == MatchStatus.COUNTDOWN && isHost) {
            engine.tickMalletsOnly(dt)
            publishSnapshot()
        }
    }

    fun consumeCollisionTelemetry(): Pair<Byte, Float> = synchronized(simulationLock) {
        val evt = pendingCollisionEvent
        val intensity = pendingCollisionIntensity
        pendingCollisionEvent = 0
        pendingCollisionIntensity = 0f
        Pair(evt, intensity)
    }

    private fun applyPlayerTouch(mallet: Mallet, isPlayer1: Boolean, x: Float, y: Float) {
        if (!isInputAllowed()) return
        if (_matchState.value.status == MatchStatus.COUNTDOWN) {
            val (cx, cy) = engine.clampPreparatoryPosition(x, y, mallet.radius, isPlayer1 = isPlayer1)
            mallet.targetX = cx
            mallet.targetY = cy
            mallet.x = cx
            mallet.y = cy
        } else {
            mallet.targetX = x
            mallet.targetY = y
        }
        publishSnapshot()
    }

    fun onTouchPlayer1(x: Float, y: Float) = synchronized(simulationLock) {
        applyPlayerTouch(engine.player1Mallet, isPlayer1 = true, x = x, y = y)
    }

    fun onTouchPlayer2(x: Float, y: Float) = synchronized(simulationLock) {
        applyPlayerTouch(engine.player2Mallet, isPlayer1 = false, x = x, y = y)
    }

    fun submitPlayer1Target(targetX: Float, targetY: Float) {
        onTouchPlayer1(targetX, targetY)
    }

    fun submitPlayer2Target(targetX: Float, targetY: Float) {
        onTouchPlayer2(targetX, targetY)
    }

    fun onTouchGuestGlobal(globalX: Float, globalY: Float): Pair<Float, Float>? = synchronized(simulationLock) {
        onTouchGuestGlobalInternal(globalX, globalY)
    }

    private fun onTouchGuestGlobalInternal(globalX: Float, globalY: Float): Pair<Float, Float>? {
        if (!isInputAllowed()) return null
        val radius = engine.player2Mallet.radius
        val clampedX = globalX.coerceIn(radius, engine.dimensions.width - radius)
        val maxY = if (engine.dimensions.isExtendedMode) {
            engine.dimensions.hostHalfBottomY - radius
        } else {
            engine.dimensions.centerLineY - radius
        }
        val clampedY = globalY.coerceIn(radius, maxY)

        engine.player2Mallet.targetX = clampedX
        engine.player2Mallet.targetY = clampedY
        engine.player2Mallet.x = clampedX
        engine.player2Mallet.y = clampedY
        publishSnapshot()
        return Pair(clampedX, clampedY)
    }

    fun onGuestTouchLocal(localX: Float, localY: Float): Pair<Float, Float>? = synchronized(simulationLock) {
        if (!isInputAllowed()) return null
        val globalX = 1000f - localX
        val globalY = 1000f - localY
        return onTouchGuestGlobalInternal(globalX, globalY)
    }

    fun onHostReceiveGuestInput(globalTargetX: Float, globalTargetY: Float) = synchronized(simulationLock) {
        engine.player2Mallet.targetX = globalTargetX
        engine.player2Mallet.targetY = globalTargetY
        if (_matchState.value.status == MatchStatus.COUNTDOWN) {
            engine.player2Mallet.x = globalTargetX
            engine.player2Mallet.y = globalTargetY
        }
        publishSnapshot()
    }

    fun applyRemoteTelemetry(
        puckX: Float, puckY: Float,
        puckVx: Float, puckVy: Float,
        hostMalletX: Float, hostMalletY: Float,
        guestMalletX: Float, guestMalletY: Float,
        collisionEvent: Byte = 0,
        collisionIntensity: Float = 0f,
        puckLastHitBy: Byte = 0
    ) = synchronized(simulationLock) {
        engine.puck.x = puckX
        engine.puck.y = puckY
        engine.puck.vx = puckVx
        engine.puck.vy = puckVy
        engine.puck.lastHitBy = puckLastHitBy.toInt()
        engine.player1Mallet.x = hostMalletX
        engine.player1Mallet.y = hostMalletY

        if (_matchState.value.status != MatchStatus.COUNTDOWN) {
            val dx = guestMalletX - engine.player2Mallet.x
            val dy = guestMalletY - engine.player2Mallet.y
            val distSq = dx * dx + dy * dy
            if (distSq > 2500f) {
                engine.player2Mallet.x = guestMalletX
                engine.player2Mallet.y = guestMalletY
            } else if (distSq > 4f) {
                engine.player2Mallet.x += dx * 0.35f
                engine.player2Mallet.y += dy * 0.35f
            }
        }

        if (collisionEvent.toInt() != 0) {
            when (collisionEvent.toInt()) {
                1 -> audioPort.playPaddleHit(collisionIntensity.coerceAtLeast(0.4f), isPlayer1 = true)
                2 -> audioPort.playPaddleHit(collisionIntensity.coerceAtLeast(0.4f), isPlayer1 = false)
                3 -> audioPort.playWallHit()
            }
        }
        publishSnapshot()
    }

    fun applyRemoteTelemetry(telemetry: HostTelemetryPacket) {
        applyRemoteTelemetry(
            puckX = telemetry.puckX,
            puckY = telemetry.puckY,
            puckVx = telemetry.puckVx,
            puckVy = telemetry.puckVy,
            hostMalletX = telemetry.hostMalletX,
            hostMalletY = telemetry.hostMalletY,
            guestMalletX = telemetry.guestMalletX,
            guestMalletY = telemetry.guestMalletY,
            collisionEvent = 0,
            collisionIntensity = 0f,
            puckLastHitBy = telemetry.puckLastHitBy
        )
    }

    fun applyRemoteState(remote: ControlMessages.RemoteMatchState) = synchronized(simulationLock) {
        val newStatus = try {
            MatchStatus.valueOf(remote.statusName)
        } catch (_: Exception) {
            MatchStatus.PLAYING
        }
        val prev = _matchState.value
        val effectiveStatus = if (prev.status == MatchStatus.PAUSED && (newStatus == MatchStatus.COUNTDOWN || newStatus == MatchStatus.PLAYING)) {
            MatchStatus.PAUSED
        } else {
            newStatus
        }
        _matchState.update {
            it.copy(
                status = effectiveStatus,
                scorePlayer1 = remote.scorePlayer1,
                scorePlayer2 = remote.scorePlayer2,
                remainingTimeSeconds = remote.remainingTimeSeconds,
                countdownNumber = remote.countdownNumber,
                winner = remote.winner,
                lastScorer = remote.lastScorer
            )
        }

        if (newStatus == MatchStatus.COUNTDOWN && remote.countdownNumber != prev.countdownNumber) {
            audioPort.playCountdownBeep(remote.countdownNumber)
        } else if (newStatus == MatchStatus.PLAYING && prev.status == MatchStatus.COUNTDOWN) {
            audioPort.playCountdownBeep(0)
        } else if (newStatus == MatchStatus.GOAL_PAUSE && prev.status != MatchStatus.GOAL_PAUSE) {
            val isGuestScored = (remote.lastScorer == 2)
            val score = if (isGuestScored) remote.scorePlayer2 else remote.scorePlayer1
            val target = _matchState.value.targetScore
            if (isGuestScored) {
                audioPort.playGoalScored(score, target)
            } else {
                audioPort.playGoalConceded(score, target)
            }
        }
        publishSnapshot()
    }

    fun setCountdown(number: Int) = synchronized(simulationLock) {
        setCountdownInternal(number)
    }

    private fun setCountdownInternal(number: Int) {
        if (_matchState.value.status == MatchStatus.PAUSED) return
        countdownStepAccumulator = 0f
        audioPort.playCountdownBeep(number)
        _matchState.update {
            if (it.status == MatchStatus.PAUSED) return@update it
            it.copy(
                countdownNumber = number,
                status = if (number == 0) MatchStatus.PLAYING else MatchStatus.COUNTDOWN
            )
        }
        publishSnapshot()
    }

    fun startCountdown() = synchronized(simulationLock) {
        startCountdownInternal()
    }

    private fun startCountdownInternal() {
        if (_matchState.value.status == MatchStatus.PAUSED) return
        realTimeAccumulator = 0f
        countdownStepAccumulator = 0f
        setCountdownInternal(3)
    }

    fun tickTimer() = synchronized(simulationLock) {
        tickTimerInternal()
    }

    private fun tickTimerInternal() {
        _matchState.update { current ->
            if (current.status != MatchStatus.PLAYING) return@update current
            current.copy(remainingTimeSeconds = current.remainingTimeSeconds + 1)
        }
        publishSnapshot()
    }

    fun pause() = synchronized(simulationLock) {
        realTimeAccumulator = 0f
        val currentStatus = _matchState.value.status
        if (currentStatus == MatchStatus.PLAYING || currentStatus == MatchStatus.COUNTDOWN || currentStatus == MatchStatus.GOAL_PAUSE || currentStatus == MatchStatus.PRESENTATION) {
            audioPort.stopAll()
            _matchState.update { it.copy(status = MatchStatus.PAUSED) }
        }
        publishSnapshot()
    }

    fun resume() = synchronized(simulationLock) {
        if (_matchState.value.status == MatchStatus.PAUSED) {
            realTimeAccumulator = 0f
            countdownStepAccumulator = 0f
            audioPort.playCountdownBeep(3)
            _matchState.update { it.copy(status = MatchStatus.COUNTDOWN, countdownNumber = 3) }
        }
        publishSnapshot()
    }

    fun finishPresentationAndStartCountdown() = synchronized(simulationLock) {
        if (_matchState.value.status == MatchStatus.PAUSED) return
        startCountdownInternal()
    }

    fun restart() = synchronized(simulationLock) {
        realTimeAccumulator = 0f
        goalPauseAccumulator = 0f
        countdownStepAccumulator = 0f
        secondTimerAccumulator = 0f
        deadPuckAccumulator = 0f
        engine.resetPositions(0)
        _matchState.update {
            it.copy(
                status = if (spec.hasIntroPresentation) MatchStatus.PRESENTATION else MatchStatus.COUNTDOWN,
                scorePlayer1 = 0,
                scorePlayer2 = 0,
                winner = 0,
                starsEarned = 0,
                countdownNumber = 3,
                remainingTimeSeconds = 0
            )
        }
        publishSnapshot()
    }

    fun resetPositions(scorer: Int = 0) = synchronized(simulationLock) {
        resetPositionsInternal(scorer)
    }

    private fun resetPositionsInternal(scorer: Int) {
        engine.resetPositions(scorer)
        publishSnapshot()
    }

    fun updateStarsEarned(stars: Int, newlyConqueredTheme: TableTheme? = null) = synchronized(simulationLock) {
        _matchState.update { it.copy(starsEarned = stars, newlyConqueredTheme = newlyConqueredTheme) }
        publishSnapshot()
    }

    fun setPeerExited() = synchronized(simulationLock) {
        realTimeAccumulator = 0f
        _matchState.update { it.copy(peerExited = true, status = MatchStatus.PAUSED) }
        publishSnapshot()
    }

    fun setIdle() = synchronized(simulationLock) {
        realTimeAccumulator = 0f
        audioPort.stopAll()
        _matchState.update { it.copy(status = MatchStatus.IDLE) }
        publishSnapshot()
    }

    private fun isInputAllowed(): Boolean {
        val s = _matchState.value.status
        if (_matchState.value.isSpectator) return false
        return s == MatchStatus.PLAYING || s == MatchStatus.COUNTDOWN
    }
}
