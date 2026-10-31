package dev.vmellos.airhockey.feature.physics

/**
 * Dimensões normalizadas da mesa de Air Hockey em unidades virtuais fixas.
 */
data class TableDimensions(
    val width: Float = 1000f,
    val viewportHeight: Float = 1000f,
    val bezelGapUnits: Float = 0f,
    val isExtendedMode: Boolean = false
) {
    val totalHeight: Float
        get() = if (isExtendedMode) (viewportHeight * 2f + bezelGapUnits) else (viewportHeight * 2f)

    val puckRadius: Float = 36f
    val malletRadius: Float = 64f

    val goalWidth: Float = 320f
    val goalLeft: Float = (width - goalWidth) / 2f
    val goalRight: Float = (width + goalWidth) / 2f

    val player1GoalY: Float = totalHeight
    val player2GoalY: Float = 0f

    val centerLineY: Float
        get() = totalHeight / 2f

    val hostHalfBottomY: Float
        get() = viewportHeight

    val guestHalfTopY: Float
        get() = viewportHeight + bezelGapUnits

    companion object {
        fun calculateBezelUnits(bezelGapMm: Float, screenHeightPx: Float, yDpi: Float): Float {
            val validDpi = if (yDpi > 0f) yDpi else 400f
            val screenHeightMm = (screenHeightPx / validDpi) * 25.4f
            val unitsPerMm = if (screenHeightMm > 10f) (1000f / screenHeightMm) else 6.5f
            return (bezelGapMm * unitsPerMm).coerceIn(0f, 180f)
        }
    }
}
