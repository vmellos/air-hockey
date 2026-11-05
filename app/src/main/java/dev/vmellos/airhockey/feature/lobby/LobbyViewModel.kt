package dev.vmellos.airhockey.feature.lobby

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.MatchSpec
import dev.vmellos.airhockey.feature.network.ControlMessages
import dev.vmellos.airhockey.feature.network.DiscoveredTable
import dev.vmellos.airhockey.feature.network.HybridSocketTransportAdapter
import dev.vmellos.airhockey.feature.network.NetworkConstants
import dev.vmellos.airhockey.feature.network.NsdDiscoveryManager
import dev.vmellos.airhockey.feature.network.TcpControlClient
import dev.vmellos.airhockey.feature.network.TcpControlServer
import dev.vmellos.airhockey.feature.network.TransportConnectionState
import dev.vmellos.airhockey.feature.network.UdpTelemetrySocket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class LobbyRole {
    NONE,
    HOST,
    GUEST
}

enum class ConnectionStatus {
    IDLE,
    BROADCASTING,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

data class LobbyState(
    val role: LobbyRole = LobbyRole.NONE,
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    val targetMode: String = "EXTENDED",
    val tableCode: String = "",
    val hostIp: String = "",
    val qrContent: String = "",
    val guestIp: String = "",
    val discoveredTables: List<DiscoveredTable> = emptyList(),
    val isReadyToPlay: Boolean = false,
    val isScanningQr: Boolean = false,
    val wifiSsid: String = "",
    val isWifiConnected: Boolean = true,
    val startMatchTrigger: Long = 0L,
    val calibratedGapMm: Float = 8.0f,
    val synchronizedPuckSpeed: Float = 6800f,
    val aiDifficulty1: String = "",
    val aiDifficulty2: String = "",
    val errorMessage: String? = null,
    val errorMessageRes: Int? = null,
    val errorMessageArgs: List<String> = emptyList(),
    val peerPlayerName: String = "",
    val selfPlayerName: String = ""
)

class LobbyViewModel : ViewModel() {

    private val context = AirHockeyApp.instance.applicationContext
    val nsdManager = NsdDiscoveryManager(context)

    private val _state = MutableStateFlow(LobbyState())
    val state: StateFlow<LobbyState> = _state.asStateFlow()

    var coordinator: MultiplayerMatchCoordinator? = null
        private set
    private var coordinatorJob: Job? = null
    private var discoveryPeriodicJob: Job? = null

    var onRemoteStateReceived: ((ControlMessages.RemoteMatchState) -> Unit)? = null
    var onControlMessageReceived: ((String) -> Unit)? = null

    init {
        viewModelScope.launch {
            AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collect { prefs ->
                _state.update { it.copy(selfPlayerName = prefs.playerName) }
            }
        }

        viewModelScope.launch {
            nsdManager.discoveredTables.collect { tables ->
                _state.update { it.copy(discoveredTables = tables) }
            }
        }
    }

    fun setTargetMode(mode: String) {
        _state.update { it.copy(targetMode = mode, discoveredTables = nsdManager.discoveredTables.value) }
    }

    @Suppress("DEPRECATION")
    private fun getNetworkSsid(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val info = wifiManager?.connectionInfo
            val rawSsid = info?.ssid ?: ""
            val clean = rawSsid.removeSurrounding("\"")
            if (clean == "<unknown ssid>" || clean.isBlank()) "" else clean
        } catch (_: Exception) {
            ""
        }
    }

    private fun checkIsWifiConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNet = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return false
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) {
            true
        }
    }

    fun startAsHost(mode: String = _state.value.targetMode, aiDiff1: String = "", aiDiff2: String = "") {
        val code = String.format("%04d", Random.nextInt(1000, 9999))
        val ip = NsdDiscoveryManager.getLocalIpAddress(context)
        val selfName = _state.value.selfPlayerName.ifBlank {
            dev.vmellos.airhockey.core.preferences.UserPreferencesRepository.getDefaultDevicePlayerName(context)
        }
        val encodedName = try { java.net.URLEncoder.encode(selfName, "UTF-8") } catch (_: Exception) { "" }
        val qr = "airhockey://connect?ip=$ip&port=8888&code=$code&mode=$mode&hostName=$encodedName"
        val ssid = getNetworkSsid()
        val wifiConnected = checkIsWifiConnected()

        _state.update {
            it.copy(
                role = LobbyRole.HOST,
                connectionStatus = ConnectionStatus.BROADCASTING,
                targetMode = mode,
                aiDifficulty1 = aiDiff1,
                aiDifficulty2 = aiDiff2,
                tableCode = code,
                hostIp = ip,
                qrContent = qr,
                wifiSsid = ssid,
                isWifiConnected = wifiConnected,
                errorMessage = null,
                errorMessageRes = null,
                errorMessageArgs = emptyList(),
                startMatchTrigger = 0L,
                peerPlayerName = ""
            )
        }

        val transport = HybridSocketTransportAdapter(isHost = true)
        val coord = MultiplayerMatchCoordinator(
            transport = transport,
            isHost = true,
            expectedTableCode = code,
            selfPlayerName = selfName,
            selfMode = mode,
            initialAiDiff1 = aiDiff1,
            initialAiDiff2 = aiDiff2
        )
        coordinator = coord
        bindCoordinator(coord)
        nsdManager.startBroadcast(code, port = NetworkConstants.DEFAULT_TCP_PORT, mode = mode, hostName = selfName)
    }

    fun startAsGuest(mode: String = _state.value.targetMode) {
        _state.update {
            it.copy(
                role = LobbyRole.GUEST,
                connectionStatus = ConnectionStatus.DISCOVERING,
                targetMode = mode,
                errorMessage = null,
                errorMessageRes = null,
                errorMessageArgs = emptyList(),
                startMatchTrigger = 0L,
                peerPlayerName = ""
            )
        }
        nsdManager.startDiscovery()
        discoveryPeriodicJob?.cancel()
        discoveryPeriodicJob = viewModelScope.launch {
            while (isActive && _state.value.role == LobbyRole.GUEST && _state.value.connectionStatus == ConnectionStatus.DISCOVERING) {
                delay(2500)
                nsdManager.sendDirectedDiscoveryProbe()
            }
        }
    }

    fun refreshDiscovery() {
        nsdManager.sendDirectedDiscoveryProbe()
    }

    fun startHostMatch(gapMm: Float, maxSpeed: Float = 6800f, aiDiff1: String = "", aiDiff2: String = "") {
        val d1 = if (aiDiff1.isNotBlank()) aiDiff1 else _state.value.aiDifficulty1
        val d2 = if (aiDiff2.isNotBlank()) aiDiff2 else _state.value.aiDifficulty2
        coordinator?.startMatch(gapMm, maxSpeed, d1, d2)
    }

    fun resetToRoleSelection() {
        stopAll()
    }

    fun sendControlMessage(message: String) {
        if (message == ControlMessages.EXIT) {
            coordinator?.terminateSession()
        }
    }

    fun connectToTable(hostIp: String, code: String = "", tableMode: String = "") {
        val selfName = _state.value.selfPlayerName.ifBlank {
            dev.vmellos.airhockey.core.preferences.UserPreferencesRepository.getDefaultDevicePlayerName(context)
        }
        val mode = if (tableMode.isNotBlank()) tableMode else _state.value.targetMode
        _state.update {
            it.copy(
                targetMode = mode,
                hostIp = hostIp,
                tableCode = code,
                connectionStatus = ConnectionStatus.CONNECTING,
                errorMessage = null,
                errorMessageRes = null,
                errorMessageArgs = emptyList(),
                startMatchTrigger = 0L,
                peerPlayerName = ""
            )
        }
        discoveryPeriodicJob?.cancel()
        discoveryPeriodicJob = null
        nsdManager.stopDiscovery()
        val transport = HybridSocketTransportAdapter(isHost = false, targetHostIp = hostIp)
        val coord = MultiplayerMatchCoordinator(
            transport = transport,
            isHost = false,
            expectedTableCode = code,
            selfPlayerName = selfName,
            selfMode = mode
        )
        coordinator = coord
        bindCoordinator(coord)
    }

    private fun bindCoordinator(coord: MultiplayerMatchCoordinator) {
        coordinatorJob?.cancel()
        coordinatorJob = viewModelScope.launch {
            coord.state.collect { cState ->
                _state.update {
                    it.copy(
                        targetMode = if (cState.mode.isNotBlank()) cState.mode else it.targetMode,
                        aiDifficulty1 = if (cState.aiDifficulty1.isNotBlank()) cState.aiDifficulty1 else it.aiDifficulty1,
                        aiDifficulty2 = if (cState.aiDifficulty2.isNotBlank()) cState.aiDifficulty2 else it.aiDifficulty2,
                        startMatchTrigger = cState.startMatchTrigger,
                        calibratedGapMm = cState.calibratedGapMm,
                        synchronizedPuckSpeed = cState.synchronizedPuckSpeed,
                        peerPlayerName = cState.peerPlayerName.ifBlank { it.peerPlayerName },
                        connectionStatus = when (cState.connectionState) {
                            TransportConnectionState.CONNECTED -> ConnectionStatus.CONNECTED
                            TransportConnectionState.CONNECTING -> ConnectionStatus.CONNECTING
                            TransportConnectionState.DISCONNECTED -> ConnectionStatus.DISCONNECTED
                            TransportConnectionState.IDLE -> it.connectionStatus
                        },
                        isReadyToPlay = (cState.connectionState == TransportConnectionState.CONNECTED && !cState.isHandshakeRejected),
                        errorMessage = null,
                        errorMessageRes = when {
                            cState.isHandshakeRejected -> {
                                if (cState.rejectReason == "INVALID_CODE") {
                                    dev.vmellos.airhockey.R.string.lobby_error_invalid_code
                                } else {
                                    dev.vmellos.airhockey.R.string.lobby_error_incompatible_mode
                                }
                            }
                            cState.connectionState == TransportConnectionState.DISCONNECTED && it.errorMessageRes == null && it.errorMessage == null -> {
                                dev.vmellos.airhockey.R.string.lobby_error_connection_lost
                            }
                            else -> it.errorMessageRes
                        },
                        errorMessageArgs = when {
                            cState.isHandshakeRejected || (cState.connectionState == TransportConnectionState.DISCONNECTED && it.errorMessageRes == null) -> emptyList()
                            else -> it.errorMessageArgs
                        }
                    )
                }
            }
        }
    }

    fun connectByCode(code: String) {
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) return

        // 1. Entrada direta de IP (ex: 192.168.0.180 ou 10.0.2.2)
        if (cleanCode.contains(".") && !cleanCode.startsWith("AIRHOCKEY")) {
            val ip = cleanCode.substringBefore(":")
            connectToTable(ip, "")
            return
        }

        // 2. Busca entre as mesas já descobertas via NSD / UDP
        val table = _state.value.discoveredTables.find { it.tableCode == cleanCode }
        if (table != null) {
            connectToTable(table.hostIp, cleanCode, table.mode)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    connectionStatus = ConnectionStatus.CONNECTING,
                    errorMessage = null,
                    errorMessageRes = null,
                    errorMessageArgs = emptyList()
                )
            }

            // 3. Emite sondagem ativa dirigida
            nsdManager.sendDirectedDiscoveryProbe()

            // 4. Tenta o Gateway DHCP (essencial quando conectado ao Hotspot do anfitrião)
            val gatewayIp = NsdDiscoveryManager.getDhcpGatewayIp(context)
            if (!gatewayIp.isNullOrBlank() && probeHost(gatewayIp, NetworkConstants.DEFAULT_TCP_PORT)) {
                withContext(Dispatchers.Main) {
                    connectToTable(gatewayIp, cleanCode)
                }
                return@launch
            }

            // 5. Varredura rápida concorrente na sub-rede /24
            val localIp = NsdDiscoveryManager.getLocalIpAddress(context)
            if (localIp.contains(".")) {
                val prefix = localIp.substringBeforeLast(".") + "."
                val foundIp = scanSubnetForHost(prefix, NetworkConstants.DEFAULT_TCP_PORT, localIp)
                if (foundIp != null) {
                    withContext(Dispatchers.Main) {
                        connectToTable(foundIp, cleanCode)
                    }
                    return@launch
                }
            }

            // 6. Caso especial de ambiente de emulador (10.0.2.x -> tenta o host da máquina 10.0.2.2)
            if (localIp.startsWith("10.0.2.")) {
                val hostPcIp = "10.0.2.2"
                if (probeHost(hostPcIp, NetworkConstants.DEFAULT_TCP_PORT)) {
                    withContext(Dispatchers.Main) {
                        connectToTable(hostPcIp, cleanCode)
                    }
                    return@launch
                }
            }

            // 7. Não encontrou na rede local
            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.DISCONNECTED,
                        errorMessage = null,
                        errorMessageRes = dev.vmellos.airhockey.R.string.lobby_error_table_not_found,
                        errorMessageArgs = listOf(cleanCode)
                    )
                }
            }
        }
    }

    private fun probeHost(ip: String, port: Int): Boolean {
        return try {
            java.net.Socket().use { s ->
                s.connect(java.net.InetSocketAddress(ip, port), 350)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun scanSubnetForHost(prefix: String, port: Int, selfIp: String): String? = coroutineScope {
        val found = CompletableDeferred<String?>()
        val jobs = mutableListOf<Job>()

        for (i in 1..254) {
            val targetIp = "$prefix$i"
            if (targetIp == selfIp) continue
            val j = launch(Dispatchers.IO) {
                if (probeHost(targetIp, port)) {
                    found.complete(targetIp)
                }
            }
            jobs.add(j)
        }

        val timeoutJob = launch {
            delay(2000)
            found.complete(null)
        }

        val result = found.await()
        jobs.forEach { it.cancel() }
        timeoutJob.cancel()
        result
    }

    fun handleQrScanResult(qrText: String) {
        _state.update { it.copy(isScanningQr = false) }
        val targetIp = when {
            qrText.contains("ip=") -> qrText.substringAfter("ip=").substringBefore("&").trim()
            qrText.contains("://") -> qrText.substringAfter("://").substringBefore(":").substringBefore("/").trim()
            qrText.contains(".") -> qrText.substringBefore(":").trim()
            else -> ""
        }
        val targetCode = if (qrText.contains("code=")) {
            qrText.substringAfter("code=").substringBefore("&").trim()
        } else ""
        val targetMode = if (qrText.contains("mode=")) {
            qrText.substringAfter("mode=").substringBefore("&").trim()
        } else {
            _state.value.targetMode
        }
        if (targetIp.isNotBlank()) {
            startAsGuest(targetMode)
            connectToTable(targetIp, targetCode)
        } else {
            _state.update {
                it.copy(
                    errorMessage = null,
                    errorMessageRes = dev.vmellos.airhockey.R.string.lobby_error_invalid_qr,
                    errorMessageArgs = emptyList()
                )
            }
        }
    }

    fun setScanningQr(scanning: Boolean) {
        _state.update { it.copy(isScanningQr = scanning) }
    }

    fun consumeStartMatchTrigger() {
        _state.update { it.copy(startMatchTrigger = 0L) }
    }

    /**
     * Constrói uma especificação imutável de partida ([MatchSpec]) com base no estado negociado do Lobby
     * e nas preferências do usuário.
     */
    fun buildMatchSpec(
        userPrefs: UserPreferences,
        aiExtendedDiff1: AiDifficulty = AiDifficulty.MEDIUM,
        aiExtendedDiff2: AiDifficulty = AiDifficulty.MEDIUM,
        p1CustomLabel: String? = null,
        p2CustomLabel: String? = null
    ): MatchSpec {
        val s = _state.value
        val isHost = s.role == LobbyRole.HOST
        val isConnected = s.targetMode == "CONNECTED"
        val isAiExtended = s.targetMode == "AI_EXTENDED"

        val gameMode = when {
            isConnected -> if (isHost) GameMode.CONNECTED_HOST else GameMode.CONNECTED_GUEST
            isAiExtended -> if (isHost) GameMode.AI_VS_AI_EXTENDED_HOST else GameMode.AI_VS_AI_EXTENDED_GUEST
            else -> if (isHost) GameMode.EXTENDED_HOST else GameMode.EXTENDED_GUEST
        }

        val effectivePuckSpeed = if (isHost) userPrefs.puckSpeed.maxSpeed else s.synchronizedPuckSpeed
        val defaultP1 = if (isHost) s.selfPlayerName.ifBlank { "Jogador 1" } else s.peerPlayerName.ifBlank { "Jogador 1" }
        val defaultP2 = if (isHost) s.peerPlayerName.ifBlank { "Jogador 2" } else s.selfPlayerName.ifBlank { "Jogador 2" }

        val effectiveAiDiff1 = if (isHost) {
            if (s.aiDifficulty1.isNotBlank()) {
                AiDifficulty.entries.find { it.name.equals(s.aiDifficulty1, ignoreCase = true) } ?: aiExtendedDiff1
            } else aiExtendedDiff1
        } else {
            AiDifficulty.entries.find { it.name.equals(s.aiDifficulty1, ignoreCase = true) } ?: AiDifficulty.MEDIUM
        }

        val effectiveAiDiff2 = if (isHost) {
            if (s.aiDifficulty2.isNotBlank()) {
                AiDifficulty.entries.find { it.name.equals(s.aiDifficulty2, ignoreCase = true) } ?: aiExtendedDiff2
            } else aiExtendedDiff2
        } else {
            AiDifficulty.entries.find { it.name.equals(s.aiDifficulty2, ignoreCase = true) } ?: AiDifficulty.MEDIUM
        }

        val p1Label = p1CustomLabel ?: defaultP1
        val p2Label = p2CustomLabel ?: defaultP2

        val effectiveGapMm = if (isConnected) 0f else {
            if (isHost) userPrefs.bezelGapMm else if (s.calibratedGapMm > 0f) s.calibratedGapMm else userPrefs.bezelGapMm
        }

        val needsPresentation = userPrefs.tableTheme == dev.vmellos.airhockey.core.preferences.TableTheme.ARCADE_3D ||
                userPrefs.tableTheme == dev.vmellos.airhockey.core.preferences.TableTheme.REALIDADE_AUMENTADA_AR

        return MatchSpec(
            mode = gameMode,
            bezelGapMm = effectiveGapMm,
            targetScore = userPrefs.targetScore,
            matchTimeSeconds = if (isConnected) userPrefs.matchTimeSeconds else 0,
            aiDifficulty = effectiveAiDiff1,
            aiDifficultyPlayer1 = effectiveAiDiff1,
            aiDifficultyPlayer2 = effectiveAiDiff2,
            puckMaxSpeed = effectivePuckSpeed,
            airFriction = userPrefs.puckSpeed.airFriction,
            player1Name = p1Label,
            player2Name = p2Label,
            hasIntroPresentation = needsPresentation
        )
    }

    fun stopAll() {
        val currentTargetMode = _state.value.targetMode
        val currentSelfName = _state.value.selfPlayerName
        coordinatorJob?.cancel()
        coordinatorJob = null
        discoveryPeriodicJob?.cancel()
        discoveryPeriodicJob = null
        coordinator?.terminateSession()
        coordinator = null
        nsdManager.stopBroadcast()
        nsdManager.stopDiscovery()
        _state.update { LobbyState(targetMode = currentTargetMode, selfPlayerName = currentSelfName) }
    }

    override fun onCleared() {
        super.onCleared()
        stopAll()
    }
}
