package dev.vmellos.airhockey.feature.customization.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.ads.AdBanner
import dev.vmellos.airhockey.core.preferences.ArenaDimension
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.ui.*
import dev.vmellos.airhockey.feature.stage.ArenaConquestCatalog
import kotlinx.coroutines.launch

private data class PreMatchArenaItem(
    val theme: TableTheme,
    val titleRes: Int,
    val icon: ImageVector,
    val primaryColor: Color,
    val unlockStage: Int
)

private data class PreMatchLockedDialogInfo(
    val titleRes: Int,
    val unlockStage: Int
)

/**
 * Seletor Pré-Jogo de Arenas (GLOSSARY.md).
 * 
 * Permite ao jogador ou Anfitrião (Host) inspecionar em tempo real com pré-visualização ao vivo
 * (3D orbital com giro contínuo ou 2D) o palco da partida antes de gerar códigos de pareamento
 * ou rolar a bola em confrontos locais.
 */
@Composable
fun PreMatchArenaSelectionScreen(
    title: String = stringResource(R.string.pre_match_arena_title),
    confirmButtonText: String = stringResource(R.string.pre_match_start_game),
    isSplitScreen: Boolean = false,
    onConfirm: (TableTheme) -> Unit,
    onBack: () -> Unit,
    onNavigateToStages: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val preferencesRepo = AirHockeyApp.instance.preferencesRepository
    val prefs by preferencesRepo.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val coroutineScope = rememberCoroutineScope()

    val stageRepo = AirHockeyApp.instance.stageRepository
    val unlockedThemes by stageRepo.unlockedThemesFlow.collectAsState(
        initial = setOf(TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D)
    )

    var selectedTheme by remember { mutableStateOf(prefs.tableTheme) }
    var selectedDimension by remember {
        mutableStateOf(prefs.arenaDimension)
    }
    var lockedDialogInfo by remember { mutableStateOf<PreMatchLockedDialogInfo?>(null) }

    val arenaItems = remember {
        listOf(
            PreMatchArenaItem(TableTheme.CLASSIC_NEON, R.string.theme_preset_classic, Icons.Default.SportsHockey, NeonCyan, 0),
            PreMatchArenaItem(TableTheme.CYBER_PONG, R.string.theme_preset_pong, Icons.Default.VideogameAsset, PongGreen, 10),
            PreMatchArenaItem(TableTheme.CYBER_SOCCER, R.string.theme_preset_cyber_soccer, Icons.Default.SportsSoccer, NeonEmerald, 20),
            PreMatchArenaItem(TableTheme.NEO_BASKETBALL, R.string.theme_preset_neo_basketball, Icons.Default.SportsBasketball, NeonAmber, 30),
            PreMatchArenaItem(TableTheme.CYBER_BASEBALL, R.string.theme_preset_baseball, Icons.Default.SportsBaseball, BaseballRed, 40),
            PreMatchArenaItem(TableTheme.GRIDIRON_FOOTBALL, R.string.theme_preset_gridiron, Icons.Default.SportsFootball, GridironCyan, 50),
            PreMatchArenaItem(TableTheme.GRAVITY_VOLLEY, R.string.theme_preset_gravity_volley, Icons.Default.SportsVolleyball, NeonViolet, 60),
            PreMatchArenaItem(TableTheme.RETRO_SYNTHWAVE, R.string.theme_preset_synthwave, Icons.Default.WbSunny, SynthPink, 70),
            PreMatchArenaItem(TableTheme.WASTELAND_APOCALYPSE, R.string.theme_preset_wasteland, Icons.Default.Whatshot, WastelandFlame, 80),
            PreMatchArenaItem(TableTheme.TOKAMAK_REACTOR, R.string.theme_preset_tokamak, Icons.Default.Bolt, TokamakRadiation, 90),
            PreMatchArenaItem(TableTheme.CYBER_COLOSSEUM, R.string.theme_preset_colosseum, Icons.Default.Shield, ColosseumGold, 100)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Cabeçalho Arcade
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xB3070D1A), CircleShape)
                        .border(1.2.dp, Color(0x6600E5FF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title.uppercase(),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 2. Vitrine Interativa de Pré-Visualização (3D com rotação contínua e gestos de pinça, ou 2D)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(CutCornerShape(12.dp))
                    .background(Color(0xFF0D1424))
                    .border(1.2.dp, Color(0xFF1E293B), CutCornerShape(12.dp))
            ) {
                if (selectedDimension == ArenaDimension.THREE_DIMENSIONAL && !isSplitScreen) {
                    Arena3DLivePreview(
                        tableTheme = selectedTheme,
                        puckSkin = prefs.puckSkin,
                        malletSkin = prefs.player1MalletSkin,
                        neonBloomIntensity = prefs.neonBloomIntensity,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ArenaLivePreview(
                        tableTheme = selectedTheme,
                        puckSkin = prefs.puckSkin,
                        malletSkin = prefs.player1MalletSkin,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Seletor de Dimensão Gráfica (2D / 3D)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val is3D = selectedDimension == ArenaDimension.THREE_DIMENSIONAL
                
                Surface(
                    onClick = { selectedDimension = ArenaDimension.TWO_DIMENSIONAL },
                    shape = CutCornerShape(8.dp),
                    color = if (!is3D) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF0D1424),
                    border = BorderStroke(1.2.dp, if (!is3D) Color(0xFF00E5FF) else Color(0xFF1E293B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewAgenda,
                            contentDescription = null,
                            tint = if (!is3D) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.pre_match_mode_2d).uppercase(),
                            color = if (!is3D) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Surface(
                    onClick = { selectedDimension = ArenaDimension.THREE_DIMENSIONAL },
                    shape = CutCornerShape(8.dp),
                    color = if (is3D) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF0D1424),
                    border = BorderStroke(1.2.dp, if (is3D) Color(0xFF00E5FF) else Color(0xFF1E293B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (is3D) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.pre_match_mode_3d).uppercase(),
                            color = if (is3D) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // 4. Carrossel Horizontal de Arenas Esportivas
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(arenaItems, key = { it.theme.name }) { item ->
                    val isLocked = item.unlockStage > 0 && !unlockedThemes.contains(item.theme)
                    val isSelected = selectedTheme == item.theme

                    val cardBorder = when {
                        isSelected -> item.primaryColor
                        isLocked -> Color(0x55FFB800)
                        else -> Color(0xFF1E293B)
                    }

                    val cardBg = when {
                        isSelected -> item.primaryColor.copy(alpha = 0.16f)
                        else -> Color(0xFF0D1424)
                    }

                    Card(
                        shape = CutCornerShape(8.dp),
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, cardBorder),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .width(136.dp)
                            .clickable {
                                selectedTheme = item.theme
                                if (isLocked) {
                                    lockedDialogInfo = PreMatchLockedDialogInfo(item.titleRes, item.unlockStage)
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CutCornerShape(6.dp))
                                        .background(item.primaryColor.copy(alpha = 0.20f))
                                        .border(1.dp, item.primaryColor.copy(alpha = 0.6f), CutCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (isLocked) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0x33FFB800), CircleShape)
                                            .border(1.dp, Color(0x88FFB800), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB800),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                } else if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(item.primaryColor.copy(alpha = 0.20f), CircleShape)
                                            .border(1.2.dp, item.primaryColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = item.primaryColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(item.titleRes),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 5. Botão de Ação Primário Arcade
            Button(
                onClick = {
                    val currentUnlocked = ArenaConquestCatalog.isDefaultUnlocked(selectedTheme) || unlockedThemes.contains(selectedTheme)
                    val finalTheme = if (currentUnlocked) selectedTheme else TableTheme.CLASSIC_NEON

                    coroutineScope.launch {
                        preferencesRepo.applyThemePreset(finalTheme)
                        preferencesRepo.updateArenaDimension(selectedDimension)
                    }
                    onConfirm(finalTheme)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = CutCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF070B14),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = confirmButtonText.uppercase(),
                    color = Color(0xFF070B14),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            AdBanner(
                adManager = AirHockeyApp.instance.adManager,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }

        // Diálogo Orientativo de Arena Bloqueada
        val dialogInfo = lockedDialogInfo
        if (dialogInfo != null) {
            AlertDialog(
                onDismissRequest = { lockedDialogInfo = null },
                shape = CutCornerShape(16.dp),
                containerColor = Color(0xF20B0F1C),
                titleContentColor = Color.White,
                textContentColor = Color(0xFF94A3B8),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.arena_locked_dialog_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.arena_locked_dialog_desc, dialogInfo.unlockStage),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            lockedDialogInfo = null
                            onNavigateToStages()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = CutCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.play_stages_mode).uppercase(),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { lockedDialogInfo = null }) {
                        Text(
                            text = stringResource(R.string.close),
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            )
        }
    }
}
