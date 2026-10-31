package dev.vmellos.airhockey.feature.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class TcpControlServer(
    private val port: Int = NetworkConstants.DEFAULT_TCP_PORT
) {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var writer: PrintWriter? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var onClientConnected: ((String) -> Unit)? = null
    var onClientDisconnected: (() -> Unit)? = null
    var onMessageReceived: ((String) -> Unit)? = null

    fun start() {
        stop()
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    clientSocket = socket
                    val clientIp = socket.inetAddress.hostAddress ?: ""
                    onClientConnected?.invoke(clientIp)

                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    writer = PrintWriter(socket.getOutputStream(), true)

                    while (isActive) {
                        val line = reader.readLine() ?: break
                        if (line.startsWith("TIME_SYNC:")) {
                            val clientSendTime = line.substringAfter("TIME_SYNC:").toLongOrNull() ?: 0L
                            val hostNow = System.currentTimeMillis()
                            sendMessage("TIME_SYNC_RESP:$clientSendTime,$hostNow")
                        } else {
                            onMessageReceived?.invoke(line)
                        }
                    }

                    onClientDisconnected?.invoke()
                }
            } catch (_: Exception) {
                if (isActive) onClientDisconnected?.invoke()
            }
        }
    }

    fun sendMessage(msg: String) {
        scope.launch {
            try {
                writer?.println(msg)
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        serverJob?.cancel()
        serverJob = null
        try {
            clientSocket?.close()
            serverSocket?.close()
        } catch (_: Exception) {}
        clientSocket = null
        serverSocket = null
        writer = null
    }
}
