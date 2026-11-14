package dev.vmellos.airhockey

import dev.vmellos.airhockey.core.preferences.CameraZoom3D
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.TablePerspective
import dev.vmellos.airhockey.feature.game.ui.getCameraPreset
import dev.vmellos.airhockey.feature.game.ui.kit
import dev.vmellos.airhockey.feature.game.ui.screenToTable3D
import dev.vmellos.airhockey.feature.game.ui.tableToScreen3D
import dev.vmellos.airhockey.feature.physics.CameraPose3D
import dev.vmellos.airhockey.feature.physics.Table3DCameraProjection
import dev.vmellos.airhockey.feature.physics.TableDimensions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals(3.35f, stdPreset.position.y, 1e-4f)
        assertEquals(0.3557f, stdPreset.position.z, 1e-3f)
        assertEquals(-90f, stdPreset.rotation.x, 1e-4f)
        assertEquals(0f, stdPreset.rotation.y, 1e-4f)

        val closePreset = getCameraPreset(CameraZoom3D.CLOSE, isExtended = true)
        val widePreset = getCameraPreset(CameraZoom3D.WIDE, isExtended = true)
        assertEquals(3.00f, closePreset.position.y, 1e-4f)
        assertEquals(0.2057f, closePreset.position.z, 1e-3f)
        assertEquals(3.80f, widePreset.position.y, 1e-4f)
        assertEquals(0.5486f, widePreset.position.z, 1e-3f)
        assertEquals(-90f, closePreset.rotation.x, 1e-4f)
        assertEquals(-90f, widePreset.rotation.x, 1e-4f)
    }

    @Test
    fun screenToTable3D_extendedHost_mapsCenterAndExtremesAccurately() {
        val extPreset = getCameraPreset(CameraZoom3D.STANDARD, isExtended = true)
        val dims = TableDimensions(width = 1000f, viewportHeight = 2000f, bezelGapUnits = 200f, isExtendedMode = true)
        val screenW = 1080f
        val screenH = 2400f

        // Top center of screen (screenY = 0) maps precisely to the top center line of host half (500, guestHalfTopY = 2200)
        val topCenterPt = screenToTable3D(
            screenX = screenW / 2f,
            screenY = 0f,
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
        assertNotNull(topCenterPt)
        topCenterPt?.let {
            assertEquals(500f, it.x, 5f)
            assertEquals(2200f, it.y, 5f)
        }

        // Test reversibility: tableToScreen3D for (500, 2200) must project to screen top (540, 0)
        val topScreenPos = tableToScreen3D(
            tableX = 500f,
            tableY = 2200f,
            w = screenW,
            h = screenH,
            camX = extPreset.position.x,
            camY = extPreset.position.y,
            camZ = extPreset.position.z,
            camPitchDeg = extPreset.rotation.x,
            camYawDeg = extPreset.rotation.y,
            elevationY = 0f,
            isExtended = true,
            isExtendedGuest = false,
            dimensions = dims
        )
        assertNotNull(topScreenPos)
        topScreenPos?.let {
            assertEquals(screenW / 2f, it.x, 2f)
            assertEquals(0f, it.y, 2f)
        }
    }

    @Test
    fun screenToTable3D_extendedGuest_mapsCenterAndExtremesAccurately() {
        val extPreset = getCameraPreset(CameraZoom3D.STANDARD, isExtended = true)
        val dims = TableDimensions(width = 1000f, viewportHeight = 2000f, bezelGapUnits = 200f, isExtendedMode = true)
        val screenW = 1080f
        val screenH = 2400f

        // Top center of screen (screenY = 0) maps precisely to center line of guest half (500, hostHalfBottomY = 2000)
        val topCenterPt = screenToTable3D(
            screenX = screenW / 2f,
            screenY = 0f,
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
        assertNotNull(topCenterPt)
        topCenterPt?.let {
            assertEquals(500f, it.x, 5f)
            assertEquals(2000f, it.y, 5f)
        }

        // Test reversibility: tableToScreen3D for (500, 2000) must project to screen top (540, 0)
        val topScreenPos = tableToScreen3D(
            tableX = 500f,
            tableY = 2000f,
            w = screenW,
            h = screenH,
            camX = extPreset.position.x,
            camY = extPreset.position.y,
            camZ = extPreset.position.z,
            camPitchDeg = extPreset.rotation.x,
            camYawDeg = extPreset.rotation.y,
            elevationY = 0f,
            isExtended = true,
            isExtendedGuest = true,
            dimensions = dims
        )
        assertNotNull(topScreenPos)
        topScreenPos?.let {
            assertEquals(screenW / 2f, it.x, 2f)
            assertEquals(0f, it.y, 2f)
        }
    }

    @Test
    fun arenaKitCatalog_extendedTable_selectsHalfTableModelsAndMascot() {
        for (theme in TableTheme.entries) {
            val kit = theme.kit()
            val p1Asset = kit.tableAsset(TablePerspective.EXTENDED_HOST)
            val p2Asset = kit.tableAsset(TablePerspective.EXTENDED_GUEST)
            val fullAsset = kit.tableAsset(TablePerspective.LOCAL_FULL)
            assertNotNull(p1Asset)
            assertNotNull(p2Asset)
            assert(p1Asset.endsWith("_half_p1.glb")) { "Theme $theme p1 asset $p1Asset should end with _half_p1.glb" }
            assert(p2Asset.endsWith("_half_p2.glb")) { "Theme $theme p2 asset $p2Asset should end with _half_p2.glb" }
            assertEquals(kit.tableFullAsset, fullAsset)
        }

        val p1Asset = TableTheme.CLASSIC_NEON.kit().tableAsset(TablePerspective.EXTENDED_HOST)
        assertEquals("models/mesa_arcade_half_p1.glb", p1Asset)

        val soccerP1 = TableTheme.CYBER_SOCCER.kit().tableAsset(TablePerspective.EXTENDED_HOST)
        assertEquals("models/mesa_cyber_soccer_half_p1.glb", soccerP1)

        val soccerP2 = TableTheme.CYBER_SOCCER.kit().tableAsset(TablePerspective.EXTENDED_GUEST)
        assertEquals("models/mesa_cyber_soccer_half_p2.glb", soccerP2)

        val standardAsset = TableTheme.CLASSIC_NEON.kit().tableAsset(TablePerspective.LOCAL_FULL)
        assertEquals("models/mesa_arcade.glb", standardAsset)

        val kiaraKit = TableTheme.KIARA_CAT.kit()
        assertEquals("models/kiara_3d.glb", kiaraKit.mascot3dAsset)
        assertTrue(kiaraKit.hasMascot)
        assertTrue(kiaraKit.needsWideMascotFraming)
        assertNull(TableTheme.CLASSIC_NEON.kit().mascot3dAsset)
        assertFalse(TableTheme.CLASSIC_NEON.kit().needsWideMascotFraming)

        // Decisão Q3: neither EXTENDED_HOST nor EXTENDED_GUEST draws top goal
        assertFalse(!TablePerspective.EXTENDED_HOST.isExtended)
        assertFalse(!TablePerspective.EXTENDED_GUEST.isExtended)
        assertTrue(!TablePerspective.LOCAL_FULL.isExtended)
        assertTrue(!TablePerspective.SPLIT_DUAL.isExtended)
        assertTrue(!TablePerspective.CONNECTED_HOST.isExtended)
        assertTrue(!TablePerspective.CONNECTED_GUEST.isExtended)
    }

    @Test
    fun table3DCameraProjection_computeEffectiveTargetPreset_adaptsToScreenAspect() {
        val dims = TableDimensions(isExtendedMode = true, bezelGapUnits = 200f)
        val proj = Table3DCameraProjection(dims, GameMode.EXTENDED_HOST)

        // Tall narrow phone (9:19.5 -> aspect 0.4615)
        val tallPreset = proj.computeEffectiveTargetPreset(CameraZoom3D.STANDARD, 1080f / 2340f)
        // Wider screen (9:16 -> aspect 0.5625)
        val widePreset = proj.computeEffectiveTargetPreset(CameraZoom3D.STANDARD, 1080f / 1920f)

        // A narrower phone requires higher camera altitude (Y) to fit the table width
        assertTrue(tallPreset.position.y > widePreset.position.y)

        // Top anchoring: z = zTop + y * tanHalfFov
        val expectedZTall = Table3DCameraProjection.Z_TOP_EXTENDED + tallPreset.position.y * Table3DCameraProjection.TAN_HALF_FOV
        assertEquals(expectedZTall, tallPreset.position.z, 1e-4f)

        // For non-extended modes, base preset is preserved unchanged in portrait (aspect <= 1.15)
        val soloProj = Table3DCameraProjection(TableDimensions(), GameMode.SOLO)
        val soloPresetPortrait = soloProj.computeEffectiveTargetPreset(CameraZoom3D.STANDARD, 1080f / 2340f)
        assertEquals(soloProj.getBasePreset(CameraZoom3D.STANDARD), soloPresetPortrait)

        // For Kiara Cat theme on portrait, camera frames wider to accommodate the mascot
        val kiaraPresetPortrait = soloProj.computeEffectiveTargetPreset(CameraZoom3D.STANDARD, 1080f / 2340f, TableTheme.KIARA_CAT)
        assertEquals(2.85f, kiaraPresetPortrait.position.y, 1e-4f)
        assertEquals(2.45f, kiaraPresetPortrait.position.z, 1e-4f)

        // For landscape aspect ratios (aspect > 1.15, typical of tablets), camera adapts closer (ADR 0041)
        val tabletAspect = 1920f / 1200f // 1.6
        val soloPresetTabletClose = soloProj.computeEffectiveTargetPreset(CameraZoom3D.CLOSE, tabletAspect)
        val soloPresetTabletStd = soloProj.computeEffectiveTargetPreset(CameraZoom3D.STANDARD, tabletAspect)
        val soloPresetTabletWide = soloProj.computeEffectiveTargetPreset(CameraZoom3D.WIDE, tabletAspect)

        assertEquals(1.70f, soloPresetTabletClose.position.y, 1e-4f)
        assertEquals(1.45f, soloPresetTabletClose.position.z, 1e-4f)
        assertEquals(-49f, soloPresetTabletClose.rotation.x, 1e-4f)

        assertEquals(2.15f, soloPresetTabletStd.position.y, 1e-4f)
        assertEquals(1.85f, soloPresetTabletStd.position.z, 1e-4f)
        assertEquals(-47f, soloPresetTabletStd.rotation.x, 1e-4f)

        assertTrue(soloPresetTabletClose.position.y < soloPresetTabletStd.position.y)
        assertTrue(soloPresetTabletStd.position.y < soloPresetTabletWide.position.y)
    }

    @Test
    fun table3DCameraProjection_computePresentationPose_interpolatesSmoothlyToTarget() {
        val dims = TableDimensions()
        val proj = Table3DCameraProjection(dims, GameMode.SOLO)
        val target = proj.getBasePreset(CameraZoom3D.STANDARD)

        val poseStart = proj.computePresentationPose(0f, target)
        val poseMid = proj.computePresentationPose(0.5f, target)
        val poseEnd = proj.computePresentationPose(1f, target)

        // At p=0, drone starts high and elevated over opponent goal
        assertEquals(2.70f, poseStart.y, 0.05f)
        assertEquals(-2.60f, poseStart.z, 0.05f)

        // At p=1, pose matches target preset position and pitch
        assertEquals(target.position.x, poseEnd.x, 0.05f)
        assertEquals(target.position.y, poseEnd.y, 0.05f)
        assertEquals(target.position.z, poseEnd.z, 0.05f)
        assertEquals(target.rotation.x, poseEnd.pitch, 0.5f)
        assertTrue("Final focusZ for host should point toward negative Z", poseEnd.focusZ < 0f)

        // Mid progression is valid and distinct
        assertTrue(poseMid.y > 0f)

        // Guest camera mode mirrors trajectory and lands with positive focusZ and 180 yaw
        val guestProj = Table3DCameraProjection(dims, GameMode.CONNECTED_GUEST)
        val guestTarget = guestProj.getBasePreset(CameraZoom3D.STANDARD)
        val guestEnd = guestProj.computePresentationPose(1f, guestTarget)
        assertEquals(guestTarget.position.z, guestEnd.z, 0.05f)
        assertEquals(180f, guestEnd.yaw, 0.5f)
        assertTrue("Final focusZ for guest should point toward positive Z", guestEnd.focusZ > 0f)

        // Split screen avoids zero-distance singularity on lookAt at zenith
        val splitProj = Table3DCameraProjection(dims, GameMode.SPLIT_SCREEN)
        val splitTarget = splitProj.getBasePreset(CameraZoom3D.STANDARD)
        val splitEnd = splitProj.computePresentationPose(1f, splitTarget)
        assertEquals(-90f, splitEnd.pitch, 0.5f)
        assertTrue("Zenith focusZ keeps non-zero offset to prevent lookAt gimbal singularity", kotlin.math.abs(splitEnd.focusZ) >= 0.004f)
    }

    @Test
    fun cyberFloorGlb_isOrientedHorizontallyInXZPlane() {
        val file = java.io.File("src/main/assets/models/piso_arena_cyber.glb").let {
            if (it.exists()) it else java.io.File("app/src/main/assets/models/piso_arena_cyber.glb")
        }
        assertTrue("piso_arena_cyber.glb must exist", file.exists())
        val bytes = file.readBytes()
        val buf = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val magic = buf.int
        val version = buf.int
        val totalLen = buf.int
        assertEquals(0x46546C67, magic)
        assertEquals(2, version)
        assertTrue(totalLen > 100)

        val jsonLen = buf.int
        val jsonType = buf.int
        assertEquals(0x4E4F534A, jsonType)
        val jsonBytes = ByteArray(jsonLen)
        buf.get(jsonBytes)
        val jsonStr = String(jsonBytes, Charsets.UTF_8)

        // Verify FloorSlab and FloorRim are flat along Y (vertical thickness < 0.1m) and extended along Z (length >= 6.4m)
        assertTrue("FloorRim must span Z from -3.25 to +3.25 (horizontal XZ plane)", jsonStr.contains("-3.25") && jsonStr.contains("3.25"))
        assertTrue("piso_arena_cyber.glb must not have vertical Y wall bounds of 3.25m", !jsonStr.contains("-3.25, -0.0225"))
    }

    @Test
    fun table3DCameraProjection_stepSmoothPose_convergesToTarget() {
        val dims = TableDimensions()
        val proj = Table3DCameraProjection(dims, GameMode.SOLO)
        val target = proj.getBasePreset(CameraZoom3D.STANDARD)

        var current = CameraPose3D(0f, 1.0f, 0f, -30f, 0f)
        for (i in 0 until 50) {
            current = proj.stepSmoothPose(current, target, factor = 0.15f)
        }

        assertEquals(target.position.y, current.y, 0.01f)
        assertEquals(target.position.z, current.z, 0.01f)
        assertEquals(target.rotation.x, current.pitch, 0.01f)
    }

    @Test
    fun table3DCameraProjection_mapEngineCoordinates_mapsHostAndGuestConsistently() {
        val dims = TableDimensions(isExtendedMode = true, bezelGapUnits = 200f)
        val hostProj = Table3DCameraProjection(dims, GameMode.EXTENDED_HOST)
        val guestProj = Table3DCameraProjection(dims, GameMode.EXTENDED_GUEST)

        // Center line X = 500 should map to Filament world X = 0
        assertEquals(0f, hostProj.mapEngineX(500f), 1e-4f)
        assertEquals(0f, guestProj.mapEngineX(500f), 1e-4f)

        // Host top of visible half is guestHalfTopY (1800) -> should map to -1.08
        val hostTopZ = hostProj.mapEngineZ(dims.guestHalfTopY)
        assertEquals(-1.08f, hostTopZ, 1e-4f)

        // Guest top of visible half is hostHalfBottomY (2200) -> should map to -1.08 from guest's inverted perspective
        val guestTopZ = guestProj.mapEngineZ(dims.hostHalfBottomY)
        assertEquals(-1.08f, guestTopZ, 1e-4f)
    }

    @Test
    fun stepSmoothPose_isFrameRateIndependentAcross30HzAnd60Hz() {
        val proj = Table3DCameraProjection(TableDimensions(), GameMode.SOLO)
        val target = proj.getBasePreset(CameraZoom3D.STANDARD)
        val start = CameraPose3D(
            x = 0.40f,
            y = 2.50f,
            z = 2.40f,
            focusX = 0f,
            focusY = 0f,
            focusZ = -0.30f,
            pitch = -50f,
            yaw = 10f
        )

        val afterFirst60 = proj.stepSmoothPose(start, target, dtSec = 1f / 60f)
        val afterTwo60 = proj.stepSmoothPose(afterFirst60, target, dtSec = 1f / 60f)
        val afterOne30 = proj.stepSmoothPose(start, target, dtSec = 1f / 30f)

        assertEquals(afterTwo60.x, afterOne30.x, 1e-4f)
        assertEquals(afterTwo60.y, afterOne30.y, 1e-4f)
        assertEquals(afterTwo60.z, afterOne30.z, 1e-4f)
        assertEquals(afterTwo60.pitch, afterOne30.pitch, 1e-3f)
        assertEquals(afterTwo60.yaw, afterOne30.yaw, 1e-3f)
    }

    @Test
    fun visualEffects_timingAndDamping_areFrameRateIndependent() {
        val damp60 = dev.vmellos.airhockey.feature.game.ui.frameRateIndependentDamping(1f / 60f, 0.94f)
        val damp30 = dev.vmellos.airhockey.feature.game.ui.frameRateIndependentDamping(1f / 30f, 0.94f)
        assertEquals(0.94f, damp60, 1e-4f)
        assertEquals(damp60 * damp60, damp30, 1e-4f)

        val waveA = dev.vmellos.airhockey.feature.game.ui.VisualShockwave().apply {
            init(startX = 100f, startY = 200f, waveColor = androidx.compose.ui.graphics.Color.Cyan, radius = 160f)
        }
        val waveB = dev.vmellos.airhockey.feature.game.ui.VisualShockwave().apply {
            init(startX = 100f, startY = 200f, waveColor = androidx.compose.ui.graphics.Color.Cyan, radius = 160f)
        }
        waveA.update(0.015f)
        waveA.update(0.015f)
        waveB.update(0.030f)

        assertEquals(waveA.progress, waveB.progress, 1e-5f)
        assertTrue(waveB.active)
    }

    @Test
    fun neonBloomDisplayState_reflectsDisabledEffectsWithoutOverwritingSavedPreference() {
        val enabledNormal = dev.vmellos.airhockey.feature.game.ui.resolveNeonBloomDisplayState(
            savedIntensity = 0.05f,
            effectsEnabledInContext = true
        )
        assertEquals(50, enabledNormal.percent)
        assertEquals(dev.vmellos.airhockey.feature.game.ui.NeonBloomBand.NORMAL, enabledNormal.band)
        assertTrue(enabledNormal.isControlEnabled)
        assertEquals(0.05f, enabledNormal.sliderValue, 1e-5f)

        val suppressedInMatch = dev.vmellos.airhockey.feature.game.ui.resolveNeonBloomDisplayState(
            savedIntensity = 0.05f,
            effectsEnabledInContext = false
        )
        assertEquals(0, suppressedInMatch.percent)
        assertEquals(dev.vmellos.airhockey.feature.game.ui.NeonBloomBand.OFF, suppressedInMatch.band)
        org.junit.Assert.assertFalse(suppressedInMatch.isControlEnabled)
        assertEquals(0.0f, suppressedInMatch.sliderValue, 1e-5f)

        val zeroSliderWhileEffectsOn = dev.vmellos.airhockey.feature.game.ui.resolveNeonBloomDisplayState(
            savedIntensity = 0.0f,
            effectsEnabledInContext = true
        )
        assertEquals(0, zeroSliderWhileEffectsOn.percent)
        assertEquals(dev.vmellos.airhockey.feature.game.ui.NeonBloomBand.OFF, zeroSliderWhileEffectsOn.band)
        assertTrue(zeroSliderWhileEffectsOn.isControlEnabled)
    }

    @Test
    fun cameraBasis3D_matchesPoseOverloadAndRoundTripsOnTablePlane() {
        val proj = Table3DCameraProjection(TableDimensions(), GameMode.CONNECTED_GUEST)
        val preset = proj.getBasePreset(CameraZoom3D.STANDARD)
        val pose = CameraPose3D(
            x = preset.position.x,
            y = preset.position.y,
            z = preset.position.z,
            pitch = preset.rotation.x,
            yaw = preset.rotation.y
        )
        val screenW = 1080f
        val screenH = 2400f
        val basis = proj.computeBasis(screenW, screenH, pose)
        assertNotNull(basis)

        val projectedViaBasis = proj.tableToScreen(320f, 450f, screenW, screenH, basis!!, elevationY = 0f)
        val projectedViaPose = proj.tableToScreen(320f, 450f, screenW, screenH, pose, elevationY = 0f)
        assertNotNull(projectedViaBasis)
        assertNotNull(projectedViaPose)
        assertEquals(projectedViaPose!!.x, projectedViaBasis!!.x, 1e-4f)
        assertEquals(projectedViaPose.y, projectedViaBasis.y, 1e-4f)

        val recovered = proj.screenToTable(projectedViaBasis.x, projectedViaBasis.y, screenW, screenH, basis)
        assertNotNull(recovered)
        assertEquals(320f, recovered!!.x, 0.5f)
        assertEquals(450f, recovered.y, 0.5f)
    }

    @Test
    fun effectiveTargetPreset_respectsNeedsWideMascotFramingAndBloomGammaCurve() {
        val proj = Table3DCameraProjection(TableDimensions(), GameMode.SOLO)
        val standardPortrait = proj.computeEffectiveTargetPreset(
            zoom = CameraZoom3D.STANDARD,
            screenAspect = 9f / 19.5f,
            theme = TableTheme.CLASSIC_NEON,
            needsWideMascotFraming = false
        )
        val mascotFramingPreset = proj.computeEffectiveTargetPreset(
            zoom = CameraZoom3D.STANDARD,
            screenAspect = 9f / 19.5f,
            theme = TableTheme.CLASSIC_NEON,
            needsWideMascotFraming = true
        )
        assertTrue(mascotFramingPreset.position.y > standardPortrait.position.y)
        assertEquals(2.85f, mascotFramingPreset.position.y, 1e-4f)

        assertEquals(0.0f, dev.vmellos.airhockey.feature.game.ui.computeCalibratedBloomStrength(0.0f), 1e-5f)
        assertEquals(0.055f, dev.vmellos.airhockey.feature.game.ui.computeCalibratedBloomStrength(0.05f), 1e-5f)
        assertEquals(0.22f, dev.vmellos.airhockey.feature.game.ui.computeCalibratedBloomStrength(0.10f), 1e-5f)
    }

    @Test
    fun connectedGuest_wideMascotFraming_preserves180DegreeCameraInversionForAllZooms() {
        val hostProj = Table3DCameraProjection(TableDimensions(), GameMode.CONNECTED_HOST)
        val guestProj = Table3DCameraProjection(TableDimensions(), GameMode.CONNECTED_GUEST)
        val portraitAspect = 9f / 19.5f

        for (zoom in CameraZoom3D.entries) {
            val hostPreset = hostProj.computeEffectiveTargetPreset(
                zoom = zoom,
                screenAspect = portraitAspect,
                theme = TableTheme.KIARA_CAT,
                needsWideMascotFraming = true
            )
            val guestPreset = guestProj.computeEffectiveTargetPreset(
                zoom = zoom,
                screenAspect = portraitAspect,
                theme = TableTheme.KIARA_CAT,
                needsWideMascotFraming = true
            )

            assertTrue("Guest Z must be negative for $zoom", guestPreset.position.z < 0f)
            assertEquals("Guest Y should mirror Host Y for $zoom", hostPreset.position.y, guestPreset.position.y, 1e-4f)
            assertEquals("Guest Z should invert Host Z for $zoom", -hostPreset.position.z, guestPreset.position.z, 1e-4f)
            assertEquals("Guest pitch must match Host for $zoom", hostPreset.rotation.x, guestPreset.rotation.x, 1e-4f)
            assertEquals("Guest yaw must be 180° for $zoom", 180f, guestPreset.rotation.y, 1e-4f)
        }
    }
}
