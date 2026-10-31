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
            viewportHeight = 1000f,
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
    fun testGoalDetectionPlayer2Score() {
        // Meta em Y <= 0 defendida pelo Player 1 -> Gol do Player 2
        engine.puck.x = 500f // Centro da meta
        engine.puck.y = 20f
        engine.puck.vx = 0f
        engine.puck.vy = -1200f

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
    fun testGoalDetectionPlayer1Score() {
        // Meta em Y >= totalHeight defendida pelo Player 2 -> Gol do Player 1
        engine.puck.x = 500f
        engine.puck.y = dimensions.totalHeight - 20f
        engine.puck.vx = 0f
        engine.puck.vy = 1200f

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
    fun testPaddleCollisionSeparationAndImpulse() {
        // Coloca rebathedor do Player 1 colidindo com o disco
        engine.puck.x = 500f
        engine.puck.y = 350f
        engine.player1Mallet.reset(500f, 300f)
        engine.player1Mallet.targetX = 500f
        engine.player1Mallet.targetY = 360f // Movendo para frente na direção do disco
        engine.player1Mallet.vx = 0f
        engine.player1Mallet.vy = 500f

        var paddleHit = false
        engine.onCollisionListener = { event, _ ->
            if (event == CollisionEvent.PADDLE_HIT_P1) {
                paddleHit = true
            }
        }

        engine.tick(0.02f)
        assertTrue("Paddle hit should be detected", paddleHit)
        assertTrue("Puck should gain downward velocity", engine.puck.vy > 0f)

        // Verifica separação geométrica
        val dx = engine.puck.x - engine.player1Mallet.x
        val dy = engine.puck.y - engine.player1Mallet.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val minDist = engine.puck.radius + engine.player1Mallet.radius
        assertTrue("Puck and mallet must not overlap after collision", dist >= minDist - 0.5f)
    }
}
