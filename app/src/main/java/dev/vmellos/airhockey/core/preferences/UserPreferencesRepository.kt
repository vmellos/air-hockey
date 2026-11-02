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
        val PUCK_SPEED = stringPreferencesKey("puck_speed")
        val VISUAL_EFFECTS_PROFILE = stringPreferencesKey("visual_effects_profile")
        val PLAYER_NAME = stringPreferencesKey("player_name")
    }

    companion object {
        fun getDefaultDevicePlayerName(context: Context): String {
            return try {
                val globalName = android.provider.Settings.Global.getString(context.contentResolver, "device_name")
                if (!globalName.isNullOrBlank()) return globalName
                val secureName = android.provider.Settings.Secure.getString(context.contentResolver, "bluetooth_name")
                if (!secureName.isNullOrBlank()) return secureName
                val model = android.os.Build.MODEL
                if (!model.isNullOrBlank()) return model
                "Jogador"
            } catch (_: Exception) {
                "Jogador"
            }
        }
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
            val puckSpeedStr = preferences[PreferencesKeys.PUCK_SPEED] ?: PuckSpeedPreset.NORMAL.name
            val puckSpeed = try {
                PuckSpeedPreset.valueOf(puckSpeedStr)
            } catch (_: Exception) {
                PuckSpeedPreset.NORMAL
            }
            val fxProfileStr = preferences[PreferencesKeys.VISUAL_EFFECTS_PROFILE] ?: VisualEffectsProfile.FULL.name
            val fxProfile = try {
                VisualEffectsProfile.valueOf(fxProfileStr)
            } catch (_: Exception) {
                VisualEffectsProfile.FULL
            }
            val storedName = preferences[PreferencesKeys.PLAYER_NAME]
            val playerName = if (!storedName.isNullOrBlank()) storedName else getDefaultDevicePlayerName(context)

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
                touchOffsetEnabled = touchOffsetEnabled,
                puckSpeed = puckSpeed,
                visualEffectsProfile = fxProfile,
                playerName = playerName
            )
        }

    suspend fun setPlayerName(name: String) {
        val trimmed = name.trim()
        context.dataStore.edit { preferences ->
            if (trimmed.isEmpty()) {
                preferences.remove(PreferencesKeys.PLAYER_NAME)
            } else {
                preferences[PreferencesKeys.PLAYER_NAME] = trimmed
            }
        }
    }

    suspend fun resetPlayerNameToDefault(): String {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.PLAYER_NAME)
        }
        return getDefaultDevicePlayerName(context)
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

    suspend fun setPuckSpeed(preset: PuckSpeedPreset) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PUCK_SPEED] = preset.name
        }
    }

    suspend fun setVisualEffectsProfile(profile: VisualEffectsProfile) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VISUAL_EFFECTS_PROFILE] = profile.name
        }
    }
}
