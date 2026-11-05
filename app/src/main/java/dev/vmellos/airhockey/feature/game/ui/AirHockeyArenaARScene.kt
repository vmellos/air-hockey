package dev.vmellos.airhockey.feature.game.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.inverse
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.preferences.ArTableScale
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.GameViewModel
import dev.vmellos.airhockey.feature.game.MatchStatus
import dev.vmellos.airhockey.feature.physics.TablePoint
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.ar.rememberAREnvironment
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Fases operacionais da experiência de Realidade Aumentada (AR).
 */
enum class ArStagingPhase {
    /** Rastreamento de superfície com retícula central ativa e botão centralizado de fixação. */
    SCANNING,
    /** Mesa ancorada e modelos 3D projetados na superfície; lobby de conferência e disparo manual. */
    POSITIONED,
    /** Partida em andamento; física e desprojeção de toque ativas. */
    PLAYING
}

/**
 * Estado desacoplado de controle da Arena em Realidade Aumentada (AR).
 */
private class SceneARState {
    var activePointerP1: PointerId? = null
    var puckYawDeg: Float = 0f
}

/**
 * Contexto volátil da sessão ARCore desacoplado do ciclo de recomposição do Jetpack Compose.
 * Armazena a sessão, o último frame e o HitResult mais recente sem disparar recomposições a 60 FPS.
 */
private class ArSessionContext {
    var session: Session? = null
    var frame: Frame? = null
    var latestHitResult: HitResult? = null
}

/**
 * Validação física e ergonômica das coordenadas métricas para colocação da mesa de Air Hockey.
 * Rejeita pontos astronômicos gerados por erros de rastreamento, pontos atrás da câmera ou suspensos no teto.
 */
internal fun isHitSane(tx: Float, ty: Float, tz: Float, cx: Float, cy: Float, cz: Float): Boolean {
    if (tx.isNaN() || ty.isNaN() || tz.isNaN() || tx.isInfinite() || ty.isInfinite() || tz.isInfinite()) return false

    val dx = tx - cx
    val dy = ty - cy
    val dz = tz - cz
    val dist = sqrt(dx * dx + dy * dy + dz * dz)

    // Distância confortável para jogar indoor em mesa (0.3m) ou chão (até 3.0m)
    if (dist < 0.28f || dist > 3.0f) return false

    // A mesa/chão deve estar abaixo da câmera ou ligeiramente acima (máximo 0.15m acima da câmera)
    // e no máximo 2.0m abaixo da câmera (altura do chão típica ao segurar o celular em pé)
    if (dy > 0.15f || dy < -2.0f) return false

    return true
}

internal fun isHitSane(trans: FloatArray, camPose: Pose): Boolean {
    if (trans.size < 3) return false
    return isHitSane(trans[0], trans[1], trans[2], camPose.tx(), camPose.ty(), camPose.tz())
}

/**
 * Desprojeção no Plano da Mesa AR:
 * Intersecta o raio da câmera AR com a superfície da mesa ancorada no mundo real
 * através da matriz inversa de transformação mundial (World-to-Local) do nó de conteúdo da mesa,
 * convertendo as coordenadas métricas para o sistema soberano (0..1000 x 0..2000).
 */
internal fun screenToTableAR(
    screenX: Float,
    screenY: Float,
    cameraNode: ARCameraNode,
    contentNode: Node
): TablePoint? {
    val ray = cameraNode.screenPointToRay(screenX, screenY) ?: return null
    val invMat = try {
        inverse(contentNode.worldTransform)
    } catch (_: Exception) {
        return null
    }

    val ro4 = invMat * Float4(ray.origin.x, ray.origin.y, ray.origin.z, 1f)
    val rd4 = invMat * Float4(ray.direction.x, ray.direction.y, ray.direction.z, 0f)

    val ro = Float3(ro4.x, ro4.y, ro4.z)
    val rd = Float3(rd4.x, rd4.y, rd4.z)

    if (abs(rd.y) < 1e-4f) return null

    val t = -ro.y / rd.y
    if (t <= 0f) return null

    val localX = ro.x + t * rd.x
    val localZ = ro.z + t * rd.z

    val tableX = 500f + localX / 0.00108f
    val tableY = 1000f + localZ / 0.00096f
    return TablePoint(tableX, tableY)
}

/**
 * Renderizador de Realidade Aumentada (AR) para o Air Hockey utilizando o Sceneview e Google ARCore.
 */
@Composable
fun AirHockeyArenaARScene(
    viewModel: GameViewModel,
    mode: GameMode,
    initialScale: ArTableScale = ArTableScale.MINIATURE_1_4,
    onStagingPhaseChanged: (ArStagingPhase) -> Unit = {},
    onExit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesRepo = AirHockeyApp.instance.preferencesRepository
    val prefs by preferencesRepo.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val touchOffset = if (prefs.touchOffsetEnabled) 110f else 0f
    val matchState by viewModel.matchState.collectAsState()
    val engine = viewModel.engine

    // Dimensões dinâmicas da tela em pixels
    var screenWidthPx by remember { mutableFloatStateOf(1080f) }
    var screenHeightPx by remember { mutableFloatStateOf(2340f) }

    LaunchedEffect(view) {
        if (view.width > 0 && view.height > 0) {
            screenWidthPx = view.width.toFloat()
            screenHeightPx = view.height.toFloat()
        }
    }

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
            onExit()
        }
    }

    // Se o dispositivo for comprovadamente incompatível com ARCore, exibe tela de fallback
    if (arAvailability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
        ARUnsupportedFallback(
            onExit = onExit,
            modifier = modifier
        )
        return
    }

    // Se a permissão de câmera ainda não foi concedida, exibe diálogo educacional
    if (!hasCameraPermission) {
        ARPermissionPrompt(
            onGrantPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onExit = onExit,
            modifier = modifier
        )
        return
    }

    // 2. Inicialização do Motor Gráfico Filament & Modelos 3D
    val engine3d = rememberEngine()
    val modelLoader = rememberModelLoader(engine3d)
    val environment = rememberAREnvironment(engine3d)
    val mainLightNode = rememberMainLightNode(engine3d) {
        intensity = 80_000f
    }

    val tableInstance = remember(modelLoader) {
        try {
            modelLoader.createModelInstance("models/mesa_arcade.glb")
        } catch (e: Exception) {
            Log.e("AirHockeyAR", "Erro ao carregar mesa_arcade.glb: ${e.message}")
            null
        }
    }
    val puckInstance = remember(modelLoader) {
        try {
            modelLoader.createModelInstance("models/disco_cyber.glb")
        } catch (e: Exception) {
            Log.e("AirHockeyAR", "Erro ao carregar disco_cyber.glb: ${e.message}")
            null
        }
    }
    val p1Instance = remember(modelLoader) {
        try {
            modelLoader.createModelInstance("models/rebatedor_p1.glb")
        } catch (e: Exception) {
            Log.e("AirHockeyAR", "Erro ao carregar rebatedor_p1.glb: ${e.message}")
            null
        }
    }
    val p2Instance = remember(modelLoader) {
        try {
            modelLoader.createModelInstance("models/rebatedor_p2.glb")
        } catch (e: Exception) {
            Log.e("AirHockeyAR", "Erro ao carregar rebatedor_p2.glb: ${e.message}")
            null
        }
    }

    val tableNode = remember(tableInstance) { tableInstance?.let { ModelNode(it) } }
    val puckNode = remember(puckInstance) { puckInstance?.let { ModelNode(it) } }
    val p1Node = remember(p1Instance) { p1Instance?.let { ModelNode(it) } }
    val p2Node = remember(p2Instance) { p2Instance?.let { ModelNode(it) } }

    val cameraNode = rememberARCameraNode(engine3d)
    val sceneARState = remember { SceneARState() }
    val inputController = remember { ArenaInputController(grabRadiusMultiplier = 8.0f) }

    // Estados do Lobby de Posicionamento AR
    var stagingPhase by remember { mutableStateOf(ArStagingPhase.SCANNING) }
    var surfaceDetected by remember { mutableStateOf(false) }
    val arContext = remember { ArSessionContext() }

    var anchorNode by remember { mutableStateOf<AnchorNode?>(null) }
    var contentNode by remember { mutableStateOf<Node?>(null) }

    var currentScale by remember { mutableStateOf(initialScale) }
    val currentScaleFactor = currentScale.scaleFactor

    // Notifica o GameScreen sobre transições do ciclo de AR
    LaunchedEffect(stagingPhase) {
        onStagingPhaseChanged(stagingPhase)
    }

    // Atualiza a escala do contentNode dinamicamente quando a preferência muda
    LaunchedEffect(currentScale, contentNode) {
        contentNode?.scale = Float3(currentScaleFactor, currentScaleFactor, currentScaleFactor)
    }

    // Higienização de Ponteiros de Toque
    LaunchedEffect(matchState.status) {
        if (matchState.status != MatchStatus.PLAYING) {
            inputController.resetPointers()
            sceneARState.activePointerP1 = null
        }
    }

    fun mapX(engineX: Float): Float = (engineX - 500f) * 0.00108f
    fun mapZ(engineY: Float): Float = (engineY - 1000f) * 0.00096f

    fun resetAnchor() {
        Log.d("AirHockeyAR", "resetAnchor chamado. Retornando para SCANNING.")
        inputController.resetPointers()
        try {
            tableNode?.parent = null
            puckNode?.parent = null
            p1Node?.parent = null
            p2Node?.parent = null
        } catch (_: Throwable) {}
        contentNode = null
        anchorNode = null
        arContext.latestHitResult = null
        surfaceDetected = false
        stagingPhase = ArStagingPhase.SCANNING
    }

    fun anchorTableAtAnchor(anchor: Anchor, camPose: Pose) {
        try {
            val ancTranslation = anchor.pose.translation
            Log.d("AirHockeyAR", "anchorTableAtAnchor: anchor em (${ancTranslation[0]}, ${ancTranslation[1]}, ${ancTranslation[2]}), scale=$currentScaleFactor")

            // Desanexa de pais anteriores antes de reorganizar a hierarquia
            try {
                tableNode?.parent = null
                puckNode?.parent = null
                p1Node?.parent = null
                p2Node?.parent = null
            } catch (_: Throwable) {}

            // Converte a posição mundial da câmera para o espaço local do Anchor
            val camWorld = floatArrayOf(camPose.tx(), camPose.ty(), camPose.tz())
            val camLocal = anchor.pose.inverse().transformPoint(camWorld)
            val localYawDeg = if (!camLocal[0].isNaN() && !camLocal[2].isNaN() && (abs(camLocal[0]) > 1e-4f || abs(camLocal[2]) > 1e-4f)) {
                Math.toDegrees(atan2(camLocal[0].toDouble(), camLocal[2].toDouble())).toFloat()
            } else {
                0f
            }
            Log.d("AirHockeyAR", "anchorTableAtAnchor: camLocal=(${camLocal[0]}, ${camLocal[1]}, ${camLocal[2]}), localYawDeg=$localYawDeg")

            // Nó de conteúdo perfeitamente alinhado na origem do AnchorNode (orientação horizontal e frontal voltada para o jogador)
            val newContentNode = Node(engine3d).apply {
                this.scale = Float3(currentScaleFactor, currentScaleFactor, currentScaleFactor)
                this.position = Float3(0f, 0f, 0f)
                this.rotation = Float3(0f, localYawDeg, 0f)
                tableNode?.let { addChildNode(it) }
                puckNode?.let { addChildNode(it) }
                p1Node?.let { addChildNode(it) }
                p2Node?.let { addChildNode(it) }
            }

            // Sincronização inicial das posições das entidades 3D no plano local da mesa
            puckNode?.position = Float3(mapX(engine.puck.x), 0.0075f, mapZ(engine.puck.y))
            p1Node?.position = Float3(mapX(engine.player1Mallet.x), 0.0f, mapZ(engine.player1Mallet.y))
            p2Node?.position = Float3(mapX(engine.player2Mallet.x), 0.0f, mapZ(engine.player2Mallet.y))

            val newAnchorNode = AnchorNode(engine3d, anchor).apply {
                isPositionEditable = false
                updateAnchorPose = true
                visibleTrackingStates = setOf(
                    TrackingState.TRACKING,
                    TrackingState.PAUSED,
                    TrackingState.STOPPED
                )
                visibleCameraTrackingStates = setOf(
                    TrackingState.TRACKING,
                    TrackingState.PAUSED,
                    TrackingState.STOPPED
                )
                addChildNode(newContentNode)
            }

            contentNode = newContentNode
            anchorNode = newAnchorNode
            stagingPhase = ArStagingPhase.POSITIONED
            Log.d("AirHockeyAR", "anchorTableAtAnchor concluído com sucesso. StagingPhase alterada para POSITIONED.")
        } catch (e: Exception) {
            Log.e("AirHockeyAR", "Falha ao ancorar mesa: ${e.message}", e)
            Toast.makeText(context, "Erro ao fixar arena: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun placeTable() {
        Log.d("AirHockeyAR", "placeTable acionado pelo jogador (surfaceDetected=$surfaceDetected)")
        val session = arContext.session ?: return
        val frame = arContext.frame ?: return

        if (frame.camera.trackingState != TrackingState.TRACKING) {
            Log.w("AirHockeyAR", "placeTable ignorado: câmera ARCore ainda não está em TRACKING")
            Toast.makeText(context, context.getString(R.string.ar_tracking_hint), Toast.LENGTH_SHORT).show()
            return
        }

        val camPose = frame.camera.pose
        val cx = screenWidthPx / 2f
        val cy = screenHeightPx / 2f

        var chosenAnchor: Anchor? = null

        // 1. Tenta usar o latestHitResult se for um Plane ou Point (Depth) válido e são
        val latestHit = arContext.latestHitResult
        if (latestHit != null && isHitSane(latestHit.hitPose.translation, camPose)) {
            chosenAnchor = try {
                latestHit.createAnchor()
            } catch (e: Exception) {
                Log.w("AirHockeyAR", "Falha ao criar anchor a partir de latestHit: ${e.message}")
                null
            }
            if (chosenAnchor != null) {
                Log.d("AirHockeyAR", "placeTable: ancorado via latestHitResult (${latestHit.trackable.javaClass.simpleName}) em ${latestHit.hitPose.translation.contentToString()}")
            }
        }

        // 2. Se não houver latestHit, testa raio central direto e amostras ao redor
        if (chosenAnchor == null) {
            val samplePoints = listOf(
                cx to cy,
                cx to (cy + 150f),
                cx to (cy + 300f),
                (cx - 150f) to (cy + 150f),
                (cx + 150f) to (cy + 150f)
            )
            for ((sx, sy) in samplePoints) {
                val hits = try {
                    frame.hitTest(sx, sy)
                } catch (_: Exception) {
                    emptyList()
                }
                val hit = hits.firstOrNull { h ->
                    val trackable = h.trackable
                    val isPlane = trackable is Plane &&
                        trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                        trackable.trackingState == TrackingState.TRACKING &&
                        (trackable.isPoseInPolygon(h.hitPose) || trackable.isPoseInExtents(h.hitPose))
                    val isPoint = trackable is Point &&
                        trackable.trackingState == TrackingState.TRACKING &&
                        trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL

                    (isPlane || isPoint) && isHitSane(h.hitPose.translation, camPose)
                }
                if (hit != null) {
                    chosenAnchor = try {
                        hit.createAnchor()
                    } catch (e: Exception) {
                        null
                    }
                    if (chosenAnchor != null) {
                        Log.d("AirHockeyAR", "placeTable: ancorado via hitTest (${hit.trackable.javaClass.simpleName}) em ${hit.hitPose.translation.contentToString()}")
                        break
                    }
                }
            }
        }

        // 3. Tenta interseção com plano horizontal rastreado (Floor Plane)
        if (chosenAnchor == null) {
            val trackingPlanes = try {
                session.getAllTrackables(Plane::class.java).filter { plane ->
                    plane.trackingState == TrackingState.TRACKING &&
                        plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                        (plane.centerPose.ty() - camPose.ty()) in -2.0f..-0.2f
                }
            } catch (_: Exception) {
                emptyList()
            }

            // Seleciona o plano horizontal são mais próximo do nível esperado (~0.5m para mesa, ~1.2m para chão)
            val floorPlane = trackingPlanes.minByOrNull { abs(it.centerPose.ty() - camPose.ty() + 0.8f) }

            if (floorPlane != null) {
                val floorY = floorPlane.centerPose.ty()
                val ray = cameraNode.screenPointToRay(cx, cy)
                if (ray.direction.y < -0.05f && ray.origin.y > floorY) {
                    val t = (floorY - ray.origin.y) / ray.direction.y
                    val ix = ray.origin.x + t * ray.direction.x
                    val iy = floorY
                    val iz = ray.origin.z + t * ray.direction.z
                    val intersectionPt = floatArrayOf(ix, iy, iz)

                    if (isHitSane(intersectionPt, camPose)) {
                        val planePose = Pose.makeTranslation(ix, iy, iz)
                        chosenAnchor = try {
                            floorPlane.createAnchor(planePose)
                        } catch (e: Exception) {
                            Log.w("AirHockeyAR", "Falha ao criar anchor com planePose: ${e.message}. Tentando centerPose...")
                            try {
                                floorPlane.createAnchor(floorPlane.centerPose)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (chosenAnchor != null) {
                            Log.d("AirHockeyAR", "placeTable: ancorado no floorPlane em ($ix, $iy, $iz)")
                        }
                    }
                }

                // Fallback para o centerPose do floorPlane se a interseção do raio não ocorreu
                if (chosenAnchor == null && isHitSane(floorPlane.centerPose.translation, camPose)) {
                    chosenAnchor = try {
                        floorPlane.createAnchor(floorPlane.centerPose)
                    } catch (_: Exception) {
                        null
                    }
                    if (chosenAnchor != null) {
                        Log.d("AirHockeyAR", "placeTable: ancorado via centerPose do floorPlane")
                    }
                }
            }
        }

        // 4. Instant Placement Fallback calibrado (para superfícies com pouca textura como mesas brancas ou carpetes lisos)
        if (chosenAnchor == null) {
            val instantHits = try {
                frame.hitTestInstantPlacement(cx, cy, 1.1f)
            } catch (_: Exception) {
                emptyList()
            }
            val instantHit = instantHits.firstOrNull { isHitSane(it.hitPose.translation, camPose) }
            if (instantHit != null) {
                chosenAnchor = try {
                    instantHit.createAnchor()
                } catch (e: Exception) {
                    null
                }
                if (chosenAnchor != null) {
                    Log.d("AirHockeyAR", "placeTable: ancorado via InstantPlacement na mesa/chão em ${instantHit.hitPose.translation.contentToString()}")
                }
            }
        }

        if (chosenAnchor != null) {
            anchorTableAtAnchor(chosenAnchor, camPose)
        } else {
            Log.w("AirHockeyAR", "placeTable: nenhuma superfície física detectada ainda. Orientando jogador a escanear o chão.")
            Toast.makeText(
                context,
                context.getString(R.string.ar_searching_surface_hint),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            inputController.resetPointers()
            sceneARState.activePointerP1 = null
            try {
                tableNode?.parent = null
                puckNode?.parent = null
                p1Node?.parent = null
                p2Node?.parent = null
            } catch (_: Throwable) {
                // Filament Scene e Engine já reciclados pelo ciclo de recomposição
            }
            contentNode = null
            anchorNode = null
        }
    }

    val childNodes = remember(anchorNode) { listOfNotNull(anchorNode) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    screenWidthPx = size.width.toFloat()
                    screenHeightPx = size.height.toFloat()
                }
            }
            .pointerInput(engine, mode, stagingPhase, contentNode, touchOffset) {
                if (stagingPhase != ArStagingPhase.PLAYING) return@pointerInput
                val cNode = contentNode ?: return@pointerInput

                inputController.resetPointers()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        inputController.processPointerEvents(
                            changes = event.changes,
                            dimensions = engine.dimensions,
                            p1MalletX = engine.player1Mallet.x,
                            p1MalletY = engine.player1Mallet.y,
                            p2MalletX = engine.player2Mallet.x,
                            p2MalletY = engine.player2Mallet.y,
                            malletRadius = engine.player1Mallet.radius,
                            mode = mode,
                            touchOffset = touchOffset,
                            isSpectator = matchState.isSpectator,
                            screenToTable = { sx, sy ->
                                screenToTableAR(
                                    screenX = sx,
                                    screenY = sy,
                                    cameraNode = cameraNode,
                                    contentNode = cNode
                                )
                            },
                            onTouchP1 = { tx, ty ->
                                viewModel.onTouchPlayer1(tx, ty)
                            },
                            onTouchP2 = { tx, ty ->
                                viewModel.onTouchPlayer2(tx, ty)
                            },
                            onTouchGuest = { gx, gy ->
                                viewModel.onTouchGuestGlobal(gx, gy)
                            }
                        )
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
            environment = environment,
            mainLightNode = mainLightNode,
            planeRenderer = stagingPhase == ArStagingPhase.SCANNING,
            onViewCreated = {
                planeRenderer.planeRendererMode = PlaneRenderer.PlaneRendererMode.RENDER_ALL
                planeRenderer.isVisible = true
                planeRenderer.isEnabled = true
            },
            sessionConfiguration = { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    config.depthMode = Config.DepthMode.AUTOMATIC
                    Log.d("AirHockeyAR", "DepthMode.AUTOMATIC ativado com sucesso para hardware Galaxy!")
                } else {
                    Log.d("AirHockeyAR", "DepthMode não suportado neste dispositivo.")
                }
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.focusMode = Config.FocusMode.AUTO
            },
            onSessionUpdated = { session, frame ->
                arContext.session = session
                arContext.frame = frame

                if (stagingPhase == ArStagingPhase.SCANNING) {
                    val cx = screenWidthPx / 2f
                    val cy = screenHeightPx / 2f
                    val camPose = frame.camera.pose

                    val samplePoints = listOf(
                        cx to cy,
                        cx to (cy + 150f),
                        cx to (cy + 300f),
                        cx to (cy - 150f),
                        (cx - 150f) to cy,
                        (cx + 150f) to cy,
                        (cx - 150f) to (cy + 150f),
                        (cx + 150f) to (cy + 150f)
                    )

                    var foundHit: HitResult? = null
                    for ((sx, sy) in samplePoints) {
                        val hits = try {
                            frame.hitTest(sx, sy)
                        } catch (_: Exception) {
                            emptyList()
                        }
                        val planeHit = hits.firstOrNull { hit ->
                            val trackable = hit.trackable
                            trackable is Plane &&
                                trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                                trackable.trackingState == TrackingState.TRACKING &&
                                (trackable.isPoseInPolygon(hit.hitPose) || trackable.isPoseInExtents(hit.hitPose)) &&
                                isHitSane(hit.hitPose.translation, camPose)
                        }
                        val pointHit = if (planeHit == null) {
                            hits.firstOrNull { hit ->
                                val trackable = hit.trackable
                                trackable is Point &&
                                    trackable.trackingState == TrackingState.TRACKING &&
                                    trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL &&
                                    isHitSane(hit.hitPose.translation, camPose)
                            }
                        } else null
                        val instantHit = if (planeHit == null && pointHit == null) {
                            hits.firstOrNull { hit ->
                                val trackable = hit.trackable
                                trackable is InstantPlacementPoint &&
                                    trackable.trackingState == TrackingState.TRACKING &&
                                    isHitSane(hit.hitPose.translation, camPose)
                            }
                        } else null

                        val validHit = planeHit ?: pointHit ?: instantHit
                        if (validHit != null) {
                            foundHit = validHit
                            break
                        }
                    }

                    if (foundHit == null) {
                        val instantHits = try {
                            frame.hitTestInstantPlacement(cx, cy, 1.1f)
                        } catch (_: Exception) {
                            emptyList()
                        }
                        foundHit = instantHits.firstOrNull { isHitSane(it.hitPose.translation, camPose) }
                    }

                    arContext.latestHitResult = foundHit

                    val hasPlaneInSession = try {
                        session.getAllTrackables(Plane::class.java).any { plane ->
                            plane.trackingState == TrackingState.TRACKING &&
                                plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                                (plane.centerPose.ty() - camPose.ty()) in -2.0f..-0.2f
                        }
                    } catch (_: Exception) {
                        false
                    }

                    val isDetected = (foundHit != null || hasPlaneInSession)
                    if (isDetected != surfaceDetected) {
                        if (isDetected) {
                            Log.d("AirHockeyAR", "Superfície detectada! (foundHit=${foundHit != null}, hasPlaneInSession=$hasPlaneInSession)")
                        }
                        surfaceDetected = isDetected
                    }
                } else {
                    // Sincronização contínua das posições físicas com as entidades 3D no plano ancorado
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

        // 3. Fase SCANNING: Retícula Guia Visual e Botão Centralizado de Fixação
        if (stagingPhase == ArStagingPhase.SCANNING) {
            ARReticleOverlay(surfaceDetected = surfaceDetected)

            // Dica Superior de Calibração
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(top = 70.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    color = Color(0xCC080E1A),
                    shape = CutCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (surfaceDetected) Color(0xFF00E5FF) else Color(0x66FFFFFF)),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = if (surfaceDetected) {
                            stringResource(R.string.ar_surface_ready_hint)
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

            // Botão Centralizado de Fixação AR ("Posicionar Mesa")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 180.dp)
                ) {
                    Surface(
                        color = if (surfaceDetected) Color(0xEE00E5FF) else Color(0xAA0B111E),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (surfaceDetected) Color(0xFF00E5FF) else Color(0xFF38BDF8)
                        ),
                        modifier = Modifier.clickable {
                            placeTable()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = if (surfaceDetected) Color.Black else Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (surfaceDetected) {
                                    stringResource(R.string.ar_place_table_button)
                                } else {
                                    stringResource(R.string.ar_tracking_hint)
                                },
                                color = if (surfaceDetected) Color.Black else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (surfaceDetected) {
                            stringResource(R.string.ar_surface_ready_hint)
                        } else {
                            stringResource(R.string.ar_searching_surface_hint)
                        },
                        color = if (surfaceDetected) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }

        // 4. Fase POSITIONED: Lobby de Conferência com Botões "Iniciar Partida" e "Reancorar Mesa"
        if (stagingPhase == ArStagingPhase.POSITIONED) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .background(Color(0xDD0B111E), RoundedCornerShape(20.dp))
                        .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 26.dp, vertical = 22.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ar_table_positioned_hint),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Botão Iniciar Partida
                    Button(
                        onClick = {
                            Log.d("AirHockeyAR", "Jogador acionou Iniciar Partida. Liberando contagem regressiva.")
                            stagingPhase = ArStagingPhase.PLAYING
                            viewModel.startCountdownAfterPresentation()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.ar_start_match_button).uppercase(),
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Botão Reancorar Mesa
                    OutlinedButton(
                        onClick = { resetAnchor() },
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.ar_reanchor_table_button),
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. Barra Superior de Controle de Escala e Reancoragem
        ARControlsOverlay(
            stagingPhase = stagingPhase,
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
            onReanchor = { resetAnchor() }
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

            // Ponto central holográfico
            drawCircle(
                color = if (surfaceDetected) Color(0xFF10B981).copy(alpha = 0.9f) else Color(0xFF00E5FF).copy(alpha = 0.5f),
                radius = 8f * pulseAlpha,
                style = Stroke(width = 2.5f)
            )
        }
    }
}

/**
 * Controles de HUD específicos do modo AR (Alternar Escala e Reancorar).
 */
@Composable
private fun ARControlsOverlay(
    stagingPhase: ArStagingPhase,
    currentScale: ArTableScale,
    onToggleScale: () -> Unit,
    onReanchor: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
    ) {
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

            // Botão Reancorar (disponível quando a partida já está em andamento)
            if (stagingPhase == ArStagingPhase.PLAYING) {
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
        }
    }
}

/**
 * Diálogo educacional solicitando permissão de câmera.
 */
@Composable
private fun ARPermissionPrompt(
    onGrantPermission: () -> Unit,
    onExit: () -> Unit,
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
                    onClick = onExit,
                    border = BorderStroke(1.dp, Color(0xFF64748B)),
                    shape = CutCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.cancel).uppercase(),
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
    onExit: () -> Unit,
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
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = CutCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.cancel).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
