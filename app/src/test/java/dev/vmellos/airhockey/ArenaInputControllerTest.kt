package dev.vmellos.airhockey

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.ui.ArenaInputController
import dev.vmellos.airhockey.feature.game.ui.PlayerSlot
import dev.vmellos.airhockey.feature.physics.MatchFrameSnapshot
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

    private fun snapshotAt(
        dimensions: TableDimensions = TableDimensions(),
        p1X: Float = 500f,
        p1Y: Float = 1600f,
        p2X: Float = 500f,
        p2Y: Float = 400f
    ): MatchFrameSnapshot {
        return MatchFrameSnapshot(
            puckX = dimensions.width / 2f,
            puckY = dimensions.centerLineY,
            puckVx = 0f,
            puckVy = 0f,
            player1MalletX = p1X,
            player1MalletY = p1Y,
            player2MalletX = p2X,
            player2MalletY = p2Y,
            dimensions = dimensions
        )
    }

    @Test
    fun testGrabRadiusCapturesMalletWithin2Point2x() {
        val controller = ArenaInputController(grabRadiusMultiplier = 2.2f)
        val dimensions = TableDimensions() // malletRadius = 75f -> 2.2x = 165f
        val frame = snapshotAt(dimensions = dimensions)

        var p1TouchedX = 0f
        var p1TouchedY = 0f

        // Toque dentro do raio de captura (distância 50f <= 165f)
        val touchWithin = listOf(createMockChange(1L, 500f, 1550f, pressed = true))
        controller.processPointerEvents(
            changes = touchWithin,
            frame = frame,
            mode = GameMode.SOLO,
            mapper = { x, y -> TablePoint(x, y) },
            sink = { slot, x, y ->
                if (slot == PlayerSlot.P1) {
                    p1TouchedX = x
                    p1TouchedY = y
                }
            },
            touchOffset = 110f
        )

        assertEquals(PointerId(1L), controller.activePointerP1)
        assertEquals(500f, p1TouchedX, 0.01f)
        assertEquals(1550f - 110f, p1TouchedY, 0.01f) // targetY com offset de 110f
    }

    @Test
    fun testTouchOutsideGrabRadiusIsIgnored() {
        val controller = ArenaInputController(grabRadiusMultiplier = 2.2f)
        val dimensions = TableDimensions()
        val frame = snapshotAt(dimensions = dimensions)

        var touched = false

        // Toque longe do rebatedor (distância 300f > 165f)
        val touchFar = listOf(createMockChange(2L, 200f, 1600f, pressed = true))
        controller.processPointerEvents(
            changes = touchFar,
            frame = frame,
            mode = GameMode.SOLO,
            mapper = { x, y -> TablePoint(x, y) },
            sink = { _, _, _ -> touched = true },
            touchOffset = 110f
        )

        assertNull("Ponteiro fora do raio de captura não deve ser adquirido", controller.activePointerP1)
        assertEquals(false, touched)
    }

    @Test
    fun testSpectatorModeDoesNotCaptureTouches() {
        val controller = ArenaInputController()
        val frame = snapshotAt()

        var touched = false
        val touch = listOf(createMockChange(3L, 500f, 1600f, pressed = true))

        controller.processPointerEvents(
            changes = touch,
            frame = frame,
            mode = GameMode.AI_VS_AI_LOCAL,
            mapper = { x, y -> TablePoint(x, y) },
            sink = { _, _, _ -> touched = true },
            touchOffset = 110f
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

    @Test
    fun testSplitScreenPointerRetainsOwnerWhenCrossingCenterLine() {
        val controller = ArenaInputController()
        val dimensions = TableDimensions()
        val frame = snapshotAt(dimensions = dimensions)

        var lastSlot: PlayerSlot? = null
        var lastY = 0f

        controller.processPointerEvents(
            changes = listOf(createMockChange(1L, 500f, 1600f, pressed = true)),
            frame = frame,
            mode = GameMode.SPLIT_SCREEN,
            mapper = { x, y -> TablePoint(x, y) },
            sink = { slot, _, y -> lastSlot = slot; lastY = y },
            touchOffset = 110f
        )
        assertEquals(PointerId(1L), controller.activePointerP1)
        assertNull(controller.activePointerP2)
        assertEquals(PlayerSlot.P1, lastSlot)

        // Arrasta o ponteiro de P1 para além da linha central (y = 800f < 1000f)
        controller.processPointerEvents(
            changes = listOf(createMockChange(1L, 500f, 800f, pressed = true)),
            frame = frame,
            mode = GameMode.SPLIT_SCREEN,
            mapper = { x, y -> TablePoint(x, y) },
            sink = { slot, _, y -> lastSlot = slot; lastY = y },
            touchOffset = 110f
        )
        assertEquals(PointerId(1L), controller.activePointerP1)
        assertNull(controller.activePointerP2)
        assertEquals(PlayerSlot.P1, lastSlot)
        assertEquals(800f - 110f, lastY, 0.01f)
    }

    @Test
    fun testReleaseClearsActivePointerEvenWhenMapperReturnsNull() {
        val controller = ArenaInputController()
        val frame = snapshotAt()

        controller.processPointerEvents(
            changes = listOf(createMockChange(5L, 500f, 1600f, pressed = true)),
            frame = frame,
            mode = GameMode.SOLO,
            mapper = { x, y -> TablePoint(x, y) },
            sink = { _, _, _ -> }
        )
        assertEquals(PointerId(5L), controller.activePointerP1)

        // Outro dedo segue pressionado enquanto o ponteiro 5 solta fora da área válida da mesa
        controller.processPointerEvents(
            changes = listOf(
                createMockChange(5L, -50f, -50f, pressed = false),
                createMockChange(9L, 100f, 100f, pressed = true)
            ),
            frame = frame,
            mode = GameMode.SOLO,
            mapper = { x, y -> if (x < 0f) null else TablePoint(x, y) },
            sink = { _, _, _ -> }
        )
        assertNull("Soltura fora da área mapeável ainda deve liberar o ponteiro ativo", controller.activePointerP1)
    }

    @Test
    fun testGuestModeRoutesToGuestSlotWithPositiveTouchOffset() {
        val controller = ArenaInputController()
        val frame = snapshotAt(p2X = 500f, p2Y = 400f)

        var dispatchedSlot: PlayerSlot? = null
        var dispatchedY = 0f

        controller.processPointerEvents(
            changes = listOf(createMockChange(7L, 500f, 400f, pressed = true)),
            frame = frame,
            mode = GameMode.CONNECTED_GUEST,
            mapper = { x, y -> TablePoint(x, y) },
            sink = { slot, _, y -> dispatchedSlot = slot; dispatchedY = y },
            touchOffset = 110f
        )

        assertEquals(PlayerSlot.GUEST, dispatchedSlot)
        assertEquals(400f + 110f, dispatchedY, 0.01f)
    }
}
