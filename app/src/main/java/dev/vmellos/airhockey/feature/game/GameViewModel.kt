package dev.vmellos.airhockey.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.core.preferences.TableAlignment
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.ai.AirHockeyAiController
import dev.vmellos.airhockey.feature.physics.AirHockeyPhysicsEngine
import dev.vmellos.airhockey.feature.physics.CollisionEvent
import dev.vmellos.airhockey.feature.physics.TableDimensions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val audioEngine = AirHockeyApp.instance.audioEngine
    private val preferencesRepo = AirHockeyApp.instance.preferencesRepository

    private val _matchState = MutableStateFlow(MatchState())
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private var dimensions = TableDimensions()
    lateinit var engine: AirHockeyPhysicsEngine
        private set

    private var aiController: AirHockeyAiController? = null
    private var physicsJob: Job? = null
    private var timerJob: Job? = null
    private var countdownJob: Job? = null

    // Callbacks de rede opcionais para modo Mesa Estendida
    var onHostBroadcastTelemetry: ((Float, Float, Float, Float, Float, Float) -> Unit)? = null
    var onGuestSendInput: ((Float, Float) -> Unit)? = null
    var onHostGoalScored: ((Int, Int, Int) -> Unit)? = null

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
        val bezelUnits = if (isExtended) TableDimensions.calculateBezelUnits(bezelGapMm, screenHeightPx, yDpi) else 0f

        dimensions = TableDimensions(
            width = 1000f,
            viewportHeight = 1000f,
            bezelGapUnits = bezelUnits,
            isExtendedMode = isExtended
        )

        engine = AirHockeyPhysicsEngine(dimensions)

        if (mode == GameMode.SOLO) {
            aiController = AirHockeyAiController(aiDifficulty)
        }

        _matchState.update {
            it.copy(
                mode = mode,
                status = MatchStatus.COUNTDOWN,
                scorePlayer1 = 0,
                scorePlayer2 = 0,
                targetScore = targetScore,
                remainingTimeSeconds = matchTimeSeconds,
                isHost = (mode != GameMode.EXTENDED_GUEST)
            )
        }

        setupCollisionListener()
        startCountdown()
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

        onHostGoalScored?.invoke(scorer, p1, p2)

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

            viewModelScope.launch {
                delay(1200)
                engine.resetPositions()
                startCountdown()
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _matchState.update { it.copy(status = MatchStatus.COUNTDOWN, countdownNumber = 3) }
            audioEngine.playCountdownBeep(isFinal = false)
            delay(900)

            _matchState.update { it.copy(countdownNumber = 2) }
            audioEngine.playCountdownBeep(isFinal = false)
            delay(900)

            _matchState.update { it.copy(countdownNumber = 1) }
            audioEngine.playCountdownBeep(isFinal = false)
            delay(900)

            audioEngine.playCountdownBeep(isFinal = true)
            _matchState.update { it.copy(status = MatchStatus.PLAYING) }

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

                    // Emite telemetria para o convidado
                    onHostBroadcastTelemetry?.invoke(
                        engine.puck.x, engine.puck.y,
                        engine.puck.vx, engine.puck.vy,
                        engine.player1Mallet.x, engine.player1Mallet.y
                    )
                }

                val elapsed = System.currentTimeMillis() - start
                val sleep = (delayMillis - elapsed).coerceAtLeast(1L)
                delay(sleep)
            }
        }

        // Loop do cronômetro de partida
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
            }
        }
    }

    private fun stopGameLoops() {
        physicsJob?.cancel()
        physicsJob = null
        timerJob?.cancel()
        timerJob = null
    }

    fun onTouchPlayer1(x: Float, y: Float) {
        if (_matchState.value.status != MatchStatus.PLAYING) return
        engine.player1Mallet.targetX = x
        engine.player1Mallet.targetY = y
    }

    fun onTouchPlayer2(x: Float, y: Float) {
        if (_matchState.value.status != MatchStatus.PLAYING) return
        engine.player2Mallet.targetX = x
        engine.player2Mallet.targetY = y
    }

    fun onGuestTouchLocal(localX: Float, localY: Float) {
        if (_matchState.value.status != MatchStatus.PLAYING) return
        // Transforma toque do convidado (rotacionado 180 graus) para o espaço global
        val globalX = 1000f - localX
        val globalY = dimensions.totalHeight - localY
        onGuestSendInput?.invoke(globalX, globalY)
    }

    fun onGuestReceiveTelemetry(
        puckX: Float, puckY: Float,
        puckVx: Float, puckVy: Float,
        hostMalletX: Float, hostMalletY: Float
    ) {
        engine.puck.x = puckX
        engine.puck.y = puckY
        engine.puck.vx = puckVx
        engine.puck.vy = puckVy
        engine.player1Mallet.x = hostMalletX
        engine.player1Mallet.y = hostMalletY
    }

    fun onHostReceiveGuestInput(globalTargetX: Float, globalTargetY: Float) {
        engine.player2Mallet.targetX = globalTargetX
        engine.player2Mallet.targetY = globalTargetY
    }

    fun togglePause() {
        if (_matchState.value.status == MatchStatus.PLAYING) {
            _matchState.update { it.copy(status = MatchStatus.PAUSED) }
            stopGameLoops()
        } else if (_matchState.value.status == MatchStatus.PAUSED) {
            _matchState.update { it.copy(status = MatchStatus.PLAYING) }
            startGameLoops()
        }
    }

    fun restartMatch() {
        stopGameLoops()
        countdownJob?.cancel()
        engine.resetPositions()
        _matchState.update {
            it.copy(
                status = MatchStatus.COUNTDOWN,
                scorePlayer1 = 0,
                scorePlayer2 = 0,
                winner = 0,
                remainingTimeSeconds = 180
            )
        }
        startCountdown()
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoops()
        countdownJob?.cancel()
    }
}
