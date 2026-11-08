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
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.core.preferences.VisualEffectsProfile
import dev.vmellos.airhockey.feature.customization.ArenaThemeCatalog
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.GameViewModel
import dev.vmellos.airhockey.feature.game.MatchStatus
import dev.vmellos.airhockey.feature.stage.ArenaConquestCatalog
import dev.vmellos.airhockey.feature.physics.CollisionEvent
import dev.vmellos.airhockey.feature.physics.TableProjection

@Composable
fun AirHockeyArenaCanvas(
    viewModel: GameViewModel,
    mode: GameMode,
    modifier: Modifier = Modifier,
    tableTheme: TableTheme? = null
) {
    val engine = viewModel.engine
    val dimensions = engine.dimensions
    val projection = remember(dimensions, mode) { TableProjection(dimensions, mode) }
    val isExtendedGuest = mode == GameMode.EXTENDED_GUEST || mode == GameMode.AI_VS_AI_EXTENDED_GUEST
    val isExtendedHost = mode == GameMode.EXTENDED_HOST || mode == GameMode.AI_VS_AI_EXTENDED_HOST
    val isConnectedGuest = mode == GameMode.CONNECTED_GUEST
    val isSplitScreen = mode == GameMode.SPLIT_SCREEN

    val prefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val activeTheme = tableTheme ?: prefs.tableTheme
    val touchOffset = if (prefs.touchOffsetEnabled) 110f else 0f
    val matchState by viewModel.matchState.collectAsState()

    val particlePool = remember { ParticlePool(maxSparks = 50, maxShockwaves = 8) }
    val puckTrail = remember { ArrayDeque<Offset>(16) }
    val pendingCollisions = remember { ArrayDeque<Triple<Float, Float, CollisionEvent>>(12) }
    val inputController = remember { ArenaInputController() }

    // Limpa o rastro de energia nas pausas de gol, contagem regressiva e reposicionamento
    androidx.compose.runtime.LaunchedEffect(matchState.status, matchState.lastScorer) {
        if (matchState.status != MatchStatus.PLAYING) {
            puckTrail.clear()
        }
    }

    // Higienização de Ponteiros de Toque: redefine ponteiros quando fora do estado PLAYING
    LaunchedEffect(matchState.status) {
        if (matchState.status != MatchStatus.PLAYING) {
            inputController.resetPointers()
        }
    }

    DisposableEffect(engine) {
        inputController.resetPointers()
        val listener: (Float, Float, CollisionEvent) -> Unit = { px, py, evt ->
            synchronized(pendingCollisions) {
                if (pendingCollisions.size < 12) {
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

    val themeVisuals = remember(activeTheme) { dev.vmellos.airhockey.feature.customization.ArenaThemeCatalog.getVisuals(activeTheme) }
    val arenaBgColor = themeVisuals.backgroundColor

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(arenaBgColor)
            .pointerInput(engine, mode, projection, touchOffset, matchState.currentStage) {
                inputController.resetPointers()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

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
                            screenToTable = { sx, sy -> projection.screenToTable(sx, sy, w, h) },
                            onTouchP1 = { x, y -> viewModel.onTouchPlayer1(x, y) },
                            onTouchP2 = { x, y -> viewModel.onTouchPlayer2(x, y) },
                            onTouchGuest = { x, y -> viewModel.onTouchGuestGlobal(x, y) }
                        )
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
            val themeVisuals = ArenaThemeCatalog.getVisuals(activeTheme)
            val p1ThemeColor = themeVisuals.primaryColor
            val p2ThemeColor = themeVisuals.secondaryColor
            val wallHitColor = themeVisuals.wallHitColor

            synchronized(pendingCollisions) {
                while (pendingCollisions.isNotEmpty()) {
                    val (tx, ty, evt) = pendingCollisions.removeFirst()
                    val sPt = projection.tableToScreen(tx, ty, screenW, screenH)
                    val color = when (evt) {
                        CollisionEvent.PADDLE_HIT_P1 -> p1ThemeColor
                        CollisionEvent.PADDLE_HIT_P2 -> p2ThemeColor
                        CollisionEvent.GOAL_PLAYER1 -> p1ThemeColor
                        CollisionEvent.GOAL_PLAYER2 -> p2ThemeColor
                        else -> wallHitColor
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

            // 2. Desenha arena da mesa completa para todos os 11 temas
            Arena2DRenderer.run {
                drawTableSurface(
                    theme = activeTheme,
                    left = offsetX,
                    top = offsetY,
                    width = vWidth * scale,
                    height = vHeight * scale,
                    scale = scale,
                    isExtendedHost = isExtendedHost,
                    isExtendedGuest = isExtendedGuest,
                    isConnectedGuest = isConnectedGuest
                )
            }

            // 3. Desenha metas da mesa
            val goalW = dimensions.goalWidth * scale
            val goalLeft = offsetX + dimensions.goalLeft * scale
            val screenBottom = offsetY + vHeight * scale
            val bottomGoalColor = if (isExtendedGuest) p2ThemeColor else p1ThemeColor

            Arena2DRenderer.run {
                drawTableGoals(
                    theme = activeTheme,
                    goalLeft = goalLeft,
                    goalWidth = goalW,
                    topY = offsetY,
                    bottomY = screenBottom,
                    scale = scale,
                    topGoalColor = p2ThemeColor,
                    bottomGoalColor = bottomGoalColor,
                    drawTopGoal = !isExtendedHost
                )
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

            // 7. Desenha Rebatedor 1 (Jogador 1)
            val p1Screen = projection.tableToScreen(engine.player1Mallet.x, engine.player1Mallet.y, screenW, screenH)
            when (prefs.player1MalletSkin) {
                MalletSkin.MAGNETIC_STRIKER -> drawCyberStrikerMallet(cx = p1Screen.x, cy = p1Screen.y, radius = engine.player1Mallet.radius * scale, accentColor = NeonEmerald, rimColor = NeonEmeraldBright, scale = scale)
                MalletSkin.DUNK_STRIKER -> drawDunkStrikerMallet(cx = p1Screen.x, cy = p1Screen.y, radius = engine.player1Mallet.radius * scale, accentColor = NeonAmber, rimColor = NeonAmberBright, scale = scale)
                MalletSkin.SPIKE_EMITTER -> drawSpikeEmitterMallet(cx = p1Screen.x, cy = p1Screen.y, radius = engine.player1Mallet.radius * scale, accentColor = NeonViolet, rimColor = NeonVioletBright, scale = scale)
                MalletSkin.LINEBACKER_STRIKER -> drawGridironMallet(cx = p1Screen.x, cy = p1Screen.y, radius = engine.player1Mallet.radius * scale, accentColor = GridironCyan, rimColor = GridironGold, scale = scale)
                MalletSkin.SLUGGER_STRIKER -> drawSluggerMallet(cx = p1Screen.x, cy = p1Screen.y, radius = engine.player1Mallet.radius * scale, accentColor = BaseballRed, rimColor = BaseballRedBright, scale = scale)
                MalletSkin.RIPPER_STRIKER -> drawRipperMallet(cx = p1Screen.x, cy = p1Screen.y, radius = engine.player1Mallet.radius * scale, accentColor = WastelandFlame, rimColor = WastelandFlameBright, scale = scale)
                MalletSkin.OUTRUN_STRIKER -> drawOutrunStrikerMallet(cx = p1Screen.x, cy = p1Screen.y, radius = engine.player1Mallet.radius * scale, accentColor = SynthPink, rimColor = SynthPinkBright, scale = scale)
                MalletSkin.MAGNETIC_COIL -> drawMagneticCoilMallet(cx = p1Screen.x, cy = p1Screen.y, radius = engine.player1Mallet.radius * scale, accentColor = TokamakRadiation, rimColor = TokamakRadiationBright, scale = scale)
                MalletSkin.PONG_PADDLE -> drawPongMallet(cx = p1Screen.x, cy = p1Screen.y, radius = engine.player1Mallet.radius * scale, accentColor = PongGreen, rimColor = PongGreenBright, scale = scale)
                MalletSkin.SPARTAN_AEGIS -> drawSpartanAegisMallet(cx = p1Screen.x, cy = p1Screen.y, radius = engine.player1Mallet.radius * scale, accentColor = ColosseumGold, rimColor = ColosseumGoldBright, scale = scale)
                MalletSkin.CLASSIC_NEON -> drawPseudo3DMallet(cx = p1Screen.x, cy = p1Screen.y, radius = engine.player1Mallet.radius * scale, accentColor = NeonCyan, rimColor = NeonCyanBright, scale = scale)
            }

            // 8. Desenha Rebatedor 2 (Jogador 2 / Guardião da Arena no Solo)
            val p2Screen = projection.tableToScreen(engine.player2Mallet.x, engine.player2Mallet.y, screenW, screenH)
            val effectiveP2MalletSkin = if (mode == GameMode.SOLO) {
                ArenaConquestCatalog.getTierForStage(matchState.currentStage).malletSkin
            } else {
                prefs.player2MalletSkin
            }
            when (effectiveP2MalletSkin) {
                MalletSkin.MAGNETIC_STRIKER -> drawCyberStrikerMallet(cx = p2Screen.x, cy = p2Screen.y, radius = engine.player2Mallet.radius * scale, accentColor = NeonRuby, rimColor = NeonRubyBright, scale = scale)
                MalletSkin.DUNK_STRIKER -> drawDunkStrikerMallet(cx = p2Screen.x, cy = p2Screen.y, radius = engine.player2Mallet.radius * scale, accentColor = NeonCyan, rimColor = NeonCyanBright, scale = scale)
                MalletSkin.SPIKE_EMITTER -> drawSpikeEmitterMallet(cx = p2Screen.x, cy = p2Screen.y, radius = engine.player2Mallet.radius * scale, accentColor = NeonMagenta, rimColor = NeonMagentaBright, scale = scale)
                MalletSkin.LINEBACKER_STRIKER -> drawGridironMallet(cx = p2Screen.x, cy = p2Screen.y, radius = engine.player2Mallet.radius * scale, accentColor = GridironGold, rimColor = GridironCyan, scale = scale)
                MalletSkin.SLUGGER_STRIKER -> drawSluggerMallet(cx = p2Screen.x, cy = p2Screen.y, radius = engine.player2Mallet.radius * scale, accentColor = BaseballCyan, rimColor = BaseballCyanBright, scale = scale)
                MalletSkin.RIPPER_STRIKER -> drawRipperMallet(cx = p2Screen.x, cy = p2Screen.y, radius = engine.player2Mallet.radius * scale, accentColor = WastelandFlameBright, rimColor = WastelandFlame, scale = scale)
                MalletSkin.OUTRUN_STRIKER -> drawOutrunStrikerMallet(cx = p2Screen.x, cy = p2Screen.y, radius = engine.player2Mallet.radius * scale, accentColor = SynthCyan, rimColor = SynthCyanBright, scale = scale)
                MalletSkin.MAGNETIC_COIL -> drawMagneticCoilMallet(cx = p2Screen.x, cy = p2Screen.y, radius = engine.player2Mallet.radius * scale, accentColor = TokamakPlasma, rimColor = TokamakPlasmaBright, scale = scale)
                MalletSkin.PONG_PADDLE -> drawPongMallet(cx = p2Screen.x, cy = p2Screen.y, radius = engine.player2Mallet.radius * scale, accentColor = PongGreenBright, rimColor = PongGreen, scale = scale)
                MalletSkin.SPARTAN_AEGIS -> drawSpartanAegisMallet(cx = p2Screen.x, cy = p2Screen.y, radius = engine.player2Mallet.radius * scale, accentColor = ColosseumCrimson, rimColor = ColosseumGold, scale = scale)
                MalletSkin.CLASSIC_NEON -> drawPseudo3DMallet(cx = p2Screen.x, cy = p2Screen.y, radius = engine.player2Mallet.radius * scale, accentColor = NeonOrange, rimColor = NeonOrangeBright, scale = scale)
            }

            // 9. Desenha Disco e Rastro de Energia (Prints 2 e 3)
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
                        val sparkColor = if (engine.puck.lastHitBy == 1) p1ThemeColor else if (engine.puck.lastHitBy == 2) p2ThemeColor else wallHitColor
                        particlePool.spawnSparks(puckScreen.x, puckScreen.y, 2, sparkColor, spreadSpeed = 200f * scale, duration = 0.35f)
                    }
                } else if (puckTrail.isNotEmpty()) {
                    puckTrail.removeLast()
                }

                // Cor dinâmica do rastro e do disco conforme o último batedor
                val (puckAccentColor, puckCoreColor) = when (prefs.puckSkin) {
                    PuckSkin.CYBER_SOCCER_BALL -> when (engine.puck.lastHitBy) {
                        1 -> Pair(NeonEmerald, NeonEmeraldCore)
                        2 -> Pair(NeonRuby, NeonRubyCore)
                        else -> Pair(CyberSoccerGold, WhiteHot)
                    }
                    PuckSkin.NEO_BASKETBALL -> when (engine.puck.lastHitBy) {
                        1 -> Pair(NeonAmber, NeonAmberCore)
                        2 -> Pair(NeonCyan, NeonCyanCore)
                        else -> Pair(NeonAmberBright, WhiteHot)
                    }
                    PuckSkin.GRAVITY_VOLLEY -> when (engine.puck.lastHitBy) {
                        1 -> Pair(NeonViolet, NeonVioletCore)
                        2 -> Pair(NeonMagenta, NeonMagentaCore)
                        else -> Pair(VolleyYellow, WhiteHot)
                    }
                    PuckSkin.GRIDIRON_FOOTBALL -> when (engine.puck.lastHitBy) {
                        1 -> Pair(GridironCyan, GridironCyanCore)
                        2 -> Pair(GridironGold, GridironGoldCore)
                        else -> Pair(GridironGoldBright, WhiteHot)
                    }
                    PuckSkin.CYBER_BASEBALL -> when (engine.puck.lastHitBy) {
                        1 -> Pair(BaseballRed, BaseballRedCore)
                        2 -> Pair(BaseballCyan, BaseballCyanCore)
                        else -> Pair(BaseballRedBright, WhiteHot)
                    }
                    PuckSkin.SAWBLADE -> when (engine.puck.lastHitBy) {
                        1 -> Pair(WastelandFlame, WastelandFlameCore)
                        2 -> Pair(WastelandFlameBright, WastelandFlameCore)
                        else -> Pair(WastelandHazardYellow, WhiteHot)
                    }
                    PuckSkin.SYNTH_DISC -> when (engine.puck.lastHitBy) {
                        1 -> Pair(SynthPink, SynthPinkCore)
                        2 -> Pair(SynthCyan, SynthCyanCore)
                        else -> Pair(SynthSunYellow, WhiteHot)
                    }
                    PuckSkin.ATOMIC_CORE -> when (engine.puck.lastHitBy) {
                        1 -> Pair(TokamakRadiation, TokamakRadiationCore)
                        2 -> Pair(TokamakPlasma, TokamakPlasmaCore)
                        else -> Pair(TokamakRadiationBright, WhiteHot)
                    }
                    PuckSkin.PONG_SQUARE -> when (engine.puck.lastHitBy) {
                        1 -> Pair(PongGreen, PongGreenCore)
                        2 -> Pair(PongGreenBright, PongGreenCore)
                        else -> Pair(PongGreenBright, WhiteHot)
                    }
                    PuckSkin.GLADIATOR_SHIELD -> when (engine.puck.lastHitBy) {
                        1 -> Pair(ColosseumGold, ColosseumGoldCore)
                        2 -> Pair(ColosseumCrimson, ColosseumCrimsonCore)
                        else -> Pair(ColosseumGoldBright, WhiteHot)
                    }
                    PuckSkin.CLASSIC_PLASMA -> when (engine.puck.lastHitBy) {
                        1 -> Pair(NeonCyan, NeonCyanCore)
                        2 -> Pair(NeonOrange, NeonOrangeCore)
                        else -> Pair(NeonCyanBright, WhiteHot)
                    }
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

                // Desenha o disco de acordo com a skin ativa
                when (prefs.puckSkin) {
                    PuckSkin.CYBER_SOCCER_BALL -> drawCyberSoccerPuck(cx = puckScreen.x, cy = puckScreen.y, radius = engine.puck.radius * scale, accentColor = puckAccentColor, scale = scale)
                    PuckSkin.NEO_BASKETBALL -> drawNeoBasketballPuck(cx = puckScreen.x, cy = puckScreen.y, radius = engine.puck.radius * scale, accentColor = puckAccentColor, scale = scale)
                    PuckSkin.GRAVITY_VOLLEY -> drawGravityVolleyPuck(cx = puckScreen.x, cy = puckScreen.y, radius = engine.puck.radius * scale, accentColor = puckAccentColor, scale = scale)
                    PuckSkin.GRIDIRON_FOOTBALL -> drawGridironPuck(cx = puckScreen.x, cy = puckScreen.y, radius = engine.puck.radius * scale, accentColor = puckAccentColor, scale = scale)
                    PuckSkin.CYBER_BASEBALL -> drawCyberBaseballPuck(cx = puckScreen.x, cy = puckScreen.y, radius = engine.puck.radius * scale, accentColor = puckAccentColor, scale = scale)
                    PuckSkin.SAWBLADE -> drawSawbladePuck(cx = puckScreen.x, cy = puckScreen.y, radius = engine.puck.radius * scale, accentColor = puckAccentColor, scale = scale)
                    PuckSkin.SYNTH_DISC -> drawSynthwavePuck(cx = puckScreen.x, cy = puckScreen.y, radius = engine.puck.radius * scale, accentColor = puckAccentColor, scale = scale)
                    PuckSkin.ATOMIC_CORE -> drawAtomicCorePuck(cx = puckScreen.x, cy = puckScreen.y, radius = engine.puck.radius * scale, accentColor = puckAccentColor, scale = scale)
                    PuckSkin.PONG_SQUARE -> drawPongPuck(cx = puckScreen.x, cy = puckScreen.y, radius = engine.puck.radius * scale, accentColor = puckAccentColor, scale = scale)
                    PuckSkin.GLADIATOR_SHIELD -> drawGladiatorShieldPuck(cx = puckScreen.x, cy = puckScreen.y, radius = engine.puck.radius * scale, accentColor = puckAccentColor, scale = scale)
                    PuckSkin.CLASSIC_PLASMA -> drawEnergyRingPuck(cx = puckScreen.x, cy = puckScreen.y, radius = engine.puck.radius * scale, accentColor = puckAccentColor, scale = scale)
                }
            }
        }
    }
}
