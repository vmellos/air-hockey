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

    private val _matchState = MutableStateFlow(MatchState())
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private var dimensions = TableDimensions()
    lateinit var engine: AirHockeyPhysicsEngine
        private set

    private var aiController: AirHockeyAiController? = null
    private var physicsJob: Job? = null
    private var countdownPhysicsJob: Job? = null
    private var timerJob: Job? = null
    private var countdownJob: Job? = null

    // Callbacks de rede para Mesa Estendida
    var coordinator: MultiplayerMatchCoordinator? = null
        private set

    var onHostBroadcastTelemetry: ((Float, Float, Float, Float, Float, Float, Float, Float) -> Unit)? = null
    var onHostBroadcastState: ((ControlMessages.RemoteMatchState) -> Unit)? = null
    var onGuestSendInput: ((Float, Float) -> Unit)? = null
    var onGuestSendControl: ((String) -> Unit)? = null

    fun attachCoordinator(coord: MultiplayerMatchCoordinator) {
        coordinator = coord
        val isHost = coord.isHost

        if (isHost) {
            onHostBroadcastTelemetry = { px, py, pvx, pvy, hmx, hmy, gmx, gmy ->
                coord.broadcastTelemetry(px, py, pvx, pvy, hmx, hmy, gmx, gmy)
            }
            onHostBroadcastState = { remote ->
                coord.broadcastState(remote)
            }
            viewModelScope.launch {
                coord.transport.guestInputs.collect { input ->
                    onHostReceiveGuestInput(input.guestTargetX, input.guestTargetY)
                }
            }
            coord.onPeerPauseRequested = { pauseMatch() }
            coord.onPeerResumeRequested = { resumeMatch() }
            coord.onPeerRestartRequested = { restartMatch() }
            viewModelScope.launch {
                coord.state.collect { state ->
                    if (state.peerExited) onPeerExited()
                }
            }
        } else {
            viewModelScope.launch {
                coord.transport.telemetryFrames.collect { t ->
                    onGuestReceiveTelemetry(
                        t.puckX, t.puckY,
                        t.puckVx, t.puckVy,
                        t.hostMalletX, t.hostMalletY,
                        t.guestMalletX, t.guestMalletY
                    )
                }
            }
            viewModelScope.launch {
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
        aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM
    ) {
        val isExtended = mode == GameMode.EXTENDED_HOST || mode == GameMode.EXTENDED_GUEST
        val isGuest = mode == GameMode.EXTENDED_GUEST || mode == GameMode.CONNECTED_GUEST
        val bezelUnits = if (isExtended) TableDimensions.calculateBezelUnits(bezelGapMm, screenHeightPx, yDpi) else 0f

        dimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 2000f,
            bezelGapUnits = bezelUnits,
            isExtendedMode = isExtended
        )

        engine = AirHockeyPhysicsEngine(dimensions)
        engine.resetPositions()

        // Limpa estritamente o controlador de IA se não for modo SOLO
        aiController = if (mode == GameMode.SOLO) {
            AirHockeyAiController(aiDifficulty)
        } else {
            null
        }

        _matchState.value = MatchState(
            mode = mode,
            status = MatchStatus.COUNTDOWN,
            scorePlayer1 = 0,
            scorePlayer2 = 0,
            targetScore = targetScore,
            countdownNumber = 3,
            remainingTimeSeconds = matchTimeSeconds,
            isHost = !isGuest,
            peerExited = false
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
                CollisionEvent.PADDLE_HIT_P1, CollisionEvent.PADDLE_HIT_P2 -> {
                    audioEngine.playPaddleHit(intensity)
                }
                CollisionEvent.WALL_HIT -> {
                    audioEngine.playWallHit()
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
                        engine.player2Mallet.x, engine.player2Mallet.y
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
            audioEngine.playCountdownBeep(isFinal = false)
            delay(900)

            _matchState.update { it.copy(countdownNumber = 2) }
            broadcastState()
            audioEngine.playCountdownBeep(isFinal = false)
            delay(900)

            _matchState.update { it.copy(countdownNumber = 1) }
            broadcastState()
            audioEngine.playCountdownBeep(isFinal = false)
            delay(900)

            countdownPhysicsJob?.cancel()
            countdownPhysicsJob = null

            _matchState.update { it.copy(countdownNumber = 0, status = MatchStatus.PLAYING) }
            broadcastState()
            audioEngine.playCountdownBeep(isFinal = true)

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
                    aiController?.update(engine, dt)
                    engine.tick(dt)

                    // Emite telemetria completa (incluindo posição conciliada do convidado)
                    onHostBroadcastTelemetry?.invoke(
                        engine.puck.x, engine.puck.y,
                        engine.puck.vx, engine.puck.vy,
                        engine.player1Mallet.x, engine.player1Mallet.y,
                        engine.player2Mallet.x, engine.player2Mallet.y
                    )
                }

                val elapsed = System.currentTimeMillis() - start
                val sleep = (delayMillis - elapsed).coerceAtLeast(1L)
                delay(sleep)
            }
        }

        // Loop do cronômetro de partida (apenas no Anfitrião/Solo/Split)
        if (_matchState.value.isHost) {
            timerJob = viewModelScope.launch {
                while (isActive && _matchState.value.status == MatchStatus.PLAYING) {
                    delay(1000)
                    _matchState.update { state ->
                        val nextTime = state.remainingTimeSeconds - 1
                        if (nextTime <= 0) {
                            val winner = if (state.scorePlayer1 > state.scorePlayer2) 1
                            else if (state.scorePlayer2 > state.scorePlayer1) 2 else 0
                            state.copy(remainingTimeSeconds = 0, winner = winner, status = MatchStatus.GAME_OVER)
                        } else {
                            state.copy(remainingTimeSeconds = nextTime)
                        }
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
        return s == MatchStatus.PLAYING || s == MatchStatus.COUNTDOWN
    }

    fun onTouchPlayer1(x: Float, y: Float) {
        if (!isInputAllowed()) return
        engine.player1Mallet.targetX = x
        engine.player1Mallet.targetY = y
    }

    fun onTouchPlayer2(x: Float, y: Float) {
        if (!isInputAllowed()) return
        engine.player2Mallet.targetX = x
        engine.player2Mallet.targetY = y
    }

    fun onTouchGuestGlobal(globalX: Float, globalY: Float) {
        if (!isInputAllowed()) return
        onGuestSendInput?.invoke(globalX, globalY)
    }

    fun onGuestTouchLocal(localX: Float, localY: Float) {
        if (!isInputAllowed()) return
        // Transforma toque do convidado (rotacionado 180 graus na Disposição Topo-a-Topo) para o espaço global
        val globalX = 1000f - localX
        val globalY = 1000f - localY
        onGuestSendInput?.invoke(globalX, globalY)
    }

    fun toggleSound() {
        val repo = AirHockeyApp.instance.preferencesRepository
        viewModelScope.launch {
            repo.setSoundEnabled(!soundEnabled.value)
        }
    }

    fun onGuestReceiveTelemetry(
        puckX: Float, puckY: Float,
        puckVx: Float, puckVy: Float,
        hostMalletX: Float, hostMalletY: Float,
        guestMalletX: Float, guestMalletY: Float
    ) {
        engine.puck.x = puckX
        engine.puck.y = puckY
        engine.puck.vx = puckVx
        engine.puck.vy = puckVy
        engine.player1Mallet.x = hostMalletX
        engine.player1Mallet.y = hostMalletY
        engine.player2Mallet.x = guestMalletX
        engine.player2Mallet.y = guestMalletY
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

        // Reprodução de áudio sincronizada no convidado
        if (newStatus == MatchStatus.COUNTDOWN && remote.countdownNumber != prev.countdownNumber) {
            if (remote.countdownNumber > 0) {
                audioEngine.playCountdownBeep(isFinal = false)
            } else {
                audioEngine.playCountdownBeep(isFinal = true)
            }
        } else if (newStatus == MatchStatus.PLAYING && prev.status == MatchStatus.COUNTDOWN) {
            audioEngine.playCountdownBeep(isFinal = true)
        } else if (newStatus == MatchStatus.GOAL_PAUSE && prev.status != MatchStatus.GOAL_PAUSE) {
            audioEngine.playGoal()
        }
    }

    fun onHostReceiveGuestInput(globalTargetX: Float, globalTargetY: Float) {
        engine.player2Mallet.targetX = globalTargetX
        engine.player2Mallet.targetY = globalTargetY
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
        if (_matchState.value.status == MatchStatus.PLAYING) {
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
            _matchState.update { it.copy(status = MatchStatus.PLAYING) }
            broadcastState()
            startGameLoops()
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
        engine.resetPositions(0)
        _matchState.update {
            it.copy(
                status = MatchStatus.COUNTDOWN,
                scorePlayer1 = 0,
                scorePlayer2 = 0,
                winner = 0,
                countdownNumber = 3,
                remainingTimeSeconds = 180
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
        _matchState.update { it.copy(status = MatchStatus.IDLE) }
    }

    override fun onCleared() {
        super.onCleared()
        stopMatch()
    }
}
