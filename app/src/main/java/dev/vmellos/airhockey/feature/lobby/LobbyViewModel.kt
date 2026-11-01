package dev.vmellos.airhockey.feature.lobby

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vmellos.airhockey.AirHockeyApp
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
    val isPeerReady: Boolean = false,
    val isSelfReady: Boolean = false,
    val startMatchTrigger: Long = 0L,
    val navigateToCalibrationTrigger: Long = 0L,
    val calibratedGapMm: Float = 8.0f,
    val errorMessage: String? = null
)

class LobbyViewModel : ViewModel() {

    private val context = AirHockeyApp.instance.applicationContext
    val nsdManager = NsdDiscoveryManager(context)
    val tcpServer = TcpControlServer()
    val tcpClient = TcpControlClient()
    val udpSocket = UdpTelemetrySocket()

    private val _state = MutableStateFlow(LobbyState())
    val state: StateFlow<LobbyState> = _state.asStateFlow()

    var coordinator: MultiplayerMatchCoordinator? = null
        private set

    var onRemoteStateReceived: ((ControlMessages.RemoteMatchState) -> Unit)? = null
    var onControlMessageReceived: ((String) -> Unit)? = null

    init {
        viewModelScope.launch {
            nsdManager.discoveredTables.collect { tables ->
                _state.update { it.copy(discoveredTables = tables) }
            }
        }

        setupTcpServerCallbacks()
        setupTcpClientCallbacks()
    }

    fun setTargetMode(mode: String) {
        _state.update { it.copy(targetMode = mode) }
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

    fun startAsHost(mode: String = _state.value.targetMode) {
        val code = String.format("%04d", Random.nextInt(1000, 9999))
        val ip = NsdDiscoveryManager.getLocalIpAddress(context)
        val qr = "airhockey://connect?ip=$ip&port=8888&code=$code&mode=$mode"
        val ssid = getNetworkSsid()
        val wifiConnected = checkIsWifiConnected()

        _state.update {
            it.copy(
                role = LobbyRole.HOST,
                connectionStatus = ConnectionStatus.BROADCASTING,
                targetMode = mode,
                tableCode = code,
                hostIp = ip,
                qrContent = qr,
                wifiSsid = ssid,
                isWifiConnected = wifiConnected,
                errorMessage = null
            )
        }

        val transport = HybridSocketTransportAdapter(isHost = true)
        val coord = MultiplayerMatchCoordinator(transport, isHost = true)
        coordinator = coord
        bindCoordinator(coord)
        nsdManager.startBroadcast(code)
    }

    fun startAsGuest(mode: String = _state.value.targetMode) {
        _state.update {
            it.copy(
                role = LobbyRole.GUEST,
                connectionStatus = ConnectionStatus.DISCOVERING,
                targetMode = mode,
                errorMessage = null
            )
        }
        nsdManager.startDiscovery()
    }

    private fun setupTcpServerCallbacks() {
        tcpServer.onClientConnected = { clientIp ->
            _state.update {
                it.copy(
                    guestIp = clientIp,
                    connectionStatus = ConnectionStatus.CONNECTED,
                    isReadyToPlay = true
                )
            }
            udpSocket.setTarget(clientIp)
            tcpServer.sendMessage("HANDSHAKE_OK:${_state.value.tableCode}:${_state.value.targetMode}")
        }

        tcpServer.onClientDisconnected = {
            _state.update {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    isReadyToPlay = false,
                    isPeerReady = false
                )
            }
            onControlMessageReceived?.invoke(ControlMessages.EXIT)
        }

        tcpServer.onMessageReceived = { msg ->
            when {
                msg == ControlMessages.READY -> {
                    _state.update { it.copy(isPeerReady = true) }
                }
                msg == ControlMessages.EXIT -> {
                    onControlMessageReceived?.invoke(ControlMessages.EXIT)
                }
                else -> {
                    onControlMessageReceived?.invoke(msg)
                }
            }
        }
    }

    private fun setupTcpClientCallbacks() {
        tcpClient.onConnected = {
            _state.update {
                it.copy(
                    connectionStatus = ConnectionStatus.CONNECTED,
                    isReadyToPlay = true
                )
            }
            udpSocket.setTarget(_state.value.hostIp)
        }

        tcpClient.onDisconnected = {
            _state.update {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    isReadyToPlay = false,
                    isPeerReady = false
                )
            }
            onControlMessageReceived?.invoke(ControlMessages.EXIT)
        }

        tcpClient.onMessageReceived = { msg ->
            when {
                msg.startsWith("HANDSHAKE_OK") -> {
                    val parts = msg.split(":")
                    val mode = if (parts.size >= 3) parts[2] else _state.value.targetMode
                    _state.update { it.copy(isReadyToPlay = true, targetMode = mode) }
                }
                msg == "NAVIGATE_CALIBRATION" -> {
                    _state.update { it.copy(navigateToCalibrationTrigger = System.currentTimeMillis()) }
                }
                msg.startsWith(ControlMessages.START) -> {
                    val gap = msg.substringAfter(":", "8.0").toFloatOrNull() ?: 8.0f
                    _state.update {
                        it.copy(
                            calibratedGapMm = gap,
                            startMatchTrigger = System.currentTimeMillis()
                        )
                    }
                }
                msg.startsWith("STATE:") -> {
                    ControlMessages.decodeState(msg)?.let {
                        onRemoteStateReceived?.invoke(it)
                    }
                }
                msg == ControlMessages.EXIT -> {
                    onControlMessageReceived?.invoke(ControlMessages.EXIT)
                }
                else -> {
                    onControlMessageReceived?.invoke(msg)
                }
            }
        }
    }

    fun notifyNavigateToCalibration() {
        coordinator?.notifyNavigateToCalibration()
    }

    fun setGuestReady(gapMm: Float) {
        coordinator?.signalCalibrationReady(gapMm)
    }

    fun startHostMatch(gapMm: Float) {
        coordinator?.startMatch(gapMm)
    }

    fun sendControlMessage(message: String) {
        if (message == ControlMessages.EXIT) {
            coordinator?.terminateSession()
        }
    }

    fun connectToTable(hostIp: String) {
        _state.update { it.copy(hostIp = hostIp, connectionStatus = ConnectionStatus.CONNECTING, errorMessage = null) }
        nsdManager.stopDiscovery()
        val transport = HybridSocketTransportAdapter(isHost = false, targetHostIp = hostIp)
        val coord = MultiplayerMatchCoordinator(transport, isHost = false)
        coordinator = coord
        bindCoordinator(coord)
    }

    private fun bindCoordinator(coord: MultiplayerMatchCoordinator) {
        viewModelScope.launch {
            coord.state.collect { cState ->
                _state.update {
                    it.copy(
                        isPeerReady = cState.isPeerReady,
                        isSelfReady = cState.isSelfReady,
                        startMatchTrigger = cState.startMatchTrigger,
                        navigateToCalibrationTrigger = cState.navigateToCalibrationTrigger,
                        calibratedGapMm = cState.calibratedGapMm,
                        connectionStatus = when (cState.connectionState) {
                            TransportConnectionState.CONNECTED -> ConnectionStatus.CONNECTED
                            TransportConnectionState.CONNECTING -> ConnectionStatus.CONNECTING
                            TransportConnectionState.DISCONNECTED -> ConnectionStatus.DISCONNECTED
                            TransportConnectionState.IDLE -> it.connectionStatus
                        },
                        isReadyToPlay = (cState.connectionState == TransportConnectionState.CONNECTED),
                        errorMessage = if (cState.connectionState == TransportConnectionState.DISCONNECTED && it.errorMessage == null) {
                            "Conexão perdida ou não estabelecida com o anfitrião."
                        } else {
                            it.errorMessage
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
            connectToTable(ip)
            return
        }

        // 2. Busca entre as mesas já descobertas via NSD / UDP
        val table = _state.value.discoveredTables.find { it.tableCode == cleanCode }
        if (table != null) {
            connectToTable(table.hostIp)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(connectionStatus = ConnectionStatus.CONNECTING, errorMessage = null) }

            // 3. Emite sondagem ativa dirigida
            nsdManager.sendDirectedDiscoveryProbe()

            // 4. Tenta o Gateway DHCP (essencial quando conectado ao Hotspot do anfitrião)
            val gatewayIp = NsdDiscoveryManager.getDhcpGatewayIp(context)
            if (!gatewayIp.isNullOrBlank() && probeHost(gatewayIp, NetworkConstants.DEFAULT_TCP_PORT)) {
                withContext(Dispatchers.Main) {
                    connectToTable(gatewayIp)
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
                        connectToTable(foundIp)
                    }
                    return@launch
                }
            }

            // 6. Caso especial de ambiente de emulador (10.0.2.x -> tenta o host da máquina 10.0.2.2)
            if (localIp.startsWith("10.0.2.")) {
                val hostPcIp = "10.0.2.2"
                if (probeHost(hostPcIp, NetworkConstants.DEFAULT_TCP_PORT)) {
                    withContext(Dispatchers.Main) {
                        connectToTable(hostPcIp)
                    }
                    return@launch
                }
            }

            // 7. Não encontrou na rede local
            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.DISCONNECTED,
                        errorMessage = "Mesa não encontrada com o código $cleanCode. Verifique se ambos estão no mesmo Wi-Fi ou digite o IP do anfitrião."
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
        val targetMode = if (qrText.contains("mode=")) {
            qrText.substringAfter("mode=").substringBefore("&").trim()
        } else {
            _state.value.targetMode
        }
        if (targetIp.isNotBlank()) {
            startAsGuest(targetMode)
            connectToTable(targetIp)
        } else {
            _state.update {
                it.copy(errorMessage = "Código QR inválido ou ilegível.")
            }
        }
    }

    fun setScanningQr(scanning: Boolean) {
        _state.update { it.copy(isScanningQr = scanning) }
    }

    fun stopAll() {
        val currentTargetMode = _state.value.targetMode
        coordinator?.terminateSession()
        coordinator = null
        nsdManager.stopBroadcast()
        nsdManager.stopDiscovery()
        _state.update { LobbyState(targetMode = currentTargetMode) }
    }

    override fun onCleared() {
        super.onCleared()
        stopAll()
    }
}
