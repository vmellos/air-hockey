package dev.vmellos.airhockey.feature.network

/**
 * Registro de Descoberta de Mesas na rede local.
 *
 * Centraliza o estado das mesas anunciadas via UDP Beacons e NSD (mDNS),
 * gerenciando deduplicação por IP/código, expiração por TTL e encerramento gracioso.
 */
class TableDiscoveryRegistry(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val ttlMillis: Long = 3500L
) {
    private val lock = Any()
    private val tablesMap = mutableMapOf<String, DiscoveredTable>()

    /**
     * Retorna a lista imutável atual de mesas ativas descobertas.
     */
    fun getTables(): List<DiscoveredTable> = synchronized(lock) {
        tablesMap.values.toList()
    }

    /**
     * Processa uma mensagem de beacon UDP recebida ("AIRHOCKEY_TABLE:..." ou "AIRHOCKEY_TABLE_CLOSED:...").
     * Retorna a lista atualizada de mesas.
     */
    fun handleBeacon(text: String, fallbackIp: String = ""): List<DiscoveredTable> {
        val trimmed = text.trim()
        if (trimmed.startsWith(BEACON_CLOSED_PREFIX)) {
            val parts = trimmed.split(":")
            if (parts.size >= 2) {
                val code = parts[1]
                val ip = if (parts.size >= 3) parts[2] else ""
                removeTable(code, ip)
            }
            return getTables()
        }

        if (trimmed.startsWith(BEACON_ANNOUNCE_PREFIX)) {
            val parsed = parseBeacon(trimmed, fallbackIp, clock())
            if (parsed != null) {
                registerTable(parsed)
            }
            return getTables()
        }

        return getTables()
    }

    /**
     * Registra ou atualiza uma mesa descoberta.
     * Se houver registro anterior para o mesmo IP com código diferente, substitui.
     * Se for o mesmo IP e código, atualiza o timestamp e metadados.
     */
    fun registerTable(table: DiscoveredTable): List<DiscoveredTable> = synchronized(lock) {
        tablesMap.entries.removeAll { (_, existing) ->
            existing.hostIp == table.hostIp && existing.tableCode != table.tableCode
        }
        val key = "${table.hostIp}:${table.tableCode}"
        tablesMap[key] = table
        tablesMap.values.toList()
    }

    /**
     * Remove mesas pelo código da mesa ou IP do anfitrião.
     */
    fun removeTable(tableCode: String, hostIp: String = ""): List<DiscoveredTable> = synchronized(lock) {
        tablesMap.entries.removeAll { (_, table) ->
            table.tableCode == tableCode || (hostIp.isNotBlank() && table.hostIp == hostIp)
        }
        tablesMap.values.toList()
    }

    /**
     * Remove mesas associadas a um nome de serviço mDNS/NSD perdido.
     */
    fun removeByServiceName(serviceName: String): List<DiscoveredTable> = synchronized(lock) {
        tablesMap.entries.removeAll { (_, table) -> table.name == serviceName }
        tablesMap.values.toList()
    }

    /**
     * Remove mesas inativas cujo último beacon recebido seja mais antigo que o TTL.
     */
    fun pruneExpired(): List<DiscoveredTable> = synchronized(lock) {
        val now = clock()
        tablesMap.entries.removeAll { (_, table) -> (now - table.lastSeenMs) >= ttlMillis }
        tablesMap.values.toList()
    }

    /**
     * Limpa todos os registros de mesas.
     */
    fun clear(): Unit = synchronized(lock) {
        tablesMap.clear()
    }

    companion object {
        const val BEACON_ANNOUNCE_PREFIX = "AIRHOCKEY_TABLE:"
        const val BEACON_CLOSED_PREFIX = "AIRHOCKEY_TABLE_CLOSED:"

        /**
         * Decodifica a string de beacon UDP em um objeto [DiscoveredTable].
         */
        fun parseBeacon(text: String, fallbackIp: String = "", timestampMs: Long = System.currentTimeMillis()): DiscoveredTable? {
            val trimmed = text.trim()
            if (!trimmed.startsWith(BEACON_ANNOUNCE_PREFIX)) return null

            val parts = trimmed.split(":")
            if (parts.size < 4) return null

            val code = parts[1]
            val ip = if (parts[2].isNotBlank() && parts[2] != "127.0.0.1") {
                parts[2]
            } else {
                fallbackIp
            }
            if (ip.isBlank()) return null

            val port = parts[3].toIntOrNull() ?: NetworkConstants.DEFAULT_TCP_PORT
            val mode = if (parts.size >= 5 && parts[4].isNotBlank()) parts[4] else "EXTENDED"
            val hostName = if (parts.size >= 6) parts.subList(5, parts.size).joinToString(":") else ""

            return DiscoveredTable(
                name = "${NetworkConstants.SERVICE_NAME_PREFIX}$code",
                hostIp = ip,
                port = port,
                tableCode = code,
                mode = mode,
                hostName = hostName,
                lastSeenMs = timestampMs
            )
        }
    }
}
