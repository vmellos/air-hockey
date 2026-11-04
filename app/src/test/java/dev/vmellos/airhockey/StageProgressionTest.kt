package dev.vmellos.airhockey

import dev.vmellos.airhockey.core.database.StageProgressDao
import dev.vmellos.airhockey.core.database.StageProgressEntity
import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.core.preferences.PuckSpeedPreset
import dev.vmellos.airhockey.feature.stage.StageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
        assertEquals(1, repository.calculateStars(25, scorePlayer = 5, scoreAi = 4))
        assertEquals(0, repository.calculateStars(25, scorePlayer = 4, scoreAi = 5))
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
}
