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
            viewportHeight = 1000f,
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
            viewportHeight = 1000f,
            bezelGapUnits = bezelGap,
            isExtendedMode = true
        )

        assertEquals(2000f + bezelGap, dims.totalHeight, 0.01f)
        assertEquals(1000f, dims.hostHalfBottomY, 0.01f)
        assertEquals(1000f + bezelGap, dims.guestHalfTopY, 0.01f)
    }

    @Test
    fun testBezelGapCalculationFromMillimeters() {
        // Para uma tela com 2400px a 400dpi: altura em mm = (2400/400)*25.4 = 152.4 mm
        // 1000 unidades / 152.4 mm = ~6.56 unidades/mm
        // 8 mm de vão -> ~52.5 unidades
        val units = TableDimensions.calculateBezelUnits(
            bezelGapMm = 8.0f,
            screenHeightPx = 2400f,
            yDpi = 400f
        )
        assertTrue("Bezel gap in units should be between 40 and 70", units in 40f..70f)
    }
}
