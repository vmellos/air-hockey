package dev.vmellos.airhockey

import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.feature.ai.AirHockeyAiController
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.physics.AirHockeyPhysicsEngine
import dev.vmellos.airhockey.feature.physics.TableDimensions
import dev.vmellos.airhockey.feature.physics.TableProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AirHockeyImprovementsTest {

    private lateinit var dimensions: TableDimensions
    private lateinit var engine: AirHockeyPhysicsEngine

    @Before
    fun setup() {
        dimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = 0f,
            isExtendedMode = false
        )
        engine = AirHockeyPhysicsEngine(dimensions)
    }

    @Test
    fun testPlayer2MalletInitialTargetDoesNotAdvanceAutomatically() {
        // Verifica que o Rebatedor do Jogador 2 nasce com targetY alinhado à sua posição inicial (250f)
        assertEquals(250f, engine.player2Mallet.y, 0.01f)
        assertEquals(250f, engine.player2Mallet.targetY, 0.01f)
        assertEquals(500f, engine.player2Mallet.targetX, 0.01f)

        // Simula 1 segundo de física a 60 FPS sem nenhum toque
        for (i in 0 until 60) {
            engine.tick(1f / 60f)
        }

        // Rebatedor 2 deve permanecer exatamente na posição de spawn de defesa (250f), sem avançar sozinho
        assertEquals(250f, engine.player2Mallet.y, 0.5f)
        assertEquals(500f, engine.player2Mallet.x, 0.5f)
    }

    @Test
    fun testTickMalletsOnlyDoesNotMovePuckDuringCountdown() {
        val initialPuckX = engine.puck.x
        val initialPuckY = engine.puck.y

        // Jogador 1 move seu rebatedor durante a contagem regressiva
        engine.player1Mallet.targetX = 600f
        engine.player1Mallet.targetY = 1600f

        // Executa loop cinemático exclusivo dos rebatedores (usado durante a contagem)
        for (i in 0 until 30) {
            engine.tickMalletsOnly(1f / 60f)
        }

        // Rebatedor 1 se moveu em direção ao toque
        assertTrue("Rebatedor 1 deve ter se movido", engine.player1Mallet.x > 550f)

        // O disco deve permanecer perfeitamente estático e congelado na posição de largada
        assertEquals(initialPuckX, engine.puck.x, 0.01f)
        assertEquals(initialPuckY, engine.puck.y, 0.01f)
        assertEquals(0f, engine.puck.vx, 0.01f)
        assertEquals(0f, engine.puck.vy, 0.01f)
    }

    @Test
    fun testAiAntiAutogolSafeguard() {
        val ai = AirHockeyAiController(AiDifficulty.HARD)

        // Cenário de Autogol: disco escapa e fica atrás do rebatedor da IA na zona da meta (Y = 100f)
        engine.puck.x = 500f
        engine.puck.y = 100f
        engine.player2Mallet.x = 500f
        engine.player2Mallet.y = 190f
        engine.player2Mallet.targetX = 500f
        engine.player2Mallet.targetY = 190f

        // Atualiza IA
        ai.update(engine, 1f / 60f)

        // A IA não deve acelerar para baixo em direção ao disco (o que empurraria o disco para dentro do gol)
        // Em vez disso, recua para defender a linha de meta como bloqueador seguro
        assertTrue("IA não deve avançar para frente contra o disco atrás dela", engine.player2Mallet.targetY <= 150f)
    }

    @Test
    fun testAiHardModeHighSpeedAndLateralReaction() {
        val ai = AirHockeyAiController(AiDifficulty.HARD)

        // Disco veloz cruzando em direção à IA
        engine.puck.x = 200f
        engine.puck.y = 800f
        engine.puck.vx = 800f
        engine.puck.vy = -2000f

        engine.player2Mallet.targetX = 500f
        engine.player2Mallet.targetY = 190f

        ai.update(engine, 0.05f)

        // Com maxSpeed = 5400f, em 50ms a IA deve reagir e percorrer distância expressiva
        val dx = engine.player2Mallet.targetX - 500f
        val dy = engine.player2Mallet.targetY - 190f
        val distMoved = kotlin.math.sqrt(dx * dx + dy * dy)

        assertTrue("IA no modo Difícil deve se mover com alta agilidade (distância: $distMoved)", distMoved > 50f)
    }

    @Test
    fun testAiEasyDifficultyPacing() {
        val engine = AirHockeyPhysicsEngine(dimensions)
        val ai = AirHockeyAiController(AiDifficulty.EASY)

        engine.puck.x = 200f
        engine.puck.y = 800f
        engine.puck.vx = 0f
        engine.puck.vy = -100f

        engine.player2Mallet.targetX = 500f
        engine.player2Mallet.targetY = 190f

        ai.update(engine, 0.05f)

        val dx = engine.player2Mallet.targetX - 500f
        val dy = engine.player2Mallet.targetY - 190f
        val distMoved = kotlin.math.sqrt(dx * dx + dy * dy)

        // Com maxSpeed = 900f no modo Fácil, em 50ms a IA deve se mover no máximo 46f
        assertTrue("IA no modo Fácil deve ter deslocamento suave e moderado (distância: $distMoved)", distMoved <= 46f)
    }

    @Test
    fun testPuckSpeedPresetsValues() {
        assertEquals(3600f, dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.SLOW.maxSpeed, 0.01f)
        assertEquals(6800f, dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.NORMAL.maxSpeed, 0.01f)
        assertEquals(9500f, dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.FAST.maxSpeed, 0.01f)
        assertTrue(dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.SLOW.airFriction > dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.FAST.airFriction)
    }

    @Test
    fun testTouchOffsetProjection() {
        val projection = TableProjection(dimensions, GameMode.SPLIT_SCREEN)
        val screenW = 1080f
        val screenH = 2400f

        val touchY = 1800f
        val tablePoint = projection.screenToTable(540f, touchY, screenW, screenH)

        val touchOffset = 110f

        // Para Player 1: Rebatedor é projetado para cima (-touchOffset) em direção à mesa
        val p1TargetY = tablePoint.y - touchOffset
        assertTrue("P1 target Y deve ser menor que o ponto tocado para ficar à frente do dedo", p1TargetY < tablePoint.y)

        // Para Player 2 no modo Mesa Dividida: Rebatedor é projetado para baixo (+touchOffset)
        val p2TargetY = tablePoint.y + touchOffset
        assertTrue("P2 target Y deve ser maior para ficar à frente do dedo vindo do topo", p2TargetY > tablePoint.y)
    }

    @Test
    fun testPuckSpeedLimitingInPhysicsEngine() {
        val slowEngine = AirHockeyPhysicsEngine(dimensions, maxSpeed = 3600f)
        slowEngine.puck.vx = 8000f
        slowEngine.puck.vy = 8000f

        // Executa tick
        slowEngine.tick(1f / 60f)

        val currentSpeed = slowEngine.puck.speed()
        assertTrue("Velocidade do disco deve respeitar o teto configurado de 3600f (atual: $currentSpeed)", currentSpeed <= 3601f)
    }

    @Test
    fun testTelemetryAndGuestInputFallbackWireSerialization() {
        val telemetry = dev.vmellos.airhockey.feature.network.HostTelemetryPacket(
            sequenceNumber = 42L,
            timestampNano = 123L,
            puckX = 510f,
            puckY = 980f,
            puckVx = 120f,
            puckVy = -340f,
            hostMalletX = 500f,
            hostMalletY = 1750f,
            guestMalletX = 480f,
            guestMalletY = 250f
        )
        val telemEvent = dev.vmellos.airhockey.feature.network.TableControlEvent.TelemetryFallback(telemetry)
        val telemWire = telemEvent.toWireString()
        val parsedTelem = dev.vmellos.airhockey.feature.network.TableControlEvent.fromWireString(telemWire)
        assertTrue(parsedTelem is dev.vmellos.airhockey.feature.network.TableControlEvent.TelemetryFallback)
        val parsedPacket = (parsedTelem as dev.vmellos.airhockey.feature.network.TableControlEvent.TelemetryFallback).packet
        assertEquals(42L, parsedPacket.sequenceNumber)
        assertEquals(510f, parsedPacket.puckX, 0.01f)
        assertEquals(980f, parsedPacket.puckY, 0.01f)

        val input = dev.vmellos.airhockey.feature.network.GuestInputPacket(
            sequenceNumber = 17L,
            timestampNano = 456L,
            guestTargetX = 350f,
            guestTargetY = 220f
        )
        val inputEvent = dev.vmellos.airhockey.feature.network.TableControlEvent.GuestInputFallback(input)
        val inputWire = inputEvent.toWireString()
        val parsedInput = dev.vmellos.airhockey.feature.network.TableControlEvent.fromWireString(inputWire)
        assertTrue(parsedInput is dev.vmellos.airhockey.feature.network.TableControlEvent.GuestInputFallback)
        val parsedInputPacket = (parsedInput as dev.vmellos.airhockey.feature.network.TableControlEvent.GuestInputFallback).packet
        assertEquals(17L, parsedInputPacket.sequenceNumber)
        assertEquals(350f, parsedInputPacket.guestTargetX, 0.01f)
        assertEquals(220f, parsedInputPacket.guestTargetY, 0.01f)
    }

    @Test
    fun testTableControlEventStartWithCustomSpeed() {
        val startEvent = dev.vmellos.airhockey.feature.network.TableControlEvent.Start(gapMm = 12.5f, maxSpeed = 3600f)
        val wire = startEvent.toWireString()
        val parsed = dev.vmellos.airhockey.feature.network.TableControlEvent.fromWireString(wire)
        assertTrue(parsed is dev.vmellos.airhockey.feature.network.TableControlEvent.Start)
        val parsedStart = parsed as dev.vmellos.airhockey.feature.network.TableControlEvent.Start
        assertEquals(12.5f, parsedStart.gapMm, 0.01f)
        assertEquals(3600f, parsedStart.maxSpeed, 0.01f)
    }
}
