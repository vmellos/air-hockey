package dev.vmellos.airhockey.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.ads.AdBanner
import dev.vmellos.airhockey.core.localization.LocaleHelper
import dev.vmellos.airhockey.core.preferences.AiDifficulty
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesRepo = AirHockeyApp.instance.preferencesRepository
    val prefs by preferencesRepo.userPreferencesFlow.collectAsState(initial = dev.vmellos.airhockey.core.preferences.UserPreferences())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_title),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Seção Perfil do Jogador
        Text(
            text = stringResource(R.string.settings_profile),
            color = Color(0xFF00E5FF),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                var playerNameInput by remember { mutableStateOf(prefs.playerName) }
                var showSavedFeedback by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(prefs.playerName) {
                    if (playerNameInput.isEmpty() && prefs.playerName.isNotEmpty()) {
                        playerNameInput = prefs.playerName
                    }
                }

                Text(
                    text = stringResource(R.string.settings_player_name),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_player_name_desc),
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = playerNameInput,
                    onValueChange = { newValue ->
                        if (newValue.length <= 24) {
                            playerNameInput = newValue
                            errorMessage = null
                            showSavedFeedback = false
                        }
                    },
                    singleLine = true,
                    placeholder = {
                        Text(text = "Ex: Jogador 1", color = Color(0xFF64748B))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (showSavedFeedback) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.settings_player_name_saved),
                            color = Color(0xFF10B981),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val defaultName = preferencesRepo.resetPlayerNameToDefault()
                                playerNameInput = defaultName
                                errorMessage = null
                                showSavedFeedback = true
                                kotlinx.coroutines.delay(2500)
                                showSavedFeedback = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.settings_player_name_reset),
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            val trimmed = playerNameInput.trim()
                            if (trimmed.isEmpty()) {
                                errorMessage = context.getString(R.string.settings_player_name_empty)
                            } else {
                                scope.launch {
                                    preferencesRepo.setPlayerName(trimmed)
                                    errorMessage = null
                                    showSavedFeedback = true
                                    kotlinx.coroutines.delay(2500)
                                    showSavedFeedback = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0A0E1A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_player_name_save),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Seção Idioma (I18n do iptv_app)
        Text(
            text = stringResource(R.string.settings_language),
            color = Color(0xFF00E5FF),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                val languages = listOf(
                    LocaleHelper.SYSTEM_DEFAULT to stringResource(R.string.settings_language_system),
                    "pt-BR" to "Português (Brasil)",
                    "en" to "English",
                    "es" to "Español"
                )

                languages.forEach { (code, name) ->
                    val isSelected = prefs.appLanguage.equals(code, ignoreCase = true)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    preferencesRepo.setAppLanguage(code)
                                    LocaleHelper.applySystemLocale(context, code)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(text = name, color = Color.White, fontSize = 15.sp)
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                scope.launch {
                                    preferencesRepo.setAppLanguage(code)
                                    LocaleHelper.applySystemLocale(context, code)
                                }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E5FF))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Seção Áudio e Vibração
        Text(
            text = stringResource(R.string.settings_audio_haptics),
            color = Color(0xFF00E5FF),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.settings_sound_effects), color = Color.White, fontSize = 15.sp)
                    Switch(
                        checked = prefs.soundEnabled,
                        onCheckedChange = { scope.launch { preferencesRepo.setSoundEnabled(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x6600E5FF))
                    )
                }

                if (prefs.soundEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_sound_volume),
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Slider(
                        value = prefs.soundVolume,
                        onValueChange = { scope.launch { preferencesRepo.setSoundVolume(it) } },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.settings_haptics), color = Color.White, fontSize = 15.sp)
                    Switch(
                        checked = prefs.hapticsEnabled,
                        onCheckedChange = { scope.launch { preferencesRepo.setHapticsEnabled(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x6600E5FF))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Seção Regras de Jogo / IA
        Text(
            text = stringResource(R.string.settings_gameplay),
            color = Color(0xFF00E5FF),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_ai_difficulty),
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (diff in AiDifficulty.values()) {
                        val isSelected = prefs.aiDifficulty == diff
                        val label = when (diff) {
                            AiDifficulty.EASY -> stringResource(R.string.settings_ai_easy)
                            AiDifficulty.MEDIUM -> stringResource(R.string.settings_ai_medium)
                            AiDifficulty.HARD -> stringResource(R.string.settings_ai_hard)
                        }

                        Card(
                            onClick = { scope.launch { preferencesRepo.setAiDifficulty(diff) } },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth()
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF0A0E1A) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.settings_puck_speed),
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_puck_speed_desc),
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (speed in dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.values()) {
                        val isSelected = prefs.puckSpeed == speed
                        val label = when (speed) {
                            dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.SLOW -> stringResource(R.string.puck_speed_slow)
                            dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.NORMAL -> stringResource(R.string.puck_speed_normal)
                            dev.vmellos.airhockey.core.preferences.PuckSpeedPreset.FAST -> stringResource(R.string.puck_speed_fast)
                        }

                        Card(
                            onClick = { scope.launch { preferencesRepo.setPuckSpeed(speed) } },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth()
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF0A0E1A) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Seção Controles / Ergonomia de Toque
        Text(
            text = stringResource(R.string.settings_controls),
            color = Color(0xFF00E5FF),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_touch_offset),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_touch_offset_desc),
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = prefs.touchOffsetEnabled,
                        onCheckedChange = { scope.launch { preferencesRepo.setTouchOffsetEnabled(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x6600E5FF))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AdBanner(
            adManager = AirHockeyApp.instance.adManager,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
