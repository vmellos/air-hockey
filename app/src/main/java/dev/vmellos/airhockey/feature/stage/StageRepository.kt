package dev.vmellos.airhockey.feature.stage

import dev.vmellos.airhockey.core.database.StageProgressDao
import dev.vmellos.airhockey.core.database.StageProgressEntity
import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.PuckSpeedPreset
import dev.vmellos.airhockey.core.preferences.TableTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class StageConfig(
    val stageNumber: Int,
    val targetScore: Int,
    val aiDifficulty: AiDifficulty,
    val puckSpeed: PuckSpeedPreset,
    val tierName: String,
    val tableTheme: TableTheme = TableTheme.CLASSIC_NEON
)

class StageRepository(private val stageProgressDao: StageProgressDao) {

    private var onProgressUpdated: (() -> Unit)? = null

    fun setOnProgressUpdatedListener(listener: () -> Unit) {
        onProgressUpdated = listener
    }

    val stagesFlow: Flow<List<StageProgressEntity>> = stageProgressDao.getAllStages()

    val totalStarsFlow: Flow<Int> = stageProgressDao.getTotalStars().map { it ?: 0 }

    val unlockedThemesFlow: Flow<Set<TableTheme>> = stagesFlow.map { stages ->
        val completedStages = stages.filter { it.starsEarned > 0 }.map { it.stageNumber }.toSet()
        val unlocked = mutableSetOf(TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D)
        ArenaConquestCatalog.TIERS.forEach { tier ->
            if (completedStages.contains(tier.unlockStage)) {
                unlocked.add(tier.theme)
            }
        }
        unlocked
    }

    val unlockedPuckSkinsFlow: Flow<Set<PuckSkin>> = stagesFlow.map { stages ->
        val completedStages = stages.filter { it.starsEarned > 0 }.map { it.stageNumber }.toSet()
        val unlocked = mutableSetOf(PuckSkin.CLASSIC_PLASMA)
        ArenaConquestCatalog.TIERS.forEach { tier ->
            if (completedStages.contains(tier.unlockStage)) {
                unlocked.add(tier.puckSkin)
            }
        }
        unlocked
    }

    val unlockedMalletSkinsFlow: Flow<Set<MalletSkin>> = stagesFlow.map { stages ->
        val completedStages = stages.filter { it.starsEarned > 0 }.map { it.stageNumber }.toSet()
        val unlocked = mutableSetOf(MalletSkin.CLASSIC_NEON)
        ArenaConquestCatalog.TIERS.forEach { tier ->
            if (completedStages.contains(tier.unlockStage)) {
                unlocked.add(tier.malletSkin)
            }
        }
        unlocked
    }

    suspend fun isThemeUnlocked(theme: TableTheme): Boolean {
        if (ArenaConquestCatalog.isDefaultUnlocked(theme)) return true
        val requiredStage = ArenaConquestCatalog.getUnlockStageForTheme(theme)
        if (requiredStage <= 0) return true
        val stage = stageProgressDao.getStageDirect(requiredStage)
        return (stage?.starsEarned ?: 0) > 0
    }

    suspend fun ensureStagesInitialized() {
        val count = stageProgressDao.getStagesCount()
        if (count < 100) {
            val stages = (1..100).map { stageNum ->
                StageProgressEntity(
                    stageNumber = stageNum,
                    starsEarned = 0,
                    isUnlocked = stageNum == 1
                )
            }
            stageProgressDao.insertAll(stages)
        }
    }

    fun getStageConfig(stageNumber: Int): StageConfig {
        val validStage = stageNumber.coerceIn(1, 100)
        val stageTheme = ArenaConquestCatalog.getThemeForStage(validStage)
        return when (validStage) {
            in 1..10 -> StageConfig(
                stageNumber = validStage,
                targetScore = 3,
                aiDifficulty = AiDifficulty.EASY,
                puckSpeed = PuckSpeedPreset.NORMAL,
                tierName = "Iniciante",
                tableTheme = stageTheme
            )
            in 11..20 -> StageConfig(
                stageNumber = validStage,
                targetScore = 5,
                aiDifficulty = AiDifficulty.EASY,
                puckSpeed = PuckSpeedPreset.NORMAL,
                tierName = "Iniciante",
                tableTheme = stageTheme
            )
            in 21..50 -> StageConfig(
                stageNumber = validStage,
                targetScore = 5,
                aiDifficulty = AiDifficulty.MEDIUM,
                puckSpeed = PuckSpeedPreset.NORMAL,
                tierName = "Intermediario",
                tableTheme = stageTheme
            )
            in 51..80 -> StageConfig(
                stageNumber = validStage,
                targetScore = 7,
                aiDifficulty = AiDifficulty.HARD,
                puckSpeed = PuckSpeedPreset.FAST,
                tierName = "Avancado",
                tableTheme = stageTheme
            )
            in 81..90 -> StageConfig(
                stageNumber = validStage,
                targetScore = 7,
                aiDifficulty = AiDifficulty.HARD,
                puckSpeed = PuckSpeedPreset.FAST,
                tierName = "Mestre",
                tableTheme = stageTheme
            )
            else -> StageConfig(
                stageNumber = validStage,
                targetScore = 10,
                aiDifficulty = AiDifficulty.HARD,
                puckSpeed = PuckSpeedPreset.FAST,
                tierName = "Mestre",
                tableTheme = stageTheme
            )
        }
    }

    fun calculateStars(stageNumber: Int, scorePlayer: Int, scoreAi: Int): Int {
        val config = getStageConfig(stageNumber)
        if (scorePlayer < config.targetScore) return 0

        return when {
            config.targetScore <= 3 -> when {
                scoreAi == 0 -> 3
                scoreAi == 1 -> 2
                else -> 1
            }
            config.targetScore <= 5 -> when {
                scoreAi <= 1 -> 3
                scoreAi <= 3 -> 2
                else -> 1
            }
            config.targetScore <= 7 -> when {
                scoreAi <= 2 -> 3
                scoreAi <= 4 -> 2
                else -> 1
            }
            else -> when {
                scoreAi <= 3 -> 3
                scoreAi <= 6 -> 2
                else -> 1
            }
        }
    }

    suspend fun recordStageResult(
        stageNumber: Int,
        scorePlayer: Int,
        scoreAi: Int
    ): Int {
        val stars = calculateStars(stageNumber, scorePlayer, scoreAi)
        val isWin = stars > 0

        val existing = stageProgressDao.getStageDirect(stageNumber)
        val highestStars = if (existing != null) maxOf(existing.starsEarned, stars) else stars
        val bestScoreP = if (existing != null) maxOf(existing.bestScorePlayer, scorePlayer) else scorePlayer
        val bestScoreA = if (existing != null) minOf(existing.bestScoreAi, scoreAi) else scoreAi

        val updated = StageProgressEntity(
            stageNumber = stageNumber,
            starsEarned = highestStars,
            isUnlocked = true,
            bestScorePlayer = bestScoreP,
            bestScoreAi = bestScoreA,
            completedTimestamp = if (isWin) System.currentTimeMillis() else (existing?.completedTimestamp ?: 0L)
        )
        stageProgressDao.upsertStage(updated)

        if (isWin && stageNumber < 100) {
            stageProgressDao.unlockStage(stageNumber + 1)
        }

        if (isWin) {
            onProgressUpdated?.invoke()
        }

        return stars
    }
}
