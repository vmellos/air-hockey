package dev.vmellos.airhockey.feature.game.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import dev.romainguy.kotlin.math.Float3
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.preferences.ArTableScale
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.GameViewModel
import dev.vmellos.airhockey.feature.game.MatchStatus
import dev.vmellos.airhockey.feature.physics.TablePoint
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Estado desacoplado de controle da Arena em Realidade Aumentada (AR).
 */
private class SceneARState {
    var activePointerP1: PointerId? = null
    var puckYawDeg: Float = 0f
}

/**
 * Desprojeção no Plano da Mesa AR:
 * Intersecta o raio da câmera AR com o plano horizontal local da mesa ancorada,
 * convertendo as coordenadas métricas para o sistema soberano (0..1000 x 0..2000).
 */
internal fun screenToTableAR(
    screenX: Float,
    screenY: Float,
    cameraNode: ARCameraNode,
    anchorPos: Float3,
    anchorYawRad: Float,
    scaleFactor: Float
): TablePoint? {
    val ray = cameraNode.screenPointToRay(screenX, screenY) ?: return null
    val ro = ray.origin
    val rd = ray.direction
    val dy = rd.y
    if (abs(dy) < 1e-4f) return null

    val t = (anchorPos.y - ro.y) / dy
    if (t <= 0f) return null

    val worldX = ro.x + t * rd.x
    val worldZ = ro.z + t * rd.z

    val dx = (worldX - anchorPos.x) / scaleFactor
    val dz = (worldZ - anchorPos.z) / scaleFactor

    val cosA = cos(-anchorYawRad)
    val sinA = sin(-anchorYawRad)
    val localX = dx * cosA - dz * sinA
    val localZ = dx * sinA + dz * cosA

    val tableX = 500f + localX / 0.00108f
    val tableY = 1000f + localZ / 0.00096f
    return TablePoint(tableX, tableY)
}

/**
 * Renderizador de Realidade Aumentada (AR) para o Air Hockey utilizando o Sceneview e Google ARCore.
 *
 * Suporta:
 * - Rastreamento de planos horizontais do mundo real (chão e tampos de mesa)
 * - Retícula de mira animada procedural com indicação de superfície
 * - Ancoragem da mesa com orientação frontal alinhada ao jogador
 * - Alternância dinâmica de escala (1:1 Real para chão e 1:4 Miniatura para mesas)
 * - Projeção de toque no plano da âncora com raio de agarre e deslocamento ergonômico
 * - Transição fluida de retorno para a Arena 3D Padrão a qualquer momento
 */
@Composable
fun AirHockeyArenaARScene(
    viewModel: GameViewModel,
    mode: GameMode,
    initialScale: ArTableScale = ArTableScale.MINIATURE_1_4,
    onSwitchTo3D: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesRepo = AirHockeyApp.instance.preferencesRepository
    val prefs by preferencesRepo.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val touchOffset = if (prefs.touchOffsetEnabled) 110f else 0f
    val matchState by viewModel.matchState.collectAsState()
    val engine = viewModel.engine

    // 1. Verificação de suporte a ARCore e Permissões
    var arAvailability by remember { mutableStateOf<ArCoreApk.Availability?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(Unit) {
        try {
            arAvailability = ArCoreApk.getInstance().checkAvailability(context)
        } catch (_: Exception) {
            arAvailability = ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            onSwitchTo3D()
        }
    }

    // Se o dispositivo for comprovadamente incompatível com ARCore, exibe tela de fallback
    if (arAvailability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
        ARUnsupportedFallback(
            onSwitchTo3D = onSwitchTo3D,
            modifier = modifier
        )
        return
    }

    // Se a permissão de câmera ainda não foi concedida, exibe diálogo educacional
    if (!hasCameraPermission) {
        ARPermissionPrompt(
            onGrantPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onSwitchTo3D = onSwitchTo3D,
            modifier = modifier
        )
        return
    }

    // 2. Inicialização do Motor Gráfico Filament & ARCore
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

    val tableNode = remember(tableInstance) { tableInstance?.let { ModelNode(it) } }
    val puckNode = remember(puckInstance) { puckInstance?.let { ModelNode(it) } }
    val p1Node = remember(p1Instance) { p1Instance?.let { ModelNode(it) } }
    val p2Node = remember(p2Instance) { p2Instance?.let { ModelNode(it) } }

    val cameraNode = rememberARCameraNode(engine3d)
    val sceneARState = remember { SceneARState() }

    // Estados de Rastreamento e Ancoragem
    var isAnchored by remember { mutableStateOf(false) }
    var surfaceDetected by remember { mutableStateOf(false) }
    var detectedHitResult by remember { mutableStateOf<HitResult?>(null) }
    var anchorNode by remember { mutableStateOf<AnchorNode?>(null) }
    var anchorPos by remember { mutableStateOf<Float3?>(null) }
    var anchorYawRad by remember { mutableFloatStateOf(0f) }

    var currentScale by remember { mutableStateOf(initialScale) }
    val currentScaleFactor = currentScale.scaleFactor

    // Atualiza a escala do AnchorNode dinamicamente quando a preferência muda
    LaunchedEffect(currentScale, anchorNode) {
        anchorNode?.scale = Float3(currentScaleFactor, currentScaleFactor, currentScaleFactor)
    }

    // Higienização de Ponteiros de Toque
    LaunchedEffect(matchState.status) {
        if (matchState.status != MatchStatus.PLAYING) {
            sceneARState.activePointerP1 = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sceneARState.activePointerP1 = null
            anchorNode?.let {
                it.destroy()
            }
        }
    }

    fun mapX(engineX: Float): Float = (engineX - 500f) * 0.00108f
    fun mapZ(engineY: Float): Float = (engineY - 1000f) * 0.00096f

    val childNodes = remember(anchorNode) { listOfNotNull(anchorNode) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(engine, isAnchored, anchorPos, anchorYawRad, currentScaleFactor, touchOffset) {
                if (!isAnchored) return@pointerInput

                sceneARState.activePointerP1 = null
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val ancPos = anchorPos ?: continue

                        if (event.changes.none { it.pressed }) {
                            sceneARState.activePointerP1 = null
                        }

                        if (matchState.isSpectator || matchState.status == MatchStatus.PAUSED || matchState.status == MatchStatus.GAME_OVER) {
                            continue
                        }

                        val grabRadiusSq = (engine.player1Mallet.radius * 2.8f).let { it * it }

                        for (change in event.changes) {
                            val tablePt = screenToTableAR(
                                screenX = change.position.x,
                                screenY = change.position.y,
                                cameraNode = cameraNode,
                                anchorPos = ancPos,
                                anchorYawRad = anchorYawRad,
                                scaleFactor = currentScaleFactor
                            ) ?: continue

                            if (change.pressed) {
                                if (sceneARState.activePointerP1 == null) {
                                    val dx = tablePt.x - engine.player1Mallet.x
                                    val dy = tablePt.y - engine.player1Mallet.y
                                    if ((dx * dx + dy * dy) <= grabRadiusSq) {
                                        sceneARState.activePointerP1 = change.id
                                    }
                                }

                                if (sceneARState.activePointerP1 == change.id) {
                                    val targetY = (tablePt.y - touchOffset).coerceIn(
                                        engine.dimensions.centerLineY + engine.player1Mallet.radius,
                                        engine.dimensions.totalHeight - engine.player1Mallet.radius
                                    )
                                    val targetX = tablePt.x.coerceIn(
                                        engine.player1Mallet.radius,
                                        engine.dimensions.width - engine.player1Mallet.radius
                                    )
                                    engine.player1Mallet.targetX = targetX
                                    engine.player1Mallet.targetY = targetY
                                    change.consume()
                                }
                            } else if (sceneARState.activePointerP1 == change.id) {
                                sceneARState.activePointerP1 = null
                            }
                        }
                    }
                }
            }
    ) {
        // Cena ARCore do Sceneview
        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine3d,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            childNodes = childNodes,
            planeRenderer = !isAnchored,
            sessionConfiguration = { _, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            },
            onSessionUpdated = { _, frame ->
                if (!isAnchored) {
                    val viewWidth = frame.camera.imageIntrinsics?.imageDimensions?.get(0)?.toFloat() ?: 1080f
                    val viewHeight = frame.camera.imageIntrinsics?.imageDimensions?.get(1)?.toFloat() ?: 1920f
                    val hits = frame.hitTest(viewWidth / 2f, viewHeight / 2f)
                    val validPlaneHit = hits.firstOrNull { hit ->
                        val trackable = hit.trackable
                        trackable is Plane &&
                                trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                                trackable.isPoseInPolygon(hit.hitPose)
                    }
                    if (validPlaneHit != null) {
                        surfaceDetected = true
                        detectedHitResult = validPlaneHit
                    } else {
                        surfaceDetected = false
                        detectedHitResult = null
                    }
                } else {
                    // Sincronização em tempo real das coordenadas físicas soberanas com os nós 3D
                    puckNode?.position = Float3(
                        mapX(engine.puck.x),
                        0.0075f,
                        mapZ(engine.puck.y)
                    )

                    val spd = engine.puck.speed()
                    if (spd > 15f) {
                        sceneARState.puckYawDeg = (sceneARState.puckYawDeg + spd * 0.035f) % 360f
                    }
                    puckNode?.rotation = Float3(0f, sceneARState.puckYawDeg, 0f)

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
            }
        )

        // 3. Retícula Guia Visual & Camada de Ancoragem
        if (!isAnchored) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        val hit = detectedHitResult
                        if (hit != null) {
                            val anchor = hit.createAnchor()
                            val ancTranslation = anchor.pose.translation
                            val camTranslation = cameraNode.pose?.translation ?: floatArrayOf(0f, 1f, 1f)
                            val vx = camTranslation[0] - ancTranslation[0]
                            val vz = camTranslation[2] - ancTranslation[2]
                            val yawRad = atan2(vx, vz)
                            val yawDeg = Math.toDegrees(yawRad.toDouble()).toFloat()

                            anchorYawRad = yawRad
                            anchorPos = Float3(ancTranslation[0], ancTranslation[1], ancTranslation[2])

                            val newAnchorNode = AnchorNode(engine3d, anchor).apply {
                                this.scale = Float3(currentScaleFactor, currentScaleFactor, currentScaleFactor)
                                rotation = Float3(0f, yawDeg, 0f)
                                tableNode?.let { addChildNode(it) }
                                puckNode?.let { addChildNode(it) }
                                p1Node?.let { addChildNode(it) }
                                p2Node?.let { addChildNode(it) }
                            }
                            anchorNode = newAnchorNode
                            isAnchored = true
                            viewModel.startCountdownAfterPresentation()
                        }
                    }
            ) {
                ARReticleOverlay(surfaceDetected = surfaceDetected)

                // Dica superior instruindo o usuário
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        color = Color(0xCC080E1A),
                        shape = CutCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (surfaceDetected) Color(0xFF00E5FF) else Color(0x66FFFFFF)),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = if (surfaceDetected) {
                                stringResource(R.string.ar_surface_detected)
                            } else {
                                stringResource(R.string.ar_tracking_hint)
                            },
                            color = if (surfaceDetected) Color(0xFF00E5FF) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // 4. Barra Superior de Controles de AR (Reancorar, Alternar Escala e Mudar para 3D)
        ARControlsOverlay(
            isAnchored = isAnchored,
            currentScale = currentScale,
            onToggleScale = {
                val nextScale = if (currentScale == ArTableScale.MINIATURE_1_4) {
                    ArTableScale.REAL_1_1
                } else {
                    ArTableScale.MINIATURE_1_4
                }
                currentScale = nextScale
                coroutineScope.launch {
                    preferencesRepo.updateArTableScale(nextScale)
                }
            },
            onReanchor = {
                isAnchored = false
                viewModel.pauseMatch()
                anchorNode?.let { node ->
                    node.destroy()
                }
                anchorNode = null
                anchorPos = null
            },
            onSwitchTo3D = onSwitchTo3D
        )
    }
}

/**
 * Retícula de Rastreamento AR Sci-Fi procedural.
 */
@Composable
private fun ARReticleOverlay(surfaceDetected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "reticlePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseColor = if (surfaceDetected) Color(0xFF00E5FF) else Color(0xFF38BDF8)
            val strokeColor = baseColor.copy(alpha = pulseAlpha)

            // Círculo externo pulsante
            drawCircle(
                color = strokeColor,
                radius = size.width * 0.42f,
                style = Stroke(width = 2.5f)
            )

            // Marcadores de mira cardinais
            val r = size.width * 0.42f
            drawLine(strokeColor, Offset(center.x - r - 12f, center.y), Offset(center.x - r + 8f, center.y), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(strokeColor, Offset(center.x + r - 8f, center.y), Offset(center.x + r + 12f, center.y), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(strokeColor, Offset(center.x, center.y - r - 12f), Offset(center.x, center.y - r + 8f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(strokeColor, Offset(center.x, center.y + r - 8f), Offset(center.x, center.y + r + 12f), strokeWidth = 3f, cap = StrokeCap.Round)

            // Círculo interno holográfico
            drawCircle(
                color = if (surfaceDetected) Color(0xFF10B981).copy(alpha = 0.8f) else Color(0xFF00E5FF).copy(alpha = 0.5f),
                radius = 10f * pulseAlpha,
                style = Stroke(width = 3f)
            )
        }
    }
}

/**
 * Controles de HUD específicos do modo AR.
 */
@Composable
private fun ARControlsOverlay(
    isAnchored: Boolean,
    currentScale: ArTableScale,
    onToggleScale: () -> Unit,
    onReanchor: () -> Unit,
    onSwitchTo3D: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
    ) {
        // Controles alinhados no topo à esquerda
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão Alternar Escala (1:1 / 1:4)
            Surface(
                color = Color(0xCC0B111E),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.7f)),
                modifier = Modifier.clickable { onToggleScale() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CropSquare,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (currentScale == ArTableScale.MINIATURE_1_4) {
                            stringResource(R.string.ar_scale_1_4)
                        } else {
                            stringResource(R.string.ar_scale_1_1)
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Botão Reancorar (visível quando a mesa já está posicionada)
            if (isAnchored) {
                Surface(
                    color = Color(0xCC0B111E),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                    modifier = Modifier.clickable { onReanchor() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.ar_reanchor_button),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Botão Alternar para Modo 3D
            Surface(
                color = Color(0xCC0B111E),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                modifier = Modifier.clickable { onSwitchTo3D() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = stringResource(R.string.ar_switch_to_3d),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Diálogo educacional solicitando permissão de câmera.
 */
@Composable
private fun ARPermissionPrompt(
    onGrantPermission: () -> Unit,
    onSwitchTo3D: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE6050B14)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xF20B111E),
            shape = CutCornerShape(16.dp),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.ar_permission_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.ar_permission_desc),
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onGrantPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = CutCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.ar_permission_grant).uppercase(),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSwitchTo3D,
                    border = BorderStroke(1.dp, Color(0xFF64748B)),
                    shape = CutCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.ar_switch_to_3d).uppercase(),
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Diálogo de fallback para dispositivos sem suporte ao ARCore.
 */
@Composable
private fun ARUnsupportedFallback(
    onSwitchTo3D: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE6050B14)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xF20B111E),
            shape = CutCornerShape(16.dp),
            border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.ar_unsupported_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.ar_unsupported_desc),
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSwitchTo3D,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = CutCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.ar_switch_to_3d).uppercase(),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
