package dev.vmellos.airhockey

import dev.vmellos.airhockey.core.database.PlayerProfileStatsEntity
import dev.vmellos.airhockey.core.database.StageProgressEntity
import dev.vmellos.airhockey.core.playgames.PlayGamesSavePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayGamesSavePayloadTest {

    @Test
    fun testSerializationAndDeserializationRoundtrip() {
        val stages = listOf(
            StageProgressEntity(stageNumber = 1, starsEarned = 3, isUnlocked = true, bestScorePlayer = 5, bestScoreAi = 0),
            StageProgressEntity(stageNumber = 2, starsEarned = 2, isUnlocked = true, bestScorePlayer = 5, bestScoreAi = 2),
            StageProgressEntity(stageNumber = 3, starsEarned = 0, isUnlocked = true)
        )
        val stats = PlayerProfileStatsEntity(
            playerName = "CyberStriker",
            ratingElo = 1250,
            soloWins = 15,
            soloLosses = 3,
            multiplayerWins = 8,
            bestWinStreak = 7,
            goalsScored = 95,
            goalsConceded = 40
        )
        val payload = PlayGamesSavePayload(
            version = 1,
            timestamp = 1700000000000L,
            stages = stages,
            stats = stats
        )

        val bytes = payload.toByteArray()
        assertTrue(bytes.isNotEmpty())

        val restored = PlayGamesSavePayload.fromByteArray(bytes)
        assertNotNull(restored)
        assertEquals(1, restored!!.version)
        assertEquals(1700000000000L, restored.timestamp)
        assertEquals(3, restored.stages.size)
        assertEquals(5, restored.totalStars)
        assertEquals(3, restored.highestUnlockedStage)

        val stage1 = restored.stages.first { it.stageNumber == 1 }
        assertEquals(3, stage1.starsEarned)
        assertEquals(5, stage1.bestScorePlayer)
        assertEquals(0, stage1.bestScoreAi)

        assertNotNull(restored.stats)
        assertEquals("CyberStriker", restored.stats?.playerName)
        assertEquals(1250, restored.stats?.ratingElo)
        assertEquals(15, restored.stats?.soloWins)
        assertEquals(8, restored.stats?.multiplayerWins)
    }

    @Test
    fun testMergeMultiDeviceConflictResolution_UnionOfProgress() {
        // Dispositivo A jogou as fases 1 a 10 e ganhou 30 estrelas
        val stagesA = (1..10).map { num ->
            StageProgressEntity(
                stageNumber = num,
                starsEarned = 3,
                isUnlocked = true,
                bestScorePlayer = 5,
                bestScoreAi = 1
            )
        }
        val payloadA = PlayGamesSavePayload(
            stages = stagesA,
            stats = PlayerProfileStatsEntity(
                playerName = "Jogador",
                ratingElo = 1100,
                soloWins = 10,
                multiplayerWins = 2
            )
        )

        // Dispositivo B jogou offline e liberou fases 11 e 12 com 2 estrelas cada
        val stagesB = listOf(
            StageProgressEntity(stageNumber = 1, starsEarned = 1, isUnlocked = true),
            StageProgressEntity(stageNumber = 11, starsEarned = 2, isUnlocked = true, bestScorePlayer = 5, bestScoreAi = 3),
            StageProgressEntity(stageNumber = 12, starsEarned = 2, isUnlocked = true, bestScorePlayer = 5, bestScoreAi = 4),
            StageProgressEntity(stageNumber = 13, starsEarned = 0, isUnlocked = true)
        )
        val payloadB = PlayGamesSavePayload(
            stages = stagesB,
            stats = PlayerProfileStatsEntity(
                playerName = "Jogador",
                ratingElo = 1150,
                soloWins = 12,
                multiplayerWins = 5
            )
        )

        val merged = PlayGamesSavePayload.merge(payloadA, payloadB)

        // O merge deve conter todas as 100 fases
        assertEquals(100, merged.stages.size)

        // Fases 1 a 10 devem ter mantido as 3 estrelas do Dispositivo A
        for (i in 1..10) {
            val stage = merged.stages.first { it.stageNumber == i }
            assertTrue("Fase $i deve estar desbloqueada", stage.isUnlocked)
            assertEquals("Fase $i deve ter 3 estrelas", 3, stage.starsEarned)
        }

        // Fases 11 e 12 do Dispositivo B devem estar desbloqueadas e com 2 estrelas
        val stage11 = merged.stages.first { it.stageNumber == 11 }
        assertTrue(stage11.isUnlocked)
        assertEquals(2, stage11.starsEarned)

        val stage12 = merged.stages.first { it.stageNumber == 12 }
        assertTrue(stage12.isUnlocked)
        assertEquals(2, stage12.starsEarned)

        val stage13 = merged.stages.first { it.stageNumber == 13 }
        assertTrue(stage13.isUnlocked)
        assertEquals(0, stage13.starsEarned)

        // Maior fase desbloqueada deve ser 13
        assertEquals(13, merged.highestUnlockedStage)

        // Total de estrelas = 30 (de A) + 4 (de B nas fases 11 e 12) = 34
        assertEquals(34, merged.totalStars)

        // Stats consolidados devem pegar o maior valor (maxOf)
        assertNotNull(merged.stats)
        assertEquals(1150, merged.stats?.ratingElo)
        assertEquals(12, merged.stats?.soloWins)
        assertEquals(5, merged.stats?.multiplayerWins)
    }
}
