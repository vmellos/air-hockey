package dev.vmellos.airhockey.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.feature.ai.AirHockeyAiController
import dev.vmellos.airhockey.feature.lobby.MultiplayerMatchCoordinator
import dev.vmellos.airhockey.feature.network.ControlMessages
import dev.vmellos.airhockey.feature.physics.AirHockeyPhysicsEngine
import dev.vmellos.airhockey.feature.physics.CollisionEvent
import dev.vmellos.airhockey.feature.physics.TableDimensions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val audioEngine = AirHockeyApp.instance.audioEngine

    val soundEnabled: StateFlow<Boolean> = AirHockeyApp.instance.preferencesRepository.userPreferencesFlow
        .map { it.soundEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val musicEnabled: StateFlow<Boolean> = AirHockeyApp.instance.preferencesRepository.userPreferencesFlow
        .map { it.musicEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _matchState = MutableStateFlow(MatchState())
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private var dimensions = TableDimensions()
    lateinit var engine: AirHockeyPhysicsEngine
        private set

    private var aiControllerP1: AirHockeyAiController? = null
    private var aiControllerP2: AirHockeyAiController? = null
    private var physicsJob: Job? = null
    private var countdownPhysicsJob: Job? = null
    private var timerJob: Job? = null
    private var countdownJob: Job? = null

    // Callbacks de rede para Mesa Estendida
    var coordinator: MultiplayerMatchCoordinator? = null
        private set
    private val coordinatorJobs = mutableListOf<Job>()

    @Volatile
    private var pendingCollisionEvent: Byte = 0
    @Volatile
    private var pendingCollisionIntensity: Float = 0f

    @Volatile
    private var hasRecordedMatch = false
    private var currentMatchTimeSeconds: Int = 180

    var onHostBroadcastTelemetry: ((Float, Float, Float, Float, Float, Float, Float, Float, Byte, Float, Byte) -> Unit)? = null
    var onHostBroadcastState: ((ControlMessages.RemoteMatchState) -> Unit)? = null
    var onGuestSendInput: ((Float, Float) -> Unit)? = null
    var onGuestSendControl: ((String) -> Unit)? = null

    fun attachCoordinator(coord: MultiplayerMatchCoordinator) {
        coordinatorJobs.forEach { it.cancel() }
        coordinatorJobs.clear()
        coordinator = coord
        val isHost = coord.isHost

        if (isHost) {
            onHostBroadcastTelemetry = { px, py, pvx, pvy, hmx, hmy, gmx, gmy, evt, intensity, lastHit ->
                coord.broadcastTelemetry(px, py, pvx, pvy, hmx, hmy, gmx, gmy, collisionEvent = evt, collisionIntensity = intensity, puckLastHitBy = lastHit)
            }
            onHostBroadcastState = { remote ->
                coord.broadcastState(remote)
            }
            coordinatorJobs += viewModelScope.launch {
                coord.transport.guestInputs.collect { input ->
                    onHostReceiveGuestInput(input.guestTargetX, input.guestTargetY)
                }
            }
            coord.onPeerPauseRequested = { pauseMatch() }
            coord.onPeerResumeRequested = { resumeMatch() }
            coord.onPeerRestartRequested = { restartMatch() }
            coordinatorJobs += viewModelScope.launch {
                coord.state.collect { state ->
                    if (state.peerExited) onPeerExited()
                }
            }
        } else {
            coordinatorJobs += viewModelScope.launch {
                coord.transport.telemetryFrames.collect { t ->
                    onGuestReceiveTelemetry(
                        t.puckX, t.puckY,
                        t.puckVx, t.puckVy,
                        t.hostMalletX, t.hostMalletY,
                        t.guestMalletX, t.guestMalletY,
                        t.collisionEvent,
                        t.collisionIntensity,
                        t.puckLastHitBy
                    )
                }
            }
            coordinatorJobs += viewModelScope.launch {
                coord.state.collect { state ->
                    state.remoteMatchState?.let { onGuestReceiveState(it) }
                    if (state.peerExited) onPeerExited()
                }
            }
            onGuestSendInput = { gx, gy ->
                coord.sendGuestInput(gx, gy)
            }
            onGuestSendControl = { msg ->
                when (msg) {
                    ControlMessages.PAUSE -> coord.requestPause()
                    ControlMessages.RESUME -> coord.requestResume()
                    ControlMessages.RESTART -> coord.requestRestart()
                }
            }
        }
    }

    fun initializeMatch(
        mode: GameMode,
        bezelGapMm: Float = 8.0f,
        screenHeightPx: Float = 2400f,
        yDpi: Float = 420f,
        targetScore: Int = 7,
        matchTimeSeconds: Int = 180,
        aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
        aiDifficultyPlayer1: AiDifficulty = AiDifficulty.MEDIUM,
        aiDifficultyPlayer2: AiDifficulty = aiDifficulty,
        puckMaxSpeed: Float = 6800f,
        airFriction: Float = 0.40f,
        player1Name: String = "Jogador 1",
        player2Name: String = "Jogador 2",
        stageNumber: Int = 1
    ) {
        val isExtended = mode == GameMode.EXTENDED_HOST || mode == GameMode.EXTENDED_GUEST ||
                mode == GameMode.AI_VS_AI_EXTENDED || mode == GameMode.AI_VS_AI_EXTENDED_HOST || mode == GameMode.AI_VS_AI_EXTENDED_GUEST
        val isGuest = mode == GameMode.EXTENDED_GUEST || mode == GameMode.CONNECTED_GUEST || mode == GameMode.AI_VS_AI_EXTENDED_GUEST
        val bezelUnits = if (isExtended) TableDimensions.calculateBezelUnits(bezelGapMm, screenHeightPx, yDpi) else 0f

        dimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = bezelUnits,
            isExtendedMode = isExtended
        )

        val effectiveTargetScore: Int
        val effectiveAiDiff: AiDifficulty
        val effectivePuckMaxSpeed: Float
        val effectiveAirFriction: Float

        if (mode == GameMode.SOLO) {
            val stageConfig = AirHockeyApp.instance.stageRepository.getStageConfig(stageNumber)
            effectiveTargetScore = stageConfig.targetScore
            effectiveAiDiff = stageConfig.aiDifficulty
            effectivePuckMaxSpeed = stageConfig.puckSpeed.maxSpeed
            effectiveAirFriction = stageConfig.puckSpeed.airFriction
        } else {
            effectiveTargetScore = targetScore
            effectiveAiDiff = aiDifficulty
            effectivePuckMaxSpeed = puckMaxSpeed
            effectiveAirFriction = airFriction
        }

        engine = AirHockeyPhysicsEngine(dimensions, maxSpeed = effectivePuckMaxSpeed, airFrictionPerSec = effectiveAirFriction)
        engine.resetPositions()
        hasRecordedMatch = false
        currentMatchTimeSeconds = matchTimeSeconds

        // Inicializa controladores de IA conforme o modo
        aiControllerP1 = when (mode) {
            GameMode.AI_VS_AI_LOCAL -> AirHockeyAiController(aiDifficultyPlayer1, isPlayer1 = true)
            GameMode.AI_VS_AI_EXTENDED, GameMode.AI_VS_AI_EXTENDED_HOST -> if (!isGuest) AirHockeyAiController(aiDifficultyPlayer1, isPlayer1 = true) else null
            else -> null
        }

        aiControllerP2 = when (mode) {
            GameMode.SOLO -> AirHockeyAiController(effectiveAiDiff, isPlayer1 = false)
            GameMode.AI_VS_AI_LOCAL -> AirHockeyAiController(aiDifficultyPlayer2, isPlayer1 = false)
            GameMode.AI_VS_AI_EXTENDED, GameMode.AI_VS_AI_EXTENDED_HOST -> if (!isGuest) AirHockeyAiController(aiDifficultyPlayer2, isPlayer1 = false) else null
            else -> null
        }

        _matchState.value = MatchState(
            mode = mode,
            status = MatchStatus.COUNTDOWN,
            scorePlayer1 = 0,
            scorePlayer2 = 0,
            targetScore = effectiveTargetScore,
            countdownNumber = 3,
            remainingTimeSeconds = 0,
            isHost = !isGuest,
            peerExited = false,
            player1Name = player1Name,
            player2Name = player2Name,
            currentStage = stageNumber,
            starsEarned = 0
        )

        setupCollisionListener()

        if (!isGuest) {
            broadcastState()
            startCountdown()
        }
    }

    private fun setupCollisionListener() {
        engine.onCollisionListener = { event, intensity ->
            when (event) {
                CollisionEvent.PADDLE_HIT_P1 -> {
                    audioEngine.playPaddleHit(intensity)
                    pendingCollisionEvent = 1
                    pendingCollisionIntensity = intensity
                }
                CollisionEvent.PADDLE_HIT_P2 -> {
                    audioEngine.playPaddleHit(intensity)
                    pendingCollisionEvent = 2
                    pendingCollisionIntensity = intensity
                }
                CollisionEvent.WALL_HIT -> {
                    audioEngine.playWallHit()
                    pendingCollisionEvent = 3
                    pendingCollisionIntensity = intensity
                }
                CollisionEvent.GOAL_PLAYER1 -> {
                    // Ponto para Player 1 (disco entrou na meta do Player 2 / topo)
                    handleGoal(scorer = 1)
                }
                CollisionEvent.GOAL_PLAYER2 -> {
                    // Ponto para Player 2 (disco entrou na meta do Player 1 / base)
                    handleGoal(scorer = 2)
                }
                CollisionEvent.NONE -> {}
            }
        }
    }

    private fun handleGoal(scorer: Int) {
        if (_matchState.value.status != MatchStatus.PLAYING) return

        audioEngine.playGoal()

        val p1 = if (scorer == 1) _matchState.value.scorePlayer1 + 1 else _matchState.value.scorePlayer1
        val p2 = if (scorer == 2) _matchState.value.scorePlayer2 + 1 else _matchState.value.scorePlayer2

        val target = _matchState.value.targetScore
        if (p1 >= target || p2 >= target) {
            _matchState.update {
                it.copy(
                    scorePlayer1 = p1,
                    scorePlayer2 = p2,
                    lastScorer = scorer,
                    winner = if (p1 >= target) 1 else 2,
                    status = MatchStatus.GAME_OVER
                )
            }
            handleGameOver()
            broadcastState()
            stopGameLoops()
        } else {
            _matchState.update {
                it.copy(
                    scorePlayer1 = p1,
                    scorePlayer2 = p2,
                    lastScorer = scorer,
                    status = MatchStatus.GOAL_PAUSE
                )
            }
            broadcastState()

            viewModelScope.launch {
                delay(1200)
                engine.resetPositions(scorer)
                startCountdown()
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownPhysicsJob?.cancel()

        // Loop cinemático dos rebatedores a 120 Hz durante a contagem regressiva
        if (_matchState.value.isHost) {
            countdownPhysicsJob = viewModelScope.launch {
                val dt = 1f / 120f
                val delayMillis = 8L
                while (isActive && _matchState.value.status == MatchStatus.COUNTDOWN) {
                    val start = System.currentTimeMillis()
                    engine.tickMalletsOnly(dt)

                    onHostBroadcastTelemetry?.invoke(
                        engine.puck.x, engine.puck.y,
                        0f, 0f,
                        engine.player1Mallet.x, engine.player1Mallet.y,
                        engine.player2Mallet.x, engine.player2Mallet.y,
                        0.toByte(), 0f,
                        engine.puck.lastHitBy.toByte()
                    )

                    val elapsed = System.currentTimeMillis() - start
                    val sleep = (delayMillis - elapsed).coerceAtLeast(1L)
                    delay(sleep)
                }
            }
        }

        countdownJob = viewModelScope.launch {
            _matchState.update { it.copy(status = MatchStatus.COUNTDOWN, countdownNumber = 3) }
            broadcastState()
            audioEngine.playCountdownBeep(3)
            delay(900)

            _matchState.update { it.copy(countdownNumber = 2) }
            broadcastState()
            audioEngine.playCountdownBeep(2)
            delay(900)

            _matchState.update { it.copy(countdownNumber = 1) }
            broadcastState()
            audioEngine.playCountdownBeep(1)
            delay(900)

            countdownPhysicsJob?.cancel()
            countdownPhysicsJob = null

            _matchState.update { it.copy(countdownNumber = 0, status = MatchStatus.PLAYING) }
            broadcastState()
            audioEngine.playCountdownBeep(0)

            startGameLoops()
        }
    }

    private fun startGameLoops() {
        stopGameLoops()

        // Loop físico a 120 Hz
        physicsJob = viewModelScope.launch {
            val dt = 1f / 120f
            val delayMillis = 8L
            while (isActive && _matchState.value.status == MatchStatus.PLAYING) {
                val start = System.currentTimeMillis()

                if (_matchState.value.isHost) {
                    aiControllerP1?.update(engine, dt)
                    aiControllerP2?.update(engine, dt)
                    engine.tick(dt)

                    val evt = pendingCollisionEvent
                    val intensity = pendingCollisionIntensity
                    pendingCollisionEvent = 0
                    pendingCollisionIntensity = 0f

                    // Emite telemetria completa (incluindo posição conciliada do convidado e telemetria de colisão)
                    onHostBroadcastTelemetry?.invoke(
                        engine.puck.x, engine.puck.y,
                        engine.puck.vx, engine.puck.vy,
                        engine.player1Mallet.x, engine.player1Mallet.y,
                        engine.player2Mallet.x, engine.player2Mallet.y,
                        evt, intensity,
                        engine.puck.lastHitBy.toByte()
                    )
                } else if (aiControllerP2 != null) {
                    aiControllerP2?.update(engine, dt)
                    onGuestSendInput?.invoke(engine.player2Mallet.targetX, engine.player2Mallet.targetY)
                }

                val elapsed = System.currentTimeMillis() - start
                val sleep = (delayMillis - elapsed).coerceAtLeast(1L)
                delay(sleep)
            }
        }

        // Loop de tempo decorrido da partida (apenas no Anfitrião/Solo/Split para estatísticas)
        if (_matchState.value.isHost) {
            timerJob = viewModelScope.launch {
                while (isActive && _matchState.value.status == MatchStatus.PLAYING) {
                    delay(1000)
                    _matchState.update { state ->
                        state.copy(remainingTimeSeconds = state.remainingTimeSeconds + 1)
                    }
                    broadcastState()
                }
            }
        }
    }

    private fun stopGameLoops() {
        physicsJob?.cancel()
        physicsJob = null
        countdownPhysicsJob?.cancel()
        countdownPhysicsJob = null
        timerJob?.cancel()
        timerJob = null
    }

    private fun isInputAllowed(): Boolean {
        val s = _matchState.value.status
        if (_matchState.value.isSpectator) return false
        return s == MatchStatus.PLAYING || s == MatchStatus.COUNTDOWN
    }

    fun onTouchPlayer1(x: Float, y: Float) {
        if (!::engine.isInitialized || !isInputAllowed()) return
        if (_matchState.value.status == MatchStatus.COUNTDOWN) {
            val (cx, cy) = engine.clampPositionAgainstFrozenPuck(x, y, engine.player1Mallet.radius)
            engine.player1Mallet.targetX = cx
            engine.player1Mallet.targetY = cy
            engine.player1Mallet.x = cx
            engine.player1Mallet.y = cy
            engine.clampMalletAgainstFrozenPuck(engine.player1Mallet)
        } else {
            engine.player1Mallet.targetX = x
            engine.player1Mallet.targetY = y
        }
    }

    fun onTouchPlayer2(x: Float, y: Float) {
        if (!::engine.isInitialized || !isInputAllowed()) return
        if (_matchState.value.status == MatchStatus.COUNTDOWN) {
            val (cx, cy) = engine.clampPositionAgainstFrozenPuck(x, y, engine.player2Mallet.radius)
            engine.player2Mallet.targetX = cx
            engine.player2Mallet.targetY = cy
            engine.player2Mallet.x = cx
            engine.player2Mallet.y = cy
            engine.clampMalletAgainstFrozenPuck(engine.player2Mallet)
        } else {
            engine.player2Mallet.targetX = x
            engine.player2Mallet.targetY = y
        }
    }

    fun onTouchGuestGlobal(globalX: Float, globalY: Float) {
        if (!::engine.isInitialized || !isInputAllowed()) return

        // Previsão no Cliente: movimenta imediatamente o rebatedor do Jogador 2 no aparelho convidado
        val radius = engine.player2Mallet.radius
        val clampedX = globalX.coerceIn(radius, engine.dimensions.width - radius)
        val maxY = if (engine.dimensions.isExtendedMode) {
            engine.dimensions.hostHalfBottomY - radius
        } else {
            engine.dimensions.centerLineY - radius
        }
        val clampedY = globalY.coerceIn(radius, maxY)

        engine.player2Mallet.targetX = clampedX
        engine.player2Mallet.targetY = clampedY
        engine.player2Mallet.x = clampedX
        engine.player2Mallet.y = clampedY

        onGuestSendInput?.invoke(clampedX, clampedY)
    }

    fun onGuestTouchLocal(localX: Float, localY: Float) {
        if (!isInputAllowed()) return
        // Transforma toque do convidado (rotacionado 180 graus na Disposição Topo-a-Topo) para o espaço global
        val globalX = 1000f - localX
        val globalY = 1000f - localY
        onTouchGuestGlobal(globalX, globalY)
    }

    fun toggleSound() {
        val repo = AirHockeyApp.instance.preferencesRepository
        viewModelScope.launch {
            repo.setSoundEnabled(!soundEnabled.value)
        }
    }

    fun toggleMusic() {
        val repo = AirHockeyApp.instance.preferencesRepository
        viewModelScope.launch {
            repo.setMusicEnabled(!musicEnabled.value)
        }
    }

    fun onGuestReceiveTelemetry(
        puckX: Float, puckY: Float,
        puckVx: Float, puckVy: Float,
        hostMalletX: Float, hostMalletY: Float,
        guestMalletX: Float, guestMalletY: Float,
        collisionEvent: Byte = 0,
        collisionIntensity: Float = 0f,
        puckLastHitBy: Byte = 0
    ) {
        if (!::engine.isInitialized) return

        engine.puck.x = puckX
        engine.puck.y = puckY
        engine.puck.vx = puckVx
        engine.puck.vy = puckVy
        engine.puck.lastHitBy = puckLastHitBy.toInt()
        engine.player1Mallet.x = hostMalletX
        engine.player1Mallet.y = hostMalletY

        // Reconciliação suave da Previsão no Cliente do Jogador 2
        // Durante a contagem regressiva, permite movimentação tática preparatória livre sem puxões
        if (_matchState.value.status != MatchStatus.COUNTDOWN) {
            val dx = guestMalletX - engine.player2Mallet.x
            val dy = guestMalletY - engine.player2Mallet.y
            val distSq = dx * dx + dy * dy
            if (distSq > 2500f) {
                engine.player2Mallet.x = guestMalletX
                engine.player2Mallet.y = guestMalletY
            } else if (distSq > 4f) {
                engine.player2Mallet.x += dx * 0.35f
                engine.player2Mallet.y += dy * 0.35f
            }
        }

        // Telemetria de Colisão: reproduz som de colisão de rebatedor ou borda no dispositivo convidado
        if (collisionEvent.toInt() != 0) {
            when (collisionEvent.toInt()) {
                1, 2 -> audioEngine.playPaddleHit(collisionIntensity.coerceAtLeast(0.4f))
                3 -> audioEngine.playWallHit()
            }
        }
    }

    fun onGuestReceiveState(remote: ControlMessages.RemoteMatchState) {
        val newStatus = try {
            MatchStatus.valueOf(remote.statusName)
        } catch (_: Exception) {
            MatchStatus.PLAYING
        }
        val prev = _matchState.value
        _matchState.update {
            it.copy(
                status = newStatus,
                scorePlayer1 = remote.scorePlayer1,
                scorePlayer2 = remote.scorePlayer2,
                remainingTimeSeconds = remote.remainingTimeSeconds,
                countdownNumber = remote.countdownNumber,
                winner = remote.winner,
                lastScorer = remote.lastScorer
            )
        }

        if (newStatus == MatchStatus.GAME_OVER) {
            handleGameOver()
        }

        // Reprodução de áudio sincronizada no convidado
        if (newStatus == MatchStatus.COUNTDOWN && remote.countdownNumber != prev.countdownNumber) {
            audioEngine.playCountdownBeep(remote.countdownNumber)
        } else if (newStatus == MatchStatus.PLAYING && prev.status == MatchStatus.COUNTDOWN) {
            audioEngine.playCountdownBeep(0)
        } else if (newStatus == MatchStatus.GOAL_PAUSE && prev.status != MatchStatus.GOAL_PAUSE) {
            audioEngine.playGoal()
        }
    }

    fun onHostReceiveGuestInput(globalTargetX: Float, globalTargetY: Float) {
        if (!::engine.isInitialized) return
        engine.player2Mallet.targetX = globalTargetX
        engine.player2Mallet.targetY = globalTargetY
        if (_matchState.value.status == MatchStatus.COUNTDOWN) {
            engine.player2Mallet.x = globalTargetX
            engine.player2Mallet.y = globalTargetY
        }
    }

    private fun broadcastState() {
        if (_matchState.value.isHost) {
            val s = _matchState.value
            val remote = ControlMessages.RemoteMatchState(
                statusName = s.statusName,
                scorePlayer1 = s.scorePlayer1,
                scorePlayer2 = s.scorePlayer2,
                remainingTimeSeconds = s.remainingTimeSeconds,
                countdownNumber = s.countdownNumber,
                winner = s.winner,
                lastScorer = s.lastScorer
            )
            onHostBroadcastState?.invoke(remote)
        }
    }

    private val MatchState.statusName: String
        get() = status.name

    fun pauseMatch() {
        if (_matchState.value.mode == GameMode.EXTENDED_GUEST || _matchState.value.mode == GameMode.CONNECTED_GUEST) {
            onGuestSendControl?.invoke(ControlMessages.PAUSE)
            return
        }
        val currentStatus = _matchState.value.status
        if (currentStatus == MatchStatus.PLAYING || currentStatus == MatchStatus.COUNTDOWN || currentStatus == MatchStatus.GOAL_PAUSE) {
            countdownJob?.cancel()
            countdownJob = null
            countdownPhysicsJob?.cancel()
            countdownPhysicsJob = null
            audioEngine.stopAll()
            _matchState.update { it.copy(status = MatchStatus.PAUSED) }
            broadcastState()
            stopGameLoops()
        }
    }

    fun resumeMatch() {
        if (_matchState.value.mode == GameMode.EXTENDED_GUEST || _matchState.value.mode == GameMode.CONNECTED_GUEST) {
            onGuestSendControl?.invoke(ControlMessages.RESUME)
            return
        }
        if (_matchState.value.status == MatchStatus.PAUSED) {
            startCountdown()
        }
    }

    fun togglePause() {
        if (_matchState.value.status == MatchStatus.PLAYING) {
            pauseMatch()
        } else if (_matchState.value.status == MatchStatus.PAUSED) {
            resumeMatch()
        }
    }

    fun restartMatch() {
        if (_matchState.value.mode == GameMode.EXTENDED_GUEST || _matchState.value.mode == GameMode.CONNECTED_GUEST) {
            onGuestSendControl?.invoke(ControlMessages.RESTART)
            return
        }
        stopGameLoops()
        countdownJob?.cancel()
        hasRecordedMatch = false
        if (::engine.isInitialized) {
            engine.resetPositions(0)
        }
        _matchState.update {
            it.copy(
                status = MatchStatus.COUNTDOWN,
                scorePlayer1 = 0,
                scorePlayer2 = 0,
                winner = 0,
                starsEarned = 0,
                countdownNumber = 3,
                remainingTimeSeconds = 0
            )
        }
        broadcastState()
        startCountdown()
    }

    fun onPeerExited() {
        stopGameLoops()
        countdownJob?.cancel()
        _matchState.update { it.copy(peerExited = true, status = MatchStatus.PAUSED) }
    }

    fun stopMatch() {
        stopGameLoops()
        countdownJob?.cancel()
        countdownJob = null
        countdownPhysicsJob?.cancel()
        countdownPhysicsJob = null
        audioEngine.stopAll()
        coordinatorJobs.forEach { it.cancel() }
        coordinatorJobs.clear()
        coordinator = null
        onHostBroadcastTelemetry = null
        onHostBroadcastState = null
        onGuestSendInput = null
        onGuestSendControl = null
        _matchState.update { it.copy(status = MatchStatus.IDLE) }
    }

    private fun handleGameOver() {
        if (hasRecordedMatch) return
        hasRecordedMatch = true
        val state = _matchState.value
        val duration = state.remainingTimeSeconds.coerceAtLeast(0)
        val modeStr = state.mode.name
        val diff = if (state.mode == GameMode.SOLO) aiControllerP2?.difficulty?.name else null

        viewModelScope.launch {
            try {
                AirHockeyApp.instance.leaderboardRepository.recordMatch(
                    gameMode = modeStr,
                    aiDifficulty = diff,
                    playerAName = state.player1Name,
                    playerBName = state.player2Name,
                    scoreA = state.scorePlayer1,
                    scoreB = state.scorePlayer2,
                    durationSeconds = duration
                )
            } catch (_: Exception) {}

            if (state.mode == GameMode.SOLO) {
                try {
                    val stars = AirHockeyApp.instance.stageRepository.recordStageResult(
                        stageNumber = state.currentStage,
                        scorePlayer = state.scorePlayer1,
                        scoreAi = state.scorePlayer2
                    )
                    _matchState.update { it.copy(starsEarned = stars) }
                } catch (_: Exception) {}
            }

            AirHockeyApp.instance.adManager.recordMatchFinished()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMatch()
    }
}
