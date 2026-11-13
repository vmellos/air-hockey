package dev.vmellos.airhockey.feature.customization.ui

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
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
import dev.vmellos.airhockey.core.catalog.ArenaPresetCatalog
import dev.vmellos.airhockey.core.preferences.ArenaDimension
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.ui.*
import kotlinx.coroutines.launch

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
    isExtendedTable: Boolean = false,
    onConfirm: (TableTheme, ArenaDimension) -> Unit,
    onBack: () -> Unit,
    onNavigateToStages: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val preferencesRepo = AirHockeyApp.instance.preferencesRepository
    val prefs by preferencesRepo.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val coroutineScope = rememberCoroutineScope()

    val stageRepo = AirHockeyApp.instance.stageRepository
    val unlockedThemes by stageRepo.unlockedThemesFlow.collectAsState(
        initial = setOf(TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D, TableTheme.KIARA_CAT)
    )

    var selectedTheme by remember { mutableStateOf(prefs.tableTheme) }
    var selectedDimension by remember {
        mutableStateOf(prefs.arenaDimension)
    }
    var lockedDialogInfo by remember { mutableStateOf<LockedArenaInfo?>(null) }

    val arenaItems = ArenaKitCatalog.ALL_KITS
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLandscape) 4.dp else 8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(if (isLandscape) 36.dp else 40.dp)
                    .background(Color(0xB3070D1A), CircleShape)
                    .border(1.2.dp, Color(0x6600E5FF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = title.uppercase(),
                color = Color.White,
                fontSize = if (isLandscape) 15.sp else 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    val previewBoxContent = @Composable { boxModifier: Modifier ->
        val selectedKit = remember(selectedTheme) { selectedTheme.kit() }
        val previewPuckSkin = selectedKit.puckSkin
        val previewMalletSkin = selectedKit.malletSkin

        Box(
            modifier = boxModifier
                .clip(CutCornerShape(12.dp))
                .background(Color(0xFF0D1424))
                .border(1.2.dp, Color(0xFF1E293B), CutCornerShape(12.dp))
        ) {
            if (selectedDimension == ArenaDimension.THREE_DIMENSIONAL) {
                Arena3DLivePreview(
                    tableTheme = selectedTheme,
                    puckSkin = previewPuckSkin,
                    malletSkin = previewMalletSkin,
                    neonBloomIntensity = prefs.neonBloomIntensity,
                    isExtended = false,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ArenaLivePreview(
                    tableTheme = selectedTheme,
                    puckSkin = previewPuckSkin,
                    malletSkin = previewMalletSkin,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    val dimensionSelectorContent = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLandscape) 4.dp else 10.dp),
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
                    modifier = Modifier.padding(vertical = if (isLandscape) 6.dp else 10.dp),
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
                    modifier = Modifier.padding(vertical = if (isLandscape) 6.dp else 10.dp, horizontal = 4.dp),
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
    }

    val arenasCarouselContent = @Composable {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLandscape) 6.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(arenaItems, key = { it.theme.name }) { item ->
                val isLocked = item.unlockStage > 0 && !unlockedThemes.contains(item.theme)
                val isSelected = selectedTheme == item.theme

                val cardBorder = when {
                    isSelected -> item.visuals.primaryColor
                    isLocked -> Color(0x55FFB800)
                    else -> Color(0xFF1E293B)
                }

                val cardBg = when {
                    isSelected -> item.visuals.primaryColor.copy(alpha = 0.16f)
                    else -> Color(0xFF0D1424)
                }

                Card(
                    shape = CutCornerShape(8.dp),
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, cardBorder),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier
                        .width(if (isLandscape) 124.dp else 136.dp)
                        .clickable {
                            selectedTheme = item.theme
                            if (isLocked) {
                                lockedDialogInfo = LockedArenaInfo(item.titleRes, item.unlockStage)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (isLandscape) 6.dp else 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isLandscape) 28.dp else 32.dp)
                                    .clip(CutCornerShape(6.dp))
                                    .background(item.visuals.primaryColor.copy(alpha = 0.20f))
                                    .border(1.dp, item.visuals.primaryColor.copy(alpha = 0.6f), CutCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.visuals.primaryColor,
                                    modifier = Modifier.size(if (isLandscape) 16.dp else 18.dp)
                                )
                            }

                            if (isLocked) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isLandscape) 22.dp else 24.dp)
                                        .background(Color(0x33FFB800), CircleShape)
                                        .border(1.dp, Color(0x88FFB800), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB800),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            } else if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isLandscape) 22.dp else 24.dp)
                                        .background(item.visuals.primaryColor.copy(alpha = 0.20f), CircleShape)
                                        .border(1.2.dp, item.visuals.primaryColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = item.visuals.primaryColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))

                        Text(
                            text = stringResource(item.titleRes),
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    val actionButtonContent = @Composable {
        Button(
            onClick = {
                val currentUnlocked = ArenaPresetCatalog.isDefaultUnlocked(selectedTheme) || unlockedThemes.contains(selectedTheme)
                val finalTheme = if (currentUnlocked) selectedTheme else TableTheme.CLASSIC_NEON

                coroutineScope.launch {
                    preferencesRepo.applyThemePreset(finalTheme)
                    preferencesRepo.updateArenaDimension(selectedDimension)
                    onConfirm(finalTheme, selectedDimension)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            shape = CutCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isLandscape) 44.dp else 50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF070B14),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = confirmButtonText.uppercase(),
                color = Color(0xFF070B14),
                fontSize = if (isLandscape) 14.sp else 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .safeDrawingPadding()
    ) {
        if (isLandscape) {
            // Layout Bicolunar no Seletor Pré-Jogo em Modo Paisagem (ADR 0037)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Coluna Esquerda: Vitrine de Pré-Visualização com altura total e alternador 2D/3D
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    previewBoxContent(Modifier.fillMaxWidth().weight(1f))
                    Spacer(modifier = Modifier.height(4.dp))
                    dimensionSelectorContent()
                }

                // Coluna Direita: Cabeçalho, Carrossel de Arenas, Botão de Ação e Anúncio em Banner
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        headerContent()
                        arenasCarouselContent()
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        actionButtonContent()
                        Spacer(modifier = Modifier.height(4.dp))
                        AdBanner(
                            adManager = AirHockeyApp.instance.adManager,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp)
                        )
                    }
                }
            }
        } else {
            // Modo Retrato: Coluna Única Padrão
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                headerContent()
                previewBoxContent(Modifier.fillMaxWidth().weight(1f))
                Spacer(modifier = Modifier.height(10.dp))
                dimensionSelectorContent()
                arenasCarouselContent()
                actionButtonContent()
                Spacer(modifier = Modifier.height(6.dp))
                AdBanner(
                    adManager = AirHockeyApp.instance.adManager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }
        }

        // Diálogo Orientativo de Arena Bloqueada
        val dialogInfo = lockedDialogInfo
        if (dialogInfo != null) {
            LockedArenaDialog(
                info = dialogInfo,
                onDismiss = { lockedDialogInfo = null },
                onNavigateToStages = {
                    lockedDialogInfo = null
                    onNavigateToStages()
                }
            )
        }
    }
}
