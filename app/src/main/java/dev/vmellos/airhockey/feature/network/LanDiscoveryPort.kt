package dev.vmellos.airhockey.feature.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface de porta de descoberta na rede local (LAN/Hotspot/NSD),
 * isolando a lógica de lobby de detalhes do Android NsdManager e sockets UDP.
 */
interface LanDiscoveryPort {
    val discoveredTables: StateFlow<List<DiscoveredTable>>

    fun startBroadcast(
        tableCode: String,
        port: Int = NetworkConstants.DEFAULT_TCP_PORT,
        mode: String = "EXTENDED",
        hostName: String = ""
    )

    fun stopBroadcast()
    fun startDiscovery()
    fun stopDiscovery()
    fun sendDirectedDiscoveryProbe()
    fun restartNsdDiscovery()
    fun getLocalIpAddress(): String
    fun getDhcpGatewayIp(): String?
}
