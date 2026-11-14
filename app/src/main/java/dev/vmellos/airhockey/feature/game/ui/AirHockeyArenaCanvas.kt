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
import dev.vmellos.airhockey.core.catalog.ArenaPresetCatalog
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.core.preferences.VisualEffectsProfile
import dev.vmellos.airhockey.feature.game.ArenaRuntime
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.MatchStatus
import dev.vmellos.airhockey.feature.physics.CollisionEvent
import dev.vmellos.airhockey.feature.physics.TableProjection
import dev.vmellos.airhockey.feature.physics.VisualCollisionBurst
import kotlin.math.hypot

private data class MascotPawPrint2D(val x: Float, val y: Float, val timestampNanos: Long)

@Composable
fun AirHockeyArenaCanvas(
    runtime: ArenaRuntime,
    mode: GameMode,
    modifier: Modifier = Modifier,
    tableTheme: TableTheme? = null
) {
    val matchState by runtime.matchState.collectAsState()
    val dimensions = matchState.dimensions
    val projection = remember(dimensions, mode) { TableProjection(dimensions, mode) }
    val isExtendedGuest = mode.capabilities.isExtendedGuest
    val isExtendedHost = mode.capabilities.isExtendedHost
    val isConnectedGuest = mode.capabilities.isConnectedGuest
    val isSplitScreen = mode.capabilities.isSplitScreen

    val prefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val activeTheme = tableTheme ?: prefs.tableTheme
    val touchOffset = if (prefs.touchOffsetEnabled) 110f else 0f

    val particlePool = remember { ParticlePool(maxSparks = 50, maxShockwaves = 8) }
    val puckTrail = remember { ArrayDeque<Offset>(16) }
    val burstBuffer = remember { mutableListOf<VisualCollisionBurst>() }
    var lastBurstSeq by remember(runtime) { mutableLongStateOf(0L) }
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

    val arenaKit = remember(activeTheme) { activeTheme.kit() }
    val cosmetics = remember(arenaKit, activeTheme, tableTheme, mode, matchState.currentStage, prefs) {
        arenaKit.resolveCosmetics(
            activeTheme = activeTheme,
            explicitTableTheme = tableTheme,
            mode = mode,
            currentStage = matchState.currentStage,
            prefs = prefs
        )
    }
    val themeVisuals = arenaKit.visuals
    val arenaBgColor = themeVisuals.backgroundColor
    val pawPrints2D = remember(activeTheme) { ArrayDeque<MascotPawPrint2D>(32) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(arenaBgColor)
            .pointerInput(runtime, mode, projection, touchOffset, matchState.currentStage) {
                inputController.resetPointers()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        val snap = runtime.currentSnapshot()
                        inputController.processPointerEvents(
                            changes = event.changes,
                            frame = snap,
                            mode = mode,
                            mapper = { sx, sy -> projection.screenToTable(sx, sy, w, h) },
                            sink = runtime::submitMalletTarget,
                            touchOffset = touchOffset
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
            val p1ThemeColor = themeVisuals.primaryColor
            val p2ThemeColor = themeVisuals.secondaryColor
            val wallHitColor = themeVisuals.wallHitColor

            burstBuffer.clear()
            lastBurstSeq = runtime.collectVisualBurstsSince(lastBurstSeq, burstBuffer)
            for (i in 0 until burstBuffer.size) {
                val burst = burstBuffer[i]
                val evt = burst.event
                val sPt = projection.tableToScreen(burst.tableX, burst.tableY, screenW, screenH)
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

            particlePool.update(dt)

            // 2. Desenha superfície da arena conforme a perspectiva da mesa
            Arena2DRenderer.run {
                drawTableSurface(
                    theme = activeTheme,
                    left = offsetX,
                    top = offsetY,
                    width = vWidth * scale,
                    height = vHeight * scale,
                    scale = scale,
                    perspective = mode.capabilities.perspective
                )
            }

            // Mascote 2D em Circuito de Patrulha Móvel com Rastro de Patinhas (ADR 0042)
            if (arenaKit.hasMascot && !isExtendedGuest) {
                val topMarginY = if (offsetY > 30f * scale) offsetY * 0.5f else (offsetY + 35f * scale)
                val leftX = if (offsetX > 25f * scale) offsetX * 0.5f else (offsetX + 35f * scale)
                val rightX = if (offsetX > 25f * scale) (screenW - offsetX * 0.5f) else (screenW - 35f * scale)
                val bottomMarginY = if (offsetY > 30f * scale) (screenH - offsetY * 0.5f) else (screenH - 35f * scale)

                val segW = (rightX - leftX).coerceAtLeast(10f)
                val segH = (bottomMarginY - topMarginY).coerceAtLeast(10f)
                val totalPerimeter = 2f * (segW + segH)

                // Período de 22 segundos para dar uma volta completa no campo
                val periodNanos = 22_000_000_000L
                val timeFraction = ((framePulse % periodNanos).toDouble() / periodNanos.toDouble()).toFloat()
                val distAlong = timeFraction * totalPerimeter

                val (kx, ky) = when {
                    distAlong < segW -> {
                        // 1. Topo: da esquerda para a direita
                        (leftX + distAlong) to topMarginY
                    }
                    distAlong < segW + segH -> {
                        // 2. Lateral Direita: do topo para o rodapé
                        val d = distAlong - segW
                        rightX to (topMarginY + d)
                    }
                    distAlong < 2f * segW + segH -> {
                        // 3. Rodapé: da direita para a esquerda
                        val d = distAlong - (segW + segH)
                        (rightX - d) to bottomMarginY
                    }
                    else -> {
                        // 4. Lateral Esquerda: do rodapé para o topo
                        val d = distAlong - (2f * segW + segH)
                        leftX to (bottomMarginY - d)
                    }
                }

                // Registro de pegada efêmera a cada ~32px percorridos
                val lastPrint = pawPrints2D.lastOrNull()
                if (lastPrint == null || hypot(kx - lastPrint.x, ky - lastPrint.y) > 32f * scale) {
                    if (pawPrints2D.size >= 32) {
                        pawPrints2D.removeFirst()
                    }
                    pawPrints2D.addLast(MascotPawPrint2D(kx, ky, framePulse))
                }

                // Pruning e renderização de pegadas com fade out suave de 1.2s
                val cutoffNanos = framePulse - 1_200_000_000L
                while (pawPrints2D.isNotEmpty() && pawPrints2D.first().timestampNanos < cutoffNanos) {
                    pawPrints2D.removeFirst()
                }

                for (p in pawPrints2D) {
                    val age = (framePulse - p.timestampNanos) / 1_200_000_000f
                    val alpha = (1f - age).coerceIn(0f, 1f)
                    drawKiaraPawTrailPrint(p.x, p.y, scale, alpha)
                }

                // Silhueta vetorial da Mascote Kiara 2D
                drawKiara2DGutterMascot(kx, ky, scale * 1.08f)
            }

            // 3. Desenha metas da mesa
            val goalW = dimensions.goalWidth * scale
            val goalLeft = offsetX + dimensions.goalLeft * scale
            val screenBottom = offsetY + vHeight * scale
            val topGoalColor = if (isConnectedGuest) p1ThemeColor else p2ThemeColor
            val bottomGoalColor = if (isExtendedGuest || isConnectedGuest) p2ThemeColor else p1ThemeColor

            Arena2DRenderer.run {
                drawTableGoals(
                    theme = activeTheme,
                    goalLeft = goalLeft,
                    goalWidth = goalW,
                    topY = offsetY,
                    bottomY = screenBottom,
                    scale = scale,
                    topGoalColor = topGoalColor,
                    bottomGoalColor = bottomGoalColor,
                    perspective = mode.capabilities.perspective
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
            val snap = runtime.currentSnapshot()
            val p1Screen = projection.tableToScreen(snap.player1MalletX, snap.player1MalletY, screenW, screenH)
            val p1Skin = cosmetics.p1MalletSkin
            val p1Visuals = p1Skin.spec().visuals(isPlayer1 = true)
            Arena2DRenderer.run {
                drawMalletSkin(
                    skin = p1Skin,
                    x = p1Screen.x,
                    y = p1Screen.y,
                    radius = dimensions.malletRadius * scale,
                    color = p1Visuals.color,
                    rimColor = p1Visuals.rimColor,
                    scale = scale
                )
            }

            // 8. Desenha Rebatedor 2 (Jogador 2 / Guardião da Arena no Solo)
            val p2Screen = projection.tableToScreen(snap.player2MalletX, snap.player2MalletY, screenW, screenH)
            val effectiveP2MalletSkin = cosmetics.p2MalletSkin
            val p2Visuals = effectiveP2MalletSkin.spec().visuals(isPlayer1 = false)
            Arena2DRenderer.run {
                drawMalletSkin(
                    skin = effectiveP2MalletSkin,
                    x = p2Screen.x,
                    y = p2Screen.y,
                    radius = dimensions.malletRadius * scale,
                    color = p2Visuals.color,
                    rimColor = p2Visuals.rimColor,
                    scale = scale
                )
            }

            // 9. Desenha Disco e Rastro de Energia (Prints 2 e 3)
            if (projection.isVisibleInViewport(snap.puckX, snap.puckY, dimensions.puckRadius)) {
                val puckScreen = projection.tableToScreen(snap.puckX, snap.puckY, screenW, screenH)
                val spd = snap.puckSpeed()

                // Atualiza histórico do rastro de plasma com amostragem espacial (higienizado fora do estado PLAYING)
                if (matchState.status != MatchStatus.PLAYING) {
                    puckTrail.clear()
                } else if (spd > 220f && prefs.visualEffectsProfile == VisualEffectsProfile.FULL) {
                    val maxPoints = 14
                    val head = puckTrail.firstOrNull()
                    if (head != null) {
                        val moved = hypot(puckScreen.x - head.x, puckScreen.y - head.y)
                        val minStep = (3.5f * scale).coerceAtLeast(1.5f)
                        if (moved > 350f * scale) {
                            puckTrail.clear()
                            puckTrail.addFirst(Offset(puckScreen.x, puckScreen.y))
                        } else if (moved >= minStep) {
                            puckTrail.addFirst(Offset(puckScreen.x, puckScreen.y))
                            if (puckTrail.size > maxPoints) puckTrail.removeLast()
                        }
                    } else {
                        puckTrail.addFirst(Offset(puckScreen.x, puckScreen.y))
                    }

                    // Em velocidade ultra-alta, emite micro-faíscas incandescentes da cauda de fogo
                    if (prefs.visualEffectsProfile == VisualEffectsProfile.FULL && spd > 2200f && (framePulse % 4L == 0L)) {
                        val sparkColor = if (snap.puckLastHitBy == 1) p1ThemeColor else if (snap.puckLastHitBy == 2) p2ThemeColor else wallHitColor
                        particlePool.spawnSparks(puckScreen.x, puckScreen.y, 2, sparkColor, spreadSpeed = 200f * scale, duration = 0.35f)
                    }
                } else if (puckTrail.isNotEmpty()) {
                    puckTrail.removeLast()
                }

                // Cor dinâmica do rastro e do disco conforme o último batedor
                val puckSpec = cosmetics.puckSkin.spec()
                val (puckAccentColor, puckCoreColor) = puckSpec.trailColors(snap.puckLastHitBy)

                // Desenha rastro fluido de plasma com ondulação de chamas
                drawDynamicEnergyTrail(
                    trailPoints = puckTrail,
                    puckRadius = dimensions.puckRadius * scale,
                    color = puckAccentColor,
                    coreColor = puckCoreColor,
                    framePulse = framePulse,
                    profile = prefs.visualEffectsProfile,
                    scale = scale
                )

                // Desenha o disco de acordo com a skin ativa
                Arena2DRenderer.run {
                    drawPuckSkin(
                        skin = cosmetics.puckSkin,
                        x = puckScreen.x,
                        y = puckScreen.y,
                        radius = dimensions.puckRadius * scale,
                        accentColor = puckAccentColor,
                        scale = scale
                    )
                }
            } else if (puckTrail.isNotEmpty()) {
                puckTrail.clear()
            }
        }
    }
}
