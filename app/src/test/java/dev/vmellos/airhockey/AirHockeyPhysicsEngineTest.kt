package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.physics.AirHockeyPhysicsEngine
import dev.vmellos.airhockey.feature.physics.CollisionEvent
import dev.vmellos.airhockey.feature.physics.TableDimensions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AirHockeyPhysicsEngineTest {

    private lateinit var dimensions: TableDimensions
    private lateinit var engine: AirHockeyPhysicsEngine

    @Before
    fun setup() {
        dimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = 50f,
            isExtendedMode = false
        )
        engine = AirHockeyPhysicsEngine(dimensions)
    }

    @Test
    fun testInitialPositions() {
        assertEquals(500f, engine.puck.x, 0.1f)
        assertEquals(1000f, engine.puck.y, 0.1f)
        assertEquals(0f, engine.puck.vx, 0.01f)
        assertEquals(0f, engine.puck.vy, 0.01f)
    }

    @Test
    fun testWallCollisionBounce() {
        // Posiciona disco perto da parede direita se movendo rapidamente para a direita
        engine.puck.x = 980f
        engine.puck.y = 500f
        engine.puck.vx = 800f
        engine.puck.vy = 0f

        var wallHitDetected = false
        engine.onCollisionListener = { event, _ ->
            if (event == CollisionEvent.WALL_HIT) {
                wallHitDetected = true
            }
        }

        // Executa tick
        engine.tick(0.05f)

        // Disco deve ter rebatido e invertido a velocidade vx
        assertTrue("Wall hit should have been detected", wallHitDetected)
        assertTrue("Puck vx should be negative after bounce", engine.puck.vx < 0f)
        assertTrue("Puck x should be within table bounds", engine.puck.x <= 1000f - engine.puck.radius)
    }

    @Test
    fun testGoalDetectionPlayer1Score() {
        // Meta em Y <= 0 defendida pelo Player 2 (Topo) -> Gol do Player 1
        engine.puck.x = 500f // Centro da meta
        engine.puck.y = 20f
        engine.puck.vx = 0f
        engine.puck.vy = -1200f

        var goalDetected = false
        engine.onCollisionListener = { event, _ ->
            if (event == CollisionEvent.GOAL_PLAYER1) {
                goalDetected = true
            }
        }

        engine.tick(0.05f)
        assertTrue("Goal for Player 1 should be detected", goalDetected)
    }

    @Test
    fun testGoalDetectionPlayer2Score() {
        // Meta em Y >= totalHeight defendida pelo Player 1 (Base) -> Gol do Player 2
        engine.puck.x = 500f
        engine.puck.y = dimensions.totalHeight - 20f
        engine.puck.vx = 0f
        engine.puck.vy = 1200f

        var goalDetected = false
        engine.onCollisionListener = { event, _ ->
            if (event == CollisionEvent.GOAL_PLAYER2) {
                goalDetected = true
            }
        }

        engine.tick(0.05f)
        assertTrue("Goal for Player 2 should be detected", goalDetected)
    }

    @Test
    fun testPaddleCollisionSeparationAndImpulse() {
        // Coloca rebathedor do Player 1 colidindo com o disco na metade inferior
        engine.puck.x = 500f
        engine.puck.y = 1650f
        engine.player1Mallet.reset(500f, 1700f)
        engine.player1Mallet.targetX = 500f
        engine.player1Mallet.targetY = 1640f // Movendo para cima na direção do disco
        engine.player1Mallet.vx = 0f
        engine.player1Mallet.vy = -500f

        var paddleHit = false
        engine.onCollisionListener = { event, _ ->
            if (event == CollisionEvent.PADDLE_HIT_P1) {
                paddleHit = true
            }
        }

        engine.tick(0.02f)
        assertTrue("Paddle hit should be detected", paddleHit)
        assertTrue("Puck should gain upward velocity", engine.puck.vy < 0f)
        assertEquals(1, engine.puck.lastHitBy)

        // Verifica separação geométrica
        val dx = engine.puck.x - engine.player1Mallet.x
        val dy = engine.puck.y - engine.player1Mallet.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val minDist = engine.puck.radius + engine.player1Mallet.radius
        assertTrue("Puck and mallet must not overlap after collision", dist >= minDist - 0.5f)
    }

    @Test
    fun testExtendedModeResetPositionsServesCorrectly() {
        val extDimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = 40f,
            isExtendedMode = true
        )
        val extEngine = AirHockeyPhysicsEngine(extDimensions)

        // When P1 scores (scorer = 1), P2 serves (top half: hostHalfBottomY - 400f = 1600f)
        extEngine.resetPositions(scorer = 1)
        assertEquals(500f, extEngine.puck.x, 0.1f)
        assertEquals(1600f, extEngine.puck.y, 0.1f)

        // When P2 scores (scorer = 2), P1 serves (bottom half: guestHalfTopY + 400f = 2040 + 400 = 2440f)
        extEngine.resetPositions(scorer = 2)
        assertEquals(500f, extEngine.puck.x, 0.1f)
        assertEquals(2440f, extEngine.puck.y, 0.1f)
    }

    @Test
    fun testCountdownMalletCannotPenetratePuck() {
        // Disco estacionado no centro (500, 1000)
        engine.resetPositions()
        val puckX = engine.puck.x
        val puckY = engine.puck.y

        // Tenta mover o rebatedor do Player 1 diretamente para cima do disco
        engine.player1Mallet.targetX = puckX
        engine.player1Mallet.targetY = puckY
        engine.player1Mallet.x = puckX
        engine.player1Mallet.y = puckY

        // Executa a cinemática de contagem regressiva
        engine.tickMalletsOnly(0.05f)

        // O disco deve permanecer exatamente no centro e imóvel
        assertEquals(puckX, engine.puck.x, 0.01f)
        assertEquals(puckY, engine.puck.y, 0.01f)
        assertEquals(0f, engine.puck.vx, 0.01f)
        assertEquals(0f, engine.puck.vy, 0.01f)

        // O rebatedor deve ter sido empurrado para a borda externa do disco
        val dx = engine.player1Mallet.x - engine.puck.x
        val dy = engine.player1Mallet.y - engine.puck.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val minDist = engine.puck.radius + engine.player1Mallet.radius
        assertTrue("Mallet must be blocked at minDist during countdown", dist >= minDist - 0.1f)
    }

    @Test
    fun testCountdownPreparatoryPositionClampedToHalf() {
        engine.resetPositions()
        val r = engine.player1Mallet.radius

        // Tenta mover P1 para o meio-campo de P2 (y = 300f quando centro é 1000f)
        val (p1x, p1y) = engine.clampPreparatoryPosition(500f, 300f, r, isPlayer1 = true)
        assertTrue("P1 deve ficar no meio-campo inferior", p1y >= engine.dimensions.centerLineY + r)

        // Tenta mover P2 para o meio-campo de P1 (y = 1700f)
        val (p2x, p2y) = engine.clampPreparatoryPosition(500f, 1700f, r, isPlayer1 = false)
        assertTrue("P2 deve ficar no meio-campo superior", p2y <= engine.dimensions.centerLineY - r)

        // Verifica que tickMalletsOnly também preserva os limites de meio-campo
        engine.player1Mallet.x = 500f
        engine.player1Mallet.y = 400f // Forçado anômalo
        engine.player1Mallet.targetY = 300f
        engine.tickMalletsOnly(0.016f)
        assertTrue("tickMalletsOnly deve conter P1 em seu meio-campo", engine.player1Mallet.y >= engine.dimensions.centerLineY + r)
    }

    @Test
    fun testAiControllerPlayer1Symmetry() {
        val aiPlayer1 = dev.vmellos.airhockey.feature.ai.AirHockeyAiController(
            difficulty = dev.vmellos.airhockey.core.preferences.AiDifficulty.HARD,
            isPlayer1 = true
        )

        // Disco na metade inferior se aproximando da meta do Player 1
        engine.puck.x = 500f
        engine.puck.y = 1500f
        engine.puck.vx = 0f
        engine.puck.vy = 200f

        aiPlayer1.update(engine, 0.05f)

        // A IA do Player 1 deve se posicionar estritamente na metade inferior
        assertTrue(
            "Player 1 AI target must be in bottom half",
            engine.player1Mallet.targetY > dimensions.centerLineY
        )
        assertTrue(
            "Player 1 AI target must not cross center line",
            engine.player1Mallet.targetY >= dimensions.centerLineY + engine.player1Mallet.radius
        )
    }
}
