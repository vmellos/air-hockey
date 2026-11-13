package dev.vmellos.airhockey.feature.game

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.feature.game.ui.PlayerSlot
import dev.vmellos.airhockey.feature.lobby.MultiplayerMatchCoordinator
import dev.vmellos.airhockey.feature.network.ControlMessages
import dev.vmellos.airhockey.feature.physics.ArbitroVirtual
import dev.vmellos.airhockey.feature.physics.AudioOutputPort
import dev.vmellos.airhockey.feature.physics.MatchFrameSnapshot
import dev.vmellos.airhockey.feature.physics.TableDimensions
import dev.vmellos.airhockey.feature.physics.VisualCollisionBurst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@Stable
interface ArenaRuntime {
    val matchState: StateFlow<MatchState>
    val soundEnabled: StateFlow<Boolean>
    val musicEnabled: StateFlow<Boolean>
    fun currentSnapshot(): MatchFrameSnapshot
    fun collectVisualBurstsSince(lastSeq: Long, out: MutableList<VisualCollisionBurst>): Long
    fun submitMalletTarget(slot: PlayerSlot, targetX: Float, targetY: Float)
    fun startPresentationCompleted()
    fun requestPause()
    fun requestResume()
    fun requestRestart()
    fun toggleSound()
    fun toggleMusic()
    fun exitMatch()
}

class GameViewModel : ViewModel(), ArenaRuntime {

    private val audioEngine = AirHockeyApp.instance.audioEngine

    override val soundEnabled: StateFlow<Boolean> = AirHockeyApp.instance.preferencesRepository.userPreferencesFlow
        .map { it.soundEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    override val musicEnabled: StateFlow<Boolean> = AirHockeyApp.instance.preferencesRepository.userPreferencesFlow
        .map { it.musicEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _matchState = MutableStateFlow(MatchState())
    override val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val fallbackDimensions = TableDimensions()
    private val fallbackSnapshot = MatchFrameSnapshot(
        puckX = fallbackDimensions.width / 2f,
        puckY = fallbackDimensions.centerLineY,
        puckVx = 0f,
        puckVy = 0f,
        player1MalletX = fallbackDimensions.width / 2f,
        player1MalletY = fallbackDimensions.totalHeight - 250f,
        player2MalletX = fallbackDimensions.width / 2f,
        player2MalletY = 250f,
        dimensions = fallbackDimensions
    )

    private var arbitro: ArbitroVirtual? = null
    private var coordinator: MultiplayerMatchCoordinator? = null
    private val coordinatorJobs = mutableListOf<Job>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val simulationDispatcher = Dispatchers.Default.limitedParallelism(1)
    private var physicsJob: Job? = null
    private var matchStateCollectJob: Job? = null

    @Volatile
    private var hasRecordedMatch = false

    private val outcomeRecorder = MatchOutcomeRecorder(
        leaderboardRepository = AirHockeyApp.instance.leaderboardRepository,
        stageRepository = AirHockeyApp.instance.stageRepository,
        playGamesPort = { leaderboardId, score ->
            AirHockeyApp.instance.playGamesManager.submitScore(leaderboardId, score)
        },
        eloLeaderboardId = AirHockeyApp.instance.getString(R.string.leaderboard_elo_id),
        stageLeaderboardId = AirHockeyApp.instance.getString(R.string.leaderboard_stages_id)
    )

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

    override fun currentSnapshot(): MatchFrameSnapshot = arbitro?.currentSnapshot() ?: fallbackSnapshot

    override fun collectVisualBurstsSince(
        lastSeq: Long,
        out: MutableList<VisualCollisionBurst>
    ): Long = arbitro?.collectVisualBurstsSince(lastSeq, out) ?: lastSeq

    fun startMatch(spec: MatchSpec, coordinator: MultiplayerMatchCoordinator? = null) {
        val capabilities = spec.mode.capabilities
        val isExtended = capabilities.isExtended
        val isGuest = capabilities.isGuest
        val bezelUnits = if (isExtended) {
            TableDimensions.calculateBezelUnits(spec.bezelGapMm, spec.screenHeightPx, spec.yDpi)
        } else {
            0f
        }

        val dimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = bezelUnits,
            isExtendedMode = isExtended
        )

        val resolvedSpec = when {
            spec.mode == GameMode.SOLO -> {
                val stageConfig = AirHockeyApp.instance.stageRepository.getStageConfig(spec.stageNumber)
                spec.copy(
                    targetScore = stageConfig.targetScore,
                    aiDifficulty = stageConfig.aiDifficulty,
                    puckMaxSpeed = stageConfig.puckSpeed.maxSpeed,
                    airFriction = stageConfig.puckSpeed.airFriction,
                    tableTheme = stageConfig.tableTheme,
                    customAiMaxSpeed = stageConfig.aiMaxSpeed,
                    customAiReactionDelay = stageConfig.aiReactionDelay,
                    customAiAimingErrorRange = stageConfig.aiAimingError,
                    customAiBankShotChance = stageConfig.aiBankShotChance
                )
            }
            isExtended -> spec.copy(hasIntroPresentation = false)
            else -> spec
        }

        stopGameLoops()
        clearCoordinator()
        hasRecordedMatch = false

        val newArbitro = ArbitroVirtual(
            dimensions = dimensions,
            spec = resolvedSpec,
            audioPort = audioPort,
            onGoalScoredCallback = { _, isGameOver ->
                if (isGameOver) {
                    handleGameOver()
                    broadcastState()
                    stopGameLoops()
                } else {
                    broadcastState()
                }
            }
        )
        arbitro = newArbitro
        _matchState.value = newArbitro.matchState.value

        if (coordinator != null) {
            bindCoordinator(coordinator)
        }

        matchStateCollectJob?.cancel()
        var prevCountdown = -1
        var prevStatus = MatchStatus.IDLE
        matchStateCollectJob = viewModelScope.launch {
            newArbitro.matchState.collect { s ->
                val stateChanged = s.countdownNumber != prevCountdown || s.status != prevStatus
                prevCountdown = s.countdownNumber
                prevStatus = s.status
                _matchState.value = s
                if (stateChanged && !isGuest) {
                    broadcastState()
                }
            }
        }

        if (!isGuest) {
            broadcastState()
            if (!resolvedSpec.hasIntroPresentation) {
                startCountdown()
            }
        }
    }

    private fun clearCoordinator() {
        coordinatorJobs.forEach { it.cancel() }
        coordinatorJobs.clear()
        coordinator = null
    }

    private fun bindCoordinator(coord: MultiplayerMatchCoordinator) {
        coordinator = coord
        if (coord.isHost) {
            coordinatorJobs += viewModelScope.launch {
                coord.transport.guestInputs.collect { input ->
                    arbitro?.onHostReceiveGuestInput(input.guestTargetX, input.guestTargetY)
                }
            }
            coord.onPeerPauseRequested = { requestPause() }
            coord.onPeerResumeRequested = { requestResume() }
            coord.onPeerRestartRequested = { requestRestart() }
            coordinatorJobs += viewModelScope.launch {
                coord.state.collect { state ->
                    if (state.peerExited) onPeerExited()
                }
            }
        } else {
            coordinatorJobs += viewModelScope.launch {
                coord.transport.telemetryFrames.collect { t ->
                    arbitro?.applyRemoteTelemetry(
                        puckX = t.puckX,
                        puckY = t.puckY,
                        puckVx = t.puckVx,
                        puckVy = t.puckVy,
                        hostMalletX = t.hostMalletX,
                        hostMalletY = t.hostMalletY,
                        guestMalletX = t.guestMalletX,
                        guestMalletY = t.guestMalletY,
                        collisionEvent = t.collisionEvent,
                        collisionIntensity = t.collisionIntensity,
                        puckLastHitBy = t.puckLastHitBy
                    )
                }
            }
            coordinatorJobs += viewModelScope.launch {
                coord.state.collect { state ->
                    state.remoteMatchState?.let { remote ->
                        arbitro?.applyRemoteState(remote)
                        if (remote.statusName == MatchStatus.GAME_OVER.name) {
                            handleGameOver()
                        }
                    }
                    if (state.peerExited) onPeerExited()
                }
            }
        }
    }

    override fun startPresentationCompleted() {
        if (_matchState.value.status == MatchStatus.PAUSED) return
        if (_matchState.value.status == MatchStatus.PRESENTATION) {
            arbitro?.finishPresentationAndStartCountdown()
            broadcastState()
            startGameLoops()
        } else if (_matchState.value.status == MatchStatus.COUNTDOWN && physicsJob?.isActive != true) {
            startGameLoops()
        }
    }

    private fun startCountdown() {
        if (_matchState.value.status == MatchStatus.PAUSED) return
        arbitro?.startCountdown()
        broadcastState()
        startGameLoops()
    }

    private fun startGameLoops() {
        stopGameLoops()
        val currentArbitro = arbitro ?: return

        // Loop unificado de simulação física e regras esportivas em ritmo real (passo fixo 120 Hz) fora da thread de UI
        physicsJob = viewModelScope.launch(simulationDispatcher) {
            var lastTimeNanos = System.nanoTime()
            val frameDelayMillis = 8L
            while (isActive && currentArbitro.matchState.value.status in ACTIVE_LOOP_STATUSES) {
                val loopStartNanos = System.nanoTime()
                val elapsedSec = ((loopStartNanos - lastTimeNanos) / 1_000_000_000f).coerceAtLeast(0f)
                lastTimeNanos = loopStartNanos

                val stepsExecuted = currentArbitro.advanceRealTime(elapsedSec)
                if (stepsExecuted > 0) {
                    emitLoopTelemetry(currentArbitro)
                }

                val workMillis = (System.nanoTime() - loopStartNanos) / 1_000_000L
                val sleepMillis = (frameDelayMillis - workMillis).coerceAtLeast(1L)
                delay(sleepMillis)
            }
        }
    }

    private fun emitLoopTelemetry(currentArbitro: ArbitroVirtual) {
        val coord = coordinator ?: return
        val status = currentArbitro.matchState.value.status
        val snap = currentArbitro.currentSnapshot()
        if (status == MatchStatus.PLAYING) {
            if (currentArbitro.isHost) {
                val (evt, intensity) = currentArbitro.consumeCollisionTelemetry()
                coord.broadcastTelemetry(
                    snap.puckX, snap.puckY,
                    snap.puckVx, snap.puckVy,
                    snap.player1MalletX, snap.player1MalletY,
                    snap.player2MalletX, snap.player2MalletY,
                    collisionEvent = evt,
                    collisionIntensity = intensity,
                    puckLastHitBy = snap.puckLastHitBy.toByte()
                )
            } else if (currentArbitro.aiControllerP2 != null) {
                val (tx, ty) = currentArbitro.getPlayer2Target()
                coord.sendGuestInput(tx, ty)
            }
        } else if (status == MatchStatus.COUNTDOWN && currentArbitro.isHost) {
            coord.broadcastTelemetry(
                snap.puckX, snap.puckY,
                0f, 0f,
                snap.player1MalletX, snap.player1MalletY,
                snap.player2MalletX, snap.player2MalletY,
                collisionEvent = 0.toByte(),
                collisionIntensity = 0f,
                puckLastHitBy = snap.puckLastHitBy.toByte()
            )
        }
    }

    companion object {
        private val ACTIVE_LOOP_STATUSES = setOf(
            MatchStatus.COUNTDOWN,
            MatchStatus.PLAYING,
            MatchStatus.GOAL_PAUSE
        )
    }

    private fun stopGameLoops() {
        physicsJob?.cancel()
        physicsJob = null
    }

    override fun submitMalletTarget(slot: PlayerSlot, targetX: Float, targetY: Float) {
        when (slot) {
            PlayerSlot.P1 -> arbitro?.onTouchPlayer1(targetX, targetY)
            PlayerSlot.P2 -> arbitro?.onTouchPlayer2(targetX, targetY)
            PlayerSlot.GUEST -> {
                val coords = arbitro?.onTouchGuestGlobal(targetX, targetY)
                if (coords != null) {
                    coordinator?.sendGuestInput(coords.first, coords.second)
                }
            }
        }
    }

    override fun toggleSound() {
        val repo = AirHockeyApp.instance.preferencesRepository
        viewModelScope.launch {
            repo.setSoundEnabled(!soundEnabled.value)
        }
    }

    override fun toggleMusic() {
        val repo = AirHockeyApp.instance.preferencesRepository
        viewModelScope.launch {
            repo.setMusicEnabled(!musicEnabled.value)
        }
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
            coordinator?.broadcastState(remote)
        }
    }

    override fun requestPause() {
        stopGameLoops()
        audioEngine.stopAll()

        if (_matchState.value.capabilities.isGuest) {
            coordinator?.requestPause()
            arbitro?.pause()
            return
        }
        val currentStatus = _matchState.value.status
        if (currentStatus == MatchStatus.PLAYING || currentStatus == MatchStatus.COUNTDOWN || currentStatus == MatchStatus.GOAL_PAUSE || currentStatus == MatchStatus.PRESENTATION) {
            arbitro?.pause()
            broadcastState()
            stopGameLoops()
        }
    }

    override fun requestResume() {
        if (_matchState.value.capabilities.isGuest) {
            coordinator?.requestResume()
            return
        }
        if (_matchState.value.status == MatchStatus.PAUSED) {
            arbitro?.resume()
            startCountdown()
        }
    }

    override fun requestRestart() {
        if (_matchState.value.capabilities.isGuest) {
            coordinator?.requestRestart()
            return
        }
        stopGameLoops()
        hasRecordedMatch = false
        arbitro?.restart()
        broadcastState()
        if (_matchState.value.status != MatchStatus.PRESENTATION) {
            startCountdown()
        }
    }

    private fun onPeerExited() {
        stopGameLoops()
        arbitro?.setPeerExited()
    }

    override fun exitMatch() {
        stopGameLoops()
        audioEngine.stopAll()
        clearCoordinator()
        arbitro?.setIdle()
    }

    private fun handleGameOver() {
        if (hasRecordedMatch) return
        hasRecordedMatch = true
        val state = _matchState.value
        val diff = if (state.mode == GameMode.SOLO) arbitro?.aiControllerP2?.difficulty?.name else null

        viewModelScope.launch {
            val result = outcomeRecorder.recordOutcome(state, diff)
            if (result.starsEarned > 0) {
                arbitro?.updateStarsEarned(result.starsEarned, result.newlyConqueredTheme)
            }
            AirHockeyApp.instance.adManager.recordMatchFinished()
        }
    }

    override fun onCleared() {
        super.onCleared()
        exitMatch()
    }
}
