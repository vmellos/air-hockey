package dev.vmellos.airhockey.core.playgames

import dev.vmellos.airhockey.core.database.PlayerProfileStatsEntity
import dev.vmellos.airhockey.core.database.StageProgressEntity
import org.json.JSONArray
import org.json.JSONObject

data class PlayGamesPreferencesPayload(
    val tableTheme: String? = null,
    val puckSkin: String? = null,
    val malletSkinP1: String? = null,
    val malletSkinP2: String? = null,
    val arenaDimension: String? = null,
    val arena3dIntroEnabled: Boolean? = null
)

/**
 * Payload estruturado serializado para os Jogos Salvos (SnapshotsClient) do Google Play Games.
 */
data class PlayGamesSavePayload(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val stages: List<StageProgressEntity>,
    val stats: PlayerProfileStatsEntity? = null,
    val preferences: PlayGamesPreferencesPayload? = null
) {
    val totalStars: Int
        get() = stages.sumOf { it.starsEarned }

    val highestUnlockedStage: Int
        get() = stages.filter { it.isUnlocked }.maxOfOrNull { it.stageNumber } ?: 1

    fun toByteArray(): ByteArray {
        val root = JSONObject()
        root.put("version", version)
        root.put("timestamp", timestamp)

        val stagesArr = JSONArray()
        stages.forEach { stage ->
            val obj = JSONObject()
            obj.put("stageNumber", stage.stageNumber)
            obj.put("starsEarned", stage.starsEarned)
            obj.put("isUnlocked", stage.isUnlocked)
            obj.put("bestScorePlayer", stage.bestScorePlayer)
            obj.put("bestScoreAi", stage.bestScoreAi)
            obj.put("completedTimestamp", stage.completedTimestamp)
            stagesArr.put(obj)
        }
        root.put("stages", stagesArr)

        stats?.let { s ->
            val statsObj = JSONObject()
            statsObj.put("playerName", s.playerName)
            statsObj.put("ratingElo", s.ratingElo)
            statsObj.put("soloWins", s.soloWins)
            statsObj.put("soloLosses", s.soloLosses)
            statsObj.put("soloDraws", s.soloDraws)
            statsObj.put("multiplayerWins", s.multiplayerWins)
            statsObj.put("multiplayerLosses", s.multiplayerLosses)
            statsObj.put("multiplayerDraws", s.multiplayerDraws)
            statsObj.put("currentWinStreak", s.currentWinStreak)
            statsObj.put("bestWinStreak", s.bestWinStreak)
            statsObj.put("goalsScored", s.goalsScored)
            statsObj.put("goalsConceded", s.goalsConceded)
            statsObj.put("lastMatchTimestamp", s.lastMatchTimestamp)
            root.put("stats", statsObj)
        }

        preferences?.let { p ->
            val prefObj = JSONObject()
            p.tableTheme?.let { prefObj.put("tableTheme", it) }
            p.puckSkin?.let { prefObj.put("puckSkin", it) }
            p.malletSkinP1?.let { prefObj.put("malletSkinP1", it) }
            p.malletSkinP2?.let { prefObj.put("malletSkinP2", it) }
            p.arenaDimension?.let { prefObj.put("arenaDimension", it) }
            p.arena3dIntroEnabled?.let { prefObj.put("arena3dIntroEnabled", it) }
            root.put("preferences", prefObj)
        }

        return root.toString().toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): PlayGamesSavePayload? {
            if (bytes.isEmpty()) return null
            return try {
                val str = String(bytes, Charsets.UTF_8)
                val root = JSONObject(str)
                val version = root.optInt("version", 1)
                val timestamp = root.optLong("timestamp", 0L)
                val stagesArr = root.optJSONArray("stages") ?: JSONArray()
                val stagesList = mutableListOf<StageProgressEntity>()
                for (i in 0 until stagesArr.length()) {
                    val obj = stagesArr.getJSONObject(i)
                    stagesList.add(
                        StageProgressEntity(
                            stageNumber = obj.getInt("stageNumber"),
                            starsEarned = obj.optInt("starsEarned", 0),
                            isUnlocked = obj.optBoolean("isUnlocked", false),
                            bestScorePlayer = obj.optInt("bestScorePlayer", 0),
                            bestScoreAi = obj.optInt("bestScoreAi", 0),
                            completedTimestamp = obj.optLong("completedTimestamp", 0L)
                        )
                    )
                }

                val statsObj = root.optJSONObject("stats")
                val stats = statsObj?.let {
                    PlayerProfileStatsEntity(
                        playerName = it.optString("playerName", "Jogador"),
                        ratingElo = it.optInt("ratingElo", 1000),
                        soloWins = it.optInt("soloWins", 0),
                        soloLosses = it.optInt("soloLosses", 0),
                        soloDraws = it.optInt("soloDraws", 0),
                        multiplayerWins = it.optInt("multiplayerWins", 0),
                        multiplayerLosses = it.optInt("multiplayerLosses", 0),
                        multiplayerDraws = it.optInt("multiplayerDraws", 0),
                        currentWinStreak = it.optInt("currentWinStreak", 0),
                        bestWinStreak = it.optInt("bestWinStreak", 0),
                        goalsScored = it.optInt("goalsScored", 0),
                        goalsConceded = it.optInt("goalsConceded", 0),
                        lastMatchTimestamp = it.optLong("lastMatchTimestamp", System.currentTimeMillis())
                    )
                }

                val prefObj = root.optJSONObject("preferences")
                val preferences = prefObj?.let {
                    PlayGamesPreferencesPayload(
                        tableTheme = it.optString("tableTheme").takeIf { s -> s.isNotBlank() },
                        puckSkin = it.optString("puckSkin").takeIf { s -> s.isNotBlank() },
                        malletSkinP1 = it.optString("malletSkinP1").takeIf { s -> s.isNotBlank() },
                        malletSkinP2 = it.optString("malletSkinP2").takeIf { s -> s.isNotBlank() },
                        arenaDimension = it.optString("arenaDimension").takeIf { s -> s.isNotBlank() },
                        arena3dIntroEnabled = if (it.has("arena3dIntroEnabled")) it.optBoolean("arena3dIntroEnabled") else null
                    )
                }

                PlayGamesSavePayload(version, timestamp, stagesList, stats, preferences)
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Regra de Ouro da Resolução de Conflitos (HIGHEST_PROGRESS & União de Fases).
         * Garante que o jogador NUNCA perca nenhuma fase concluída ou estrela conquistada,
         * mesmo alternando entre múltiplos dispositivos ou após jogar offline.
         */
        fun merge(
            local: PlayGamesSavePayload,
            remote: PlayGamesSavePayload
        ): PlayGamesSavePayload {
            val localMap = local.stages.associateBy { it.stageNumber }
            val remoteMap = remote.stages.associateBy { it.stageNumber }
            val allNumbers = (1..100).toSet()

            val mergedStages = allNumbers.map { num ->
                val l = localMap[num]
                val r = remoteMap[num]

                val stars = maxOf(l?.starsEarned ?: 0, r?.starsEarned ?: 0)
                val isUnlocked = (l?.isUnlocked == true) || (r?.isUnlocked == true) || (num == 1)
                val bestP = maxOf(l?.bestScorePlayer ?: 0, r?.bestScorePlayer ?: 0)
                val bestA = when {
                    (l?.bestScoreAi ?: 0) > 0 && (r?.bestScoreAi ?: 0) > 0 -> minOf(l!!.bestScoreAi, r!!.bestScoreAi)
                    (l?.bestScoreAi ?: 0) > 0 -> l!!.bestScoreAi
                    else -> r?.bestScoreAi ?: 0
                }
                val completedTs = maxOf(l?.completedTimestamp ?: 0L, r?.completedTimestamp ?: 0L)

                StageProgressEntity(
                    stageNumber = num,
                    starsEarned = stars,
                    isUnlocked = isUnlocked,
                    bestScorePlayer = bestP,
                    bestScoreAi = bestA,
                    completedTimestamp = completedTs
                )
            }

            val mergedStats = when {
                local.stats != null && remote.stats != null -> {
                    val l = local.stats
                    val r = remote.stats
                    val resolvedPlayerName = when {
                        l.playerName.isNotBlank() && l.playerName != "Jogador" -> l.playerName
                        r.playerName.isNotBlank() && r.playerName != "Jogador" -> r.playerName
                        l.playerName.isNotBlank() -> l.playerName
                        else -> r.playerName
                    }
                    PlayerProfileStatsEntity(
                        playerName = resolvedPlayerName,
                        ratingElo = maxOf(l.ratingElo, r.ratingElo),
                        soloWins = maxOf(l.soloWins, r.soloWins),
                        soloLosses = maxOf(l.soloLosses, r.soloLosses),
                        soloDraws = maxOf(l.soloDraws, r.soloDraws),
                        multiplayerWins = maxOf(l.multiplayerWins, r.multiplayerWins),
                        multiplayerLosses = maxOf(l.multiplayerLosses, r.multiplayerLosses),
                        multiplayerDraws = maxOf(l.multiplayerDraws, r.multiplayerDraws),
                        currentWinStreak = maxOf(l.currentWinStreak, r.currentWinStreak),
                        bestWinStreak = maxOf(l.bestWinStreak, r.bestWinStreak),
                        goalsScored = maxOf(l.goalsScored, r.goalsScored),
                        goalsConceded = maxOf(l.goalsConceded, r.goalsConceded),
                        lastMatchTimestamp = maxOf(l.lastMatchTimestamp, r.lastMatchTimestamp)
                    )
                }
                local.stats != null -> local.stats
                else -> remote.stats
            }

            val mergedPrefs = PlayGamesPreferencesPayload(
                tableTheme = local.preferences?.tableTheme ?: remote.preferences?.tableTheme,
                puckSkin = local.preferences?.puckSkin ?: remote.preferences?.puckSkin,
                malletSkinP1 = local.preferences?.malletSkinP1 ?: remote.preferences?.malletSkinP1,
                malletSkinP2 = local.preferences?.malletSkinP2 ?: remote.preferences?.malletSkinP2,
                arenaDimension = local.preferences?.arenaDimension ?: remote.preferences?.arenaDimension,
                arena3dIntroEnabled = local.preferences?.arena3dIntroEnabled ?: remote.preferences?.arena3dIntroEnabled
            )

            return PlayGamesSavePayload(
                version = maxOf(local.version, remote.version),
                timestamp = System.currentTimeMillis(),
                stages = mergedStages,
                stats = mergedStats,
                preferences = mergedPrefs
            )
        }
    }
}
