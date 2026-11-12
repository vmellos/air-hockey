package dev.vmellos.airhockey.feature.game.ui

import dev.romainguy.kotlin.math.Float3
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Estado comportamental da Mascote Kiara 3D ao redor da Arena (GLOSSARY.md).
 */
enum class KiaraBehaviorState {
    WALKING,
    LICKING_PAW,
    RESTING_LOOKOUT,
    SWATTING
}

/**
 * Ponto de navegação no Circuito de Patrulha da Mascote (ADR 0042).
 */
data class MascotWaypoint(
    val x: Float,
    val z: Float,
    val isStopPoint: Boolean = false,
    val stopDurationSec: Float = 0f,
    val stopLookoutYaw: Float = 0f,
    val stopAnimation: String = "LickPaw"
)

/**
 * Controlador cinemático e comportamental da Mascote Kiara 3D.
 *
 * Encapsula:
 * - Patrulha contínua no Circuito em Ferradura Expandida ("U") pelas calhas laterais no chão da arena (X ≈ ±0.74m)
 *   e pelo topo panorâmico atrás da meta adversária (Z ≈ -1.30m), mantendo a zona de toque do Jogador 1 livre.
 * - Alternância entre trote quadrúpede felino ('Walk'), limpeza e lambida de pata ('LickPaw') e contemplação ('Idle').
 * - Sensor de Proximidade e Alerta do Disco: ao detectar o disco próximo da borda (< 0.38m), desfere
 *   patada lúdica na barreira translúcida neon ('Swat') como base da futura Interferência Felina.
 * - Interpolação angular suave de orientação (yaw) e ondulação vertical suave (bobbing).
 */
class KiaraMascotController(
    val waypoints: List<MascotWaypoint> = defaultHorseshoePath()
) {
    var state: KiaraBehaviorState = KiaraBehaviorState.LICKING_PAW
        private set

    var currentX: Float = waypoints.firstOrNull()?.x ?: -0.74f
        private set
    var currentY: Float = -0.045f
        private set
    var currentZ: Float = waypoints.firstOrNull()?.z ?: -0.15f
        private set
    var currentYaw: Float = waypoints.firstOrNull()?.stopLookoutYaw ?: 90f
        private set

    var currentAnimation: String = waypoints.firstOrNull()?.stopAnimation ?: "LickPaw"
        private set

    private var currentWaypointIndex: Int = 0
    private var stateTimerSec: Float = 0f
    private var walkBobPhase: Float = 0f
    private var swatCooldownSec: Float = 0f

    val walkSpeedMps: Float = 0.35f
    val swatProximityDist: Float = 0.38f

    companion object {
        /**
         * Circuito de Patrulha em Ferradura Expandida ("U"):
         * Calha Lateral Esquerda <-> Topo Panorâmico (Meta Rival) <-> Calha Lateral Direita.
         */
        fun defaultHorseshoePath(): List<MascotWaypoint> = listOf(
            // 0. Meia-quadra esquerda no piso da arena (repouso e lambida de pata olhando para a mesa)
            MascotWaypoint(
                x = -0.74f,
                z = -0.15f,
                isStopPoint = true,
                stopDurationSec = 5.0f,
                stopLookoutYaw = 90f,
                stopAnimation = "LickPaw"
            ),
            // 1. Subida pela calha esquerda
            MascotWaypoint(x = -0.74f, z = -0.85f),
            // 2. Curva para o topo esquerdo
            MascotWaypoint(x = -0.55f, z = -1.28f),
            // 3. Topo Panorâmico Central (atrás da meta adversária - olhando para o campo)
            MascotWaypoint(
                x = 0.0f,
                z = -1.30f,
                isStopPoint = true,
                stopDurationSec = 5.0f,
                stopLookoutYaw = 0f,
                stopAnimation = "LickPaw"
            ),
            // 4. Curva para o topo direito
            MascotWaypoint(x = 0.55f, z = -1.28f),
            // 5. Descida pela calha direita
            MascotWaypoint(x = 0.74f, z = -0.85f),
            // 6. Meia-quadra direita no piso da arena (olhando para a mesa)
            MascotWaypoint(
                x = 0.74f,
                z = -0.15f,
                isStopPoint = true,
                stopDurationSec = 5.0f,
                stopLookoutYaw = 270f,
                stopAnimation = "LickPaw"
            ),
            // Retorno em sentido oposto pela ferradura
            MascotWaypoint(x = 0.74f, z = -0.85f),
            MascotWaypoint(x = 0.55f, z = -1.28f),
            MascotWaypoint(
                x = 0.0f,
                z = -1.30f,
                isStopPoint = true,
                stopDurationSec = 4.0f,
                stopLookoutYaw = 0f,
                stopAnimation = "Idle"
            ),
            MascotWaypoint(x = -0.55f, z = -1.28f),
            MascotWaypoint(x = -0.74f, z = -0.85f)
        )
    }

    fun update(
        dt: Float,
        puckX: Float = Float.NaN,
        puckZ: Float = Float.NaN
    ) {
        if (waypoints.isEmpty()) return
        val delta = dt.coerceIn(0.0001f, 2.0f)
        stateTimerSec += delta
        if (swatCooldownSec > 0f) {
            swatCooldownSec -= delta
        }

        // Sensor de Proximidade e Alerta do Disco (Preparo para a Interferência Felina)
        if (!puckX.isNaN() && !puckZ.isNaN() && swatCooldownSec <= 0f && state != KiaraBehaviorState.SWATTING) {
            val distToPuck = hypot(puckX - currentX, puckZ - currentZ)
            if (distToPuck < swatProximityDist) {
                // Ativa patada na barreira neon em direção ao disco
                state = KiaraBehaviorState.SWATTING
                stateTimerSec = 0f
                currentAnimation = "Swat"
                swatCooldownSec = 3.5f // Evita patadas consecutivas frenéticas

                val dirX = puckX - currentX
                val dirZ = puckZ - currentZ
                val targetYawRad = atan2(dirX, -dirZ)
                currentYaw = (Math.toDegrees(targetYawRad.toDouble()).toFloat() % 360f + 360f) % 360f
                return
            }
        }

        val targetWp = waypoints[currentWaypointIndex]

        when (state) {
            KiaraBehaviorState.SWATTING -> {
                currentY = -0.045f
                if (stateTimerSec >= 1.0f) {
                    // Retoma a caminhada ou postura anterior
                    state = KiaraBehaviorState.WALKING
                    stateTimerSec = 0f
                    currentAnimation = "Walk"
                }
            }

            KiaraBehaviorState.WALKING -> {
                currentAnimation = "Walk"
                val dx = targetWp.x - currentX
                val dz = targetWp.z - currentZ
                val dist = hypot(dx, dz)

                if (dist < 0.04f) {
                    currentX = targetWp.x
                    currentZ = targetWp.z
                    if (targetWp.isStopPoint) {
                        state = KiaraBehaviorState.LICKING_PAW
                        stateTimerSec = 0f
                        currentAnimation = targetWp.stopAnimation
                    } else {
                        currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.size
                    }
                } else {
                    val moveDist = (walkSpeedMps * delta).coerceAtMost(dist)
                    val dirX = dx / dist
                    val dirZ = dz / dist
                    currentX += dirX * moveDist
                    currentZ += dirZ * moveDist

                    // Orientação de movimento (Yaw): alinha o eixo frontal (+Z) com o vetor (dirX, dirZ)
                    val targetYawRad = atan2(dirX.toDouble(), dirZ.toDouble())
                    val targetYawDeg = (Math.toDegrees(targetYawRad).toFloat() + 360f) % 360f
                    currentYaw = lerpAngle(currentYaw, targetYawDeg, 8f * delta)

                    // Bobbing sutil ao trotar
                    walkBobPhase += delta * 14f
                    currentY = -0.045f + sin(walkBobPhase) * 0.004f
                }
            }

            KiaraBehaviorState.LICKING_PAW, KiaraBehaviorState.RESTING_LOOKOUT -> {
                currentYaw = lerpAngle(currentYaw, targetWp.stopLookoutYaw, 6f * delta)
                currentY = -0.045f

                if (stateTimerSec >= targetWp.stopDurationSec) {
                    currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.size
                    state = KiaraBehaviorState.WALKING
                    stateTimerSec = 0f
                    currentAnimation = "Walk"
                }
            }
        }
    }

    fun lerpAngle(fromDeg: Float, toDeg: Float, alpha: Float): Float {
        var diff = (toDeg - fromDeg) % 360f
        if (diff < -180f) diff += 360f
        if (diff > 180f) diff -= 360f
        val result = fromDeg + diff * alpha.coerceIn(0f, 1f)
        return (result % 360f + 360f) % 360f
    }

    fun toPosition(): Float3 = Float3(currentX, currentY, currentZ)
    fun toRotation(): Float3 = Float3(0f, currentYaw, 0f)
}
