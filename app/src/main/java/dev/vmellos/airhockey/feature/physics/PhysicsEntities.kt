package dev.vmellos.airhockey.feature.physics

/**
 * Entidade mutável para simulação física de alta taxa (120 FPS) sem alocações no loop.
 */
class Puck(
    var x: Float = 500f,
    var y: Float = 1000f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var radius: Float = 36f
) {
    fun reset(centerX: Float, centerY: Float) {
        x = centerX
        y = centerY
        vx = 0f
        vy = 0f
    }

    fun speedSquared(): Float = vx * vx + vy * vy

    fun speed(): Float = kotlin.math.sqrt(speedSquared())

    fun createSnapshot(): PuckSnapshot = PuckSnapshot(x, y, vx, vy, radius)
}

data class PuckSnapshot(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float
)

class Mallet(
    var x: Float = 500f,
    var y: Float = 1750f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var radius: Float = 64f,
    var targetX: Float = 500f,
    var targetY: Float = 1750f
) {
    fun reset(initialX: Float, initialY: Float) {
        x = initialX
        y = initialY
        targetX = initialX
        targetY = initialY
        vx = 0f
        vy = 0f
    }

    fun createSnapshot(): MalletSnapshot = MalletSnapshot(x, y, vx, vy, radius)
}

data class MalletSnapshot(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float
)
