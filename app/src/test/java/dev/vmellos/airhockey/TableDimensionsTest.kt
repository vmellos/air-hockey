package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.physics.TableDimensions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableDimensionsTest {

    @Test
    fun testSingleDeviceDimensions() {
        val dims = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = 0f,
            isExtendedMode = false
        )

        assertEquals(1000f, dims.width, 0.01f)
        assertEquals(2000f, dims.totalHeight, 0.01f)
        assertEquals(1000f, dims.centerLineY, 0.01f)
        assertEquals(320f, dims.goalWidth, 0.01f)
        assertEquals(340f, dims.goalLeft, 0.01f)
        assertEquals(660f, dims.goalRight, 0.01f)
    }

    @Test
    fun testExtendedModeWithBezelGap() {
        val bezelGap = 65f
        val dims = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = bezelGap,
            isExtendedMode = true
        )

        assertEquals(4000f + bezelGap, dims.totalHeight, 0.01f)
        assertEquals(2000f, dims.hostHalfBottomY, 0.01f)
        assertEquals(2000f + bezelGap, dims.guestHalfTopY, 0.01f)
    }

    @Test
    fun testBezelGapCalculationFromMillimeters() {
        // Para uma tela com 2400px a 400dpi: altura em mm = (2400/400)*25.4 = 152.4 mm
        // 2000 unidades / 152.4 mm = ~13.12 unidades/mm
        // 8 mm de vão -> ~105 unidades
        val units = TableDimensions.calculateBezelUnits(
            bezelGapMm = 8.0f,
            screenHeightPx = 2400f,
            yDpi = 400f
        )
        assertTrue("Bezel gap in units should be between 80 and 140", units in 80f..140f)
    }
}
