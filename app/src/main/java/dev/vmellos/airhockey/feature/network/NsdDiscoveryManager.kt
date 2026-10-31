package dev.vmellos.airhockey.feature.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
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
            multicastLock?.acquire()
        } catch (_: Exception) {
            // Ignora se não permitido em dispositivo específico
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {
            // Ignora
        }
    }

    fun startBroadcast(tableCode: String, port: Int = NetworkConstants.DEFAULT_TCP_PORT) {
        acquireMulticastLock()
        stopBroadcast()

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
        } catch (_: Exception) {
            // Trata falhas de registro
        }
    }

    fun stopBroadcast() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (_: Exception) {}
            registrationListener = null
        }
        releaseMulticastLock()
    }

    fun startDiscovery() {
        acquireMulticastLock()
        stopDiscovery()
        _discoveredTables.value = emptyList()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("airhockey") || service.serviceName.startsWith(NetworkConstants.SERVICE_NAME_PREFIX)) {
                    resolveChannel.trySend(service)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                _discoveredTables.value = _discoveredTables.value.filter { it.name != service.serviceName }
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
        } catch (_: Exception) {
            // Fallback
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (_: Exception) {}
            discoveryListener = null
        }
        releaseMulticastLock()
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

                _discoveredTables.value = (_discoveredTables.value.filter { it.hostIp != host } + table)
            }
        }

        try {
            nsdManager.resolveService(service, resolveListener)
        } catch (_: Exception) {}
    }

    companion object {
        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    // Ignora loopback e interfaces inativas
                    if (intf.isLoopback || !intf.isUp) continue
                    val addresses = intf.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val ip = addr.hostAddress ?: ""
                            // Prioriza redes Wi-Fi e Hotspot comuns (192.168.x.x ou 10.x.x.x)
                            if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
                                return ip
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
            return "192.168.43.1" // IP padrão típico de Hotspot Android
        }
    }
}
