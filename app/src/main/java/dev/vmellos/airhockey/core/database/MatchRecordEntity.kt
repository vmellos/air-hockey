package dev.vmellos.airhockey.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_records")
data class MatchRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameMode: String, // "SOLO", "EXTENDED_TABLE", "SPLIT_SCREEN"
    val aiDifficulty: String? = null, // "EASY", "MEDIUM", "HARD"
    val playerAName: String,
    val playerBName: String,
    val playerAScore: Int,
    val playerBScore: Int,
    val durationSeconds: Int,
    val winner: String, // "PLAYER_A", "PLAYER_B", "DRAW"
    val ratingDeltaA: Int = 0,
    val ratingDeltaB: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
