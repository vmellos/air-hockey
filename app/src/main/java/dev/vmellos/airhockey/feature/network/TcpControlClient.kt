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
import java.net.Socket

class TcpControlClient {

    @Volatile private var socket: Socket? = null
    @Volatile private var writer: PrintWriter? = null
    private var clientJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Envio sequencial em IO: preserva a ordem das mensagens e nunca usa a thread de UI.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val sendScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))

    var clockOffsetMs: Long = 0L
        private set

    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onMessageReceived: ((String) -> Unit)? = null

    fun connect(hostIp: String, port: Int = NetworkConstants.DEFAULT_TCP_PORT) {
        disconnect()
        clientJob = scope.launch {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(hostIp, port), 4000)
                socket = s
                writer = PrintWriter(s.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))

                onConnected?.invoke()

                // Cristian's Algorithm: sincronização inicial de relógio
                performClockSync()

                while (isActive) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("TIME_SYNC_RESP:")) {
                        handleTimeSyncResponse(line.substringAfter("TIME_SYNC_RESP:"))
                    } else {
                        onMessageReceived?.invoke(line)
                    }
                }

                writer = null
                if (isActive) onDisconnected?.invoke()
            } catch (e: Exception) {
                writer = null
                if (isActive) {
                    Log.w(TAG, "Conexão TCP com $hostIp:$port falhou ou caiu", e)
                    onDisconnected?.invoke()
                }
            }
        }
    }

    private fun performClockSync() {
        val now = System.currentTimeMillis()
        sendMessage("TIME_SYNC:$now")
    }

    private fun handleTimeSyncResponse(data: String) {
        val parts = data.split(",")
        if (parts.size == 2) {
            val t0 = parts[0].toLongOrNull() ?: return
            val tHost = parts[1].toLongOrNull() ?: return
            val t1 = System.currentTimeMillis()
            val rtt = t1 - t0
            clockOffsetMs = tHost - (t1 - rtt / 2)
        }
    }

    fun sendMessage(msg: String) {
        sendScope.launch {
            val w = writer
            if (w == null) {
                Log.w(TAG, "Mensagem descartada (sem conexão com o anfitrião): $msg")
                return@launch
            }
            w.println(msg)
            if (w.checkError()) Log.w(TAG, "Falha ao enviar mensagem TCP: $msg")
        }
    }

    fun disconnect() {
        clientJob?.cancel()
        clientJob = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        writer = null
    }

    private companion object {
        const val TAG = "TcpControlClient"
    }
}
