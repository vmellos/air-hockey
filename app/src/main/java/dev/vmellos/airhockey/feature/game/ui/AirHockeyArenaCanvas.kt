package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.core.preferences.VisualEffectsProfile
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.GameViewModel
import dev.vmellos.airhockey.feature.game.MatchStatus
import dev.vmellos.airhockey.feature.physics.CollisionEvent
import dev.vmellos.airhockey.feature.physics.TableProjection

@Composable
fun AirHockeyArenaCanvas(
    viewModel: GameViewModel,
    mode: GameMode,
    modifier: Modifier = Modifier
) {
    val engine = viewModel.engine
    val dimensions = engine.dimensions
    val projection = remember(dimensions, mode) { TableProjection(dimensions, mode) }
    val isExtendedGuest = mode == GameMode.EXTENDED_GUEST || mode == GameMode.AI_VS_AI_EXTENDED_GUEST
    val isExtendedHost = mode == GameMode.EXTENDED_HOST || mode == GameMode.AI_VS_AI_EXTENDED_HOST
    val isConnectedGuest = mode == GameMode.CONNECTED_GUEST
    val isSplitScreen = mode == GameMode.SPLIT_SCREEN

    val prefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val touchOffset = if (prefs.touchOffsetEnabled) 110f else 0f
    val matchState by viewModel.matchState.collectAsState()

    val particlePool = remember { ParticlePool(maxSparks = 50, maxShockwaves = 8) }
    val puckTrail = remember { ArrayDeque<Offset>(16) }
    val pendingCollisions = remember { ArrayDeque<Triple<Float, Float, CollisionEvent>>(12) }

    // Limpa o rastro de energia nas pausas de gol, contagem regressiva e reposicionamento
    androidx.compose.runtime.LaunchedEffect(matchState.status, matchState.lastScorer) {
        if (matchState.status != MatchStatus.PLAYING) {
            puckTrail.clear()
        }
    }

    DisposableEffect(engine) {
        val listener: (Float, Float, CollisionEvent) -> Unit = { px, py, evt ->
            synchronized(pendingCollisions) {
                if (pendingCollisions.size < 12) {
                    pendingCollisions.addLast(Triple(px, py, evt))
                }
            }
        }
        engine.onVisualCollisionListener = listener
        onDispose {
            if (engine.onVisualCollisionListener === listener) {
                engine.onVisualCollisionListener = null
            }
        }
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

    var activePointerP1 by remember { androidx.compose.runtime.mutableStateOf<PointerId?>(null) }
    var activePointerP2 by remember { androidx.compose.runtime.mutableStateOf<PointerId?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TableBgDark)
            .pointerInput(mode, projection, touchOffset) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        if (matchState.isSpectator) {
                            // No modo espectador IA x IA, os toques não interferem nos rebatedores
                            continue
                        }

                        val grabRadiusSq = (engine.player1Mallet.radius * 2.2f).let { it * it }

                        for (change in event.changes) {
                            val tablePt = projection.screenToTable(change.position.x, change.position.y, w, h)

                            if (change.pressed) {
                                when (mode) {
                                    GameMode.EXTENDED_GUEST, GameMode.CONNECTED_GUEST -> {
                                        val mallet = engine.player2Mallet
                                        val targetY = tablePt.y + touchOffset
                                        val dx = tablePt.x - mallet.x
                                        val dy = targetY - mallet.y
                                        val distSq = dx * dx + dy * dy

                                        if (activePointerP2 == change.id) {
                                            viewModel.onTouchGuestGlobal(tablePt.x, targetY)
                                            change.consume()
                                        } else if (activePointerP2 == null && distSq <= grabRadiusSq) {
                                            activePointerP2 = change.id
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

                                        if (activePointerP1 == change.id) {
                                            viewModel.onTouchPlayer1(tablePt.x, targetY)
                                            change.consume()
                                        } else if (activePointerP1 == null && distSq <= grabRadiusSq) {
                                            activePointerP1 = change.id
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

                                            if (activePointerP2 == change.id) {
                                                viewModel.onTouchPlayer2(tablePt.x, targetY)
                                                change.consume()
                                            } else if (activePointerP2 == null && distSq <= grabRadiusSq) {
                                                activePointerP2 = change.id
                                                viewModel.onTouchPlayer2(tablePt.x, targetY)
                                                change.consume()
                                            }
                                        } else {
                                            val mallet = engine.player1Mallet
                                            val targetY = tablePt.y - touchOffset
                                            val dx = tablePt.x - mallet.x
                                            val dy = targetY - mallet.y
                                            val distSq = dx * dx + dy * dy

                                            if (activePointerP1 == change.id) {
                                                viewModel.onTouchPlayer1(tablePt.x, targetY)
                                                change.consume()
                                            } else if (activePointerP1 == null && distSq <= grabRadiusSq) {
                                                activePointerP1 = change.id
                                                viewModel.onTouchPlayer1(tablePt.x, targetY)
                                                change.consume()
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            } else {
                                // Liberação do toque
                                if (activePointerP1 == change.id) {
                                    activePointerP1 = null
                                }
                                if (activePointerP2 == change.id) {
                                    activePointerP2 = null
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val pulse = framePulse

            val screenW = size.width
            val screenH = size.height
            val scale = projection.calculateScale(screenW, screenH)
            val offsetX = projection.calculateOffsetX(screenW, screenH)
            val offsetY = projection.calculateOffsetY(screenW, screenH)
            val vWidth = projection.viewportVirtualWidth
            val vHeight = projection.viewportVirtualHeight

            // Atualiza física das partículas com delta time real
            val dt = if (lastFrameTime > 0L) {
                ((framePulse - lastFrameTime) / 1_000_000_000f).coerceIn(0.001f, 0.050f)
            } else {
                0.016f
            }
            lastFrameTime = framePulse

            // 1. Drena colisões pendentes e dispara efeitos visuais no pool de partículas
            synchronized(pendingCollisions) {
                while (pendingCollisions.isNotEmpty()) {
                    val (tx, ty, evt) = pendingCollisions.removeFirst()
                    val sPt = projection.tableToScreen(tx, ty, screenW, screenH)
                    val color = when (evt) {
                        CollisionEvent.PADDLE_HIT_P1 -> NeonCyan
                        CollisionEvent.PADDLE_HIT_P2 -> NeonOrange
                        CollisionEvent.GOAL_PLAYER1 -> NeonCyan
                        CollisionEvent.GOAL_PLAYER2 -> NeonOrange
                        else -> Color(0xFFE2E8F0)
                    }

                    if (prefs.visualEffectsProfile != VisualEffectsProfile.OFF) {
                        if (evt == CollisionEvent.GOAL_PLAYER1 || evt == CollisionEvent.GOAL_PLAYER2) {
                            val isTop = evt == CollisionEvent.GOAL_PLAYER1
                            particlePool.spawnGoalBurst(sPt.x, sPt.y, isTop, color)
                        } else {
                            particlePool.spawnShockwave(sPt.x, sPt.y, color, maxRadius = 140f * scale)
                            if (prefs.visualEffectsProfile == VisualEffectsProfile.FULL) {
                                val count = if (evt == CollisionEvent.WALL_HIT) 6 else 12
                                particlePool.spawnSparks(sPt.x, sPt.y, count, color, spreadSpeed = 480f * scale)
                            }
                        }
                    }
                }
            }

            particlePool.update(dt)

            // 2. Desenha arena da mesa com Bordas Chanfradas Neon
            drawChamferedTable(
                left = offsetX,
                top = offsetY,
                width = vWidth * scale,
                height = vHeight * scale,
                scale = scale,
                isExtendedHost = isExtendedHost,
                isExtendedGuest = isExtendedGuest,
                isConnectedGuest = isConnectedGuest
            )

            // 3. Desenha metas trapezoidais anguladas
            val goalW = dimensions.goalWidth * scale
            val goalLeft = offsetX + dimensions.goalLeft * scale
            val screenBottom = offsetY + vHeight * scale

            if (isConnectedGuest) {
                drawAngledGoal(goalLeft, goalW, screenBottom, scale, NeonOrange, isTop = false)
                drawAngledGoal(goalLeft, goalW, offsetY, scale, NeonCyan, isTop = true)
            } else if (isExtendedGuest) {
                drawAngledGoal(goalLeft, goalW, screenBottom, scale, NeonOrange, isTop = false)
            } else if (isExtendedHost) {
                drawAngledGoal(goalLeft, goalW, screenBottom, scale, NeonCyan, isTop = false)
            } else {
                // Modo Solo e Split
                drawAngledGoal(goalLeft, goalW, screenBottom, scale, NeonCyan, isTop = false)
                drawAngledGoal(goalLeft, goalW, offsetY, scale, NeonOrange, isTop = true)
            }

            // 4. Vórtice de Contagem Regressiva (Print 1)
            if (matchState.status == MatchStatus.COUNTDOWN && prefs.visualEffectsProfile != VisualEffectsProfile.OFF) {
                val centerPt = projection.tableToScreen(dimensions.width / 2f, dimensions.centerLineY, screenW, screenH)
                drawCountdownVortex(
                    centerX = centerPt.x,
                    centerY = centerPt.y,
                    countdownNumber = matchState.countdownNumber,
                    framePulse = framePulse,
                    scale = scale
                )
            }

            // 5. Desenha ondas de choque expansivas e ondas de choque radiais
            val timeSec = (framePulse / 1_000_000_000.0).toFloat()
            particlePool.drawShockwaves(this, scale, timeSec)

            // 6. Desenha faíscas incandescentes
            if (prefs.visualEffectsProfile == VisualEffectsProfile.FULL) {
                particlePool.drawSparks(this, scale)
            }

            // 7. Desenha Rebatedor 1 (Jogador 1 - Ciano) com relevo pseudo-3D
            val p1Screen = projection.tableToScreen(engine.player1Mallet.x, engine.player1Mallet.y, screenW, screenH)
            drawPseudo3DMallet(
                cx = p1Screen.x,
                cy = p1Screen.y,
                radius = engine.player1Mallet.radius * scale,
                accentColor = NeonCyan,
                rimColor = NeonCyanBright,
                scale = scale
            )

            // 8. Desenha Rebatedor 2 (Jogador 2 - Laranja) com relevo pseudo-3D
            val p2Screen = projection.tableToScreen(engine.player2Mallet.x, engine.player2Mallet.y, screenW, screenH)
            drawPseudo3DMallet(
                cx = p2Screen.x,
                cy = p2Screen.y,
                radius = engine.player2Mallet.radius * scale,
                accentColor = NeonOrange,
                rimColor = NeonOrangeBright,
                scale = scale
            )

            // 9. Desenha Disco em Anel Toroidal e Rastro de Energia (Prints 2 e 3)
            if (projection.isVisibleInViewport(engine.puck.x, engine.puck.y, engine.puck.radius)) {
                val puckScreen = projection.tableToScreen(engine.puck.x, engine.puck.y, screenW, screenH)
                val spd = engine.puck.speed()

                // Atualiza histórico do rastro de plasma (higienizado fora do estado PLAYING)
                if (matchState.status != MatchStatus.PLAYING) {
                    puckTrail.clear()
                } else if (spd > 220f && prefs.visualEffectsProfile == VisualEffectsProfile.FULL) {
                    puckTrail.addFirst(Offset(puckScreen.x, puckScreen.y))
                    val maxPoints = 14
                    if (puckTrail.size > maxPoints) puckTrail.removeLast()

                    // Em velocidade ultra-alta, emite micro-faíscas incandescentes da cauda de fogo
                    if (prefs.visualEffectsProfile == VisualEffectsProfile.FULL && spd > 2200f && (framePulse % 4L == 0L)) {
                        val sparkColor = if (engine.puck.lastHitBy == 1) NeonCyan else if (engine.puck.lastHitBy == 2) NeonOrange else GoldSpark
                        particlePool.spawnSparks(puckScreen.x, puckScreen.y, 2, sparkColor, spreadSpeed = 200f * scale, duration = 0.35f)
                    }
                } else if (puckTrail.isNotEmpty()) {
                    puckTrail.removeLast()
                }

                // Cor dinâmica do rastro e do disco conforme o último batedor
                val (puckAccentColor, puckCoreColor) = when (engine.puck.lastHitBy) {
                    1 -> Pair(NeonCyan, NeonCyanCore)
                    2 -> Pair(NeonOrange, NeonOrangeCore)
                    else -> Pair(NeonCyanBright, WhiteHot)
                }

                // Desenha rastro fluido de plasma com ondulação de chamas
                drawDynamicEnergyTrail(
                    trailPoints = puckTrail,
                    puckRadius = engine.puck.radius * scale,
                    color = puckAccentColor,
                    coreColor = puckCoreColor,
                    framePulse = framePulse,
                    profile = prefs.visualEffectsProfile,
                    scale = scale
                )

                // Desenha o disco anelar toroidal
                drawEnergyRingPuck(
                    cx = puckScreen.x,
                    cy = puckScreen.y,
                    radius = engine.puck.radius * scale,
                    accentColor = puckAccentColor,
                    scale = scale
                )
            }
        }
    }
}
