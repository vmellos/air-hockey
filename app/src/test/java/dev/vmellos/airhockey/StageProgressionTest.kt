package dev.vmellos.airhockey

import dev.vmellos.airhockey.core.database.StageProgressDao
import dev.vmellos.airhockey.core.database.StageProgressEntity
import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.core.preferences.PuckSpeedPreset
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.feature.stage.ArenaConquestCatalog
import dev.vmellos.airhockey.feature.stage.StageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeStageProgressDao : StageProgressDao {
    private val stagesMap = mutableMapOf<Int, StageProgressEntity>()
    private val stagesFlowInternal = MutableStateFlow<List<StageProgressEntity>>(emptyList())

    override fun getAllStages(): Flow<List<StageProgressEntity>> = stagesFlowInternal

    override suspend fun getAllStagesDirect(): List<StageProgressEntity> {
        return stagesMap.values.sortedBy { it.stageNumber }
    }

    override fun getStage(stageNumber: Int): Flow<StageProgressEntity?> {
        return stagesFlowInternal.map { list -> list.find { it.stageNumber == stageNumber } }
    }

    override suspend fun getStageDirect(stageNumber: Int): StageProgressEntity? {
        return stagesMap[stageNumber]
    }

    override suspend fun getStagesCount(): Int = stagesMap.size

    override suspend fun insertAll(stages: List<StageProgressEntity>) {
        stages.forEach { stagesMap[it.stageNumber] = it }
        stagesFlowInternal.value = stagesMap.values.sortedBy { it.stageNumber }
    }

    override suspend fun upsertAll(stages: List<StageProgressEntity>) {
        stages.forEach { stagesMap[it.stageNumber] = it }
        stagesFlowInternal.value = stagesMap.values.sortedBy { it.stageNumber }
    }

    override suspend fun upsertStage(stage: StageProgressEntity) {
        stagesMap[stage.stageNumber] = stage
        stagesFlowInternal.value = stagesMap.values.sortedBy { it.stageNumber }
    }

    override suspend fun unlockStage(stageNumber: Int) {
        val existing = stagesMap[stageNumber] ?: StageProgressEntity(stageNumber = stageNumber)
        stagesMap[stageNumber] = existing.copy(isUnlocked = true)
        stagesFlowInternal.value = stagesMap.values.sortedBy { it.stageNumber }
    }

    override fun getTotalStars(): Flow<Int?> {
        return stagesFlowInternal.map { list -> list.sumOf { it.starsEarned } }
    }

    override fun getHighestUnlockedStage(): Flow<Int?> {
        return stagesFlowInternal.map { list -> list.filter { it.isUnlocked }.maxOfOrNull { it.stageNumber } }
    }
}

class StageProgressionTest {

    private lateinit var dao: FakeStageProgressDao
    private lateinit var repository: StageRepository

    @Before
    fun setup() {
        dao = FakeStageProgressDao()
        repository = StageRepository(dao)
    }

    @Test
    fun testStageDifficultyConfigurations() {
        val s1 = repository.getStageConfig(1)
        assertEquals(3, s1.targetScore)
        assertEquals(AiDifficulty.EASY, s1.aiDifficulty)
        assertEquals(PuckSpeedPreset.NORMAL, s1.puckSpeed)

        val s15 = repository.getStageConfig(15)
        assertEquals(5, s15.targetScore)
        assertEquals(AiDifficulty.EASY, s15.aiDifficulty)

        val s35 = repository.getStageConfig(35)
        assertEquals(5, s35.targetScore)
        assertEquals(AiDifficulty.MEDIUM, s35.aiDifficulty)

        val s65 = repository.getStageConfig(65)
        assertEquals(7, s65.targetScore)
        assertEquals(AiDifficulty.HARD, s65.aiDifficulty)
        assertEquals(PuckSpeedPreset.FAST, s65.puckSpeed)

        val s85 = repository.getStageConfig(85)
        assertEquals(7, s85.targetScore)
        assertEquals(AiDifficulty.HARD, s85.aiDifficulty)

        val s95 = repository.getStageConfig(95)
        assertEquals(10, s95.targetScore)
        assertEquals(AiDifficulty.HARD, s95.aiDifficulty)
        assertEquals(PuckSpeedPreset.FAST, s95.puckSpeed)
    }

    @Test
    fun testStageGradualDifficultyParameters() {
        val s1 = repository.getStageConfig(1)
        assertEquals(900f, s1.aiMaxSpeed, 0.01f)
        assertEquals(0.32f, s1.aiReactionDelay, 0.01f)
        assertEquals(70f, s1.aiAimingError, 0.01f)
        assertEquals(0f, s1.aiBankShotChance, 0.01f)

        val s20 = repository.getStageConfig(20)
        val s21 = repository.getStageConfig(21)
        // Garante que não há salto brusco: a diferença entre as fases 20 e 21 é suave (~43f)
        assertTrue(s21.aiMaxSpeed > s20.aiMaxSpeed)
        assertEquals(43.43f, s21.aiMaxSpeed - s20.aiMaxSpeed, 1.0f)
        assertTrue(s21.aiReactionDelay < s20.aiReactionDelay)

        val s100 = repository.getStageConfig(100)
        assertEquals(5200f, s100.aiMaxSpeed, 0.01f)
        assertEquals(0.0f, s100.aiReactionDelay, 0.01f)
        assertEquals(0.0f, s100.aiAimingError, 0.01f)
        assertEquals(0.80f, s100.aiBankShotChance, 0.01f)
    }

    @Test
    fun testStarRatingCalculation() {
        // Stage 1: target 3
        assertEquals(3, repository.calculateStars(1, scorePlayer = 3, scoreAi = 0))
        assertEquals(2, repository.calculateStars(1, scorePlayer = 3, scoreAi = 1))
        assertEquals(1, repository.calculateStars(1, scorePlayer = 3, scoreAi = 2))
        assertEquals(0, repository.calculateStars(1, scorePlayer = 2, scoreAi = 3))

        // Stage 25: target 5
        assertEquals(3, repository.calculateStars(25, scorePlayer = 5, scoreAi = 0))
        assertEquals(3, repository.calculateStars(25, scorePlayer = 5, scoreAi = 1))
        assertEquals(2, repository.calculateStars(25, scorePlayer = 5, scoreAi = 2))
        assertEquals(2, repository.calculateStars(25, scorePlayer = 5, scoreAi = 3))
        assertEquals(1, repository.calculateStars(25, scorePlayer = 5, scoreAi = 4))
        assertEquals(0, repository.calculateStars(25, scorePlayer = 4, scoreAi = 5))

        // Stage 65: target 7
        assertEquals(3, repository.calculateStars(65, scorePlayer = 7, scoreAi = 2))
        assertEquals(2, repository.calculateStars(65, scorePlayer = 7, scoreAi = 4))
        assertEquals(1, repository.calculateStars(65, scorePlayer = 7, scoreAi = 6))
        assertEquals(0, repository.calculateStars(65, scorePlayer = 6, scoreAi = 7))

        // Stage 95: target 10
        assertEquals(3, repository.calculateStars(95, scorePlayer = 10, scoreAi = 3))
        assertEquals(2, repository.calculateStars(95, scorePlayer = 10, scoreAi = 6))
        assertEquals(1, repository.calculateStars(95, scorePlayer = 10, scoreAi = 9))
        assertEquals(0, repository.calculateStars(95, scorePlayer = 9, scoreAi = 10))
    }

    @Test
    fun testStageInitializationAndUnlocking() = runBlocking {
        repository.ensureStagesInitialized()
        assertEquals(100, dao.getStagesCount())

        val stage1 = dao.getStageDirect(1)
        assertTrue(stage1?.isUnlocked == true)

        val stage2Before = dao.getStageDirect(2)
        assertFalse(stage2Before?.isUnlocked == true)

        // Player wins Stage 1 with 3 stars
        val stars = repository.recordStageResult(stageNumber = 1, scorePlayer = 3, scoreAi = 0)
        assertEquals(3, stars)

        val stage2After = dao.getStageDirect(2)
        assertTrue(stage2After?.isUnlocked == true)

        val updatedStage1 = dao.getStageDirect(1)
        assertEquals(3, updatedStage1?.starsEarned)
    }

    @Test
    fun testRoomMigration1To2Definition() {
        assertEquals(1, dev.vmellos.airhockey.core.database.AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, dev.vmellos.airhockey.core.database.AppDatabase.MIGRATION_1_2.endVersion)
    }

    @Test
    fun testArenaConquestCatalogTiersAndThemesMapping() {
        assertEquals(10, ArenaConquestCatalog.TIERS.size)

        // Stage 1-10 -> CYBER_PONG, unlock at 10
        assertEquals(TableTheme.CYBER_PONG, ArenaConquestCatalog.getThemeForStage(1))
        assertEquals(TableTheme.CYBER_PONG, ArenaConquestCatalog.getThemeForStage(10))
        assertEquals(10, ArenaConquestCatalog.getUnlockStageForTheme(TableTheme.CYBER_PONG))

        // Stage 11-20 -> CYBER_SOCCER, unlock at 20
        assertEquals(TableTheme.CYBER_SOCCER, ArenaConquestCatalog.getThemeForStage(15))
        assertEquals(20, ArenaConquestCatalog.getUnlockStageForTheme(TableTheme.CYBER_SOCCER))

        // Stage 21-30 -> NEO_BASKETBALL, unlock at 30
        assertEquals(TableTheme.NEO_BASKETBALL, ArenaConquestCatalog.getThemeForStage(25))
        assertEquals(30, ArenaConquestCatalog.getUnlockStageForTheme(TableTheme.NEO_BASKETBALL))

        // Classic and Kiara Cat arenas are default unlocked
        assertTrue(ArenaConquestCatalog.isDefaultUnlocked(TableTheme.CLASSIC_NEON))
        assertTrue(ArenaConquestCatalog.isDefaultUnlocked(TableTheme.ARCADE_3D))
        assertTrue(ArenaConquestCatalog.isDefaultUnlocked(TableTheme.KIARA_CAT))
        assertEquals(0, ArenaConquestCatalog.getUnlockStageForTheme(TableTheme.CLASSIC_NEON))
        assertEquals(0, ArenaConquestCatalog.getUnlockStageForTheme(TableTheme.KIARA_CAT))
    }

    @Test
    fun testArenaUnlockingProgressionThroughRepository() = runBlocking {
        repository.ensureStagesInitialized()

        // Before completing stage 10, CYBER_PONG is locked
        assertFalse(repository.isThemeUnlocked(TableTheme.CYBER_PONG))
        val initialThemes = repository.unlockedThemesFlow.first()
        assertTrue(initialThemes.contains(TableTheme.CLASSIC_NEON))
        assertTrue(initialThemes.contains(TableTheme.KIARA_CAT))
        assertFalse(initialThemes.contains(TableTheme.CYBER_PONG))

        // Complete stage 10 with stars
        repository.recordStageResult(stageNumber = 10, scorePlayer = 3, scoreAi = 1)

        // Now CYBER_PONG is unlocked, but CYBER_SOCCER (stage 20) is still locked
        assertTrue(repository.isThemeUnlocked(TableTheme.CYBER_PONG))
        assertFalse(repository.isThemeUnlocked(TableTheme.CYBER_SOCCER))
        val updatedThemes = repository.unlockedThemesFlow.first()
        assertTrue(updatedThemes.contains(TableTheme.CYBER_PONG))
        assertFalse(updatedThemes.contains(TableTheme.CYBER_SOCCER))
    }
}
