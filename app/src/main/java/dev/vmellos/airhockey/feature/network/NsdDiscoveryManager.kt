package dev.vmellos.airhockey.feature.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface

data class DiscoveredTable(
    val name: String,
    val hostIp: String,
    val port: Int,
    val tableCode: String
)

class NsdDiscoveryManager(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private var isBroadcasting = false
    private var isDiscovering = false

    private var udpBroadcastJob: Job? = null
    private var udpDiscoveryJob: Job? = null
    private var udpBroadcastSocket: DatagramSocket? = null
    private var udpDiscoverySocket: DatagramSocket? = null

    private val _discoveredTables = MutableStateFlow<List<DiscoveredTable>>(emptyList())
    val discoveredTables: StateFlow<List<DiscoveredTable>> = _discoveredTables.asStateFlow()

    private val resolveChannel = Channel<NsdServiceInfo>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        startResolveWorker()
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("AirHockey:MulticastLock").apply {
                    setReferenceCounted(false)
                }
            }
            if (multicastLock?.isHeld == false) {
                multicastLock?.acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseMulticastLockIfNeeded() {
        if (!isBroadcasting && !isDiscovering) {
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock?.release()
                }
            } catch (_: Exception) {}
        }
    }

    fun startBroadcast(tableCode: String, port: Int = NetworkConstants.DEFAULT_TCP_PORT) {
        stopBroadcast()
        isBroadcasting = true
        acquireMulticastLock()

        val hostIp = getLocalIpAddress(context)

        // 1. Canal NSD / mDNS
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "${NetworkConstants.SERVICE_NAME_PREFIX}$tableCode"
            serviceType = NetworkConstants.SERVICE_TYPE
            setPort(port)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAttribute("code", tableCode)
            }
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {}
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (_: Exception) {}

        // 2. Canal Ativo UDP Broadcast (Beacon + Resposta Direta a Consultas)
        startUdpBeacon(tableCode, port, hostIp)
    }

    private fun startUdpBeacon(tableCode: String, port: Int, hostIp: String) {
        udpBroadcastJob?.cancel()
        udpBroadcastJob = scope.launch {
            try {
                val socket = try {
                    DatagramSocket(null).apply {
                        reuseAddress = true
                        broadcast = true
                        bind(InetSocketAddress(NetworkConstants.DISCOVERY_UDP_PORT))
                    }
                } catch (_: Exception) {
                    DatagramSocket().apply { broadcast = true }
                }
                udpBroadcastSocket = socket

                // Responde a sondagens de convidados que enviem "AIRHOCKEY_DISCOVER"
                launch {
                    val buf = ByteArray(512)
                    while (isActive) {
                        try {
                            val packet = DatagramPacket(buf, buf.size)
                            socket.receive(packet)
                            val req = String(packet.data, packet.offset, packet.length).trim()
                            if (req.startsWith("AIRHOCKEY_DISCOVER")) {
                                val reply = "AIRHOCKEY_TABLE:$tableCode:$hostIp:$port".toByteArray()
                                val replyPacket = DatagramPacket(reply, reply.size, packet.address, packet.port)
                                socket.send(replyPacket)
                            }
                        } catch (_: Exception) {
                            break
                        }
                    }
                }

                // Envia beacon periódico via Broadcast a cada 1000ms
                while (isActive) {
                    val msg = "AIRHOCKEY_TABLE:$tableCode:$hostIp:$port".toByteArray()

                    // Broadcast global
                    try {
                        val pGlobal = DatagramPacket(msg, msg.size, InetAddress.getByName("255.255.255.255"), NetworkConstants.DISCOVERY_UDP_PORT)
                        socket.send(pGlobal)
                    } catch (_: Exception) {}

                    // Broadcasts dirigidos por interface de rede
                    val bcasts = getAllBroadcastAddresses()
                    for (b in bcasts) {
                        try {
                            val p = DatagramPacket(msg, msg.size, b, NetworkConstants.DISCOVERY_UDP_PORT)
                            socket.send(p)
                        } catch (_: Exception) {}
                    }

                    delay(1000)
                }
            } catch (_: Exception) {}
        }
    }

    fun stopBroadcast() {
        isBroadcasting = false
        udpBroadcastJob?.cancel()
        udpBroadcastJob = null
        try {
            udpBroadcastSocket?.close()
        } catch (_: Exception) {}
        udpBroadcastSocket = null

        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (_: Exception) {}
            registrationListener = null
        }

        releaseMulticastLockIfNeeded()
    }

    fun startDiscovery() {
        stopDiscovery()
        isDiscovering = true
        acquireMulticastLock()
        _discoveredTables.value = emptyList()

        // 1. Canal NSD / mDNS
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("airhockey") || service.serviceName.startsWith(NetworkConstants.SERVICE_NAME_PREFIX)) {
                    resolveChannel.trySend(service)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                _discoveredTables.update { current -> current.filter { it.name != service.serviceName } }
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                stopDiscovery()
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                stopDiscovery()
            }
        }

        try {
            nsdManager.discoverServices(NetworkConstants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (_: Exception) {}

        // 2. Canal Ativo UDP Discovery (Sondagem via Broadcast e Gateway de Hotspot)
        startUdpDiscovery()
    }

    private fun startUdpDiscovery() {
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = scope.launch {
            try {
                val socket = try {
                    DatagramSocket(null).apply {
                        reuseAddress = true
                        broadcast = true
                        bind(InetSocketAddress(NetworkConstants.DISCOVERY_UDP_PORT))
                    }
                } catch (_: Exception) {
                    DatagramSocket().apply { broadcast = true }
                }
                udpDiscoverySocket = socket

                // Receptor de Beacons de Mesas Ativas
                launch {
                    val buf = ByteArray(512)
                    while (isActive) {
                        try {
                            val packet = DatagramPacket(buf, buf.size)
                            socket.receive(packet)
                            val text = String(packet.data, packet.offset, packet.length).trim()
                            if (text.startsWith("AIRHOCKEY_TABLE:")) {
                                val parts = text.split(":")
                                if (parts.size >= 4) {
                                    val code = parts[1]
                                    val ip = if (parts[2].isNotBlank() && parts[2] != "127.0.0.1") {
                                        parts[2]
                                    } else {
                                        packet.address.hostAddress ?: ""
                                    }
                                    val port = parts[3].toIntOrNull() ?: NetworkConstants.DEFAULT_TCP_PORT
                                    if (ip.isNotBlank()) {
                                        val table = DiscoveredTable(
                                            name = "${NetworkConstants.SERVICE_NAME_PREFIX}$code",
                                            hostIp = ip,
                                            port = port,
                                            tableCode = code
                                        )
                                        _discoveredTables.update { current ->
                                            if (current.any { it.hostIp == ip && it.tableCode == code }) {
                                                current
                                            } else {
                                                current.filter { it.hostIp != ip } + table
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            break
                        }
                    }
                }

                // Emissor de sondagens periódicas a cada 1200ms
                while (isActive) {
                    val msg = "AIRHOCKEY_DISCOVER".toByteArray()

                    // Broadcast global
                    try {
                        val pGlobal = DatagramPacket(msg, msg.size, InetAddress.getByName("255.255.255.255"), NetworkConstants.DISCOVERY_UDP_PORT)
                        socket.send(pGlobal)
                    } catch (_: Exception) {}

                    // Broadcasts dirigidos por interface
                    val bcasts = getAllBroadcastAddresses()
                    for (b in bcasts) {
                        try {
                            val p = DatagramPacket(msg, msg.size, b, NetworkConstants.DISCOVERY_UDP_PORT)
                            socket.send(p)
                        } catch (_: Exception) {}
                    }

                    // Envio direto ao Gateway DHCP (essencial para Hotspot onde o anfitrião é o roteador)
                    val gatewayIp = getDhcpGatewayIp(context)
                    if (!gatewayIp.isNullOrBlank()) {
                        try {
                            val pGateway = DatagramPacket(msg, msg.size, InetAddress.getByName(gatewayIp), NetworkConstants.DISCOVERY_UDP_PORT)
                            socket.send(pGateway)
                        } catch (_: Exception) {}
                    }

                    delay(1200)
                }
            } catch (_: Exception) {}
        }
    }

    fun stopDiscovery() {
        isDiscovering = false
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = null
        try {
            udpDiscoverySocket?.close()
        } catch (_: Exception) {}
        udpDiscoverySocket = null

        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (_: Exception) {}
            discoveryListener = null
        }

        releaseMulticastLockIfNeeded()
    }

    private fun startResolveWorker() {
        scope.launch {
            for (service in resolveChannel) {
                resolveServiceSequentially(service)
            }
        }
    }

    private fun resolveServiceSequentially(service: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress ?: return
                val port = serviceInfo.port
                val name = serviceInfo.serviceName

                val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    serviceInfo.attributes["code"]?.let { String(it) }
                        ?: name.substringAfter(NetworkConstants.SERVICE_NAME_PREFIX, "")
                } else {
                    name.substringAfter(NetworkConstants.SERVICE_NAME_PREFIX, "")
                }

                val table = DiscoveredTable(
                    name = name,
                    hostIp = host,
                    port = port,
                    tableCode = code
                )

                _discoveredTables.update { current ->
                    if (current.any { it.hostIp == host && it.tableCode == code }) {
                        current
                    } else {
                        current.filter { it.hostIp != host } + table
                    }
                }
            }
        }

        try {
            nsdManager.resolveService(service, resolveListener)
        } catch (_: Exception) {}
    }

    companion object {

        /**
         * Retorna a lista de endereços de broadcast dirigidos das interfaces locais ativas.
         */
        fun getAllBroadcastAddresses(): List<InetAddress> {
            val result = mutableListOf<InetAddress>()
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces() ?: return result
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) continue
                    for (ifaceAddr in iface.interfaceAddresses) {
                        val bcast = ifaceAddr.broadcast
                        if (bcast != null && bcast is Inet4Address) {
                            result.add(bcast)
                        }
                    }
                }
            } catch (_: Exception) {}
            return result
        }

        /**
         * Obtém o endereço IPv4 do Gateway DHCP da conexão Wi-Fi.
         * Quando o jogador está conectado ao Ponto de Acesso Móvel (Hotspot) do anfitrião,
         * o Gateway é exatamente o endereço IP do anfitrião.
         */
        @Suppress("DEPRECATION")
        fun getDhcpGatewayIp(context: Context): String? {
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val dhcp = wm?.dhcpInfo ?: return null
                val gateway = dhcp.gateway
                if (gateway != 0) {
                    val ip = "${gateway and 0xFF}.${(gateway shr 8) and 0xFF}.${(gateway shr 16) and 0xFF}.${(gateway shr 24) and 0xFF}"
                    if (ip != "0.0.0.0") return ip
                }
            } catch (_: Exception) {}
            return null
        }

        /**
         * Detecta o endereço IPv4 local do dispositivo priorizando interfaces sem fio
         * (Wi-Fi e Ponto de Acesso Móvel) e ignorando estritamente interfaces de dados móveis (4G/5G).
         */
        @Suppress("DEPRECATION")
        fun getLocalIpAddress(context: Context? = null): String {
            // 1. Tenta IP do Wi-Fi ativo via WifiManager
            if (context != null) {
                try {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    val ipInt = wm?.connectionInfo?.ipAddress ?: 0
                    if (ipInt != 0) {
                        val ip = "${ipInt and 0xFF}.${(ipInt shr 8) and 0xFF}.${(ipInt shr 16) and 0xFF}.${(ipInt shr 24) and 0xFF}"
                        if (ip != "0.0.0.0") return ip
                    }
                } catch (_: Exception) {}
            }

            // 2. Varredura por interfaces de rede de hardware
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()

                // Prioridade 1: Interfaces sem fio locais (wlan, swlan, ap, softap, wifi, p2p)
                val wireless = interfaces.filter { iface ->
                    val name = iface.name.lowercase()
                    (name.startsWith("wlan") || name.startsWith("swlan") || name.startsWith("ap") ||
                     name.startsWith("softap") || name.startsWith("wifi") || name.startsWith("p2p")) &&
                     !iface.isLoopback && iface.isUp
                }

                for (iface in wireless) {
                    for (addr in iface.inetAddresses) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val host = addr.hostAddress ?: ""
                            if (host.isNotBlank() && host != "127.0.0.1") {
                                return host
                            }
                        }
                    }
                }

                // Prioridade 2: Outras interfaces ativas não-celulares e não-túneis (ethernet, etc.)
                val nonCellular = interfaces.filter { iface ->
                    val name = iface.name.lowercase()
                    !name.startsWith("rmnet") && !name.startsWith("ccmni") && !name.startsWith("pdp") &&
                    !name.startsWith("tun") && !name.startsWith("dummy") && !iface.isLoopback && iface.isUp
                }

                for (iface in nonCellular) {
                    for (addr in iface.inetAddresses) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val host = addr.hostAddress ?: ""
                            if (host.isNotBlank() && host != "127.0.0.1") {
                                return host
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            return "192.168.43.1"
        }
    }
}
