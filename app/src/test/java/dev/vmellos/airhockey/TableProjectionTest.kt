package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.physics.TableDimensions
import dev.vmellos.airhockey.feature.physics.TablePoint
import dev.vmellos.airhockey.feature.physics.TableProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableProjectionTest {

    @Test
    fun testSoloModeScreenToTableRoundTrip() {
        val dimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = 0f,
            isExtendedMode = false
        )
        val projection = TableProjection(dimensions, GameMode.SOLO)
        val screenW = 1080f
        val screenH = 2400f

        val original = TablePoint(350f, 1500f)
        val screen = projection.tableToScreen(original.x, original.y, screenW, screenH)
        val backToTable = projection.screenToTable(screen.x, screen.y, screenW, screenH)

        assertEquals(original.x, backToTable.x, 0.05f)
        assertEquals(original.y, backToTable.y, 0.05f)
    }

    @Test
    fun testExtendedHostViewportProjectionAndVisibility() {
        val dimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = 50f,
            isExtendedMode = true
        )
        // guestHalfTopY = 2050f, totalHeight = 4050f
        val hostProjection = TableProjection(dimensions, GameMode.EXTENDED_HOST)
        val screenW = 1080f
        val screenH = 2400f

        // Topo da tela do Anfitrião deve corresponder exatamente a Y = guestHalfTopY (2050f) ancorado em Y = 0 (sem letterbox superior)
        val topBezelScreen = hostProjection.tableToScreen(500f, 2050f, screenW, screenH)
        assertEquals(0f, topBezelScreen.y, 0.1f)

        val topBezelTable = hostProjection.screenToTable(topBezelScreen.x, topBezelScreen.y, screenW, screenH)
        assertEquals(500f, topBezelTable.x, 0.1f)
        assertEquals(2050f, topBezelTable.y, 0.1f)

        // Visibilidade no Anfitrião (metade inferior [2050..4050])
        assertTrue("Disco no campo do anfitrião deve ser visível", hostProjection.isVisibleInViewport(500f, 3000f, 36f))
        assertFalse("Disco no campo do convidado não deve ser visível no anfitrião", hostProjection.isVisibleInViewport(500f, 1000f, 36f))
    }

    @Test
    fun testExtendedGuest180DegreeRotationAndRoundTrip() {
        val dimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = 50f,
            isExtendedMode = true
        )
        // hostHalfBottomY = 2000f
        val guestProjection = TableProjection(dimensions, GameMode.EXTENDED_GUEST)
        val screenW = 1080f
        val screenH = 2400f

        // No convidado com rotação de 180°, o topo da tela deve ser Y = 2000f (borda de contato com o anfitrião, ancorada em Y = 0)
        val guestTopScreen = guestProjection.tableToScreen(500f, 2000f, screenW, screenH)
        assertEquals(0f, guestTopScreen.y, 0.1f)

        val guestTopTable = guestProjection.screenToTable(guestTopScreen.x, guestTopScreen.y, screenW, screenH)
        assertEquals(500f, guestTopTable.x, 0.1f)
        assertEquals(2000f, guestTopTable.y, 0.1f)

        // A meta do convidado fica em Y = 0 (base da tela do convidado)
        val guestGoalScreen = guestProjection.tableToScreen(500f, 0f, screenW, screenH)
        val guestGoalTable = guestProjection.screenToTable(guestGoalScreen.x, guestGoalScreen.y, screenW, screenH)
        assertEquals(500f, guestGoalTable.x, 0.1f)
        assertEquals(0f, guestGoalTable.y, 0.1f)

        // Teste de inversão horizontal (X = 200 no convidado vira X = 800)
        val original = TablePoint(200f, 800f)
        val screenPt = guestProjection.tableToScreen(original.x, original.y, screenW, screenH)
        val roundTrip = guestProjection.screenToTable(screenPt.x, screenPt.y, screenW, screenH)
        assertEquals(original.x, roundTrip.x, 0.1f)
        assertEquals(original.y, roundTrip.y, 0.1f)

        // Visibilidade no Convidado (metade superior [0..2000])
        assertTrue("Disco no campo do convidado deve ser visível no convidado", guestProjection.isVisibleInViewport(500f, 1000f, 36f))
        assertFalse("Disco no campo do anfitrião não deve ser visível no convidado", guestProjection.isVisibleInViewport(500f, 3000f, 36f))
    }

    @Test
    fun testConnectedModeHostAndGuestProjection() {
        val dimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = 0f,
            isExtendedMode = false
        )
        val hostProjection = TableProjection(dimensions, GameMode.CONNECTED_HOST)
        val guestProjection = TableProjection(dimensions, GameMode.CONNECTED_GUEST)
        val screenW = 1080f
        val screenH = 2400f

        // Host vê seu rebatedor na base (Y = 1750) e adversário no topo (Y = 250)
        val hostP1Screen = hostProjection.tableToScreen(500f, 1750f, screenW, screenH)
        val hostP2Screen = hostProjection.tableToScreen(500f, 250f, screenW, screenH)
        assertTrue("Na tela do Host, P1 deve ficar abaixo de P2", hostP1Screen.y > hostP2Screen.y)

        // Convidado (180°): vê seu rebatedor (P2, Y = 250) na base da sua tela e Host (P1, Y = 1750) no topo da sua tela
        val guestP2Screen = guestProjection.tableToScreen(500f, 250f, screenW, screenH)
        val guestP1Screen = guestProjection.tableToScreen(500f, 1750f, screenW, screenH)
        assertTrue("Na tela do Convidado, P2 deve ficar na base (abaixo de P1)", guestP2Screen.y > guestP1Screen.y)

        // Toque na base da tela do Convidado deve converter para a metade defensiva de P2 (Y próximo a 250)
        val touchedTablePt = guestProjection.screenToTable(guestP2Screen.x, guestP2Screen.y, screenW, screenH)
        assertEquals(500f, touchedTablePt.x, 0.1f)
        assertEquals(250f, touchedTablePt.y, 0.1f)
    }

    @Test
    fun testMalletClamping() {
        val dimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = 40f,
            isExtendedMode = true
        )
        val projection = TableProjection(dimensions, GameMode.EXTENDED_HOST)
        val r = 64f

        // Player 1 tenta invadir o vão ou a metade do convidado (< guestHalfTopY = 2040f)
        val clampedP1 = projection.clampMallet(500f, 1800f, r, isPlayer1 = true)
        assertEquals(2040f + r, clampedP1.y, 0.01f)

        // Player 2 tenta invadir o campo do Player 1 (> hostHalfBottomY = 2000f)
        val clampedP2 = projection.clampMallet(500f, 2500f, r, isPlayer1 = false)
        assertEquals(2000f - r, clampedP2.y, 0.01f)
    }
}
