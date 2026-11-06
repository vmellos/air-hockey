package dev.vmellos.airhockey.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.feature.lobby.MultiplayerMatchCoordinator
import dev.vmellos.airhockey.feature.network.ControlMessages
import dev.vmellos.airhockey.feature.physics.AirHockeyPhysicsEngine
import dev.vmellos.airhockey.feature.physics.ArbitroVirtual
import dev.vmellos.airhockey.feature.physics.AudioOutputPort
import dev.vmellos.airhockey.feature.physics.TableDimensions
import dev.vmellos.airhockey.feature.stage.ArenaConquestCatalog
import dev.vmellos.airhockey.core.preferences.TableTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private var fallbackEngine = AirHockeyPhysicsEngine(TableDimensions())
    var arbitro: ArbitroVirtual? = null
        private set

    val engine: AirHockeyPhysicsEngine
        get() = arbitro?.engine ?: fallbackEngine

    private var physicsJob: Job? = null
    private var countdownPhysicsJob: Job? = null
    private var timerJob: Job? = null
    private var countdownJob: Job? = null
    private var matchStateCollectJob: Job? = null

    // Callbacks de rede para Mesa Estendida
    var coordinator: MultiplayerMatchCoordinator? = null
        private set
    private val coordinatorJobs = mutableListOf<Job>()

    @Volatile
    private var hasRecordedMatch = false

    var onHostBroadcastTelemetry: ((Float, Float, Float, Float, Float, Float, Float, Float, Byte, Float, Byte) -> Unit)? = null
    var onHostBroadcastState: ((ControlMessages.RemoteMatchState) -> Unit)? = null
    var onGuestSendInput: ((Float, Float) -> Unit)? = null
    var onGuestSendControl: ((String) -> Unit)? = null

    private val audioPort = object : AudioOutputPort {
        override fun playPaddleHit(intensity: Float, isPlayer1: Boolean) {
            audioEngine.playPaddleHit(intensity, isPlayer1)
        }

        override fun playWallHit() {
            audioEngine.playWallHit()
        }

        override fun playGoalScored(score: Int, target: Int) {
            audioEngine.playGoalScored(score, target)
        }

        override fun playGoalConceded(score: Int, target: Int) {
            audioEngine.playGoalConceded(score, target)
        }

        override fun playCountdownBeep(number: Int) {
            audioEngine.playCountdownBeep(number)
        }

        override fun stopAll() {
            audioEngine.stopAll()
        }
    }

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

    fun initializeMatch(spec: MatchSpec) {
        val mode = spec.mode
        val isExtended = mode == GameMode.EXTENDED_HOST || mode == GameMode.EXTENDED_GUEST ||
                mode == GameMode.AI_VS_AI_EXTENDED || mode == GameMode.AI_VS_AI_EXTENDED_HOST || mode == GameMode.AI_VS_AI_EXTENDED_GUEST
        val isGuest = mode == GameMode.EXTENDED_GUEST || mode == GameMode.CONNECTED_GUEST || mode == GameMode.AI_VS_AI_EXTENDED_GUEST
        val bezelUnits = if (isExtended) TableDimensions.calculateBezelUnits(spec.bezelGapMm, spec.screenHeightPx, spec.yDpi) else 0f

        val dimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = bezelUnits,
            isExtendedMode = isExtended
        )

        val resolvedSpec = if (mode == GameMode.SOLO) {
            val stageConfig = AirHockeyApp.instance.stageRepository.getStageConfig(spec.stageNumber)
            spec.copy(
                targetScore = stageConfig.targetScore,
                aiDifficulty = stageConfig.aiDifficulty,
                puckMaxSpeed = stageConfig.puckSpeed.maxSpeed,
                airFriction = stageConfig.puckSpeed.airFriction,
                tableTheme = stageConfig.tableTheme
            )
        } else {
            spec
        }

        hasRecordedMatch = false

        val newArbitro = ArbitroVirtual(
            dimensions = dimensions,
            spec = resolvedSpec,
            audioPort = audioPort,
            onGoalScoredCallback = { scorer, isGameOver ->
                if (isGameOver) {
                    handleGameOver()
                    broadcastState()
                    stopGameLoops()
                } else {
                    broadcastState()
                    viewModelScope.launch {
                        delay(1200)
                        arbitro?.resetPositions(scorer)
                        startCountdown()
                    }
                }
            }
        )
        arbitro = newArbitro

        matchStateCollectJob?.cancel()
        matchStateCollectJob = viewModelScope.launch {
            newArbitro.matchState.collect { s ->
                _matchState.value = s
            }
        }

        if (!isGuest) {
            broadcastState()
            if (!spec.hasIntroPresentation) {
                startCountdown()
            }
        }
    }

    fun initializeMatch(
        mode: GameMode,
        bezelGapMm: Float = 8.0f,
        screenHeightPx: Float = 2400f,
        yDpi: Float = 420f,
        targetScore: Int = 3,
        matchTimeSeconds: Int = 180,
        aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
        aiDifficultyPlayer1: AiDifficulty = AiDifficulty.MEDIUM,
        aiDifficultyPlayer2: AiDifficulty = aiDifficulty,
        puckMaxSpeed: Float = 6800f,
        airFriction: Float = 0.40f,
        player1Name: String = "Jogador 1",
        player2Name: String = "Jogador 2",
        stageNumber: Int = 1,
        hasIntroPresentation: Boolean = false,
        tableTheme: TableTheme? = null
    ) {
        initializeMatch(
            MatchSpec(
                mode = mode,
                bezelGapMm = bezelGapMm,
                screenHeightPx = screenHeightPx,
                yDpi = yDpi,
                targetScore = targetScore,
                matchTimeSeconds = matchTimeSeconds,
                aiDifficulty = aiDifficulty,
                aiDifficultyPlayer1 = aiDifficultyPlayer1,
                aiDifficultyPlayer2 = aiDifficultyPlayer2,
                puckMaxSpeed = puckMaxSpeed,
                airFriction = airFriction,
                player1Name = player1Name,
                player2Name = player2Name,
                stageNumber = stageNumber,
                hasIntroPresentation = hasIntroPresentation,
                tableTheme = tableTheme
            )
        )
    }

    fun startCountdownAfterPresentation() {
        if (_matchState.value.status == MatchStatus.COUNTDOWN && countdownJob?.isActive != true) {
            startCountdown()
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownPhysicsJob?.cancel()

        val currentArbitro = arbitro ?: return

        if (currentArbitro.isHost) {
            countdownPhysicsJob = viewModelScope.launch {
                val dt = 1f / 120f
                val delayMillis = 8L
                while (isActive && currentArbitro.matchState.value.status == MatchStatus.COUNTDOWN) {
                    val start = System.currentTimeMillis()
                    currentArbitro.tickMalletsOnly(dt)

                    onHostBroadcastTelemetry?.invoke(
                        currentArbitro.engine.puck.x, currentArbitro.engine.puck.y,
                        0f, 0f,
                        currentArbitro.engine.player1Mallet.x, currentArbitro.engine.player1Mallet.y,
                        currentArbitro.engine.player2Mallet.x, currentArbitro.engine.player2Mallet.y,
                        0.toByte(), 0f,
                        currentArbitro.engine.puck.lastHitBy.toByte()
                    )

                    val elapsed = System.currentTimeMillis() - start
                    val sleep = (delayMillis - elapsed).coerceAtLeast(1L)
                    delay(sleep)
                }
            }
        }

        countdownJob = viewModelScope.launch {
            currentArbitro.setCountdown(3)
            broadcastState()
            delay(900)

            currentArbitro.setCountdown(2)
            broadcastState()
            delay(900)

            currentArbitro.setCountdown(1)
            broadcastState()
            delay(900)

            countdownPhysicsJob?.cancel()
            countdownPhysicsJob = null

            currentArbitro.setCountdown(0)
            broadcastState()

            startGameLoops()
        }
    }

    private fun startGameLoops() {
        stopGameLoops()
        val currentArbitro = arbitro ?: return

        // Loop físico a 120 Hz
        physicsJob = viewModelScope.launch {
            val dt = 1f / 120f
            val delayMillis = 8L
            while (isActive && currentArbitro.matchState.value.status == MatchStatus.PLAYING) {
                val start = System.currentTimeMillis()

                currentArbitro.tickPhysics(dt)

                if (currentArbitro.isHost) {
                    val (evt, intensity) = currentArbitro.consumeCollisionTelemetry()
                    onHostBroadcastTelemetry?.invoke(
                        currentArbitro.engine.puck.x, currentArbitro.engine.puck.y,
                        currentArbitro.engine.puck.vx, currentArbitro.engine.puck.vy,
                        currentArbitro.engine.player1Mallet.x, currentArbitro.engine.player1Mallet.y,
                        currentArbitro.engine.player2Mallet.x, currentArbitro.engine.player2Mallet.y,
                        evt, intensity,
                        currentArbitro.engine.puck.lastHitBy.toByte()
                    )
                } else if (currentArbitro.aiControllerP2 != null) {
                    onGuestSendInput?.invoke(currentArbitro.engine.player2Mallet.targetX, currentArbitro.engine.player2Mallet.targetY)
                }

                val elapsed = System.currentTimeMillis() - start
                val sleep = (delayMillis - elapsed).coerceAtLeast(1L)
                delay(sleep)
            }
        }

        // Loop de tempo decorrido da partida
        if (currentArbitro.isHost) {
            timerJob = viewModelScope.launch {
                while (isActive && currentArbitro.matchState.value.status == MatchStatus.PLAYING) {
                    delay(1000)
                    currentArbitro.tickTimer()
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

    fun onTouchPlayer1(x: Float, y: Float) {
        arbitro?.onTouchPlayer1(x, y)
    }

    fun onTouchPlayer2(x: Float, y: Float) {
        arbitro?.onTouchPlayer2(x, y)
    }

    fun onTouchGuestGlobal(globalX: Float, globalY: Float) {
        val coords = arbitro?.onTouchGuestGlobal(globalX, globalY)
        if (coords != null) {
            onGuestSendInput?.invoke(coords.first, coords.second)
        }
    }

    fun onGuestTouchLocal(localX: Float, localY: Float) {
        val coords = arbitro?.onGuestTouchLocal(localX, localY)
        if (coords != null) {
            onGuestSendInput?.invoke(coords.first, coords.second)
        }
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
        arbitro?.applyRemoteTelemetry(
            puckX = puckX, puckY = puckY,
            puckVx = puckVx, puckVy = puckVy,
            hostMalletX = hostMalletX, hostMalletY = hostMalletY,
            guestMalletX = guestMalletX, guestMalletY = guestMalletY,
            collisionEvent = collisionEvent,
            collisionIntensity = collisionIntensity,
            puckLastHitBy = puckLastHitBy
        )
    }

    fun onGuestReceiveState(remote: ControlMessages.RemoteMatchState) {
        arbitro?.applyRemoteState(remote)
        if (remote.statusName == MatchStatus.GAME_OVER.name) {
            handleGameOver()
        }
    }

    fun onHostReceiveGuestInput(globalTargetX: Float, globalTargetY: Float) {
        arbitro?.onHostReceiveGuestInput(globalTargetX, globalTargetY)
    }

    private fun broadcastState() {
        if (_matchState.value.isHost) {
            val s = _matchState.value
            val remote = ControlMessages.RemoteMatchState(
                statusName = s.status.name,
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

    private val MatchState.isGuestMode: Boolean
        get() = mode == GameMode.EXTENDED_GUEST || mode == GameMode.CONNECTED_GUEST || mode == GameMode.AI_VS_AI_EXTENDED_GUEST

    fun pauseMatch() {
        if (_matchState.value.isGuestMode) {
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
            arbitro?.pause()
            broadcastState()
            stopGameLoops()
        }
    }

    fun resumeMatch() {
        if (_matchState.value.isGuestMode) {
            onGuestSendControl?.invoke(ControlMessages.RESUME)
            return
        }
        if (_matchState.value.status == MatchStatus.PAUSED) {
            arbitro?.resume()
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
        if (_matchState.value.isGuestMode) {
            onGuestSendControl?.invoke(ControlMessages.RESTART)
            return
        }
        stopGameLoops()
        countdownJob?.cancel()
        hasRecordedMatch = false
        arbitro?.restart()
        broadcastState()
        startCountdown()
    }

    fun onPeerExited() {
        stopGameLoops()
        countdownJob?.cancel()
        arbitro?.setPeerExited()
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
        arbitro?.setIdle()
    }

    private fun handleGameOver() {
        if (hasRecordedMatch) return
        hasRecordedMatch = true
        val state = _matchState.value
        val duration = state.remainingTimeSeconds.coerceAtLeast(0)
        val modeStr = state.mode.name
        val diff = if (state.mode == GameMode.SOLO) arbitro?.aiControllerP2?.difficulty?.name else null

        viewModelScope.launch {
            try {
                val record = AirHockeyApp.instance.leaderboardRepository.recordMatch(
                    gameMode = modeStr,
                    aiDifficulty = diff,
                    playerAName = state.player1Name,
                    playerBName = state.player2Name,
                    scoreA = state.scorePlayer1,
                    scoreB = state.scorePlayer2,
                    durationSeconds = duration
                )
                val eloLeaderboardId = AirHockeyApp.instance.getString(R.string.leaderboard_elo_id)
                if (eloLeaderboardId.isNotBlank()) {
                    val profile = AirHockeyApp.instance.leaderboardRepository.getPlayerStatsDirect(state.player1Name)
                    val newElo = profile?.ratingElo ?: (1000 + record.ratingDeltaA)
                    AirHockeyApp.instance.playGamesManager.submitScore(eloLeaderboardId, newElo.toLong())
                }
            } catch (_: Exception) {}

            if (state.mode == GameMode.SOLO) {
                try {
                    val currentTheme = ArenaConquestCatalog.getThemeForStage(state.currentStage)
                    val previouslyUnlocked = AirHockeyApp.instance.stageRepository.isThemeUnlocked(currentTheme)
                    val stars = AirHockeyApp.instance.stageRepository.recordStageResult(
                        stageNumber = state.currentStage,
                        scorePlayer = state.scorePlayer1,
                        scoreAi = state.scorePlayer2
                    )
                    val newlyConquered = if (stars > 0 && state.currentStage % 10 == 0 && !previouslyUnlocked) currentTheme else null
                    arbitro?.updateStarsEarned(stars, newlyConquered)

                    if (stars > 0) {
                        val stageLeaderboardId = AirHockeyApp.instance.getString(R.string.leaderboard_stages_id)
                        if (stageLeaderboardId.isNotBlank()) {
                            val totalStars = AirHockeyApp.instance.stageRepository.getTotalStarsDirect()
                            AirHockeyApp.instance.playGamesManager.submitScore(stageLeaderboardId, totalStars.toLong())
                        }
                    }
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
