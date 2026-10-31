package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.MatchStatus
import dev.vmellos.airhockey.feature.physics.ArbitroVirtual
import dev.vmellos.airhockey.feature.physics.AudioOutputPort
import dev.vmellos.airhockey.feature.physics.TableDimensions
import org.junit.Assert.assertEquals
import org.junit.Test

class ArbitroVirtualTest {

    private class TestAudioPort : AudioOutputPort {
        var goalPlayed = false
        var wallHitPlayed = false
        var paddleHitPlayed = false
        var countdownBeeps = mutableListOf<Boolean>()

        override fun playPaddleHit(intensity: Float) { paddleHitPlayed = true }
        override fun playWallHit() { wallHitPlayed = true }
        override fun playGoal() { goalPlayed = true }
        override fun playCountdownBeep(isFinal: Boolean) { countdownBeeps.add(isFinal) }
    }

    @Test
    fun testCountdownTransition() {
        val audio = TestAudioPort()
        val dimensions = TableDimensions()
        val arbitro = ArbitroVirtual(dimensions, mode = GameMode.SOLO, audioPort = audio)

        assertEquals(MatchStatus.COUNTDOWN, arbitro.frames.value.matchStatus)
        assertEquals(3, arbitro.frames.value.countdownNumber)

        arbitro.setCountdown(2)
        assertEquals(2, arbitro.frames.value.countdownNumber)
        assertEquals(MatchStatus.COUNTDOWN, arbitro.frames.value.matchStatus)

        arbitro.setCountdown(0) // Largada
        assertEquals(0, arbitro.frames.value.countdownNumber)
        assertEquals(MatchStatus.PLAYING, arbitro.frames.value.matchStatus)
        assertEquals(true, audio.countdownBeeps.last())
    }

    @Test
    fun testGoalDetectionAndScoreIncrement() {
        val audio = TestAudioPort()
        val dimensions = TableDimensions()
        val arbitro = ArbitroVirtual(
            dimensions = dimensions,
            mode = GameMode.SOLO,
            targetScore = 3,
            audioPort = audio
        )
        arbitro.setCountdown(0) // Inicia partida

        // Posiciona disco perto da meta do Player 2 com velocidade em direção à meta
        arbitro.engine.puck.x = 500f
        arbitro.engine.puck.y = 20f
        arbitro.engine.puck.vx = 0f
        arbitro.engine.puck.vy = -1200f

        arbitro.tick(0.05f)

        assertEquals(1, arbitro.frames.value.scorePlayer1)
        assertEquals(0, arbitro.frames.value.scorePlayer2)
        assertEquals(MatchStatus.GOAL_PAUSE, arbitro.frames.value.matchStatus)
        assertEquals(true, audio.goalPlayed)
    }

    @Test
    fun testGameOverWhenTargetScoreReached() {
        val audio = TestAudioPort()
        val dimensions = TableDimensions()
        val arbitro = ArbitroVirtual(
            dimensions = dimensions,
            mode = GameMode.SOLO,
            targetScore = 1, // Alvo rápido de 1 ponto
            audioPort = audio
        )
        arbitro.setCountdown(0)

        // Simula gol do Player 1
        arbitro.engine.puck.x = 500f
        arbitro.engine.puck.y = 20f
        arbitro.engine.puck.vx = 0f
        arbitro.engine.puck.vy = -1200f

        arbitro.tick(0.05f)

        assertEquals(1, arbitro.frames.value.scorePlayer1)
        assertEquals(1, arbitro.frames.value.winner)
        assertEquals(MatchStatus.GAME_OVER, arbitro.frames.value.matchStatus)
    }

    @Test
    fun testSnapshotToTelemetryRoundTrip() {
        val dimensions = TableDimensions()
        val arbitro = ArbitroVirtual(dimensions)
        val snapshot = arbitro.frames.value

        val packet = snapshot.toTelemetryPacket(sequenceNumber = 42L)
        assertEquals(42L, packet.sequenceNumber)
        assertEquals(snapshot.puckX, packet.puckX, 0.01f)
        assertEquals(snapshot.player1MalletX, packet.hostMalletX, 0.01f)
        assertEquals(snapshot.player2MalletX, packet.guestMalletX, 0.01f)
    }
}
