package dev.vmellos.airhockey.feature.lobby

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.feature.network.DiscoveredTable
import dev.vmellos.airhockey.feature.network.NsdDiscoveryManager
import dev.vmellos.airhockey.feature.network.TcpControlClient
import dev.vmellos.airhockey.feature.network.TcpControlServer
import dev.vmellos.airhockey.feature.network.UdpTelemetrySocket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val tableCode: String = "",
    val hostIp: String = "",
    val qrContent: String = "",
    val guestIp: String = "",
    val discoveredTables: List<DiscoveredTable> = emptyList(),
    val isReadyToPlay: Boolean = false,
    val isScanningQr: Boolean = false
)

class LobbyViewModel : ViewModel() {

    private val context = AirHockeyApp.instance.applicationContext
    val nsdManager = NsdDiscoveryManager(context)
    val tcpServer = TcpControlServer()
    val tcpClient = TcpControlClient()
    val udpSocket = UdpTelemetrySocket()

    private val _state = MutableStateFlow(LobbyState())
    val state: StateFlow<LobbyState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            nsdManager.discoveredTables.collect { tables ->
                _state.update { it.copy(discoveredTables = tables) }
            }
        }

        setupTcpServerCallbacks()
        setupTcpClientCallbacks()
    }

    fun startAsHost() {
        val code = String.format("%04d", Random.nextInt(1000, 9999))
        val ip = NsdDiscoveryManager.getLocalIpAddress()
        val qr = "airhockey://connect?ip=$ip&port=8888&code=$code"

        _state.update {
            it.copy(
                role = LobbyRole.HOST,
                connectionStatus = ConnectionStatus.BROADCASTING,
                tableCode = code,
                hostIp = ip,
                qrContent = qr
            )
        }

        tcpServer.start()
        udpSocket.startListening()
        nsdManager.startBroadcast(code)
    }

    fun startAsGuest() {
        _state.update {
            it.copy(
                role = LobbyRole.GUEST,
                connectionStatus = ConnectionStatus.DISCOVERING
            )
        }
        nsdManager.startDiscovery()
        udpSocket.startListening()
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
            tcpServer.sendMessage("HANDSHAKE_OK:${_state.value.tableCode}")
        }

        tcpServer.onClientDisconnected = {
            _state.update {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    isReadyToPlay = false
                )
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
                    isReadyToPlay = false
                )
            }
        }

        tcpClient.onMessageReceived = { msg ->
            if (msg.startsWith("HANDSHAKE_OK")) {
                _state.update { it.copy(isReadyToPlay = true) }
            }
        }
    }

    fun connectToTable(hostIp: String) {
        _state.update { it.copy(hostIp = hostIp, connectionStatus = ConnectionStatus.CONNECTING) }
        nsdManager.stopDiscovery()
        tcpClient.connect(hostIp)
    }

    fun connectByCode(code: String) {
        val table = _state.value.discoveredTables.find { it.tableCode == code.trim() }
        if (table != null) {
            connectToTable(table.hostIp)
        } else {
            // Em rede hotspot direta, anfitrião costuma ser o gateway 192.168.43.1
            connectToTable("192.168.43.1")
        }
    }

    fun handleQrScanResult(qrText: String) {
        _state.update { it.copy(isScanningQr = false) }
        // Exemplo: airhockey://connect?ip=192.168.1.15&port=8888&code=1234
        if (qrText.contains("ip=")) {
            val ip = qrText.substringAfter("ip=").substringBefore("&")
            if (ip.isNotBlank()) {
                connectToTable(ip)
            }
        }
    }

    fun setScanningQr(scanning: Boolean) {
        _state.update { it.copy(isScanningQr = scanning) }
    }

    fun stopAll() {
        nsdManager.stopBroadcast()
        nsdManager.stopDiscovery()
        tcpServer.stop()
        tcpClient.disconnect()
        udpSocket.stop()
        _state.update { LobbyState() }
    }

    override fun onCleared() {
        super.onCleared()
        stopAll()
    }
}
