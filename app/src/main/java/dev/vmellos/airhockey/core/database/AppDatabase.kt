package dev.vmellos.airhockey.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MatchRecordEntity::class,
        PlayerProfileStatsEntity::class,
        StageProgressEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun stageProgressDao(): StageProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stage_progress` (
                        `stageNumber` INTEGER NOT NULL,
                        `starsEarned` INTEGER NOT NULL,
                        `isUnlocked` INTEGER NOT NULL,
                        `bestScorePlayer` INTEGER NOT NULL,
                        `bestScoreAi` INTEGER NOT NULL,
                        `completedTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`stageNumber`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "airhockey_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
