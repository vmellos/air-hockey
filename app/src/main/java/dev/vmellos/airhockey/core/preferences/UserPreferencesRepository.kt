package dev.vmellos.airhockey.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.vmellos.airhockey.core.localization.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "air_hockey_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val AI_DIFFICULTY = stringPreferencesKey("ai_difficulty")
        val BEZEL_GAP_MM = floatPreferencesKey("bezel_gap_mm")
        val TABLE_ALIGNMENT = stringPreferencesKey("table_alignment")
        val TARGET_SCORE = intPreferencesKey("target_score")
        val MATCH_TIME_SECONDS = intPreferencesKey("match_time_seconds")
        val TOUCH_OFFSET_ENABLED = booleanPreferencesKey("touch_offset_enabled")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val lang = preferences[PreferencesKeys.APP_LANGUAGE] ?: LocaleHelper.SYSTEM_DEFAULT
            val soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: true
            val soundVolume = preferences[PreferencesKeys.SOUND_VOLUME] ?: 0.8f
            val hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true
            val difficultyStr = preferences[PreferencesKeys.AI_DIFFICULTY] ?: AiDifficulty.MEDIUM.name
            val difficulty = try {
                AiDifficulty.valueOf(difficultyStr)
            } catch (_: Exception) {
                AiDifficulty.MEDIUM
            }
            val bezelGapMm = preferences[PreferencesKeys.BEZEL_GAP_MM] ?: 8.0f
            val alignmentStr = preferences[PreferencesKeys.TABLE_ALIGNMENT] ?: TableAlignment.TOP_TO_TOP.name
            val alignment = try {
                TableAlignment.valueOf(alignmentStr)
            } catch (_: Exception) {
                TableAlignment.TOP_TO_TOP
            }
            val targetScore = preferences[PreferencesKeys.TARGET_SCORE] ?: 7
            val matchTime = preferences[PreferencesKeys.MATCH_TIME_SECONDS] ?: 180
            val touchOffsetEnabled = preferences[PreferencesKeys.TOUCH_OFFSET_ENABLED] ?: true

            UserPreferences(
                appLanguage = lang,
                soundEnabled = soundEnabled,
                soundVolume = soundVolume,
                hapticsEnabled = hapticsEnabled,
                aiDifficulty = difficulty,
                bezelGapMm = bezelGapMm,
                tableAlignment = alignment,
                targetScore = targetScore,
                matchTimeSeconds = matchTime,
                touchOffsetEnabled = touchOffsetEnabled
            )
        }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = languageCode
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setSoundVolume(volume: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_VOLUME] = volume.coerceIn(0f, 1f)
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setAiDifficulty(difficulty: AiDifficulty) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_DIFFICULTY] = difficulty.name
        }
    }

    suspend fun setBezelGapMm(gapMm: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BEZEL_GAP_MM] = gapMm.coerceIn(0f, 30f)
        }
    }

    suspend fun setTableAlignment(alignment: TableAlignment) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TABLE_ALIGNMENT] = alignment.name
        }
    }

    suspend fun setTargetScore(score: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TARGET_SCORE] = score.coerceIn(1, 21)
        }
    }

    suspend fun setMatchTimeSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MATCH_TIME_SECONDS] = seconds.coerceIn(60, 600)
        }
    }

    suspend fun setTouchOffsetEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOUCH_OFFSET_ENABLED] = enabled
        }
    }
}
