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
        var stopAllCalled = false
        var countdownBeeps = mutableListOf<Boolean>()

        override fun playPaddleHit(intensity: Float, isPlayer1: Boolean) { paddleHitPlayed = true }
        override fun playWallHit() { wallHitPlayed = true }
        override fun playGoalScored(score: Int, target: Int) { goalPlayed = true }
        override fun playGoalConceded(score: Int, target: Int) { goalPlayed = true }
        override fun playCountdownBeep(number: Int) { countdownBeeps.add(number == 0) }
        override fun stopAll() { stopAllCalled = true }
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
    fun testTimerDoesNotEndMatch() {
        val dimensions = TableDimensions()
        val arbitro = ArbitroVirtual(dimensions, mode = GameMode.SOLO, matchTimeSeconds = 0)
        arbitro.setCountdown(0) // PLAYING
        assertEquals(MatchStatus.PLAYING, arbitro.frames.value.matchStatus)

        // Incrementa o tempo 300 vezes (5 minutos)
        for (i in 0 until 300) {
            arbitro.tickTimer()
        }

        // A partida deve continuar em PLAYING, sem GAME_OVER por tempo
        assertEquals(MatchStatus.PLAYING, arbitro.frames.value.matchStatus)
        assertEquals(300, arbitro.frames.value.remainingTimeSeconds)
    }

    @Test
    fun testTelemetrySynchronizesLastHitBy() {
        val dimensions = TableDimensions()
        val arbitro = ArbitroVirtual(dimensions)
        arbitro.setCountdown(0)
        arbitro.engine.puck.lastHitBy = 2
        arbitro.tick(0.001f) // Publica snapshot com lastHitBy

        val packet = arbitro.frames.value.toTelemetryPacket(sequenceNumber = 1L)
        assertEquals(2.toByte(), packet.puckLastHitBy)

        val guestArbitro = ArbitroVirtual(dimensions)
        guestArbitro.applyRemoteTelemetry(packet)
        assertEquals(2, guestArbitro.engine.puck.lastHitBy)
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

    @Test
    fun testPreparatoryPositionClampingDuringCountdown() {
        val dimensions = TableDimensions()
        val arbitro = ArbitroVirtual(dimensions, mode = GameMode.SOLO)
        assertEquals(MatchStatus.COUNTDOWN, arbitro.matchState.value.status)

        // Tenta tocar fora da metade defensiva do Jogador 1 (y < centerLineY)
        arbitro.onTouchPlayer1(500f, 200f)

        // O rebatedor deve ser grampeado dentro de sua área defensiva
        val r = arbitro.engine.player1Mallet.radius
        val expectedMinY = dimensions.centerLineY + r
        org.junit.Assert.assertTrue(
            "Durante a contagem, y do Player 1 deve ficar na metade inferior (>= $expectedMinY)",
            arbitro.engine.player1Mallet.y >= expectedMinY
        )
    }

    @Test
    fun testCollisionTelemetryConsumption() {
        val dimensions = TableDimensions()
        val arbitro = ArbitroVirtual(dimensions, mode = GameMode.SOLO)
        arbitro.setCountdown(0)

        // Posiciona disco e rebatedor 1 em rota de colisão
        arbitro.engine.puck.x = 500f
        arbitro.engine.puck.y = 1200f
        arbitro.engine.puck.vx = 0f
        arbitro.engine.puck.vy = 0f

        arbitro.engine.player1Mallet.x = 500f
        arbitro.engine.player1Mallet.y = 1250f
        arbitro.engine.player1Mallet.targetX = 500f
        arbitro.engine.player1Mallet.targetY = 1200f

        arbitro.tickPhysics(0.016f)

        val (evt, intensity) = arbitro.consumeCollisionTelemetry()
        if (evt.toInt() != 0) {
            org.junit.Assert.assertTrue("Telemetria de colisão deve ter intensidade positiva", intensity > 0f)
            val (clearedEvt, clearedIntensity) = arbitro.consumeCollisionTelemetry()
            assertEquals(0.toByte(), clearedEvt)
            assertEquals(0f, clearedIntensity, 0.001f)
        }
    }

    @Test
    fun testPauseDuringPresentation() {
        val audio = TestAudioPort()
        val dimensions = TableDimensions()
        val spec = dev.vmellos.airhockey.feature.game.MatchSpec(
            mode = GameMode.SOLO,
            hasIntroPresentation = true
        )
        val arbitro = ArbitroVirtual(
            dimensions = dimensions,
            spec = spec,
            audioPort = audio
        )
        assertEquals(MatchStatus.PRESENTATION, arbitro.matchState.value.status)
        arbitro.pause()
        assertEquals("Arbitro deve pausar mesmo durante a apresentação de abertura", MatchStatus.PAUSED, arbitro.matchState.value.status)
        org.junit.Assert.assertTrue("Ao pausar durante a apresentação, stopAll deve ser acionado", audio.stopAllCalled)

        // Retomar a partir da pausa deve iniciar o countdown
        arbitro.resume()
        assertEquals("Retomar deve transitar para COUNTDOWN", MatchStatus.COUNTDOWN, arbitro.matchState.value.status)
        assertEquals(3, arbitro.matchState.value.countdownNumber)

        // Reiniciar deve restaurar a apresentação caso configurada
        arbitro.restart()
        assertEquals("Reiniciar deve restaurar PRESENTATION", MatchStatus.PRESENTATION, arbitro.matchState.value.status)
    }
}
