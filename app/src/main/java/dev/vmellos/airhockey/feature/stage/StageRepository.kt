package dev.vmellos.airhockey.feature.stage

import androidx.compose.runtime.Immutable
import dev.vmellos.airhockey.core.catalog.ArenaPresetCatalog
import dev.vmellos.airhockey.core.database.StageProgressDao
import dev.vmellos.airhockey.core.database.StageProgressEntity
import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.PuckSpeedPreset
import dev.vmellos.airhockey.core.preferences.TableTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Immutable
data class UnlockedCosmetics(
    val themes: Set<TableTheme> = setOf(TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D, TableTheme.KIARA_CAT),
    val puckSkins: Set<PuckSkin> = setOf(PuckSkin.CLASSIC_PLASMA, PuckSkin.KIARA_PAW),
    val malletSkins: Set<MalletSkin> = setOf(MalletSkin.CLASSIC_NEON, MalletSkin.NEKO_STRIKER)
)

data class StageConfig(
    val stageNumber: Int,
    val targetScore: Int,
    val aiDifficulty: AiDifficulty,
    val puckSpeed: PuckSpeedPreset,
    val tierName: String,
    val tableTheme: TableTheme = TableTheme.CLASSIC_NEON,
    val aiMaxSpeed: Float = calculateAiMaxSpeed(stageNumber),
    val aiReactionDelay: Float = calculateAiReactionDelay(stageNumber),
    val aiAimingError: Float = calculateAiAimingError(stageNumber),
    val aiBankShotChance: Float = calculateAiBankShotChance(stageNumber)
) {
    companion object {
        /**
         * Curva Gradual de Dificuldade (GLOSSARY.md / ADR 0039):
         * Acelera suavemente a velocidade máxima de 900f (Fase 1) até 5200f (Fase 100)
         * sem saltos abruptos de categoria.
         */
        fun calculateAiMaxSpeed(stage: Int): Float {
            val clamped = stage.coerceIn(1, 100)
            return 900f + (clamped - 1) * ((5200f - 900f) / 99f)
        }

        /**
         * Latência de reação com redução progressiva de 320ms (Fase 1) a 0ms (Fase 100).
         */
        fun calculateAiReactionDelay(stage: Int): Float {
            val clamped = stage.coerceIn(1, 100)
            return (0.32f * (100 - clamped) / 99f).coerceAtLeast(0f)
        }

        /**
         * Dispersão de mira humana com redução progressiva de ±70px (Fase 1) a 0px (Fase 100).
         */
        fun calculateAiAimingError(stage: Int): Float {
            val clamped = stage.coerceIn(1, 100)
            return (70f * (100 - clamped) / 99f).coerceAtLeast(0f)
        }

        /**
         * Probabilidade de jogadas laterais com tabela, iniciando na Fase 30 (10%) até 80% na Fase 100.
         */
        fun calculateAiBankShotChance(stage: Int): Float {
            val clamped = stage.coerceIn(1, 100)
            return if (clamped < 30) 0f else (0.10f + (clamped - 30) * (0.70f / 70f)).coerceIn(0f, 0.80f)
        }
    }
}

class StageRepository(private val stageProgressDao: StageProgressDao) {

    private var onProgressUpdated: (() -> Unit)? = null

    fun setOnProgressUpdatedListener(listener: () -> Unit) {
        onProgressUpdated = listener
    }

    val stagesFlow: Flow<List<StageProgressEntity>> = stageProgressDao.getAllStages()

    val totalStarsFlow: Flow<Int> = stageProgressDao.getTotalStars().map { it ?: 0 }

    suspend fun getTotalStarsDirect(): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        stageProgressDao.getAllStagesDirect().sumOf { it.starsEarned }
    }

    val unlockedCosmeticsFlow: Flow<UnlockedCosmetics> = stagesFlow.map { stages ->
        val completedStages = stages.filter { it.starsEarned > 0 }.map { it.stageNumber }.toSet()
        val themes = mutableSetOf(TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D, TableTheme.KIARA_CAT)
        val pucks = mutableSetOf(PuckSkin.CLASSIC_PLASMA, PuckSkin.KIARA_PAW)
        val mallets = mutableSetOf(MalletSkin.CLASSIC_NEON, MalletSkin.NEKO_STRIKER)
        ArenaPresetCatalog.TIERS.forEach { preset ->
            if (completedStages.contains(preset.unlockStage)) {
                themes.add(preset.theme)
                pucks.add(preset.puckSkin)
                mallets.add(preset.malletSkin)
            }
        }
        UnlockedCosmetics(
            themes = themes,
            puckSkins = pucks,
            malletSkins = mallets
        )
    }

    val unlockedThemesFlow: Flow<Set<TableTheme>> = unlockedCosmeticsFlow.map { it.themes }

    val unlockedPuckSkinsFlow: Flow<Set<PuckSkin>> = unlockedCosmeticsFlow.map { it.puckSkins }

    val unlockedMalletSkinsFlow: Flow<Set<MalletSkin>> = unlockedCosmeticsFlow.map { it.malletSkins }

    suspend fun isThemeUnlocked(theme: TableTheme): Boolean {
        if (ArenaPresetCatalog.isDefaultUnlocked(theme)) return true
        val requiredStage = ArenaPresetCatalog.getUnlockStageForTheme(theme)
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
        val stageTheme = ArenaPresetCatalog.getThemeForStage(validStage)
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
