package dev.vmellos.airhockey.feature.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class TcpControlServer(
    private val port: Int = NetworkConstants.DEFAULT_TCP_PORT
) {
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var clientSocket: Socket? = null
    @Volatile private var writer: PrintWriter? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Envio sequencial em IO: preserva a ordem das mensagens e nunca usa a thread de UI.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val sendScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))

    var onClientConnected: ((String) -> Unit)? = null
    var onClientDisconnected: (() -> Unit)? = null
    var onMessageReceived: ((String) -> Unit)? = null

    fun start() {
        stop()
        serverJob = scope.launch {
            try {
                val server = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                serverSocket = server
                while (isActive) {
                    val socket = server.accept() ?: break
                    runCatching {
                        socket.tcpNoDelay = true
                        socket.trafficClass = 0x10
                    }
                    clientSocket = socket
                    val clientIp = socket.inetAddress.hostAddress ?: ""
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    writer = PrintWriter(socket.getOutputStream(), true)
                    onClientConnected?.invoke(clientIp)

                    try {
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
                    } catch (e: Exception) {
                        if (isActive && e !is java.net.SocketException) {
                            Log.w(TAG, "Leitura TCP interrompida", e)
                        } else if (isActive) {
                            Log.d(TAG, "Conexão TCP finalizada: ${e.message}")
                        }
                    }

                    writer = null
                    if (isActive) onClientDisconnected?.invoke()
                }
            } catch (e: Exception) {
                if (isActive) {
                    if (e is java.net.SocketException) {
                        Log.d(TAG, "Servidor TCP finalizado na porta $port")
                    } else {
                        Log.e(TAG, "Servidor TCP falhou na porta $port", e)
                    }
                    onClientDisconnected?.invoke()
                }
            }
        }
    }

    fun sendMessage(msg: String) {
        sendScope.launch {
            val w = writer
            if (w == null) {
                Log.d(TAG, "Mensagem descartada (sem convidado conectado): $msg")
                return@launch
            }
            w.println(msg)
            if (w.checkError()) Log.d(TAG, "Falha ao enviar mensagem TCP (cliente desconectado): $msg")
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

    private companion object {
        const val TAG = "TcpControlServer"
    }
}
