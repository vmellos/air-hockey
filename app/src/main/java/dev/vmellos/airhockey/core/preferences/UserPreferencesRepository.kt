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
import dev.vmellos.airhockey.core.catalog.canonical
import dev.vmellos.airhockey.core.catalog.preset
import dev.vmellos.airhockey.core.localization.LocaleHelper
import dev.vmellos.airhockey.core.playgames.CloudPreferencesPort
import dev.vmellos.airhockey.core.playgames.PlayGamesPreferencesPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "air_hockey_preferences")

class UserPreferencesRepository(private val context: Context) : CloudPreferencesPort {

    private object PreferencesKeys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
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
        val TABLE_THEME = stringPreferencesKey("table_theme")
        val ARENA_DIMENSION = stringPreferencesKey("arena_dimension")
        val NEON_BLOOM_INTENSITY = floatPreferencesKey("neon_bloom_intensity")
        val ARENA_3D_INTRO_ENABLED = booleanPreferencesKey("arena_3d_intro_enabled")
        val CAMERA_ZOOM_3D = stringPreferencesKey("camera_zoom_3d")
        val PUCK_SKIN = stringPreferencesKey("puck_skin")
        val MALLET_SKIN_P1 = stringPreferencesKey("mallet_skin_p1")
        val MALLET_SKIN_P2 = stringPreferencesKey("mallet_skin_p2")
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
            val musicEnabled = preferences[PreferencesKeys.MUSIC_ENABLED] ?: true
            val musicVolume = preferences[PreferencesKeys.MUSIC_VOLUME] ?: 0.10f
            val hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: false
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
            val targetScore = preferences[PreferencesKeys.TARGET_SCORE] ?: 3
            val matchTime = preferences[PreferencesKeys.MATCH_TIME_SECONDS] ?: 180
            val touchOffsetEnabled = preferences[PreferencesKeys.TOUCH_OFFSET_ENABLED] ?: true
            val puckSpeedStr = preferences[PreferencesKeys.PUCK_SPEED] ?: PuckSpeedPreset.NORMAL.name
            val puckSpeed = try {
                PuckSpeedPreset.valueOf(puckSpeedStr)
            } catch (_: Exception) {
                PuckSpeedPreset.NORMAL
            }
            val fxProfileStr = preferences[PreferencesKeys.VISUAL_EFFECTS_PROFILE] ?: VisualEffectsProfile.FULL.name
            val fxProfile = when (fxProfileStr) {
                VisualEffectsProfile.OFF.name -> VisualEffectsProfile.OFF
                else -> VisualEffectsProfile.FULL // Mapeia FULL, BALANCED legado ou desconhecido para FULL
            }
            val storedName = preferences[PreferencesKeys.PLAYER_NAME]
            val playerName = if (!storedName.isNullOrBlank()) storedName else getDefaultDevicePlayerName(context)

            val tableThemeStr = preferences[PreferencesKeys.TABLE_THEME] ?: TableTheme.CLASSIC_NEON.name
            val rawTableTheme = try { TableTheme.valueOf(tableThemeStr) } catch (_: Exception) { TableTheme.CLASSIC_NEON }

            val arenaDimStr = preferences[PreferencesKeys.ARENA_DIMENSION]
            val arenaDimension = if (arenaDimStr != null) {
                try { ArenaDimension.valueOf(arenaDimStr) } catch (_: Exception) { ArenaDimension.THREE_DIMENSIONAL }
            } else {
                ArenaDimension.THREE_DIMENSIONAL
            }
            val tableTheme = if (rawTableTheme == TableTheme.ARCADE_3D) TableTheme.CLASSIC_NEON else rawTableTheme

            val rawNeonBloom = preferences[PreferencesKeys.NEON_BLOOM_INTENSITY] ?: 0.05f
            val neonBloomIntensity = if (rawNeonBloom == 0.01f) 0.05f else rawNeonBloom.coerceIn(0.0f, 0.10f)
            val arena3dIntroEnabled = preferences[PreferencesKeys.ARENA_3D_INTRO_ENABLED] ?: true
            val cameraZoomStr = preferences[PreferencesKeys.CAMERA_ZOOM_3D] ?: CameraZoom3D.STANDARD.name
            val cameraZoom3d = try { CameraZoom3D.valueOf(cameraZoomStr) } catch (_: Exception) { CameraZoom3D.STANDARD }

            val puckSkinStr = preferences[PreferencesKeys.PUCK_SKIN] ?: PuckSkin.CLASSIC_PLASMA.name
            val puckSkin = try { PuckSkin.valueOf(puckSkinStr) } catch (_: Exception) { PuckSkin.CLASSIC_PLASMA }

            val malletP1Str = preferences[PreferencesKeys.MALLET_SKIN_P1] ?: MalletSkin.CLASSIC_NEON.name
            val malletP1 = try { MalletSkin.valueOf(malletP1Str) } catch (_: Exception) { MalletSkin.CLASSIC_NEON }

            val malletP2Str = preferences[PreferencesKeys.MALLET_SKIN_P2] ?: MalletSkin.CLASSIC_NEON.name
            val malletP2 = try { MalletSkin.valueOf(malletP2Str) } catch (_: Exception) { MalletSkin.CLASSIC_NEON }

            UserPreferences(
                appLanguage = lang,
                soundEnabled = soundEnabled,
                soundVolume = soundVolume,
                musicEnabled = musicEnabled,
                musicVolume = musicVolume,
                hapticsEnabled = hapticsEnabled,
                aiDifficulty = difficulty,
                bezelGapMm = bezelGapMm,
                tableAlignment = alignment,
                targetScore = targetScore,
                matchTimeSeconds = matchTime,
                touchOffsetEnabled = touchOffsetEnabled,
                puckSpeed = puckSpeed,
                visualEffectsProfile = fxProfile,
                playerName = playerName,
                tableTheme = tableTheme,
                arenaDimension = arenaDimension,
                neonBloomIntensity = neonBloomIntensity,
                arena3dIntroEnabled = arena3dIntroEnabled,
                cameraZoom3d = cameraZoom3d,
                puckSkin = puckSkin,
                player1MalletSkin = malletP1,
                player2MalletSkin = malletP2
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

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MUSIC_ENABLED] = enabled
        }
    }

    suspend fun setMusicVolume(volume: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MUSIC_VOLUME] = volume.coerceIn(0f, 1f)
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

    suspend fun setTableTheme(theme: TableTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TABLE_THEME] = theme.name
        }
    }

    suspend fun setPuckSkin(skin: PuckSkin) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PUCK_SKIN] = skin.name
        }
    }

    suspend fun setMalletSkinP1(skin: MalletSkin) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MALLET_SKIN_P1] = skin.name
        }
    }

    suspend fun setMalletSkinP2(skin: MalletSkin) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MALLET_SKIN_P2] = skin.name
        }
    }

    suspend fun applyThemePreset(theme: TableTheme) {
        val canonicalTheme = theme.canonical()
        val preset = canonicalTheme.preset()
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TABLE_THEME] = canonicalTheme.name
            preferences[PreferencesKeys.PUCK_SKIN] = preset.puckSkin.name
            preferences[PreferencesKeys.MALLET_SKIN_P1] = preset.malletSkin.name
            preferences[PreferencesKeys.MALLET_SKIN_P2] = preset.malletSkin.name
        }
    }

    suspend fun updateArenaDimension(dimension: ArenaDimension) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARENA_DIMENSION] = dimension.name
        }
    }

    suspend fun updateNeonBloomIntensity(intensity: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NEON_BLOOM_INTENSITY] = intensity.coerceIn(0.0f, 0.10f)
        }
    }

    suspend fun setArena3dIntroEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARENA_3D_INTRO_ENABLED] = enabled
        }
    }

    suspend fun updateCameraZoom3d(zoom: CameraZoom3D) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CAMERA_ZOOM_3D] = zoom.name
        }
    }

    override suspend fun readSyncSnapshot(): Pair<String, PlayGamesPreferencesPayload?> {
        val current = userPreferencesFlow.firstOrNull()
        val effectiveName = current?.playerName?.ifBlank { "Jogador" } ?: "Jogador"
        val payload = current?.let { p ->
            PlayGamesPreferencesPayload(
                tableTheme = p.tableTheme.name,
                puckSkin = p.puckSkin.name,
                malletSkinP1 = p.player1MalletSkin.name,
                malletSkinP2 = p.player2MalletSkin.name,
                arenaDimension = p.arenaDimension.name,
                arena3dIntroEnabled = p.arena3dIntroEnabled
            )
        }
        return effectiveName to payload
    }

    /**
     * Aplica de forma atômica todas as preferências vindas da nuvem em uma única transação de DataStore.
     */
    override suspend fun applyCloudSyncedPreferences(
        playerName: String?,
        syncedPrefs: PlayGamesPreferencesPayload?
    ) {
        context.dataStore.edit { preferences ->
            if (!playerName.isNullOrBlank() && playerName != "Jogador") {
                preferences[PreferencesKeys.PLAYER_NAME] = playerName.trim().take(24)
            }
            syncedPrefs?.let { p ->
                p.tableTheme?.let { name ->
                    runCatching { TableTheme.valueOf(name).canonical() }.getOrNull()?.let {
                        preferences[PreferencesKeys.TABLE_THEME] = it.name
                    }
                }
                p.puckSkin?.let { name ->
                    runCatching { PuckSkin.valueOf(name) }.getOrNull()?.let {
                        preferences[PreferencesKeys.PUCK_SKIN] = it.name
                    }
                }
                p.malletSkinP1?.let { name ->
                    runCatching { MalletSkin.valueOf(name) }.getOrNull()?.let {
                        preferences[PreferencesKeys.MALLET_SKIN_P1] = it.name
                    }
                }
                p.malletSkinP2?.let { name ->
                    runCatching { MalletSkin.valueOf(name) }.getOrNull()?.let {
                        preferences[PreferencesKeys.MALLET_SKIN_P2] = it.name
                    }
                }
                p.arenaDimension?.let { name ->
                    runCatching { ArenaDimension.valueOf(name) }.getOrNull()?.let {
                        preferences[PreferencesKeys.ARENA_DIMENSION] = it.name
                    }
                }
                p.arena3dIntroEnabled?.let { intro ->
                    preferences[PreferencesKeys.ARENA_3D_INTRO_ENABLED] = intro
                }
            }
        }
    }
}
