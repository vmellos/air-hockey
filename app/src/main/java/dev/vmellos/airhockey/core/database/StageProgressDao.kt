package dev.vmellos.airhockey.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StageProgressDao {

    @Query("SELECT * FROM stage_progress ORDER BY stageNumber ASC")
    fun getAllStages(): Flow<List<StageProgressEntity>>

    @Query("SELECT * FROM stage_progress ORDER BY stageNumber ASC")
    suspend fun getAllStagesDirect(): List<StageProgressEntity>

    @Query("SELECT * FROM stage_progress WHERE stageNumber = :stageNumber LIMIT 1")
    fun getStage(stageNumber: Int): Flow<StageProgressEntity?>

    @Query("SELECT * FROM stage_progress WHERE stageNumber = :stageNumber LIMIT 1")
    suspend fun getStageDirect(stageNumber: Int): StageProgressEntity?

    @Query("SELECT COUNT(*) FROM stage_progress")
    suspend fun getStagesCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(stages: List<StageProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stages: List<StageProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStage(stage: StageProgressEntity)

    @Query("UPDATE stage_progress SET isUnlocked = 1 WHERE stageNumber = :stageNumber")
    suspend fun unlockStage(stageNumber: Int)

    @Query("SELECT SUM(starsEarned) FROM stage_progress")
    fun getTotalStars(): Flow<Int?>

    @Query("SELECT MAX(stageNumber) FROM stage_progress WHERE isUnlocked = 1")
    fun getHighestUnlockedStage(): Flow<Int?>
}
