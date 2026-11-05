package dev.vmellos.airhockey

import dev.vmellos.airhockey.core.preferences.CameraZoom3D
import dev.vmellos.airhockey.feature.game.ui.getCameraPreset
import dev.vmellos.airhockey.feature.game.ui.screenToTable3D
import dev.vmellos.airhockey.feature.game.ui.tableToScreen3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AirHockey3DSceneTest {

    @Test
    fun cameraPresets_haveCalibratedPositionsAndRotations() {
        val close = getCameraPreset(CameraZoom3D.CLOSE)
        val standard = getCameraPreset(CameraZoom3D.STANDARD)
        val wide = getCameraPreset(CameraZoom3D.WIDE)

        // Close preset is lower and closer
        assertTrue(close.position.y < standard.position.y)
        assertTrue(close.position.z < standard.position.z)

        // Wide preset is higher and further
        assertTrue(wide.position.y > standard.position.y)
        assertTrue(wide.position.z > standard.position.z)

        // All camera pitch angles look down toward table
        assertTrue(close.rotation.x < 0f)
        assertTrue(standard.rotation.x < 0f)
        assertTrue(wide.rotation.x < 0f)
    }

    @Test
    fun screenToTable3D_mapsPlayer1TouchToPlayer1Half() {
        val standard = getCameraPreset(CameraZoom3D.STANDARD)
        val screenW = 1080f
        val screenH = 2400f

        // Touch near bottom center of the screen (where Player 1 mallet is drawn)
        val touchX = 540f
        val touchY = 2000f

        val tablePt = screenToTable3D(
            screenX = touchX,
            screenY = touchY,
            w = screenW,
            h = screenH,
            camX = standard.position.x,
            camY = standard.position.y,
            camZ = standard.position.z,
            camPitchDeg = standard.rotation.x
        )

        assertNotNull("Ponto projetado não deve ser nulo", tablePt)
        tablePt?.let {
            // No centro horizontal da mesa
            assertEquals(500f, it.x, 10f)
            // No meio-campo do Jogador 1 (Y > 1000f até 2000f)
            assertTrue("P1 touch should be in host half (Y > 1000)", it.y > 1000f)
            assertTrue("P1 touch should be within defense bounds", it.y in 1400f..1950f)
        }
    }

    @Test
    fun screenToTable3D_mapsCenterScreenNearCenterLine() {
        val standard = getCameraPreset(CameraZoom3D.STANDARD)
        val screenW = 1080f
        val screenH = 2400f

        // Optical center of screen
        val tablePt = screenToTable3D(
            screenX = 540f,
            screenY = 1200f,
            w = screenW,
            h = screenH,
            camX = standard.position.x,
            camY = standard.position.y,
            camZ = standard.position.z,
            camPitchDeg = standard.rotation.x
        )

        assertNotNull(tablePt)
        tablePt?.let {
            assertEquals(500f, it.x, 5f)
            // Optical center of -46° camera lands near table center
            assertTrue("Center screen should be within reasonable proximity of center line", it.y in 500f..1200f)
        }
    }

    @Test
    fun screenToTable3D_handlesInvalidInputsGracefully() {
        val standard = getCameraPreset(CameraZoom3D.STANDARD)

        val zeroW = screenToTable3D(540f, 1200f, 0f, 2400f, 0f, standard.position.y, standard.position.z, standard.rotation.x)
        assertNull(zeroW)

        val zeroH = screenToTable3D(540f, 1200f, 1080f, 0f, 0f, standard.position.y, standard.position.z, standard.rotation.x)
        assertNull(zeroH)
    }

    @Test
    fun tableToScreen3D_projectsCenterTableNearHorizontalCenter() {
        val standard = getCameraPreset(CameraZoom3D.STANDARD)
        val screenW = 1080f
        val screenH = 2400f

        val screenPt = tableToScreen3D(
            tableX = 500f,
            tableY = 1000f,
            w = screenW,
            h = screenH,
            camX = standard.position.x,
            camY = standard.position.y,
            camZ = standard.position.z,
            camPitchDeg = standard.rotation.x,
            elevationY = 0f
        )

        assertNotNull("Ponto de tela projetado não deve ser nulo", screenPt)
        screenPt?.let {
            assertEquals("O centro da mesa X=500 deve projetar no centro horizontal da tela", 540f, it.x, 2f)
            assertTrue("O centro da mesa Y=1000 deve projetar em Y visível na tela", it.y in 800f..1800f)
        }
    }

    @Test
    fun tableToScreen3D_and_screenToTable3D_areMathematicallyReversible() {
        val standard = getCameraPreset(CameraZoom3D.STANDARD)
        val screenW = 1080f
        val screenH = 2400f

        val originalX = 500f
        val originalY = 1600f

        val screenPt = tableToScreen3D(
            tableX = originalX,
            tableY = originalY,
            w = screenW,
            h = screenH,
            camX = standard.position.x,
            camY = standard.position.y,
            camZ = standard.position.z,
            camPitchDeg = standard.rotation.x,
            elevationY = 0f
        )
        assertNotNull(screenPt)

        val unprojectedTable = screenToTable3D(
            screenX = screenPt!!.x,
            screenY = screenPt.y,
            w = screenW,
            h = screenH,
            camX = standard.position.x,
            camY = standard.position.y,
            camZ = standard.position.z,
            camPitchDeg = standard.rotation.x
        )
        assertNotNull(unprojectedTable)
        assertEquals(originalX, unprojectedTable!!.x, 2f)
        assertEquals(originalY, unprojectedTable.y, 2f)
    }

    @Test
    fun tableToScreen3D_handlesInvalidInputsGracefully() {
        val standard = getCameraPreset(CameraZoom3D.STANDARD)
        assertNull(tableToScreen3D(500f, 1000f, 0f, 2400f, 0f, standard.position.y, standard.position.z, standard.rotation.x))
        assertNull(tableToScreen3D(500f, 1000f, 1080f, 0f, 0f, standard.position.y, standard.position.z, standard.rotation.x))
    }
}
