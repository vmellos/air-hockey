package dev.vmellos.airhockey.feature.physics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import dev.romainguy.kotlin.math.Float3
import dev.vmellos.airhockey.core.catalog.canonical
import dev.vmellos.airhockey.core.preferences.CameraZoom3D
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.feature.game.GameMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Posição e rotação estática pré-calibrada da câmera 3D.
 */
data class CameraPreset(
    val position: Float3,
    val rotation: Float3
)

/**
 * Pose dinâmica e mutável da câmera 3D com orientação espacial contínua.
 */
data class CameraPose3D(
    var x: Float,
    var y: Float,
    var z: Float,
    var pitch: Float,
    var yaw: Float = 0f,
    var focusX: Float = 0f,
    var focusY: Float = 0f,
    var focusZ: Float = 0f
) {
    fun toPosition(): Float3 = Float3(x, y, z)
    fun toRotation(): Float3 = Float3(pitch, yaw, 0f)
    fun toFocus(): Float3 = Float3(focusX, focusY, focusZ)
}

/**
 * Base vetorial ortonormal pré-calculada a partir da pose da câmera e da proporção do viewport,
 * evitando recalcular trigonometria de ângulos de Euler para cada partícula ou ponto do rastro.
 */
@Immutable
data class CameraBasis3D(
    val pose: CameraPose3D,
    val aspect: Float,
    val tanHalfFov: Float = Table3DCameraProjection.TAN_HALF_FOV
) {
    val cosPitch: Float
    val sinPitch: Float
    val cosYaw: Float
    val sinYaw: Float

    init {
        val alpha = Math.toRadians(pose.pitch.toDouble()).toFloat()
        cosPitch = cos(alpha)
        sinPitch = sin(alpha)
        val beta = Math.toRadians(pose.yaw.toDouble()).toFloat()
        cosYaw = cos(beta)
        sinYaw = sin(beta)
    }
}

/**
 * Módulo profundo de projeção espacial, enquadramento e desprojeção 3D da Arena (GLOSSARY.md).
 *
 * Encapsula:
 * - Níveis de Enquadramento 3D (Close, Standard, Wide)
 * - Câmera 3D Zenital (Top-Down) para Tela Dividida (ADR 0033)
 * - Ancoragem de Topo na Mesa Estendida 3D e calibração por proporção de tela (ADR 0035)
 * - Cinemática orbital estilo drone da Apresentação de Abertura da Mesa 3D
 * - Desprojeção em Perspectiva 3D (Screen-to-Table) e Projeção (Table-to-Screen)
 */
class Table3DCameraProjection(
    val dimensions: TableDimensions,
    val mode: GameMode = GameMode.SOLO
) {
    val isExtendedHost: Boolean = mode.capabilities.isExtendedHost
    val isExtendedGuest: Boolean = mode.capabilities.isExtendedGuest
    val isExtended: Boolean = mode.capabilities.isExtended
    val isSplitScreen: Boolean = mode.capabilities.isSplitScreen
    val isGuestCamera: Boolean = mode.capabilities.isConnectedGuest

    companion object {
        // Lente padrão de 28mm em sensor full-frame (24mm vertical) do Filament
        const val TAN_HALF_FOV: Float = 12f / 28f
        const val FOV_COEFF: Float = 2f * TAN_HALF_FOV
        const val Z_TOP_EXTENDED: Float = -1.08f
        const val METRIC_SCALE_X: Float = 0.00108f
        const val METRIC_SCALE_Z_STANDARD: Float = 0.00096f
        const val METRIC_SCALE_Z_EXTENDED: Float = 0.00108f

        /**
         * Retorna o preset estático da câmera 3D para o nível de zoom e modalidade informados.
         */
        fun getCameraPreset(
            zoom: CameraZoom3D,
            isSplitScreen: Boolean = false,
            isGuest: Boolean = false,
            isExtended: Boolean = false
        ): CameraPreset {
            val mode = when {
                isExtended -> GameMode.EXTENDED_HOST
                isSplitScreen -> GameMode.SPLIT_SCREEN
                isGuest -> GameMode.CONNECTED_GUEST
                else -> GameMode.SOLO
            }
            return Table3DCameraProjection(TableDimensions(), mode).getBasePreset(zoom)
        }
    }

    /**
     * Retorna o preset de câmera base correspondente ao nível de zoom e modalidade.
     */
    fun getBasePreset(zoom: CameraZoom3D = CameraZoom3D.STANDARD): CameraPreset {
        return when {
            isExtended -> {
                val tanHalfFov = TAN_HALF_FOV
                val zTop = Z_TOP_EXTENDED
                when (zoom) {
                    CameraZoom3D.CLOSE -> {
                        val y = 3.00f
                        CameraPreset(Float3(0f, y, zTop + y * tanHalfFov), Float3(-90f, 0f, 0f))
                    }
                    CameraZoom3D.STANDARD -> {
                        val y = 3.35f
                        CameraPreset(Float3(0f, y, zTop + y * tanHalfFov), Float3(-90f, 0f, 0f))
                    }
                    CameraZoom3D.WIDE -> {
                        val y = 3.80f
                        CameraPreset(Float3(0f, y, zTop + y * tanHalfFov), Float3(-90f, 0f, 0f))
                    }
                }
            }
            isSplitScreen -> when (zoom) {
                CameraZoom3D.CLOSE -> CameraPreset(Float3(0f, 2.35f, 0f), Float3(-90f, 0f, 0f))
                CameraZoom3D.STANDARD -> CameraPreset(Float3(0f, 2.55f, 0f), Float3(-90f, 0f, 0f))
                CameraZoom3D.WIDE -> CameraPreset(Float3(0f, 2.85f, 0f), Float3(-90f, 0f, 0f))
            }
            isGuestCamera -> when (zoom) {
                CameraZoom3D.CLOSE -> CameraPreset(Float3(0f, 2.10f, -1.80f), Float3(-47f, 180f, 0f))
                CameraZoom3D.STANDARD -> CameraPreset(Float3(0f, 2.50f, -2.15f), Float3(-46f, 180f, 0f))
                CameraZoom3D.WIDE -> CameraPreset(Float3(0f, 2.95f, -2.55f), Float3(-45f, 180f, 0f))
            }
            else -> when (zoom) {
                CameraZoom3D.CLOSE -> CameraPreset(Float3(0f, 2.10f, 1.80f), Float3(-47f, 0f, 0f))
                CameraZoom3D.STANDARD -> CameraPreset(Float3(0f, 2.50f, 2.15f), Float3(-46f, 0f, 0f))
                CameraZoom3D.WIDE -> CameraPreset(Float3(0f, 2.95f, 2.55f), Float3(-45f, 0f, 0f))
            }
        }
    }

    /**
     * Calcula o preset efetivo alvo adaptado analiticamente à proporção de aspecto do display
     * (Ancoragem de Topo na Mesa Estendida 3D — ADR 0035).
     */
    fun computeEffectiveTargetPreset(
        zoom: CameraZoom3D = CameraZoom3D.STANDARD,
        screenAspect: Float,
        theme: TableTheme? = null,
        needsWideMascotFraming: Boolean = false
    ): CameraPreset {
        val base = getBasePreset(zoom)
        if (screenAspect <= 0.05f) {
            return base
        }

        if (isExtended) {
            val targetWidth = 1.28f
            val targetLength = 2.30f
            val requiredYForWidth = targetWidth / (FOV_COEFF * screenAspect)
            val requiredYForLength = targetLength / FOV_COEFF
            val baseY = kotlin.math.max(requiredYForWidth, requiredYForLength)

            val zoomFactor = when (zoom) {
                CameraZoom3D.CLOSE -> 0.90f
                CameraZoom3D.STANDARD -> 1.0f
                CameraZoom3D.WIDE -> 1.15f
            }
            val calibratedY = baseY * zoomFactor
            val calibratedZ = Z_TOP_EXTENDED + calibratedY * TAN_HALF_FOV
            return base.copy(position = Float3(0f, calibratedY, calibratedZ))
        }

        // Calibração adaptativa para modo paisagem em tablets e telas largas (screenAspect > 1.15f) — ADR 0041
        if (screenAspect > 1.15f) {
            return when {
                isSplitScreen -> when (zoom) {
                    CameraZoom3D.CLOSE -> CameraPreset(Float3(0f, 2.15f, 0f), Float3(-90f, 0f, 0f))
                    CameraZoom3D.STANDARD -> CameraPreset(Float3(0f, 2.35f, 0f), Float3(-90f, 0f, 0f))
                    CameraZoom3D.WIDE -> CameraPreset(Float3(0f, 2.65f, 0f), Float3(-90f, 0f, 0f))
                }
                isGuestCamera -> when (zoom) {
                    CameraZoom3D.CLOSE -> CameraPreset(Float3(0f, 1.70f, -1.45f), Float3(-49f, 180f, 0f))
                    CameraZoom3D.STANDARD -> CameraPreset(Float3(0f, 2.15f, -1.85f), Float3(-47f, 180f, 0f))
                    CameraZoom3D.WIDE -> CameraPreset(Float3(0f, 2.60f, -2.25f), Float3(-45f, 180f, 0f))
                }
                else -> when (zoom) {
                    CameraZoom3D.CLOSE -> CameraPreset(Float3(0f, 1.70f, 1.45f), Float3(-49f, 0f, 0f))
                    CameraZoom3D.STANDARD -> CameraPreset(Float3(0f, 2.15f, 1.85f), Float3(-47f, 0f, 0f))
                    CameraZoom3D.WIDE -> CameraPreset(Float3(0f, 2.60f, 2.25f), Float3(-45f, 0f, 0f))
                }
            }
        }

        // Calibração de Enquadramento 3D para Arena com Mascote em telas de celular retrato
        val needsWideFraming = needsWideMascotFraming || (theme != null && theme.canonical() == TableTheme.KIARA_CAT)
        if (needsWideFraming && !isSplitScreen) {
            return if (isGuestCamera) {
                when (zoom) {
                    CameraZoom3D.CLOSE -> CameraPreset(Float3(0f, 2.45f, -2.10f), Float3(-47f, 180f, 0f))
                    CameraZoom3D.STANDARD -> CameraPreset(Float3(0f, 2.85f, -2.45f), Float3(-46f, 180f, 0f))
                    CameraZoom3D.WIDE -> CameraPreset(Float3(0f, 3.25f, -2.80f), Float3(-45f, 180f, 0f))
                }
            } else {
                when (zoom) {
                    CameraZoom3D.CLOSE -> CameraPreset(Float3(0f, 2.45f, 2.10f), Float3(-47f, 0f, 0f))
                    CameraZoom3D.STANDARD -> CameraPreset(Float3(0f, 2.85f, 2.45f), Float3(-46f, 0f, 0f))
                    CameraZoom3D.WIDE -> CameraPreset(Float3(0f, 3.25f, 2.80f), Float3(-45f, 0f, 0f))
                }
            }
        }

        return base
    }

    /**
     * Calcula a coordenada longitudinal Z do ponto de foco no plano da mesa (Y = 0)
     * correspondente ao preset alvo, respeitando a inversão de 180° do Dispositivo Convidado
     * e evitando singularidade vetorial no ângulo zenital (-90°) da Mesa Dividida.
     */
    fun computeFinalFocusZ(targetPreset: CameraPreset): Float {
        val pitchRad = Math.toRadians((-targetPreset.rotation.x).toDouble()).toFloat()
        val tanPitch = kotlin.math.tan(pitchRad)
        val horizDist = if (abs(tanPitch) < 1e-3f) {
            0.005f
        } else {
            (targetPreset.position.y / tanPitch).coerceAtLeast(0.005f)
        }
        val zDir = if (isGuestCamera) 1f else -1f
        return targetPreset.position.z + zDir * horizDist
    }

    /**
     * Calcula a pose tridimensional da câmera durante a Apresentação de Abertura da Mesa 3D
     * (voo de drone cinematográfico em arco rasante com foco dinâmico e aterrissagem suave).
     */
    fun computePresentationPose(
        progress: Float,
        targetPreset: CameraPreset
    ): CameraPose3D {
        val p = progress.coerceIn(0f, 1f)
        val finalFocusZ = computeFinalFocusZ(targetPreset)

        if (p >= 1f) {
            return CameraPose3D(
                x = targetPreset.position.x,
                y = targetPreset.position.y,
                z = targetPreset.position.z,
                pitch = targetPreset.rotation.x,
                yaw = targetPreset.rotation.y,
                focusX = 0f,
                focusY = 0f,
                focusZ = finalFocusZ
            )
        }

        // Curva suave de aceleração e desaceleração (Smootherstep)
        val t = (p * p * p * (p * (p * 6f - 15f) + 10f)).coerceIn(0f, 1f)
        val sideSign = if (isGuestCamera) -1f else 1f

        val targetX = targetPreset.position.x
        val targetY = targetPreset.position.y
        val targetZ = targetPreset.position.z

        // Posição do drone e foco dinâmico ao longo da spline de sobrevoo
        val (camPos, focusPos) = if (t < 0.5f) {
            val u = t / 0.5f
            val uInv = 1f - u
            // Fase 1: Voo descendente da cabeceira adversária até a lateral média
            val cx = sideSign * (-1.60f * uInv * uInv + -1.40f * 2f * uInv * u + -1.25f * u * u)
            val cy = 2.70f * uInv * uInv + 1.65f * 2f * uInv * u + 1.45f * u * u
            val cz = sideSign * (-2.60f * uInv * uInv + -1.15f * 2f * uInv * u + 0.10f * u * u)

            val fx = 0.0f
            val fy = 0.05f * uInv + 0.02f * u
            val fz = sideSign * (-1.10f * uInv + 0.0f * u)

            Pair(Float3(cx, cy, cz), Float3(fx, fy, fz))
        } else {
            val u = (t - 0.5f) / 0.5f
            val uInv = 1f - u
            // Fase 2: Curva rasante pela lateral, contorno da meta e ascensão para targetPreset
            val cx = (sideSign * -1.25f) * uInv * uInv + (sideSign * 0.45f) * 2f * uInv * u + targetX * u * u
            val cy = 1.45f * uInv * uInv + 1.75f * 2f * uInv * u + targetY * u * u
            val cz = (sideSign * 0.10f) * uInv * uInv + (sideSign * 2.30f) * 2f * uInv * u + targetZ * u * u

            val fx = 0.0f
            val fy = 0.02f * uInv + 0.0f * u
            val fz = 0.0f * uInv + finalFocusZ * u

            Pair(Float3(cx, cy, cz), Float3(fx, fy, fz))
        }

        val dx = focusPos.x - camPos.x
        val dy = focusPos.y - camPos.y
        val dz = focusPos.z - camPos.z
        val distHorizontal = kotlin.math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()

        var pitch = -Math.toDegrees(kotlin.math.atan2(-dy.toDouble(), distHorizontal.toDouble())).toFloat()
        var yaw = Math.toDegrees(kotlin.math.atan2(dx.toDouble(), -dz.toDouble())).toFloat()

        if (isSplitScreen && t > 0.60f) {
            val blend = ((t - 0.60f) / 0.40f).coerceIn(0f, 1f)
            val blendSmooth = blend * blend * (3f - 2f * blend)
            pitch = pitch * (1f - blendSmooth) + targetPreset.rotation.x * blendSmooth
            yaw = yaw * (1f - blendSmooth)
        } else if (isGuestCamera && t > 0.60f) {
            val blend = ((t - 0.60f) / 0.40f).coerceIn(0f, 1f)
            val blendSmooth = blend * blend * (3f - 2f * blend)
            pitch = pitch * (1f - blendSmooth) + targetPreset.rotation.x * blendSmooth
            val normalizedYaw = if (yaw < 0f) yaw + 360f else yaw
            yaw = normalizedYaw * (1f - blendSmooth) + targetPreset.rotation.y * blendSmooth
        }

        return CameraPose3D(
            x = camPos.x,
            y = camPos.y,
            z = camPos.z,
            pitch = pitch,
            yaw = yaw,
            focusX = focusPos.x,
            focusY = focusPos.y,
            focusZ = focusPos.z
        )
    }

    /**
     * Interpola suavemente a pose atual em direção ao preset alvo de forma independente do FPS.
     */
    fun stepSmoothPose(
        current: CameraPose3D,
        target: CameraPreset,
        factor: Float = 0.15f,
        dtSec: Float = 1f / 60f
    ): CameraPose3D {
        val clampedDt = dtSec.coerceIn(0.001f, 0.10f)
        val effectiveFactor = (1.0 - Math.pow((1.0 - factor.coerceIn(0f, 1f)).toDouble(), (clampedDt * 60f).toDouble())).toFloat()
        val finalFocusZ = computeFinalFocusZ(target)
        return CameraPose3D(
            x = current.x + (target.position.x - current.x) * effectiveFactor,
            y = current.y + (target.position.y - current.y) * effectiveFactor,
            z = current.z + (target.position.z - current.z) * effectiveFactor,
            pitch = current.pitch + (target.rotation.x - current.pitch) * effectiveFactor,
            yaw = current.yaw + (target.rotation.y - current.yaw) * effectiveFactor,
            focusX = 0f,
            focusY = 0f,
            focusZ = finalFocusZ
        )
    }

    /**
     * Converte coordenada virtual X da física (0..1000) em coordenada métrica mundial Filament.
     */
    fun mapEngineX(engineX: Float): Float {
        val lx = if (isExtended) {
            if (isExtendedGuest) 1000f - engineX else engineX
        } else {
            engineX
        }
        return (lx - 500f) * METRIC_SCALE_X
    }

    /**
     * Converte coordenada virtual Y da física em coordenada métrica longitudinal Z Filament.
     */
    fun mapEngineZ(engineY: Float): Float {
        val ly = if (isExtended) {
            if (isExtendedGuest) dimensions.hostHalfBottomY - engineY else engineY - dimensions.guestHalfTopY
        } else {
            engineY
        }
        val scaleZ = if (isExtended) METRIC_SCALE_Z_EXTENDED else METRIC_SCALE_Z_STANDARD
        return (ly - 1000f) * scaleZ
    }

    /**
     * Pré-calcula a base vetorial ortonormal da câmera para reuso em múltiplos pontos por frame.
     */
    fun computeBasis(screenW: Float, screenH: Float, pose: CameraPose3D): CameraBasis3D? {
        if (screenW <= 0f || screenH <= 0f) return null
        return CameraBasis3D(pose = pose, aspect = screenW / screenH, tanHalfFov = TAN_HALF_FOV)
    }

    /**
     * Desprojeção em Perspectiva 3D: Converte coordenadas da tela (pixels) para coordenadas virtuais da mesa.
     */
    fun screenToTable(
        screenX: Float,
        screenY: Float,
        screenW: Float,
        screenH: Float,
        pose: CameraPose3D
    ): TablePoint? {
        val basis = computeBasis(screenW, screenH, pose) ?: return null
        return screenToTable(screenX, screenY, screenW, screenH, basis)
    }

    fun screenToTable(
        screenX: Float,
        screenY: Float,
        screenW: Float,
        screenH: Float,
        basis: CameraBasis3D
    ): TablePoint? {
        if (screenW <= 0f || screenH <= 0f) return null
        val pose = basis.pose
        val aspect = basis.aspect
        val tanHalfFov = basis.tanHalfFov

        val ndcX = (2f * screenX / screenW) - 1f
        val ndcY = 1f - (2f * screenY / screenH)

        val rx = ndcX * aspect * tanHalfFov
        val ry = ndcY * tanHalfFov
        val rz = -1f

        val pitchDx = rx
        val pitchDy = ry * basis.cosPitch - rz * basis.sinPitch
        val pitchDz = ry * basis.sinPitch + rz * basis.cosPitch

        val dx = pitchDx * basis.cosYaw + pitchDz * basis.sinYaw
        val dy = pitchDy
        val dz = -pitchDx * basis.sinYaw + pitchDz * basis.cosYaw

        if (abs(dy) < 1e-4f) return null
        val t = -pose.y / dy
        if (t <= 0f) return null

        val worldX = pose.x + t * dx
        val worldZ = pose.z + t * dz

        val scaleZ = if (isExtended) METRIC_SCALE_Z_EXTENDED else METRIC_SCALE_Z_STANDARD
        val lx = 500f + worldX / METRIC_SCALE_X
        val ly = 1000f + worldZ / scaleZ

        if (isExtended) {
            val globalX = if (isExtendedGuest) 1000f - lx else lx
            val globalY = if (isExtendedGuest) dimensions.hostHalfBottomY - ly else dimensions.guestHalfTopY + ly
            return TablePoint(globalX, globalY)
        }

        return TablePoint(lx, ly)
    }

    /**
     * Projeta um ponto métrico da mesa para coordenadas da tela em pixels sob a perspectiva da câmera 3D.
     */
    fun tableToScreen(
        tableX: Float,
        tableY: Float,
        screenW: Float,
        screenH: Float,
        pose: CameraPose3D,
        elevationY: Float = 0.0075f
    ): Offset? {
        val basis = computeBasis(screenW, screenH, pose) ?: return null
        return tableToScreen(tableX, tableY, screenW, screenH, basis, elevationY)
    }

    fun tableToScreen(
        tableX: Float,
        tableY: Float,
        screenW: Float,
        screenH: Float,
        basis: CameraBasis3D,
        elevationY: Float = 0.0075f
    ): Offset? {
        if (screenW <= 0f || screenH <= 0f) return null
        val pose = basis.pose
        val aspect = basis.aspect
        val tanHalfFov = basis.tanHalfFov

        val lx = if (isExtended) {
            if (isExtendedGuest) 1000f - tableX else tableX
        } else tableX

        val ly = if (isExtended) {
            if (isExtendedGuest) dimensions.hostHalfBottomY - tableY else tableY - dimensions.guestHalfTopY
        } else tableY

        val scaleZ = if (isExtended) METRIC_SCALE_Z_EXTENDED else METRIC_SCALE_Z_STANDARD
        val worldX = (lx - 500f) * METRIC_SCALE_X
        val worldY = elevationY
        val worldZ = (ly - 1000f) * scaleZ

        val dx = worldX - pose.x
        val dy = worldY - pose.y
        val dz = worldZ - pose.z

        // Rotação inversa de Yaw (-beta): cos(-beta) = cosYaw, sin(-beta) = -sinYaw
        val yawDx = dx * basis.cosYaw + dz * basis.sinYaw
        val yawDy = dy
        val yawDz = -dx * basis.sinYaw + dz * basis.cosYaw

        val camXPrime = yawDx
        val camYPrime = yawDy * basis.cosPitch + yawDz * basis.sinPitch
        val camZPrime = -yawDy * basis.sinPitch + yawDz * basis.cosPitch

        if (camZPrime >= -0.05f) return null

        val depth = -camZPrime
        val rx = camXPrime / depth
        val ry = camYPrime / depth

        val ndcX = rx / (aspect * tanHalfFov)
        val ndcY = ry / tanHalfFov

        val screenX = (ndcX + 1f) * 0.5f * screenW
        val screenY = (1f - ndcY) * 0.5f * screenH
        return Offset(screenX, screenY)
    }

    /**
     * Determina se um objeto com dado raio é visível no viewport ativo da mesa (metade visível ou tela inteira).
     */
    fun isVisibleInViewport(tableX: Float, tableY: Float, radius: Float): Boolean {
        if (tableX + radius < 0f || tableX - radius > dimensions.width) return false

        return when {
            isExtendedHost -> {
                tableY + radius >= dimensions.guestHalfTopY && tableY - radius <= dimensions.totalHeight
            }
            isExtendedGuest -> {
                tableY + radius >= 0f && tableY - radius <= dimensions.hostHalfBottomY
            }
            else -> {
                tableY + radius >= 0f && tableY - radius <= dimensions.totalHeight
            }
        }
    }
}
