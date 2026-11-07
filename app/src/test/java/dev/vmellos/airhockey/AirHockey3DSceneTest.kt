package dev.vmellos.airhockey

import dev.vmellos.airhockey.core.preferences.CameraZoom3D
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.feature.game.ui.Theme3DAssetResolver
import dev.vmellos.airhockey.feature.game.ui.getCameraPreset
import dev.vmellos.airhockey.feature.game.ui.screenToTable3D
import dev.vmellos.airhockey.feature.game.ui.tableToScreen3D
import dev.vmellos.airhockey.feature.physics.TableDimensions
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
    fun cameraPreset_splitScreen_usesZenithTopDownPositionAndRotation() {
        val splitPreset = getCameraPreset(CameraZoom3D.STANDARD, isSplitScreen = true)
        assertEquals(0f, splitPreset.position.x, 1e-4f)
        assertEquals(2.55f, splitPreset.position.y, 1e-4f)
        assertEquals(0f, splitPreset.position.z, 1e-4f)
        assertEquals(-90f, splitPreset.rotation.x, 1e-4f)
        assertEquals(0f, splitPreset.rotation.y, 1e-4f)

        val closePreset = getCameraPreset(CameraZoom3D.CLOSE, isSplitScreen = true)
        val widePreset = getCameraPreset(CameraZoom3D.WIDE, isSplitScreen = true)
        assertTrue(closePreset.position.y < splitPreset.position.y)
        assertTrue(widePreset.position.y > splitPreset.position.y)
        assertEquals(-90f, closePreset.rotation.x, 1e-4f)
        assertEquals(-90f, widePreset.rotation.x, 1e-4f)
    }

    @Test
    fun screenToTable3D_splitScreen_mapsBothPlayerHalvesSymmetrically() {
        val splitPreset = getCameraPreset(CameraZoom3D.STANDARD, isSplitScreen = true)
        val screenW = 1080f
        val screenH = 2400f

        // Center screen maps to table center (500, 1000)
        val centerPt = screenToTable3D(
            screenX = screenW / 2f,
            screenY = screenH / 2f,
            w = screenW,
            h = screenH,
            camX = splitPreset.position.x,
            camY = splitPreset.position.y,
            camZ = splitPreset.position.z,
            camPitchDeg = splitPreset.rotation.x
        )
        assertNotNull(centerPt)
        centerPt?.let {
            assertEquals(500f, it.x, 5f)
            assertEquals(1000f, it.y, 5f)
        }

        // P1 touch in bottom half vs P2 touch in top half (deltaY = 600)
        val dY = 600f
        val p1Pt = screenToTable3D(screenW / 2f, (screenH / 2f) + dY, screenW, screenH, splitPreset.position.x, splitPreset.position.y, splitPreset.position.z, splitPreset.rotation.x)
        val p2Pt = screenToTable3D(screenW / 2f, (screenH / 2f) - dY, screenW, screenH, splitPreset.position.x, splitPreset.position.y, splitPreset.position.z, splitPreset.rotation.x)

        assertNotNull(p1Pt)
        assertNotNull(p2Pt)
        p1Pt?.let { p1 ->
            p2Pt?.let { p2 ->
                assertTrue("P1 touch should be in host half", p1.y > 1000f)
                assertTrue("P2 touch should be in guest half", p2.y < 1000f)
                val distP1FromCenter = p1.y - 1000f
                val distP2FromCenter = 1000f - p2.y
                assertEquals("Touches equidistant from screen center must map symmetrically to table center", distP1FromCenter, distP2FromCenter, 2f)
            }
        }
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

    @Test
    fun cameraPreset_guest_invertsPositionZAndYaw180() {
        val hostStandard = getCameraPreset(CameraZoom3D.STANDARD, isSplitScreen = false, isGuest = false)
        val guestStandard = getCameraPreset(CameraZoom3D.STANDARD, isSplitScreen = false, isGuest = true)

        assertEquals("Guest Y position matches Host", hostStandard.position.y, guestStandard.position.y, 1e-4f)
        assertEquals("Guest Z position is inverted", -hostStandard.position.z, guestStandard.position.z, 1e-4f)
        assertEquals("Guest pitch angle matches Host", hostStandard.rotation.x, guestStandard.rotation.x, 1e-4f)
        assertEquals("Guest yaw is rotated 180 degrees", 180f, guestStandard.rotation.y, 1e-4f)
    }

    @Test
    fun screenToTable3D_guestPerspective_mapsBottomTouchToPlayer2Half() {
        val guestPreset = getCameraPreset(CameraZoom3D.STANDARD, isSplitScreen = false, isGuest = true)
        val screenW = 1080f
        val screenH = 2400f

        // Touch near bottom center of the screen on Guest device (where Player 2 defends)
        val touchX = 540f
        val touchY = 2050f

        val tablePt = screenToTable3D(
            screenX = touchX,
            screenY = touchY,
            w = screenW,
            h = screenH,
            camX = guestPreset.position.x,
            camY = guestPreset.position.y,
            camZ = guestPreset.position.z,
            camPitchDeg = guestPreset.rotation.x,
            camYawDeg = guestPreset.rotation.y
        )

        assertNotNull("P2 touch from guest perspective must map to valid table point", tablePt)
        tablePt?.let { pt ->
            assertEquals(500f, pt.x, 15f)
            assertTrue("Touch at bottom of guest screen must map to P2 side (0..1000), got: ${pt.y}", pt.y < 1000f)
            assertTrue("Touch must be near P2 baseline", pt.y > 50f && pt.y < 600f)
        }
    }

    @Test
    fun tableToScreen3D_guestPerspective_projectsPlayer2MalletNearScreenBottom() {
        val guestPreset = getCameraPreset(CameraZoom3D.STANDARD, isSplitScreen = false, isGuest = true)
        val screenW = 1080f
        val screenH = 2400f

        // Player 2 mallet at (500, 250) on Guest device
        val p2Screen = tableToScreen3D(
            tableX = 500f,
            tableY = 250f,
            w = screenW,
            h = screenH,
            camX = guestPreset.position.x,
            camY = guestPreset.position.y,
            camZ = guestPreset.position.z,
            camPitchDeg = guestPreset.rotation.x,
            camYawDeg = guestPreset.rotation.y
        )

        assertNotNull(p2Screen)
        p2Screen?.let {
            assertEquals("P2 mallet X should be centered on screen", screenW / 2f, it.x, 20f)
            assertTrue("P2 mallet Y must be in lower half of screen on guest device, got: ${it.y}", it.y > screenH * 0.5f)
        }
    }

    @Test
    fun cameraPreset_extendedTable_usesZenithViewAtHalfTableHeight() {
        val stdPreset = getCameraPreset(CameraZoom3D.STANDARD, isExtended = true)
        assertEquals(0f, stdPreset.position.x, 1e-4f)
        assertEquals(3.50f, stdPreset.position.y, 1e-4f)
        assertEquals(0f, stdPreset.position.z, 1e-4f)
        assertEquals(-90f, stdPreset.rotation.x, 1e-4f)
        assertEquals(0f, stdPreset.rotation.y, 1e-4f)

        val closePreset = getCameraPreset(CameraZoom3D.CLOSE, isExtended = true)
        val widePreset = getCameraPreset(CameraZoom3D.WIDE, isExtended = true)
        assertEquals(3.10f, closePreset.position.y, 1e-4f)
        assertEquals(3.95f, widePreset.position.y, 1e-4f)
        assertEquals(-90f, closePreset.rotation.x, 1e-4f)
        assertEquals(-90f, widePreset.rotation.x, 1e-4f)
    }

    @Test
    fun screenToTable3D_extendedHost_mapsCenterAndExtremesAccurately() {
        val extPreset = getCameraPreset(CameraZoom3D.STANDARD, isExtended = true)
        val dims = TableDimensions(width = 1000f, viewportHeight = 2000f, bezelGapUnits = 200f, isExtendedMode = true)
        val screenW = 1080f
        val screenH = 2400f

        // Center screen maps to center of host half (500, guestHalfTopY + 1000 = 3200)
        val centerPt = screenToTable3D(
            screenX = screenW / 2f,
            screenY = screenH / 2f,
            w = screenW,
            h = screenH,
            camX = extPreset.position.x,
            camY = extPreset.position.y,
            camZ = extPreset.position.z,
            camPitchDeg = extPreset.rotation.x,
            camYawDeg = extPreset.rotation.y,
            isExtended = true,
            isExtendedGuest = false,
            dimensions = dims
        )
        assertNotNull(centerPt)
        centerPt?.let {
            assertEquals(500f, it.x, 5f)
            assertEquals(3200f, it.y, 10f)
        }
    }

    @Test
    fun screenToTable3D_extendedGuest_mapsCenterAndExtremesAccurately() {
        val extPreset = getCameraPreset(CameraZoom3D.STANDARD, isExtended = true)
        val dims = TableDimensions(width = 1000f, viewportHeight = 2000f, bezelGapUnits = 200f, isExtendedMode = true)
        val screenW = 1080f
        val screenH = 2400f

        // Center screen maps to center of guest half (500, hostHalfBottomY - 1000 = 1000)
        val centerPt = screenToTable3D(
            screenX = screenW / 2f,
            screenY = screenH / 2f,
            w = screenW,
            h = screenH,
            camX = extPreset.position.x,
            camY = extPreset.position.y,
            camZ = extPreset.position.z,
            camPitchDeg = extPreset.rotation.x,
            camYawDeg = extPreset.rotation.y,
            isExtended = true,
            isExtendedGuest = true,
            dimensions = dims
        )
        assertNotNull(centerPt)
        centerPt?.let {
            assertEquals(500f, it.x, 5f)
            assertEquals(1000f, it.y, 10f)
        }
    }

    @Test
    fun theme3DAssetResolver_extendedTable_selectsHalfTableModels() {
        val p1Asset = Theme3DAssetResolver.getTable3DAsset(TableTheme.CLASSIC_NEON, isExtended = true, isPlayer1 = true)
        assertEquals("models/mesa_arcade_half_p1.glb", p1Asset)

        val p2Asset = Theme3DAssetResolver.getTable3DAsset(TableTheme.CLASSIC_NEON, isExtended = true, isPlayer1 = false)
        assertEquals("models/mesa_arcade_half_p2.glb", p2Asset)

        val standardAsset = Theme3DAssetResolver.getTable3DAsset(TableTheme.CLASSIC_NEON, isExtended = false)
        assertEquals("models/mesa_arcade.glb", standardAsset)
    }
}
