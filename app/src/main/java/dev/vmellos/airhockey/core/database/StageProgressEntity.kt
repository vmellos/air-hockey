package dev.vmellos.airhockey.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stage_progress")
data class StageProgressEntity(
    @PrimaryKey
    val stageNumber: Int,
    val starsEarned: Int = 0,
    val isUnlocked: Boolean = false,
    val bestScorePlayer: Int = 0,
    val bestScoreAi: Int = 0,
    val completedTimestamp: Long = 0L
)
