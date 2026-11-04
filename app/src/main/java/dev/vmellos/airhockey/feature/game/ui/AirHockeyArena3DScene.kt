package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import dev.romainguy.kotlin.math.Float3
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.core.preferences.CameraZoom3D
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.GameViewModel
import dev.vmellos.airhockey.feature.game.MatchStatus
import dev.vmellos.airhockey.feature.physics.TablePoint
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.launch

data class CameraPreset(
    val position: Float3,
    val rotation: Float3
)

fun getCameraPreset(zoom: CameraZoom3D): CameraPreset = when (zoom) {
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

/**
 * Estado operacional da Arena 3D desacoplado do ciclo de recomposição do Jetpack Compose.
 */
private class Scene3DState(
    initialPreset: CameraPreset
) {
    var presentationProgress: Float = 0f
    var isPresenting: Boolean = true
    var activePointerP1: PointerId? = null
    var activePointerP2: PointerId? = null

    // Posição e rotação contínuas interpoladas da câmera (atualizadas no onFrame a 60/120 Hz)
    var camX: Float = initialPreset.position.x
    var camY: Float = initialPreset.position.y
    var camZ: Float = initialPreset.position.z
    var camPitch: Float = initialPreset.rotation.x
    var camYaw: Float = initialPreset.rotation.y
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
    camPitchDeg: Float
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
    val cosA = kotlin.math.cos(alpha)
    val sinA = kotlin.math.sin(alpha)

    val dx = rx
    val dy = ry * cosA - rz * sinA
    val dz = ry * sinA + rz * cosA

    if (kotlin.math.abs(dy) < 1e-4f) return null
    val t = -camY / dy
    if (t <= 0f) return null

    val worldX = camX + t * dx
    val worldZ = camZ + t * dz

    val tx = 500f + worldX / 0.00108f
    val ty = 1000f + worldZ / 0.00096f
    return TablePoint(tx, ty)
}

/**
 * Renderizador Tridimensional da Arena de Air Hockey utilizando o Sceneview (Google Filament).
 * 
 * Mantém a simulação física 2D soberana via [AirHockeyPhysicsEngine] e projeta as coordenadas
 * espaciais métricas para a Mesa com Piso da Arena, Rebatedores P1/P2 e Disco gerados no Blender.
 * 
 * Executa a Apresentação de Abertura da Mesa 3D (sobrevoo cinematográfico dinâmico com variação de altitude)
 * no início da partida e suporta 3 Níveis de Enquadramento 3D (Próximo, Padrão e Amplo)
 * calibrados com Desprojeção em Perspectiva 3D para controle de toque com precisão milimétrica.
 */
@Composable
fun AirHockeyArena3DScene(
    viewModel: GameViewModel,
    mode: GameMode,
    zoom: CameraZoom3D = CameraZoom3D.STANDARD,
    onPresentationFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val engine = viewModel.engine
    val dimensions = engine.dimensions

    val prefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val touchOffset = if (prefs.touchOffsetEnabled) 110f else 0f
    val matchState by viewModel.matchState.collectAsState()

    val targetPreset = remember(zoom) { getCameraPreset(zoom) }
    val sceneState = remember { Scene3DState(targetPreset) }

    val coroutineScope = rememberCoroutineScope()
    val presentationAnim = remember { Animatable(0f) }

    // Apresentação de Abertura da Mesa 3D (Cinemática fluida desacoplada da recomposição do Compose)
    LaunchedEffect(Unit) {
        try {
            presentationAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 2600,
                    easing = FastOutSlowInEasing
                )
            ) {
                sceneState.presentationProgress = value
            }
        } finally {
            sceneState.presentationProgress = 1f
            if (sceneState.isPresenting) {
                sceneState.isPresenting = false
                onPresentationFinished()
            }
        }
    }

    // Higienização de Ponteiros de Toque
    LaunchedEffect(matchState.status) {
        if (matchState.status != MatchStatus.PLAYING) {
            sceneState.activePointerP1 = null
            sceneState.activePointerP2 = null
        }
    }

    DisposableEffect(engine) {
        sceneState.activePointerP1 = null
        sceneState.activePointerP2 = null
        onDispose {
            sceneState.activePointerP1 = null
            sceneState.activePointerP2 = null
        }
    }

    // Inicialização do Motor 3D Filament e Carregamento dos Assets .glb
    val engine3d = rememberEngine()
    val modelLoader = rememberModelLoader(engine3d)

    val tableInstance = remember(modelLoader) {
        try {
            modelLoader.createModelInstance("models/mesa_arcade.glb")
        } catch (_: Exception) {
            null
        }
    }

    val puckInstance = remember(modelLoader) {
        try {
            modelLoader.createModelInstance("models/disco_cyber.glb")
        } catch (_: Exception) {
            null
        }
    }

    val p1Instance = remember(modelLoader) {
        try {
            modelLoader.createModelInstance("models/rebatedor_p1.glb")
        } catch (_: Exception) {
            null
        }
    }

    val p2Instance = remember(modelLoader) {
        try {
            modelLoader.createModelInstance("models/rebatedor_p2.glb")
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

    // Conversão métrica de coordenadas 2D do AirHockeyPhysicsEngine (1000 x 2000)
    // para as dimensões exatas da Mesa 3D do Blender (Largura: 1.08m, Comprimento: 1.92m)
    fun mapX(engineX: Float): Float = (engineX - 500f) * 0.00108f
    fun mapZ(engineY: Float): Float = (engineY - 1000f) * 0.00096f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .pointerInput(engine, mode, touchOffset) {
                sceneState.activePointerP1 = null
                sceneState.activePointerP2 = null
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        if (event.changes.none { it.pressed }) {
                            sceneState.activePointerP1 = null
                            sceneState.activePointerP2 = null
                        }

                        // Tap to Skip: Um toque durante a apresentação encerra o sobrevoo imediatamente
                        if (sceneState.isPresenting) {
                            if (event.changes.any { it.pressed }) {
                                coroutineScope.launch {
                                    sceneState.presentationProgress = 1f
                                    sceneState.isPresenting = false
                                    presentationAnim.snapTo(1f)
                                    onPresentationFinished()
                                }
                                for (change in event.changes) {
                                    change.consume()
                                }
                            }
                            continue
                        }

                        if (matchState.isSpectator) {
                            continue
                        }

                        val grabRadiusSq = (engine.player1Mallet.radius * 2.5f).let { it * it }

                        for (change in event.changes) {
                            val tablePt = screenToTable3D(
                                screenX = change.position.x,
                                screenY = change.position.y,
                                w = w,
                                h = h,
                                camX = sceneState.camX,
                                camY = sceneState.camY,
                                camZ = sceneState.camZ,
                                camPitchDeg = sceneState.camPitch
                            ) ?: continue

                            if (change.pressed) {
                                when (mode) {
                                    GameMode.EXTENDED_GUEST, GameMode.CONNECTED_GUEST -> {
                                        val mallet = engine.player2Mallet
                                        val targetY = tablePt.y + touchOffset
                                        val dx = tablePt.x - mallet.x
                                        val dy = targetY - mallet.y
                                        val distSq = dx * dx + dy * dy

                                        if (sceneState.activePointerP2 == change.id) {
                                            viewModel.onTouchGuestGlobal(tablePt.x, targetY)
                                            change.consume()
                                        } else if (sceneState.activePointerP2 == null && distSq <= grabRadiusSq) {
                                            sceneState.activePointerP2 = change.id
                                            viewModel.onTouchGuestGlobal(tablePt.x, targetY)
                                            change.consume()
                                        }
                                    }
                                    GameMode.EXTENDED_HOST, GameMode.CONNECTED_HOST, GameMode.SOLO -> {
                                        val mallet = engine.player1Mallet
                                        val targetY = tablePt.y - touchOffset
                                        val dx = tablePt.x - mallet.x
                                        val dy = targetY - mallet.y
                                        val distSq = dx * dx + dy * dy

                                        if (sceneState.activePointerP1 == change.id) {
                                            viewModel.onTouchPlayer1(tablePt.x, targetY)
                                            change.consume()
                                        } else if (sceneState.activePointerP1 == null && distSq <= grabRadiusSq) {
                                            sceneState.activePointerP1 = change.id
                                            viewModel.onTouchPlayer1(tablePt.x, targetY)
                                            change.consume()
                                        }
                                    }
                                    GameMode.SPLIT_SCREEN -> {
                                        if (tablePt.y < dimensions.centerLineY) {
                                            val mallet = engine.player2Mallet
                                            val targetY = tablePt.y + touchOffset
                                            val dx = tablePt.x - mallet.x
                                            val dy = targetY - mallet.y
                                            val distSq = dx * dx + dy * dy

                                            if (sceneState.activePointerP2 == change.id) {
                                                viewModel.onTouchPlayer2(tablePt.x, targetY)
                                                change.consume()
                                            } else if (sceneState.activePointerP2 == null && distSq <= grabRadiusSq) {
                                                sceneState.activePointerP2 = change.id
                                                viewModel.onTouchPlayer2(tablePt.x, targetY)
                                                change.consume()
                                            }
                                        } else {
                                            val mallet = engine.player1Mallet
                                            val targetY = tablePt.y - touchOffset
                                            val dx = tablePt.x - mallet.x
                                            val dy = targetY - mallet.y
                                            val distSq = dx * dx + dy * dy

                                            if (sceneState.activePointerP1 == change.id) {
                                                viewModel.onTouchPlayer1(tablePt.x, targetY)
                                                change.consume()
                                            } else if (sceneState.activePointerP1 == null && distSq <= grabRadiusSq) {
                                                sceneState.activePointerP1 = change.id
                                                viewModel.onTouchPlayer1(tablePt.x, targetY)
                                                change.consume()
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            } else {
                                if (sceneState.activePointerP1 == change.id) {
                                    sceneState.activePointerP1 = null
                                }
                                if (sceneState.activePointerP2 == change.id) {
                                    sceneState.activePointerP2 = null
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine3d,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = null,
            childNodes = childNodes,
            onFrame = { _ ->
                val p = sceneState.presentationProgress
                if (p < 1f) {
                    // Sobrevoo Cinemático Dinâmico (Flyover)
                    val eased = FastOutSlowInEasing.transform(p)
                    val angleRad = (1f - eased) * Math.PI.toFloat() // de PI (180°) até 0°
                    val sideArch = kotlin.math.sin(eased * Math.PI.toFloat())

                    val x = sideArch * 1.55f
                    val z = kotlin.math.cos(angleRad) * (targetPreset.position.z * (1.15f - 0.15f * eased))
                    val y = targetPreset.position.y * (1.20f - 0.20f * eased) - 0.35f * sideArch

                    val pitchRad = -kotlin.math.atan2(y, kotlin.math.abs(z).coerceAtLeast(0.5f))
                    val pitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat()
                    val effectivePitch = pitchDeg * (1f - eased) + targetPreset.rotation.x * eased
                    val effectiveYaw = Math.toDegrees(angleRad.toDouble()).toFloat()

                    sceneState.camX = x
                    sceneState.camY = y
                    sceneState.camZ = z
                    sceneState.camPitch = effectivePitch
                    sceneState.camYaw = effectiveYaw

                    cameraNode.position = Float3(x, y, z)
                    cameraNode.rotation = Float3(effectivePitch, effectiveYaw, 0f)
                } else {
                    // Transição suave contínua entre Níveis de Enquadramento 3D sem recomposição
                    sceneState.camX += (targetPreset.position.x - sceneState.camX) * 0.15f
                    sceneState.camY += (targetPreset.position.y - sceneState.camY) * 0.15f
                    sceneState.camZ += (targetPreset.position.z - sceneState.camZ) * 0.15f
                    sceneState.camPitch += (targetPreset.rotation.x - sceneState.camPitch) * 0.15f
                    sceneState.camYaw = 0f

                    cameraNode.position = Float3(sceneState.camX, sceneState.camY, sceneState.camZ)
                    cameraNode.rotation = Float3(sceneState.camPitch, 0f, 0f)
                }

                // Sincronização em tempo real das coordenadas físicas para os nós 3D
                puckNode?.position = Float3(
                    mapX(engine.puck.x),
                    0.0075f,
                    mapZ(engine.puck.y)
                )

                p1Node?.position = Float3(
                    mapX(engine.player1Mallet.x),
                    0.0f,
                    mapZ(engine.player1Mallet.y)
                )

                p2Node?.position = Float3(
                    mapX(engine.player2Mallet.x),
                    0.0f,
                    mapZ(engine.player2Mallet.y)
                )
            }
        )
    }
}
