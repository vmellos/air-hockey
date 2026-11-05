package dev.vmellos.airhockey

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.ui.ArenaInputController
import dev.vmellos.airhockey.feature.physics.TableDimensions
import dev.vmellos.airhockey.feature.physics.TablePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArenaInputControllerTest {

    private fun createMockChange(id: Long, x: Float, y: Float, pressed: Boolean): PointerInputChange {
        return PointerInputChange(
            id = PointerId(id),
            uptimeMillis = 0L,
            position = Offset(x, y),
            pressed = pressed,
            pressure = 1f,
            previousUptimeMillis = 0L,
            previousPosition = Offset(x, y),
            previousPressed = false,
            isInitiallyConsumed = false
        )
    }

    @Test
    fun testGrabRadiusCapturesMalletWithin2Point2x() {
        val controller = ArenaInputController(grabRadiusMultiplier = 2.2f)
        val dimensions = TableDimensions()
        val malletRadius = 75f // 2.2x = 165f

        var p1TouchedX = 0f
        var p1TouchedY = 0f

        // Toque dentro do raio de captura (distância 100f <= 165f)
        val touchWithin = listOf(createMockChange(1L, 500f, 1550f, pressed = true))
        controller.processPointerEvents(
            changes = touchWithin,
            dimensions = dimensions,
            p1MalletX = 500f,
            p1MalletY = 1600f,
            p2MalletX = 500f,
            p2MalletY = 400f,
            malletRadius = malletRadius,
            mode = GameMode.SOLO,
            touchOffset = 110f,
            isSpectator = false,
            screenToTable = { x, y -> TablePoint(x, y) },
            onTouchP1 = { x, y -> p1TouchedX = x; p1TouchedY = y },
            onTouchP2 = { _, _ -> },
            onTouchGuest = { _, _ -> }
        )

        assertEquals(PointerId(1L), controller.activePointerP1)
        assertEquals(500f, p1TouchedX, 0.01f)
        assertEquals(1550f - 110f, p1TouchedY, 0.01f) // targetY com offset de 110f
    }

    @Test
    fun testTouchOutsideGrabRadiusIsIgnored() {
        val controller = ArenaInputController(grabRadiusMultiplier = 2.2f)
        val dimensions = TableDimensions()
        val malletRadius = 75f // 2.2x = 165f

        var touched = false

        // Toque longe do rebatedor (distância 300f > 165f)
        val touchFar = listOf(createMockChange(2L, 200f, 1600f, pressed = true))
        controller.processPointerEvents(
            changes = touchFar,
            dimensions = dimensions,
            p1MalletX = 500f,
            p1MalletY = 1600f,
            p2MalletX = 500f,
            p2MalletY = 400f,
            malletRadius = malletRadius,
            mode = GameMode.SOLO,
            touchOffset = 110f,
            isSpectator = false,
            screenToTable = { x, y -> TablePoint(x, y) },
            onTouchP1 = { _, _ -> touched = true },
            onTouchP2 = { _, _ -> },
            onTouchGuest = { _, _ -> }
        )

        assertNull("Ponteiro fora do raio de captura não deve ser adquirido", controller.activePointerP1)
        assertEquals(false, touched)
    }

    @Test
    fun testSpectatorModeDoesNotCaptureTouches() {
        val controller = ArenaInputController()
        val dimensions = TableDimensions()

        var touched = false
        val touch = listOf(createMockChange(3L, 500f, 1600f, pressed = true))

        controller.processPointerEvents(
            changes = touch,
            dimensions = dimensions,
            p1MalletX = 500f,
            p1MalletY = 1600f,
            p2MalletX = 500f,
            p2MalletY = 400f,
            malletRadius = 75f,
            mode = GameMode.AI_VS_AI_LOCAL,
            touchOffset = 110f,
            isSpectator = true, // Modo Espectador
            screenToTable = { x, y -> TablePoint(x, y) },
            onTouchP1 = { _, _ -> touched = true },
            onTouchP2 = { _, _ -> },
            onTouchGuest = { _, _ -> }
        )

        assertNull(controller.activePointerP1)
        assertEquals(false, touched)
    }

    @Test
    fun testResetPointersClearsTracking() {
        val controller = ArenaInputController()
        controller.activePointerP1 = PointerId(10L)
        controller.activePointerP2 = PointerId(20L)

        controller.resetPointers()

        assertNull(controller.activePointerP1)
        assertNull(controller.activePointerP2)
    }
}
