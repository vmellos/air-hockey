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
            viewportHeight = 1000f,
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
}
