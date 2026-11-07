package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import dev.romainguy.kotlin.math.Float3
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.core.preferences.CameraZoom3D
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.GameViewModel
import dev.vmellos.airhockey.feature.game.MatchStatus
import dev.vmellos.airhockey.feature.physics.CollisionEvent
import dev.vmellos.airhockey.feature.physics.TableDimensions
import dev.vmellos.airhockey.feature.physics.TablePoint
import dev.vmellos.airhockey.feature.physics.TableProjection
import io.github.sceneview.Scene
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class CameraPreset(
    val position: Float3,
    val rotation: Float3
)

fun getCameraPreset(
    zoom: CameraZoom3D,
    isSplitScreen: Boolean = false,
    isGuest: Boolean = false,
    isExtended: Boolean = false
): CameraPreset =
    if (isExtended) {
        val tanHalfFov = 12f / 28f
        val zTop = -1.08f
        when (zoom) {
            CameraZoom3D.CLOSE -> {
                val y = 3.00f
                CameraPreset(
                    position = Float3(0f, y, zTop + y * tanHalfFov),
                    rotation = Float3(-90f, 0f, 0f)
                )
            }
            CameraZoom3D.STANDARD -> {
                val y = 3.35f
                CameraPreset(
                    position = Float3(0f, y, zTop + y * tanHalfFov),
                    rotation = Float3(-90f, 0f, 0f)
                )
            }
            CameraZoom3D.WIDE -> {
                val y = 3.80f
                CameraPreset(
                    position = Float3(0f, y, zTop + y * tanHalfFov),
                    rotation = Float3(-90f, 0f, 0f)
                )
            }
        }
    } else if (isSplitScreen) {
        when (zoom) {
            CameraZoom3D.CLOSE -> CameraPreset(
                position = Float3(0f, 2.35f, 0f),
                rotation = Float3(-90f, 0f, 0f)
            )
            CameraZoom3D.STANDARD -> CameraPreset(
                position = Float3(0f, 2.55f, 0f),
                rotation = Float3(-90f, 0f, 0f)
            )
            CameraZoom3D.WIDE -> CameraPreset(
                position = Float3(0f, 2.85f, 0f),
                rotation = Float3(-90f, 0f, 0f)
            )
        }
    } else if (isGuest) {
        when (zoom) {
            CameraZoom3D.CLOSE -> CameraPreset(
                position = Float3(0f, 2.10f, -1.80f),
                rotation = Float3(-47f, 180f, 0f)
            )
            CameraZoom3D.STANDARD -> CameraPreset(
                position = Float3(0f, 2.50f, -2.15f),
                rotation = Float3(-46f, 180f, 0f)
            )
            CameraZoom3D.WIDE -> CameraPreset(
                position = Float3(0f, 2.95f, -2.55f),
                rotation = Float3(-45f, 180f, 0f)
            )
        }
    } else {
        when (zoom) {
            CameraZoom3D.CLOSE -> CameraPreset(
                position = Float3(0f, 2.10f, 1.80f),
                rotation = Float3(-47f, 0f, 0f)
            )
            CameraZoom3D.STANDARD -> CameraPreset(
                position = Float3(0f, 2.50f, 2.15f),
                rotation = Float3(-46f, 0f, 0f)
            )
            CameraZoom3D.WIDE -> CameraPreset(
                position = Float3(0f, 2.95f, 2.55f),
                rotation = Float3(-45f, 0f, 0f)
            )
        }
    }

/**
 * Estado operacional da Arena 3D desacoplado do ciclo de recomposição do Jetpack Compose.
 */
private class Scene3DState(
    initialPreset: CameraPreset,
    initialPresenting: Boolean = true
) {
    var presentationProgress: Float = if (initialPresenting) 0f else 1f
    var isPresenting: Boolean = initialPresenting

    // Posição e rotação contínuas interpoladas da câmera (atualizadas no onFrame a 60/120 Hz)
    var camX: Float = initialPreset.position.x
    var camY: Float = initialPreset.position.y
    var camZ: Float = initialPreset.position.z
    var camPitch: Float = initialPreset.rotation.x
    var camYaw: Float = initialPreset.rotation.y

    // Giroscópio procedural do disco (rotação em torno do eixo vertical Y)
    var puckYawDeg: Float = 0f
}

/**
 * Desprojeção em Perspectiva 3D:
 * Converte o ponto tocado na tela (pixels) para as coordenadas métricas da mesa (0..1000 x 0..2000)
 * através da interseção geométrica do raio da câmera em perspectiva com o plano Y = 0 da mesa.
 */
internal fun screenToTable3D(
    screenX: Float,
    screenY: Float,
    w: Float,
    h: Float,
    camX: Float,
    camY: Float,
    camZ: Float,
    camPitchDeg: Float,
    camYawDeg: Float = 0f,
    isExtended: Boolean = false,
    isExtendedGuest: Boolean = false,
    dimensions: TableDimensions? = null
): TablePoint? {
    if (w <= 0f || h <= 0f) return null
    val aspect = w / h

    // Lente padrão de 28mm em sensor full-frame (24mm vertical) do Filament
    val fovY = 2f * kotlin.math.atan(12f / 28f)
    val tanHalfFov = kotlin.math.tan(fovY / 2f)
    val ndcX = (2f * screenX / w) - 1f
    val ndcY = 1f - (2f * screenY / h)

    val rx = ndcX * aspect * tanHalfFov
    val ry = ndcY * tanHalfFov
    val rz = -1f

    val alpha = Math.toRadians(camPitchDeg.toDouble()).toFloat()
    val cosA = cos(alpha)
    val sinA = sin(alpha)

    val pitchDx = rx
    val pitchDy = ry * cosA - rz * sinA
    val pitchDz = ry * sinA + rz * cosA

    val beta = Math.toRadians(camYawDeg.toDouble()).toFloat()
    val cosB = cos(beta)
    val sinB = sin(beta)

    // Rotação em torno do eixo Y pelo ângulo Yaw da câmera
    val dx = pitchDx * cosB + pitchDz * sinB
    val dy = pitchDy
    val dz = -pitchDx * sinB + pitchDz * cosB

    if (abs(dy) < 1e-4f) return null
    val t = -camY / dy
    if (t <= 0f) return null

    val worldX = camX + t * dx
    val worldZ = camZ + t * dz

    val scaleZ = if (isExtended) 0.00108f else 0.00096f
    val lx = 500f + worldX / 0.00108f
    val ly = 1000f + worldZ / scaleZ

    if (isExtended && dimensions != null) {
        val globalX = if (isExtendedGuest) 1000f - lx else lx
        val globalY = if (isExtendedGuest) dimensions.hostHalfBottomY - ly else dimensions.guestHalfTopY + ly
        return TablePoint(globalX, globalY)
    }

    return TablePoint(lx, ly)
}

/**
 * Projeta um ponto métrico da mesa (0..1000 x 0..2000 ou mesa estendida) para coordenadas de tela em pixels
 * sob a perspectiva exata da câmera 3D (World-to-Screen).
 */
internal fun tableToScreen3D(
    tableX: Float,
    tableY: Float,
    w: Float,
    h: Float,
    camX: Float,
    camY: Float,
    camZ: Float,
    camPitchDeg: Float,
    camYawDeg: Float = 0f,
    elevationY: Float = 0.0075f,
    isExtended: Boolean = false,
    isExtendedGuest: Boolean = false,
    dimensions: TableDimensions? = null
): Offset? {
    if (w <= 0f || h <= 0f) return null
    val aspect = w / h

    val fovY = 2f * kotlin.math.atan(12f / 28f)
    val tanHalfFov = kotlin.math.tan(fovY / 2f)

    val lx = if (isExtended && dimensions != null) {
        if (isExtendedGuest) 1000f - tableX else tableX
    } else tableX

    val ly = if (isExtended && dimensions != null) {
        if (isExtendedGuest) dimensions.hostHalfBottomY - tableY else tableY - dimensions.guestHalfTopY
    } else tableY

    val scaleZ = if (isExtended) 0.00108f else 0.00096f
    val worldX = (lx - 500f) * 0.00108f
    val worldY = elevationY
    val worldZ = (ly - 1000f) * scaleZ

    val dx = worldX - camX
    val dy = worldY - camY
    val dz = worldZ - camZ

    val beta = Math.toRadians(-camYawDeg.toDouble()).toFloat()
    val cosB = cos(beta)
    val sinB = sin(beta)

    val yawDx = dx * cosB - dz * sinB
    val yawDy = dy
    val yawDz = dx * sinB + dz * cosB

    val alpha = Math.toRadians(camPitchDeg.toDouble()).toFloat()
    val cosA = cos(alpha)
    val sinA = sin(alpha)

    val camXPrime = yawDx
    val camYPrime = yawDy * cosA + yawDz * sinA
    val camZPrime = -yawDy * sinA + yawDz * cosA

    if (camZPrime >= -0.05f) return null

    val depth = -camZPrime
    val rx = camXPrime / depth
    val ry = camYPrime / depth

    val ndcX = rx / (aspect * tanHalfFov)
    val ndcY = ry / tanHalfFov

    val screenX = (ndcX + 1f) * 0.5f * w
    val screenY = (1f - ndcY) * 0.5f * h
    return Offset(screenX, screenY)
}

/**
 * Faísca de impacto projetada na mesa 3D.
 */
private class SparkParticle3D {
    var x: Float = 0f
    var y: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var color: Color = Color.Cyan
    var size: Float = 3.5f
    var life: Float = 0f
    var maxLife: Float = 0.5f
    var active: Boolean = false

    fun init(startX: Float, startY: Float, vX: Float, vY: Float, c: Color, s: Float, duration: Float) {
        x = startX
        y = startY
        vx = vX
        vy = vY
        color = c
        size = s
        life = 0f
        maxLife = duration
        active = true
    }

    fun update(dt: Float) {
        if (!active) return
        x += vx * dt
        y += vy * dt
        vx *= 0.92f
        vy *= 0.92f
        life += dt
        if (life >= maxLife) {
            active = false
        }
    }
}

/**
 * Onda de choque expansiva projetada na mesa 3D.
 */
private class VisualShockwave3D {
    var x: Float = 0f
    var y: Float = 0f
    var color: Color = Color.Cyan
    var maxRadius: Float = 160f
    var progress: Float = 0f
    var active: Boolean = false

    fun init(startX: Float, startY: Float, waveColor: Color, radius: Float) {
        x = startX
        y = startY
        color = waveColor
        maxRadius = radius
        progress = 0f
        active = true
    }

    fun update(speed: Float) {
        if (!active) return
        progress += speed
        if (progress >= 1f) {
            active = false
        }
    }
}

/**
 * Gerenciador de partículas e ondas de choque em coordenadas da mesa 3D.
 */
private class ParticlePool3D(
    private val maxSparks: Int = 60,
    private val maxShockwaves: Int = 10
) {
    private val sparks = Array(maxSparks) { SparkParticle3D() }
    private val shockwaves = Array(maxShockwaves) { VisualShockwave3D() }
    private val rng = Random(42)

    fun spawnSparks(
        x: Float,
        y: Float,
        count: Int,
        baseColor: Color,
        spreadSpeed: Float = 550f,
        duration: Float = 0.5f
    ) {
        var spawned = 0
        for (p in sparks) {
            if (!p.active) {
                val angle = rng.nextFloat() * 2f * Math.PI.toFloat()
                val speed = (0.35f + rng.nextFloat() * 0.65f) * spreadSpeed
                val vx = cos(angle) * speed
                val vy = sin(angle) * speed
                val size = 2.5f + rng.nextFloat() * 3.5f
                val color = if (rng.nextFloat() > 0.35f) baseColor else Color(0xFFFFD54F)
                p.init(x, y, vx, vy, color, size, duration * (0.7f + rng.nextFloat() * 0.5f))
                spawned++
                if (spawned >= count) break
            }
        }
    }

    fun spawnShockwave(x: Float, y: Float, color: Color, maxRadius: Float = 160f) {
        for (sw in shockwaves) {
            if (!sw.active) {
                sw.init(x, y, color, maxRadius)
                break
            }
        }
    }

    fun update(dt: Float) {
        for (p in sparks) {
            if (p.active) p.update(dt)
        }
        for (sw in shockwaves) {
            if (sw.active) sw.update(0.045f)
        }
    }

    fun draw(
        drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
        w: Float,
        h: Float,
        camX: Float,
        camY: Float,
        camZ: Float,
        camPitchDeg: Float,
        camYawDeg: Float = 0f,
        isExtended: Boolean = false,
        isExtendedGuest: Boolean = false,
        dimensions: TableDimensions? = null
    ) {
        for (sw in shockwaves) {
            if (!sw.active) continue
            val center = tableToScreen3D(sw.x, sw.y, w, h, camX, camY, camZ, camPitchDeg, camYawDeg, isExtended = isExtended, isExtendedGuest = isExtendedGuest, dimensions = dimensions) ?: continue
            val currentR = sw.maxRadius * sw.progress
            val edge = tableToScreen3D(sw.x + currentR, sw.y, w, h, camX, camY, camZ, camPitchDeg, camYawDeg, isExtended = isExtended, isExtendedGuest = isExtendedGuest, dimensions = dimensions)
            val screenR = if (edge != null) abs(edge.x - center.x).coerceAtLeast(4f) else (sw.maxRadius * 0.5f * sw.progress)
            val alpha = (1f - sw.progress).coerceIn(0f, 1f)

            drawScope.drawCircle(
                color = sw.color.copy(alpha = alpha * 0.8f),
                radius = screenR,
                center = center,
                style = Stroke(width = 4f * alpha)
            )
        }

        for (p in sparks) {
            if (!p.active) continue
            val pos = tableToScreen3D(p.x, p.y, w, h, camX, camY, camZ, camPitchDeg, camYawDeg, isExtended = isExtended, isExtendedGuest = isExtendedGuest, dimensions = dimensions) ?: continue
            val alpha = (1f - (p.life / p.maxLife)).coerceIn(0f, 1f)
            drawScope.drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size,
                center = pos
            )
        }
    }
}

/**
 * Renderizador Tridimensional da Arena de Air Hockey utilizando o Sceneview (Google Filament).
 * 
 * Mantém a simulação física 2D soberana via [AirHockeyPhysicsEngine] e projeta as coordenadas
 * espaciais métricas para a Mesa com Piso da Arena, Rebatedores P1/P2 e Disco gerados no Blender.
 * 
 * Incorpora Efeitos Visuais Sci-Fi Arcade de alta performance acelerados por hardware:
 * - Rastro de Energia 3D no Disco
 * - Efeitos de Impacto 3D (Ondas de Choque e Faíscas incandescentes em tempo real)
 * - Barreira de Energia da Meta 3D com Pulso de Celebração de Ponto
 * - Rotação Giroscópica do Disco com rotação angular vinculada à velocidade
 */
@Composable
fun AirHockeyArena3DScene(
    viewModel: GameViewModel,
    mode: GameMode,
    zoom: CameraZoom3D = CameraZoom3D.STANDARD,
    tableTheme: TableTheme? = null,
    onPresentationFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val engine = viewModel.engine
    val dimensions = engine.dimensions

    val prefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val activeTheme = tableTheme ?: prefs.tableTheme
    val touchOffset = if (prefs.touchOffsetEnabled) 110f else 0f
    val matchState by viewModel.matchState.collectAsState()

    val isExtendedHost = mode == GameMode.EXTENDED_HOST || mode == GameMode.AI_VS_AI_EXTENDED_HOST
    val isExtendedGuest = mode == GameMode.EXTENDED_GUEST || mode == GameMode.AI_VS_AI_EXTENDED_GUEST
    val isExtended = isExtendedHost || isExtendedGuest || mode == GameMode.AI_VS_AI_EXTENDED
    val isSplitScreen = mode == GameMode.SPLIT_SCREEN
    val isGuestCamera = mode == GameMode.CONNECTED_GUEST || mode == GameMode.AI_VS_AI_CONNECTED_GUEST
    val projection = remember(dimensions, mode) { TableProjection(dimensions, mode) }
    val canPresentIntro = !isExtended && prefs.arena3dIntroEnabled
    val targetPreset = remember(zoom, isSplitScreen, isGuestCamera, isExtended) {
        getCameraPreset(zoom, isSplitScreen, isGuestCamera, isExtended)
    }
    val sceneState = remember(isSplitScreen, isExtended, isGuestCamera) {
        Scene3DState(targetPreset, initialPresenting = canPresentIntro)
    }

    val coroutineScope = rememberCoroutineScope()
    val presentationAnim = remember(isSplitScreen, isExtended) {
        Animatable(if (canPresentIntro) 0f else 1f)
    }

    val inputController = remember { ArenaInputController() }
    val particlePool = remember { ParticlePool3D(maxSparks = 60, maxShockwaves = 10) }
    val puckTrail = remember { ArrayDeque<TablePoint>(14) }
    val pendingCollisions = remember { ArrayDeque<Triple<Float, Float, CollisionEvent>>(16) }

    // Apresentação de Abertura da Mesa 3D (Voo panorâmico cinematográfico estilo drone)
    LaunchedEffect(matchState.status, canPresentIntro) {
        if (!canPresentIntro || matchState.status != MatchStatus.PRESENTATION) {
            if (sceneState.isPresenting && !canPresentIntro) {
                sceneState.presentationProgress = 1f
                sceneState.isPresenting = false
                presentationAnim.snapTo(1f)
                onPresentationFinished()
            }
            return@LaunchedEffect
        }

        sceneState.isPresenting = true
        sceneState.presentationProgress = 0f
        presentationAnim.snapTo(0f)
        try {
            presentationAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 6500,
                    easing = FastOutSlowInEasing
                )
            ) {
                sceneState.presentationProgress = value
            }
            sceneState.presentationProgress = 1f
            if (sceneState.isPresenting) {
                sceneState.isPresenting = false
                onPresentationFinished()
            }
        } finally {
            sceneState.presentationProgress = 1f
            sceneState.isPresenting = false
        }
    }

    // Higienização de Ponteiros de Toque
    LaunchedEffect(matchState.status) {
        if (matchState.status != MatchStatus.PLAYING) {
            inputController.resetPointers()
            puckTrail.clear()
        }
    }

    DisposableEffect(engine) {
        inputController.resetPointers()
        val listener: (Float, Float, CollisionEvent) -> Unit = { px, py, evt ->
            synchronized(pendingCollisions) {
                if (pendingCollisions.size < 16) {
                    pendingCollisions.addLast(Triple(px, py, evt))
                }
            }
        }
        engine.onVisualCollisionListener = listener
        onDispose {
            inputController.resetPointers()
            if (engine.onVisualCollisionListener === listener) {
                engine.onVisualCollisionListener = null
            }
        }
    }

    // Inicialização do Motor 3D Filament e Carregamento dos Assets .glb
    val engine3d = rememberEngine()
    val modelLoader = rememberModelLoader(engine3d)
    val view3d = rememberView(engine3d) {
        SceneView.createView(engine3d).apply {
            bloomOptions = bloomOptions.apply {
                enabled = prefs.neonBloomIntensity > 0.005f
                threshold = false
                highlight = 1.0f
                strength = (prefs.neonBloomIntensity / 0.10f) * 0.45f
                blendMode = com.google.android.filament.View.BloomOptions.BlendMode.ADD
            }
        }
    }

    LaunchedEffect(prefs.neonBloomIntensity) {
        view3d.bloomOptions = view3d.bloomOptions.apply {
            enabled = prefs.neonBloomIntensity > 0.005f
            threshold = false
            highlight = 1.0f
            strength = (prefs.neonBloomIntensity / 0.10f) * 0.45f
            blendMode = com.google.android.filament.View.BloomOptions.BlendMode.ADD
        }
    }

    val tableAsset = remember(activeTheme, isExtended, isExtendedHost) {
        Theme3DAssetResolver.getTable3DAsset(activeTheme, isExtended = isExtended, isPlayer1 = isExtendedHost)
    }
    val puckAsset = remember(prefs.puckSkin, activeTheme) {
        Theme3DAssetResolver.getPuck3DAsset(prefs.puckSkin, activeTheme)
    }
    val p1MalletAsset = remember(prefs.player1MalletSkin, activeTheme) {
        Theme3DAssetResolver.getMallet3DAsset(prefs.player1MalletSkin, isPlayer1 = true, theme = activeTheme)
    }
    val p2MalletAsset = remember(prefs.player2MalletSkin, activeTheme) {
        Theme3DAssetResolver.getMallet3DAsset(prefs.player2MalletSkin, isPlayer1 = false, theme = activeTheme)
    }

    val tableInstance = remember(modelLoader, tableAsset) {
        try {
            modelLoader.createModelInstance(tableAsset)
        } catch (_: Exception) {
            null
        }
    }

    val puckInstance = remember(modelLoader, puckAsset) {
        try {
            modelLoader.createModelInstance(puckAsset)
        } catch (_: Exception) {
            null
        }
    }

    val p1Instance = remember(modelLoader, p1MalletAsset) {
        try {
            modelLoader.createModelInstance(p1MalletAsset)
        } catch (_: Exception) {
            null
        }
    }

    val p2Instance = remember(modelLoader, p2MalletAsset) {
        try {
            modelLoader.createModelInstance(p2MalletAsset)
        } catch (_: Exception) {
            null
        }
    }

    val tableNode = remember(tableInstance) {
        tableInstance?.let {
            ModelNode(it).apply {
                position = Float3(0f, 0f, 0f)
            }
        }
    }

    val puckNode = remember(puckInstance) {
        puckInstance?.let { ModelNode(it) }
    }

    val p1Node = remember(p1Instance) {
        p1Instance?.let { ModelNode(it) }
    }

    val p2Node = remember(p2Instance) {
        p2Instance?.let { ModelNode(it) }
    }

    val cameraNode = rememberCameraNode(engine3d) {
        position = Float3(sceneState.camX, sceneState.camY, sceneState.camZ)
        rotation = Float3(sceneState.camPitch, sceneState.camYaw, 0f)
    }

    val childNodes = remember(tableNode, puckNode, p1Node, p2Node) {
        listOfNotNull(tableNode, puckNode, p1Node, p2Node)
    }

    // Frame pulse acoplado ao vsync nativo do display
    var framePulse by remember { mutableLongStateOf(0L) }
    var lastFrameTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                framePulse = time
            }
        }
    }

    fun mapEngineX(engineX: Float): Float {
        val lx = if (isExtended) {
            if (isExtendedGuest) 1000f - engineX else engineX
        } else {
            engineX
        }
        return (lx - 500f) * 0.00108f
    }

    fun mapEngineZ(engineY: Float): Float {
        val ly = if (isExtended) {
            if (isExtendedGuest) dimensions.hostHalfBottomY - engineY else engineY - dimensions.guestHalfTopY
        } else {
            engineY
        }
        val scaleZ = if (isExtended) 0.00108f else 0.00096f
        return (ly - 1000f) * scaleZ
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .pointerInput(engine, mode, touchOffset, matchState.currentStage) {
                inputController.resetPointers()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        // Durante a apresentação da arena 3D, toques na tela são consumidos e a animação permanece ininterrupta
                        if (sceneState.isPresenting) {
                            for (change in event.changes) {
                                change.consume()
                            }
                            continue
                        }

                        inputController.processPointerEvents(
                            changes = event.changes,
                            dimensions = dimensions,
                            p1MalletX = engine.player1Mallet.x,
                            p1MalletY = engine.player1Mallet.y,
                            p2MalletX = engine.player2Mallet.x,
                            p2MalletY = engine.player2Mallet.y,
                            malletRadius = engine.player1Mallet.radius,
                            mode = mode,
                            touchOffset = touchOffset,
                            isSpectator = matchState.isSpectator,
                            screenToTable = { sx, sy ->
                                screenToTable3D(
                                    screenX = sx,
                                    screenY = sy,
                                    w = w,
                                    h = h,
                                    camX = sceneState.camX,
                                    camY = sceneState.camY,
                                    camZ = sceneState.camZ,
                                    camPitchDeg = sceneState.camPitch,
                                    camYawDeg = sceneState.camYaw,
                                    isExtended = isExtended,
                                    isExtendedGuest = isExtendedGuest,
                                    dimensions = dimensions
                                )
                            },
                            onTouchP1 = { x, y -> viewModel.onTouchPlayer1(x, y) },
                            onTouchP2 = { x, y -> viewModel.onTouchPlayer2(x, y) },
                            onTouchGuest = { x, y -> viewModel.onTouchGuestGlobal(x, y) }
                        )
                    }
                }
            }
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val screenAspect = if (screenH > 0f) screenW / screenH else 9f / 19.5f

        val effectiveTargetPreset = remember(targetPreset, isExtended, screenAspect, zoom) {
            if (isExtended && screenAspect > 0.05f) {
                val tanHalfFov = 12f / 28f
                val fovCoeff = 2f * tanHalfFov
                val zTop = -1.08f
                val targetWidth = 1.28f
                val targetLength = 2.30f
                val requiredYForWidth = targetWidth / (fovCoeff * screenAspect)
                val requiredYForLength = targetLength / fovCoeff
                val baseY = kotlin.math.max(requiredYForWidth, requiredYForLength)

                val zoomFactor = when (zoom) {
                    CameraZoom3D.CLOSE -> 0.90f
                    CameraZoom3D.STANDARD -> 1.0f
                    CameraZoom3D.WIDE -> 1.15f
                }
                val calibratedY = baseY * zoomFactor
                val calibratedZ = zTop + calibratedY * tanHalfFov
                targetPreset.copy(position = Float3(0f, calibratedY, calibratedZ))
            } else {
                targetPreset
            }
        }
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine3d,
            modelLoader = modelLoader,
            view = view3d,
            cameraNode = cameraNode,
            cameraManipulator = null,
            childNodes = childNodes,
            onFrame = { _ ->
                val p = sceneState.presentationProgress
                if (p < 1f) {
                    val t = (p * p * p * (p * (p * 6f - 15f) + 10f)).coerceIn(0f, 1f)
                    val totalAngleRad = 2f * Math.PI.toFloat()
                    val currentAngleRad = (1f - t) * totalAngleRad

                    val y = 1.70f * (1f - t) + effectiveTargetPreset.position.y * t
                    val rx = 3.60f * (1f - t) + 1.20f * t
                    val rz = 4.20f * (1f - t) + effectiveTargetPreset.position.z * t

                    val x = rx * sin(currentAngleRad)
                    val z = rz * cos(currentAngleRad)

                    val finalFocusZ = effectiveTargetPreset.position.z - (effectiveTargetPreset.position.y / kotlin.math.tan(Math.toRadians(-effectiveTargetPreset.rotation.x.toDouble()).toFloat()))
                    val focusZ = finalFocusZ * t
                    val focusY = 0.05f * (1f - t)

                    cameraNode.position = Float3(x, y, z)
                    cameraNode.lookAt(Float3(0f, focusY, focusZ))

                    if (isSplitScreen && t > 0.60f) {
                        val blend = ((t - 0.60f) / 0.40f).coerceIn(0f, 1f)
                        val blendSmooth = blend * blend * (3f - 2f * blend)
                        val pitch = cameraNode.rotation.x * (1f - blendSmooth) + effectiveTargetPreset.rotation.x * blendSmooth
                        val yaw = cameraNode.rotation.y * (1f - blendSmooth)
                        cameraNode.rotation = Float3(pitch, yaw, 0f)
                    } else if (isGuestCamera && t > 0.60f) {
                        val blend = ((t - 0.60f) / 0.40f).coerceIn(0f, 1f)
                        val blendSmooth = blend * blend * (3f - 2f * blend)
                        val pitch = cameraNode.rotation.x * (1f - blendSmooth) + effectiveTargetPreset.rotation.x * blendSmooth
                        val yaw = cameraNode.rotation.y * (1f - blendSmooth) + effectiveTargetPreset.rotation.y * blendSmooth
                        cameraNode.rotation = Float3(pitch, yaw, 0f)
                    }

                    sceneState.camX = x
                    sceneState.camY = y
                    sceneState.camZ = z
                    sceneState.camPitch = cameraNode.rotation.x
                    sceneState.camYaw = cameraNode.rotation.y
                } else {
                    sceneState.camX += (effectiveTargetPreset.position.x - sceneState.camX) * 0.15f
                    sceneState.camY += (effectiveTargetPreset.position.y - sceneState.camY) * 0.15f
                    sceneState.camZ += (effectiveTargetPreset.position.z - sceneState.camZ) * 0.15f
                    sceneState.camPitch += (effectiveTargetPreset.rotation.x - sceneState.camPitch) * 0.15f
                    sceneState.camYaw += (effectiveTargetPreset.rotation.y - sceneState.camYaw) * 0.15f

                    cameraNode.position = Float3(sceneState.camX, sceneState.camY, sceneState.camZ)
                    cameraNode.rotation = Float3(sceneState.camPitch, sceneState.camYaw, 0f)
                }

                // Sincronização em tempo real das coordenadas físicas
                val puckVisible = if (isExtended) projection.isVisibleInViewport(engine.puck.x, engine.puck.y, engine.puck.radius) else true
                if (puckVisible) {
                    puckNode?.position = Float3(
                        mapEngineX(engine.puck.x),
                        0.0075f,
                        mapEngineZ(engine.puck.y)
                    )
                } else {
                    puckNode?.position = Float3(0f, -5f, 0f)
                }

                // Rotação Giroscópica do Disco: rotação contínua proporcional à velocidade
                val spd = engine.puck.speed()
                if (spd > 15f) {
                    sceneState.puckYawDeg = (sceneState.puckYawDeg + spd * 0.035f) % 360f
                }
                puckNode?.rotation = Float3(0f, sceneState.puckYawDeg, 0f)

                val p1Visible = if (isExtended) projection.isVisibleInViewport(engine.player1Mallet.x, engine.player1Mallet.y, engine.player1Mallet.radius) else true
                if (p1Visible) {
                    p1Node?.position = Float3(
                        mapEngineX(engine.player1Mallet.x),
                        0.0f,
                        mapEngineZ(engine.player1Mallet.y)
                    )
                } else {
                    p1Node?.position = Float3(0f, -5f, 0f)
                }

                val p2Visible = if (isExtended) projection.isVisibleInViewport(engine.player2Mallet.x, engine.player2Mallet.y, engine.player2Mallet.radius) else true
                if (p2Visible) {
                    p2Node?.position = Float3(
                        mapEngineX(engine.player2Mallet.x),
                        0.0f,
                        mapEngineZ(engine.player2Mallet.y)
                    )
                } else {
                    p2Node?.position = Float3(0f, -5f, 0f)
                }
            }
        )

        // Camada de Efeitos Visuais Sci-Fi em Tempo Real com Aceleração por Hardware
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dt = if (lastFrameTime > 0L) {
                ((framePulse - lastFrameTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            } else 0.016f
            lastFrameTime = framePulse

            val w = size.width
            val h = size.height
            val camX = sceneState.camX
            val camY = sceneState.camY
            val camZ = sceneState.camZ
            val camPitch = sceneState.camPitch
            val camYaw = sceneState.camYaw

            // 1. Pulso de Celebração de Ponto na Barreira da Meta
            if (matchState.status == MatchStatus.GOAL_PAUSE) {
                val scorer = matchState.lastScorer
                val goalColor = if (scorer == 2) Color(0xFFFF6D00) else Color(0xFF00E5FF)
                val goalTableY = if (isExtended) {
                    if (scorer == 2) dimensions.totalHeight - 150f else 150f
                } else {
                    if (scorer == 2) 200f else 1800f
                }
                val goalScreenCenter = tableToScreen3D(
                    500f,
                    goalTableY,
                    w,
                    h,
                    camX,
                    camY,
                    camZ,
                    camPitch,
                    camYaw,
                    isExtended = isExtended,
                    isExtendedGuest = isExtendedGuest,
                    dimensions = dimensions
                ) ?: Offset(w / 2f, if (scorer == 2) h * 0.25f else h * 0.75f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            goalColor.copy(alpha = 0.35f),
                            goalColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = goalScreenCenter,
                        radius = w * 0.85f
                    ),
                    center = goalScreenCenter,
                    radius = w * 0.85f
                )
            }

            // 2. Rastro de Energia 3D do Disco (Puck Trail)
            if (matchState.status != MatchStatus.PLAYING) {
                puckTrail.clear()
            } else {
                val spd = engine.puck.speed()
                if (spd > 220f) {
                    puckTrail.addFirst(TablePoint(engine.puck.x, engine.puck.y))
                    if (puckTrail.size > 14) puckTrail.removeLast()
                } else if (puckTrail.isNotEmpty()) {
                    puckTrail.removeLast()
                }
            }

            if (puckTrail.size >= 2) {
                val trailColor = if (engine.puck.lastHitBy == 2) Color(0xFFFF6D00) else Color(0xFF00E5FF)
                val points = puckTrail.mapNotNull { pt ->
                    if (isExtended && !projection.isVisibleInViewport(pt.x, pt.y, 10f)) null
                    else tableToScreen3D(
                        pt.x,
                        pt.y,
                        w,
                        h,
                        camX,
                        camY,
                        camZ,
                        camPitch,
                        camYaw,
                        isExtended = isExtended,
                        isExtendedGuest = isExtendedGuest,
                        dimensions = dimensions
                    )
                }
                for (i in 0 until points.size - 1) {
                    val pA = points[i]
                    val pB = points[i + 1]
                    val frac = 1f - (i.toFloat() / points.size.toFloat())
                    val alpha = (frac * 0.85f).coerceIn(0f, 1f)
                    val strokeW = (frac * 12f + 2f)
                    drawLine(
                        color = trailColor.copy(alpha = alpha),
                        start = pA,
                        end = pB,
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Processamento de Eventos de Colisão (Ondas de Choque e Faíscas)
            synchronized(pendingCollisions) {
                while (pendingCollisions.isNotEmpty()) {
                    val (cx, cy, evt) = pendingCollisions.removeFirst()
                    val color = when (evt) {
                        CollisionEvent.PADDLE_HIT_P1 -> Color(0xFF00E5FF)
                        CollisionEvent.PADDLE_HIT_P2 -> Color(0xFFFF6D00)
                        CollisionEvent.GOAL_PLAYER1 -> Color(0xFF00E5FF)
                        CollisionEvent.GOAL_PLAYER2 -> Color(0xFFFF6D00)
                        CollisionEvent.WALL_HIT -> if (cy > dimensions.centerLineY) Color(0xFF00E5FF) else Color(0xFFFF6D00)
                        else -> Color(0xFF00E5FF)
                    }
                    particlePool.spawnShockwave(cx, cy, color, maxRadius = 160f)
                    val sparkCount = if (evt == CollisionEvent.WALL_HIT) 8 else 14
                    particlePool.spawnSparks(cx, cy, sparkCount, color, spreadSpeed = 520f)
                }
            }

            // 4. Desenha Ondas de Choque e Faíscas com Perspectiva 3D
            particlePool.update(dt)
            particlePool.draw(
                this,
                w,
                h,
                camX,
                camY,
                camZ,
                camPitch,
                camYaw,
                isExtended = isExtended,
                isExtendedGuest = isExtendedGuest,
                dimensions = dimensions
            )
        }
    }
}
