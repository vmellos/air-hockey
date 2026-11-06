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
    val isGuest: Boolean = mode == GameMode.EXTENDED_GUEST || mode == GameMode.CONNECTED_GUEST || mode == GameMode.AI_VS_AI_EXTENDED_GUEST
    val isHost: Boolean = !isGuest

    val engine: AirHockeyPhysicsEngine = AirHockeyPhysicsEngine(
        dimensions = dimensions,
        maxSpeed = spec.puckMaxSpeed,
        airFrictionPerSec = spec.airFriction
    )

    val aiControllerP1: AirHockeyAiController? = when (mode) {
        GameMode.AI_VS_AI_LOCAL -> AirHockeyAiController(spec.aiDifficultyPlayer1, isPlayer1 = true)
        GameMode.AI_VS_AI_EXTENDED, GameMode.AI_VS_AI_EXTENDED_HOST -> if (isHost) AirHockeyAiController(spec.aiDifficultyPlayer1, isPlayer1 = true) else null
        else -> null
    }

    val aiControllerP2: AirHockeyAiController? = when (mode) {
        GameMode.SOLO -> AirHockeyAiController(spec.aiDifficulty, isPlayer1 = false)
        GameMode.AI_VS_AI_LOCAL -> AirHockeyAiController(spec.aiDifficultyPlayer2, isPlayer1 = false)
        GameMode.AI_VS_AI_EXTENDED, GameMode.AI_VS_AI_EXTENDED_HOST -> if (isHost) AirHockeyAiController(spec.aiDifficultyPlayer2, isPlayer1 = false) else null
        else -> null
    }

    private val _matchState = MutableStateFlow(
        MatchState(
            mode = mode,
            status = MatchStatus.COUNTDOWN,
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
            tableTheme = spec.tableTheme
        )
    )
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val _frames = MutableStateFlow(buildSnapshot())
    val frames: StateFlow<MatchFrameSnapshot> = _frames.asStateFlow()

    var pendingCollisionEvent: Byte = 0
        private set
    var pendingCollisionIntensity: Float = 0f
        private set

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

    private fun publishSnapshot() {
        _frames.value = buildSnapshot()
    }

    private fun setupCollisionListeners() {
        engine.onCollisionListener = { event, intensity ->
            when (event) {
                CollisionEvent.PADDLE_HIT_P1 -> {
                    pendingCollisionEvent = 1
                    pendingCollisionIntensity = intensity
                    audioPort.playPaddleHit(intensity, isPlayer1 = true)
                }
                CollisionEvent.PADDLE_HIT_P2 -> {
                    pendingCollisionEvent = 2
                    pendingCollisionIntensity = intensity
                    audioPort.playPaddleHit(intensity, isPlayer1 = false)
                }
                CollisionEvent.WALL_HIT -> {
                    pendingCollisionEvent = 3
                    pendingCollisionIntensity = intensity
                    audioPort.playWallHit()
                }
                CollisionEvent.GOAL_PLAYER1 -> {
                    handleGoal(scorer = 1)
                }
                CollisionEvent.GOAL_PLAYER2 -> {
                    handleGoal(scorer = 2)
                }
                CollisionEvent.NONE -> {}
            }
        }
    }

    fun handleGoal(scorer: Int) {
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

    fun tickPhysics(dt: Float) {
        if (_matchState.value.status != MatchStatus.PLAYING) return

        if (isHost) {
            aiControllerP1?.update(engine, dt)
            aiControllerP2?.update(engine, dt)
            engine.tick(dt)
        } else if (aiControllerP2 != null) {
            aiControllerP2.update(engine, dt)
        }
        publishSnapshot()
    }

    fun tick(dt: Float) {
        tickPhysics(dt)
    }

    fun tickMalletsOnly(dt: Float) {
        if (_matchState.value.status == MatchStatus.COUNTDOWN && isHost) {
            engine.tickMalletsOnly(dt)
            publishSnapshot()
        }
    }

    fun consumeCollisionTelemetry(): Pair<Byte, Float> {
        val evt = pendingCollisionEvent
        val intensity = pendingCollisionIntensity
        pendingCollisionEvent = 0
        pendingCollisionIntensity = 0f
        return Pair(evt, intensity)
    }

    fun onTouchPlayer1(x: Float, y: Float) {
        if (!isInputAllowed()) return
        if (_matchState.value.status == MatchStatus.COUNTDOWN) {
            val (cx, cy) = engine.clampPreparatoryPosition(x, y, engine.player1Mallet.radius, isPlayer1 = true)
            engine.player1Mallet.targetX = cx
            engine.player1Mallet.targetY = cy
            engine.player1Mallet.x = cx
            engine.player1Mallet.y = cy
        } else {
            engine.player1Mallet.targetX = x
            engine.player1Mallet.targetY = y
        }
        publishSnapshot()
    }

    fun onTouchPlayer2(x: Float, y: Float) {
        if (!isInputAllowed()) return
        if (_matchState.value.status == MatchStatus.COUNTDOWN) {
            val (cx, cy) = engine.clampPreparatoryPosition(x, y, engine.player2Mallet.radius, isPlayer1 = false)
            engine.player2Mallet.targetX = cx
            engine.player2Mallet.targetY = cy
            engine.player2Mallet.x = cx
            engine.player2Mallet.y = cy
        } else {
            engine.player2Mallet.targetX = x
            engine.player2Mallet.targetY = y
        }
        publishSnapshot()
    }

    fun submitPlayer1Target(targetX: Float, targetY: Float) {
        onTouchPlayer1(targetX, targetY)
    }

    fun submitPlayer2Target(targetX: Float, targetY: Float) {
        onTouchPlayer2(targetX, targetY)
    }

    fun onTouchGuestGlobal(globalX: Float, globalY: Float): Pair<Float, Float>? {
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

    fun onGuestTouchLocal(localX: Float, localY: Float): Pair<Float, Float>? {
        if (!isInputAllowed()) return null
        val globalX = 1000f - localX
        val globalY = 1000f - localY
        return onTouchGuestGlobal(globalX, globalY)
    }

    fun onHostReceiveGuestInput(globalTargetX: Float, globalTargetY: Float) {
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
    ) {
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

    fun applyRemoteState(remote: ControlMessages.RemoteMatchState) {
        val newStatus = try {
            MatchStatus.valueOf(remote.statusName)
        } catch (_: Exception) {
            MatchStatus.PLAYING
        }
        val prev = _matchState.value
        _matchState.update {
            it.copy(
                status = newStatus,
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

    fun setCountdown(number: Int) {
        audioPort.playCountdownBeep(number)
        _matchState.update {
            it.copy(
                countdownNumber = number,
                status = if (number == 0) MatchStatus.PLAYING else MatchStatus.COUNTDOWN
            )
        }
        publishSnapshot()
    }

    fun tickTimer() {
        _matchState.update { current ->
            if (current.status != MatchStatus.PLAYING) return@update current
            current.copy(remainingTimeSeconds = current.remainingTimeSeconds + 1)
        }
        publishSnapshot()
    }

    fun pause() {
        val currentStatus = _matchState.value.status
        if (currentStatus == MatchStatus.PLAYING || currentStatus == MatchStatus.COUNTDOWN || currentStatus == MatchStatus.GOAL_PAUSE) {
            audioPort.stopAll()
            _matchState.update { it.copy(status = MatchStatus.PAUSED) }
        }
        publishSnapshot()
    }

    fun resume() {
        if (_matchState.value.status == MatchStatus.PAUSED) {
            _matchState.update { it.copy(status = MatchStatus.COUNTDOWN, countdownNumber = 3) }
        }
        publishSnapshot()
    }

    fun restart() {
        engine.resetPositions(0)
        _matchState.update {
            it.copy(
                status = MatchStatus.COUNTDOWN,
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

    fun resetPositions(scorer: Int = 0) {
        engine.resetPositions(scorer)
        publishSnapshot()
    }

    fun updateStarsEarned(stars: Int, newlyConqueredTheme: TableTheme? = null) {
        _matchState.update { it.copy(starsEarned = stars, newlyConqueredTheme = newlyConqueredTheme) }
        publishSnapshot()
    }

    fun setPeerExited() {
        _matchState.update { it.copy(peerExited = true, status = MatchStatus.PAUSED) }
        publishSnapshot()
    }

    fun setIdle() {
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
