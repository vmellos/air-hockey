package dev.vmellos.airhockey.feature.physics

import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.MatchStatus
import dev.vmellos.airhockey.feature.network.ControlMessages
import dev.vmellos.airhockey.feature.network.HostTelemetryPacket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Snapshot imutável do estado e da física da partida a cada instante.
 *
 * Consumido pela thread de renderização da UI (Canvas) e pela serialização
 * de telemetria UDP sem concorrência ou ponteiros mutáveis compartilhados.
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
 * Porta de áudio no seam interno do Árbitro Virtual.
 */
interface AudioOutputPort {
    fun playPaddleHit(intensity: Float)
    fun playWallHit()
    fun playGoal()
    fun playCountdownBeep(isFinal: Boolean)
}

object NoOpAudioOutputPort : AudioOutputPort {
    override fun playPaddleHit(intensity: Float) {}
    override fun playWallHit() {}
    override fun playGoal() {}
    override fun playCountdownBeep(isFinal: Boolean) {}
}

/**
 * Módulo profundo do Árbitro Virtual (GLOSSARY.md).
 *
 * Unifica a simulação física determinística de 120 Hz com a validação de regras
 * (pontuação, cronômetro, contagem regressiva e fim de jogo), emitindo snapshots
 * imutáveis e isolando a concorrência da thread de renderização da UI.
 */
class ArbitroVirtual(
    val dimensions: TableDimensions,
    val mode: GameMode = GameMode.SOLO,
    val targetScore: Int = 3,
    val matchTimeSeconds: Int = 180,
    val audioPort: AudioOutputPort = NoOpAudioOutputPort
) {
    val engine: AirHockeyPhysicsEngine = AirHockeyPhysicsEngine(dimensions)
    val projection: TableProjection = TableProjection(dimensions, mode)

    private val _currentSnapshot = MutableStateFlow(
        createSnapshot(
            status = MatchStatus.COUNTDOWN,
            countdown = 3
        )
    )
    val frames: StateFlow<MatchFrameSnapshot> = _currentSnapshot.asStateFlow()

    init {
        setupCollisionListeners()
    }

    private fun setupCollisionListeners() {
        engine.onCollisionListener = { event, intensity ->
            when (event) {
                CollisionEvent.PADDLE_HIT_P1, CollisionEvent.PADDLE_HIT_P2 -> {
                    audioPort.playPaddleHit(intensity)
                }
                CollisionEvent.WALL_HIT -> {
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

    private fun handleGoal(scorer: Int) {
        val current = _currentSnapshot.value
        if (current.matchStatus != MatchStatus.PLAYING) return

        audioPort.playGoal()

        val p1 = if (scorer == 1) current.scorePlayer1 + 1 else current.scorePlayer1
        val p2 = if (scorer == 2) current.scorePlayer2 + 1 else current.scorePlayer2

        if (p1 >= targetScore || p2 >= targetScore) {
            val win = if (p1 >= targetScore) 1 else 2
            _currentSnapshot.update {
                it.copy(
                    scorePlayer1 = p1,
                    scorePlayer2 = p2,
                    lastScorer = scorer,
                    winner = win,
                    matchStatus = MatchStatus.GAME_OVER
                )
            }
        } else {
            _currentSnapshot.update {
                it.copy(
                    scorePlayer1 = p1,
                    scorePlayer2 = p2,
                    lastScorer = scorer,
                    matchStatus = MatchStatus.GOAL_PAUSE
                )
            }
        }
    }

    fun tick(dt: Float) {
        val status = _currentSnapshot.value.matchStatus
        if (status == MatchStatus.PLAYING) {
            engine.tick(dt)
        }
        publishSnapshot()
    }

    fun submitPlayer1Target(targetX: Float, targetY: Float) {
        engine.player1Mallet.targetX = targetX
        engine.player1Mallet.targetY = targetY
    }

    fun submitPlayer2Target(targetX: Float, targetY: Float) {
        engine.player2Mallet.targetX = targetX
        engine.player2Mallet.targetY = targetY
    }

    fun setCountdown(number: Int) {
        audioPort.playCountdownBeep(isFinal = (number == 0))
        _currentSnapshot.update {
            it.copy(
                countdownNumber = number,
                matchStatus = if (number == 0) MatchStatus.PLAYING else MatchStatus.COUNTDOWN
            )
        }
    }

    fun tickTimer() {
        _currentSnapshot.update { current ->
            if (current.matchStatus != MatchStatus.PLAYING) return@update current
            current.copy(remainingTimeSeconds = current.remainingTimeSeconds + 1)
        }
    }

    fun pause() {
        if (_currentSnapshot.value.matchStatus == MatchStatus.PLAYING) {
            _currentSnapshot.update { it.copy(matchStatus = MatchStatus.PAUSED) }
        }
    }

    fun resume() {
        if (_currentSnapshot.value.matchStatus == MatchStatus.PAUSED) {
            _currentSnapshot.update { it.copy(matchStatus = MatchStatus.PLAYING) }
        }
    }

    fun resetPositions(scorer: Int = 0) {
        engine.resetPositions(scorer)
        publishSnapshot()
    }

    fun restart() {
        resetPositions(0)
        _currentSnapshot.value = createSnapshot(
            status = MatchStatus.COUNTDOWN,
            countdown = 3
        )
    }

    fun applyRemoteTelemetry(telemetry: HostTelemetryPacket) {
        engine.puck.x = telemetry.puckX
        engine.puck.y = telemetry.puckY
        engine.puck.vx = telemetry.puckVx
        engine.puck.vy = telemetry.puckVy
        engine.puck.lastHitBy = telemetry.puckLastHitBy.toInt()
        engine.player1Mallet.x = telemetry.hostMalletX
        engine.player1Mallet.y = telemetry.hostMalletY
        engine.player2Mallet.x = telemetry.guestMalletX
        engine.player2Mallet.y = telemetry.guestMalletY
        publishSnapshot()
    }

    fun applyRemoteState(state: ControlMessages.RemoteMatchState) {
        val newStatus = try {
            MatchStatus.valueOf(state.statusName)
        } catch (_: Exception) {
            MatchStatus.PLAYING
        }
        val prev = _currentSnapshot.value
        _currentSnapshot.update {
            it.copy(
                matchStatus = newStatus,
                scorePlayer1 = state.scorePlayer1,
                scorePlayer2 = state.scorePlayer2,
                remainingTimeSeconds = state.remainingTimeSeconds,
                countdownNumber = state.countdownNumber,
                winner = state.winner,
                lastScorer = state.lastScorer
            )
        }

        // Áudio sincronizado no convidado
        if (newStatus == MatchStatus.COUNTDOWN && state.countdownNumber != prev.countdownNumber) {
            audioPort.playCountdownBeep(isFinal = (state.countdownNumber == 0))
        } else if (newStatus == MatchStatus.PLAYING && prev.matchStatus == MatchStatus.COUNTDOWN) {
            audioPort.playCountdownBeep(isFinal = true)
        } else if (newStatus == MatchStatus.GOAL_PAUSE && prev.matchStatus != MatchStatus.GOAL_PAUSE) {
            audioPort.playGoal()
        }
    }

    private fun publishSnapshot() {
        val current = _currentSnapshot.value
        _currentSnapshot.value = current.copy(
            puckX = engine.puck.x,
            puckY = engine.puck.y,
            puckVx = engine.puck.vx,
            puckVy = engine.puck.vy,
            player1MalletX = engine.player1Mallet.x,
            player1MalletY = engine.player1Mallet.y,
            player2MalletX = engine.player2Mallet.x,
            player2MalletY = engine.player2Mallet.y,
            puckLastHitBy = engine.puck.lastHitBy,
            timestampNano = System.nanoTime()
        )
    }

    private fun createSnapshot(status: MatchStatus, countdown: Int): MatchFrameSnapshot {
        return MatchFrameSnapshot(
            puckX = engine.puck.x,
            puckY = engine.puck.y,
            puckVx = engine.puck.vx,
            puckVy = engine.puck.vy,
            player1MalletX = engine.player1Mallet.x,
            player1MalletY = engine.player1Mallet.y,
            player2MalletX = engine.player2Mallet.x,
            player2MalletY = engine.player2Mallet.y,
            scorePlayer1 = 0,
            scorePlayer2 = 0,
            targetScore = targetScore,
            remainingTimeSeconds = matchTimeSeconds,
            countdownNumber = countdown,
            matchStatus = status,
            winner = 0,
            lastScorer = 0
        )
    }
}
