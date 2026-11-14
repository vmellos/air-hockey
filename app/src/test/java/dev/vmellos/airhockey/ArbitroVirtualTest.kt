package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.MatchStatus
import dev.vmellos.airhockey.feature.physics.ArbitroVirtual
import dev.vmellos.airhockey.feature.physics.AudioOutputPort
import dev.vmellos.airhockey.feature.physics.TableDimensions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun testExtendedModeRespectsSelectedArenaDimension() {
        val extDims = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = 50f,
            isExtendedMode = true
        )
        val spec3D = dev.vmellos.airhockey.feature.game.MatchSpec(
            mode = GameMode.EXTENDED_HOST,
            arenaDimension = dev.vmellos.airhockey.core.preferences.ArenaDimension.THREE_DIMENSIONAL
        )
        val arbitro3D = ArbitroVirtual(
            dimensions = extDims,
            spec = spec3D
        )
        assertEquals(
            "Mesa Estendida deve respeitar ArenaDimension.THREE_DIMENSIONAL quando especificado",
            dev.vmellos.airhockey.core.preferences.ArenaDimension.THREE_DIMENSIONAL,
            arbitro3D.matchState.value.arenaDimension
        )

        val spec2D = dev.vmellos.airhockey.feature.game.MatchSpec(
            mode = GameMode.EXTENDED_HOST,
            arenaDimension = dev.vmellos.airhockey.core.preferences.ArenaDimension.TWO_DIMENSIONAL
        )
        val arbitro2D = ArbitroVirtual(
            dimensions = extDims,
            spec = spec2D
        )
        assertEquals(
            "Mesa Estendida deve respeitar ArenaDimension.TWO_DIMENSIONAL quando especificado",
            dev.vmellos.airhockey.core.preferences.ArenaDimension.TWO_DIMENSIONAL,
            arbitro2D.matchState.value.arenaDimension
        )
    }

    @Test
    fun testSoloModePreservesThreeDimensionalRendering() {
        val dims = TableDimensions()
        val spec = dev.vmellos.airhockey.feature.game.MatchSpec(
            mode = GameMode.SOLO,
            arenaDimension = dev.vmellos.airhockey.core.preferences.ArenaDimension.THREE_DIMENSIONAL
        )
        val arbitro = ArbitroVirtual(
            dimensions = dims,
            spec = spec
        )
        assertEquals(
            "Modo Solo deve preservar ArenaDimension.THREE_DIMENSIONAL",
            dev.vmellos.airhockey.core.preferences.ArenaDimension.THREE_DIMENSIONAL,
            arbitro.matchState.value.arenaDimension
        )
    }

    @Test
    fun countdownDoesNotRemovePauseState() {
        val dims = TableDimensions()
        val spec = dev.vmellos.airhockey.feature.game.MatchSpec(
            mode = GameMode.SOLO,
            hasIntroPresentation = false
        )
        val arbitro = ArbitroVirtual(dimensions = dims, spec = spec)

        // Pausa a partida
        arbitro.pause()
        assertEquals(MatchStatus.PAUSED, arbitro.matchState.value.status)

        // Tenta disparar setCountdown enquanto pausado
        arbitro.setCountdown(2)
        assertEquals("setCountdown não deve remover o estado PAUSED", MatchStatus.PAUSED, arbitro.matchState.value.status)

        arbitro.setCountdown(0)
        assertEquals("setCountdown(0) não deve iniciar PLAYING enquanto pausado", MatchStatus.PAUSED, arbitro.matchState.value.status)

        // Tenta aplicar remote countdown enquanto pausado
        arbitro.applyRemoteState(
            dev.vmellos.airhockey.feature.network.ControlMessages.RemoteMatchState(
                statusName = MatchStatus.COUNTDOWN.name,
                scorePlayer1 = 0,
                scorePlayer2 = 0,
                remainingTimeSeconds = 180,
                countdownNumber = 1,
                winner = 0,
                lastScorer = 0
            )
        )
        assertEquals("applyRemoteState com COUNTDOWN não deve sobrescrever o estado PAUSED", MatchStatus.PAUSED, arbitro.matchState.value.status)
    }

    @Test
    fun testDeterministicCountdownProgression_advancesViaTick() {
        val dims = TableDimensions()
        val spec = dev.vmellos.airhockey.feature.game.MatchSpec(
            mode = GameMode.SOLO,
            hasIntroPresentation = false
        )
        val arbitro = ArbitroVirtual(dimensions = dims, spec = spec)

        assertEquals(MatchStatus.COUNTDOWN, arbitro.matchState.value.status)
        assertEquals(3, arbitro.matchState.value.countdownNumber)

        // Ticking less than 0.9s stays at 3
        arbitro.tick(0.50f)
        assertEquals(3, arbitro.matchState.value.countdownNumber)

        // Crossing 0.9s moves to 2
        arbitro.tick(0.45f)
        assertEquals(2, arbitro.matchState.value.countdownNumber)

        // Next 0.9s step moves to 1
        arbitro.tick(0.91f)
        assertEquals(1, arbitro.matchState.value.countdownNumber)

        // Next 0.9s step moves to 0 and starts PLAYING
        arbitro.tick(0.91f)
        assertEquals(0, arbitro.matchState.value.countdownNumber)
        assertEquals(MatchStatus.PLAYING, arbitro.matchState.value.status)
    }

    @Test
    fun testDeterministicGoalPauseProgression_resetsAndCountsDownViaTick() {
        val audio = TestAudioPort()
        val dims = TableDimensions()
        val arbitro = ArbitroVirtual(dims, mode = GameMode.SOLO, targetScore = 5, audioPort = audio)
        arbitro.setCountdown(0) // PLAYING

        // Simulate goal for Player 1
        arbitro.engine.puck.x = 500f
        arbitro.engine.puck.y = 20f
        arbitro.engine.puck.vx = 0f
        arbitro.engine.puck.vy = -1200f
        arbitro.tick(0.05f)

        assertEquals(MatchStatus.GOAL_PAUSE, arbitro.matchState.value.status)
        assertEquals(1, arbitro.matchState.value.scorePlayer1)

        // Ticking halfway through goal pause (0.6s)
        arbitro.tick(0.60f)
        assertEquals("Still in goal pause", MatchStatus.GOAL_PAUSE, arbitro.matchState.value.status)

        // Ticking past 1.2s threshold automatically starts countdown and resets positions
        arbitro.tick(0.65f)
        assertEquals("Should transition to COUNTDOWN", MatchStatus.COUNTDOWN, arbitro.matchState.value.status)
        assertEquals(3, arbitro.matchState.value.countdownNumber)
    }

    @Test
    fun testDeterministicTimer_advancesViaTick() {
        val dims = TableDimensions()
        val arbitro = ArbitroVirtual(dims, mode = GameMode.SOLO)
        arbitro.setCountdown(0) // PLAYING

        assertEquals(0, arbitro.matchState.value.remainingTimeSeconds)

        // Ticking 1.05s increments remaining seconds
        arbitro.tick(1.05f)
        assertEquals(1, arbitro.matchState.value.remainingTimeSeconds)

        arbitro.tick(2.10f)
        assertEquals(3, arbitro.matchState.value.remainingTimeSeconds)
    }

    @Test
    fun testDeadPuckRule_resetsPuckAfterTimeout() {
        val audio = TestAudioPort()
        val dims = TableDimensions()
        val arbitro = ArbitroVirtual(dims, mode = GameMode.SOLO, audioPort = audio)
        arbitro.setCountdown(0) // PLAYING

        // Coloca o disco parado no canto
        arbitro.engine.puck.x = 900f
        arbitro.engine.puck.y = 100f
        arbitro.engine.puck.vx = 0f
        arbitro.engine.puck.vy = 0f

        // Avança 2.0 segundos: ainda não atingiu os 3.5s
        arbitro.tick(2.0f)
        assertEquals(900f, arbitro.frames.value.puckX, 1.0f)

        // Avança mais 1.6s (total 3.6s > 3.5s): no Modo Fases, Disco Preso no lado da IA cede Saque de Início ao jogador humano
        arbitro.tick(1.6f)
        assertEquals(dims.width / 2f, arbitro.frames.value.puckX, 1.0f)
        assertEquals(dims.centerLineY + 300f, arbitro.frames.value.puckY, 1.0f)
    }

    @Test
    fun advanceRealTime_runsCountdownIn2700MsEvenOn33HzDevice() {
        val dims = TableDimensions()
        val audio = TestAudioPort()
        val arbitro = ArbitroVirtual(dims, mode = GameMode.SOLO, audioPort = audio)
        assertEquals(MatchStatus.COUNTDOWN, arbitro.currentSnapshot().matchStatus)
        assertEquals(3, arbitro.currentSnapshot().countdownNumber)

        var totalSteps = 0
        // Simula dispositivo lento (~33 Hz / ~30ms por frame): 91 iterações = 2,73s reais
        repeat(91) { index ->
            totalSteps += arbitro.advanceRealTime(0.030f)
            if (index == 30) {
                assertEquals(2, arbitro.currentSnapshot().countdownNumber)
            }
            if (index == 60) {
                assertEquals(1, arbitro.currentSnapshot().countdownNumber)
            }
        }

        assertTrue("Expected ~327 fixed steps for 2.73s, got $totalSteps", totalSteps in 326..328)
        assertEquals(MatchStatus.PLAYING, arbitro.matchState.value.status)
        assertEquals(MatchStatus.PLAYING, arbitro.currentSnapshot().matchStatus)
        assertEquals(0, arbitro.currentSnapshot().countdownNumber)
    }

    @Test
    fun advanceRealTime_puckDisplacementMatchesAcross60HzAnd30HzFrames() {
        val dims = TableDimensions()
        val arbitro60 = ArbitroVirtual(dims, mode = GameMode.SPLIT_SCREEN, audioPort = TestAudioPort()).apply {
            setCountdown(0)
            engine.puck.x = dims.width * 0.25f
            engine.puck.y = dims.centerLineY
            engine.puck.vx = 0f
            engine.puck.vy = 1000f
        }
        val arbitro30 = ArbitroVirtual(dims, mode = GameMode.SPLIT_SCREEN, audioPort = TestAudioPort()).apply {
            setCountdown(0)
            engine.puck.x = dims.width * 0.25f
            engine.puck.y = dims.centerLineY
            engine.puck.vx = 0f
            engine.puck.vy = 1000f
        }

        var steps60 = 0
        repeat(24) { steps60 += arbitro60.advanceRealTime(1f / 60f) }

        var steps30 = 0
        repeat(12) { steps30 += arbitro30.advanceRealTime(1f / 30f) }

        val snap60 = arbitro60.currentSnapshot()
        val snap30 = arbitro30.currentSnapshot()
        assertTrue(kotlin.math.abs(steps60 - steps30) <= 1)
        assertTrue("Puck must travel real-time distance at 30 Hz", snap30.puckY > dims.centerLineY + 320f)
        assertEquals(snap60.puckY, snap30.puckY, 12.0f)
        assertEquals(snap60.puckVy, snap30.puckVy, 12.0f)
    }

    @Test
    fun advanceRealTime_clampsSpiralOfDeathAndResetsAccumulatorOnPauseAndRestart() {
        val dims = TableDimensions()
        val arbitro = ArbitroVirtual(dims, mode = GameMode.SPLIT_SCREEN, audioPort = TestAudioPort())
        arbitro.setCountdown(0)

        assertEquals(0, arbitro.advanceRealTime(0f))
        assertEquals(0, arbitro.advanceRealTime(-0.5f))

        // Trava longa (2,5s) é limitada pelo teto MAX_FRAME_ELAPSED_SEC / MAX_CATCHUP_STEPS
        val stepsAfterHugeStall = arbitro.advanceRealTime(2.5f)
        assertTrue(
            "Expected catchup steps in 11..${ArbitroVirtual.MAX_CATCHUP_STEPS}, got $stepsAfterHugeStall",
            stepsAfterHugeStall in 11..ArbitroVirtual.MAX_CATCHUP_STEPS
        )

        // Reinicia acumulador limpo e valida descarte de resto sub-step em pause()/resume() e restart()
        arbitro.restart()
        arbitro.setCountdown(0)
        assertEquals(0, arbitro.advanceRealTime(0.005f))
        arbitro.pause()
        arbitro.resume()
        assertEquals(0, arbitro.advanceRealTime(0.005f))

        arbitro.restart()
        arbitro.setCountdown(0)
        assertEquals(0, arbitro.advanceRealTime(0.005f))
    }

    @Test
    fun recordPendingCollision_prioritizesPaddleHitOverWallHitAcrossMultiStepBatch() {
        val dims = TableDimensions()
        val arbitro = ArbitroVirtual(dims, mode = GameMode.SPLIT_SCREEN, audioPort = TestAudioPort())
        arbitro.setCountdown(0)

        arbitro.engine.onCollisionListener?.invoke(dev.vmellos.airhockey.feature.physics.CollisionEvent.PADDLE_HIT_P1, 0.90f)
        arbitro.engine.onCollisionListener?.invoke(dev.vmellos.airhockey.feature.physics.CollisionEvent.WALL_HIT, 0.40f)

        val (eventCode, intensity) = arbitro.consumeCollisionTelemetry()
        assertEquals(1.toByte(), eventCode)
        assertEquals(0.90f, intensity, 1e-4f)

        val (clearedCode, clearedIntensity) = arbitro.consumeCollisionTelemetry()
        assertEquals(0.toByte(), clearedCode)
        assertEquals(0f, clearedIntensity, 1e-4f)

        arbitro.engine.onCollisionListener?.invoke(dev.vmellos.airhockey.feature.physics.CollisionEvent.PADDLE_HIT_P1, 0.40f)
        arbitro.engine.onCollisionListener?.invoke(dev.vmellos.airhockey.feature.physics.CollisionEvent.PADDLE_HIT_P2, 0.85f)
        val (paddleCode, paddleIntensity) = arbitro.consumeCollisionTelemetry()
        assertEquals(2.toByte(), paddleCode)
        assertEquals(0.85f, paddleIntensity, 1e-4f)

        arbitro.engine.onCollisionListener?.invoke(dev.vmellos.airhockey.feature.physics.CollisionEvent.WALL_HIT, 0.35f)
        val (wallCode, _) = arbitro.consumeCollisionTelemetry()
        assertEquals(3.toByte(), wallCode)
    }

    @Test
    fun deadPuckRule_doesNotResetPuckStoppedInHumanHalfDuringStageMode() {
        val dims = TableDimensions()
        val arbitro = ArbitroVirtual(dims, mode = GameMode.SOLO, audioPort = TestAudioPort())
        arbitro.setCountdown(0)

        val humanHalfX = 300f
        val humanHalfY = dims.centerLineY + dims.puckRadius + 150f
        arbitro.engine.puck.x = humanHalfX
        arbitro.engine.puck.y = humanHalfY
        arbitro.engine.puck.vx = 0f
        arbitro.engine.puck.vy = 0f

        // Avança 6 segundos em passos pequenos: disco parado no campo humano nunca é movido para a IA
        repeat(60) { arbitro.tick(0.10f) }

        val snap = arbitro.currentSnapshot()
        assertEquals(humanHalfX, snap.puckX, 1.0f)
        assertEquals(humanHalfY, snap.puckY, 1.0f)
    }

    @Test
    fun deadPuckRule_doesNotResetInSplitScreenHumanMatch() {
        val dims = TableDimensions()
        val arbitro = ArbitroVirtual(dims, mode = GameMode.SPLIT_SCREEN, audioPort = TestAudioPort())
        arbitro.setCountdown(0)

        arbitro.engine.puck.x = 250f
        arbitro.engine.puck.y = 400f
        arbitro.engine.puck.vx = 0f
        arbitro.engine.puck.vy = 0f

        repeat(60) { arbitro.tick(0.10f) }

        val snap = arbitro.currentSnapshot()
        assertEquals(250f, snap.puckX, 1.0f)
        assertEquals(400f, snap.puckY, 1.0f)
    }

    @Test
    fun deadPuckRule_resetsInAiVsAiMatchToNeutralCenter() {
        val dims = TableDimensions()
        val arbitro = ArbitroVirtual(dims, mode = GameMode.AI_VS_AI_LOCAL, audioPort = TestAudioPort())
        arbitro.setCountdown(0)

        // Disco travado em uma metade controlada por bot em IA x IA reinicia no centro neutro após 3,5s
        arbitro.engine.puck.x = 880f
        arbitro.engine.puck.y = 1880f
        arbitro.engine.puck.vx = 0f
        arbitro.engine.puck.vy = 0f
        // Mantém ambos os bots longe do disco durante o teste simulando bloqueio
        arbitro.engine.player1Mallet.x = 200f
        arbitro.engine.player1Mallet.y = 1500f

        arbitro.tick(3.6f)
        assertEquals(dims.width / 2f, arbitro.currentSnapshot().puckX, 1.0f)
        assertEquals(dims.centerLineY, arbitro.currentSnapshot().puckY, 1.0f)
    }

    @Test
    fun deadPuckRule_resetsExtendedBezelGapDeadPuckOutsideGap() {
        val extDims = TableDimensions(isExtendedMode = true, bezelGapUnits = 200f)
        val arbitro = ArbitroVirtual(extDims, mode = GameMode.EXTENDED_HOST, audioPort = TestAudioPort())
        arbitro.setCountdown(0)

        // 1. Disco na metade humana inferior não sofre reset após 6s
        val playableX = 280f
        val playableY = extDims.guestHalfTopY + 300f
        arbitro.engine.puck.x = playableX
        arbitro.engine.puck.y = playableY
        arbitro.engine.puck.vx = 0f
        arbitro.engine.puck.vy = 0f
        repeat(60) { arbitro.tick(0.10f) }
        assertEquals(playableX, arbitro.currentSnapshot().puckX, 1.0f)
        assertEquals(playableY, arbitro.currentSnapshot().puckY, 1.0f)

        // 2. Disco imóvel na zona inalcançável do Vão Físico (lastHitBy = 1 -> cede Saque de Início ao P2 fora do vão)
        arbitro.engine.puck.x = 500f
        arbitro.engine.puck.y = extDims.hostHalfBottomY + 100f
        arbitro.engine.puck.vx = 0f
        arbitro.engine.puck.vy = 0f
        arbitro.engine.puck.lastHitBy = 1

        arbitro.tick(3.6f)
        val snap = arbitro.currentSnapshot()
        assertEquals(extDims.width / 2f, snap.puckX, 1.0f)
        assertEquals(extDims.hostHalfBottomY - 400f, snap.puckY, 1.0f)
    }

    @Test
    fun collectVisualBurstsSince_returnsMonotonicBurstsOnlyOncePerSeq() {
        val dims = TableDimensions()
        val arbitro = ArbitroVirtual(dims, mode = GameMode.SPLIT_SCREEN, audioPort = TestAudioPort())
        arbitro.setCountdown(0)

        arbitro.engine.onVisualCollisionListener?.invoke(120f, 340f, dev.vmellos.airhockey.feature.physics.CollisionEvent.WALL_HIT)
        arbitro.engine.onVisualCollisionListener?.invoke(500f, 1500f, dev.vmellos.airhockey.feature.physics.CollisionEvent.PADDLE_HIT_P1)

        val collected = mutableListOf<dev.vmellos.airhockey.feature.physics.VisualCollisionBurst>()
        val seqAfterFirst = arbitro.collectVisualBurstsSince(0L, collected)

        assertEquals(2, collected.size)
        assertEquals(120f, collected[0].tableX, 0.01f)
        assertEquals(dev.vmellos.airhockey.feature.physics.CollisionEvent.PADDLE_HIT_P1, collected[1].event)
        assertTrue(seqAfterFirst > collected[0].seq)

        collected.clear()
        val seqAfterNoop = arbitro.collectVisualBurstsSince(seqAfterFirst, collected)
        assertEquals(seqAfterFirst, seqAfterNoop)
        assertTrue(collected.isEmpty())

        arbitro.restart()
        collected.clear()
        assertEquals(seqAfterFirst, arbitro.collectVisualBurstsSince(seqAfterFirst, collected))
        assertTrue(collected.isEmpty())
    }

    @Test
    fun guestExtrapolation_advancesPuckAndReflectsWallsBetweenTelemetryPackets() {
        val dims = TableDimensions()
        val guestArbitro = ArbitroVirtual(dims, mode = GameMode.CONNECTED_GUEST, audioPort = TestAudioPort())
        guestArbitro.setCountdown(0) // PLAYING

        guestArbitro.applyRemoteTelemetry(
            puckX = 500f,
            puckY = 1000f,
            puckVx = 0f,
            puckVy = -2400f,
            hostMalletX = 500f,
            hostMalletY = 1750f,
            guestMalletX = 500f,
            guestMalletY = 250f
        )

        repeat(6) {
            guestArbitro.tick(ArbitroVirtual.FIXED_DT_SEC)
        }

        val snapAfterTicks = guestArbitro.currentSnapshot()
        assertTrue(
            "Guest must extrapolate puck movement between network packets (expected y < 920, got ${snapAfterTicks.puckY})",
            snapAfterTicks.puckY < 920f
        )

        // Verifica que colisão lateral refletida no Convidado mantém o disco na arena sem alterar placar
        guestArbitro.applyRemoteTelemetry(
            puckX = dims.width - dims.puckRadius - 2f,
            puckY = 1000f,
            puckVx = 1800f,
            puckVy = 0f,
            hostMalletX = 500f,
            hostMalletY = 1750f,
            guestMalletX = 500f,
            guestMalletY = 250f
        )
        guestArbitro.tick(ArbitroVirtual.FIXED_DT_SEC)
        assertTrue("Lateral wall bounce must invert puckVx", guestArbitro.currentSnapshot().puckVx < 0f)
        assertEquals(0, guestArbitro.currentSnapshot().scorePlayer1)
    }

    @Test
    fun guestLocalTouch_remainsSovereignOverStaleTelemetryDuringActiveDrag() {
        val dims = TableDimensions()
        val guestArbitro = ArbitroVirtual(dims, mode = GameMode.CONNECTED_GUEST, audioPort = TestAudioPort())
        guestArbitro.setCountdown(0) // PLAYING

        // Jogador P2 arrasta rapidamente o rebatedor local para (260, 420)
        val clamped = guestArbitro.onTouchGuestGlobal(260f, 420f)
        assertTrue(clamped != null)
        assertEquals(260f, guestArbitro.currentSnapshot().player2MalletX, 1f)
        assertEquals(420f, guestArbitro.currentSnapshot().player2MalletY, 1f)

        // Telemetria atrasada do Host chega indicando posição antiga de 1 RTT atrás (720, 220)
        guestArbitro.applyRemoteTelemetry(
            puckX = 500f,
            puckY = 1000f,
            puckVx = 0f,
            puckVy = 0f,
            hostMalletX = 500f,
            hostMalletY = 1750f,
            guestMalletX = 720f,
            guestMalletY = 220f
        )

        assertEquals(
            "Active local touch must stay sovereign on X without rubber-banding",
            260f,
            guestArbitro.currentSnapshot().player2MalletX,
            1f
        )
        assertEquals(
            "Active local touch must stay sovereign on Y without rubber-banding",
            420f,
            guestArbitro.currentSnapshot().player2MalletY,
            1f
        )
    }
}
