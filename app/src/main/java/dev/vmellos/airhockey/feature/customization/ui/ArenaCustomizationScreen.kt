package dev.vmellos.airhockey.feature.customization.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.ui.CourtDark
import dev.vmellos.airhockey.feature.game.ui.CyberTurfDark
import dev.vmellos.airhockey.feature.game.ui.NeonAmber
import dev.vmellos.airhockey.feature.game.ui.NeonAmberBright
import dev.vmellos.airhockey.feature.game.ui.NeonCyan
import dev.vmellos.airhockey.feature.game.ui.NeonCyanBright
import dev.vmellos.airhockey.feature.game.ui.NeonEmerald
import dev.vmellos.airhockey.feature.game.ui.NeonEmeraldBright
import dev.vmellos.airhockey.feature.game.ui.NeonMagenta
import dev.vmellos.airhockey.feature.game.ui.NeonMagentaBright
import dev.vmellos.airhockey.feature.game.ui.NeonOrange
import dev.vmellos.airhockey.feature.game.ui.NeonOrangeBright
import dev.vmellos.airhockey.feature.game.ui.NeonRuby
import dev.vmellos.airhockey.feature.game.ui.NeonRubyBright
import dev.vmellos.airhockey.feature.game.ui.NeonViolet
import dev.vmellos.airhockey.feature.game.ui.NeonVioletBright
import dev.vmellos.airhockey.feature.game.ui.TableBgDark
import dev.vmellos.airhockey.feature.game.ui.VolleySpaceDark
import dev.vmellos.airhockey.feature.game.ui.drawAngledGoal
import dev.vmellos.airhockey.feature.game.ui.drawBasketballGoal
import dev.vmellos.airhockey.feature.game.ui.drawChamferedTable
import dev.vmellos.airhockey.feature.game.ui.drawCyberSoccerGoal
import dev.vmellos.airhockey.feature.game.ui.drawCyberSoccerPuck
import dev.vmellos.airhockey.feature.game.ui.drawCyberSoccerTable
import dev.vmellos.airhockey.feature.game.ui.drawCyberStrikerMallet
import dev.vmellos.airhockey.feature.game.ui.drawDunkStrikerMallet
import dev.vmellos.airhockey.feature.game.ui.drawEnergyRingPuck
import dev.vmellos.airhockey.feature.game.ui.drawGravityVolleyPuck
import dev.vmellos.airhockey.feature.game.ui.drawGravityVolleyTable
import dev.vmellos.airhockey.feature.game.ui.drawNeoBasketballPuck
import dev.vmellos.airhockey.feature.game.ui.drawNeoBasketballTable
import dev.vmellos.airhockey.feature.game.ui.drawPseudo3DMallet
import dev.vmellos.airhockey.feature.game.ui.drawSpikeEmitterMallet
import dev.vmellos.airhockey.feature.game.ui.drawVolleyGoal
import kotlinx.coroutines.launch

private data class ThemeBundleItem(
    val id: String,
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tableTheme: TableTheme,
    val puckSkin: PuckSkin,
    val malletSkin: MalletSkin,
    val isComingSoon: Boolean = false
)

private data class TableItem(
    val theme: TableTheme,
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color
)

private data class PuckItem(
    val skin: PuckSkin,
    val titleRes: Int,
    val icon: ImageVector,
    val primaryColor: Color
)

private data class MalletItem(
    val skin: MalletSkin,
    val titleRes: Int,
    val icon: ImageVector,
    val primaryColor: Color
)

@Composable
fun ArenaCustomizationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferencesRepo = AirHockeyApp.instance.preferencesRepository
    val prefs by preferencesRepo.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Estado da categoria selecionada (0: Conjuntos, 1: Mesas, 2: Discos, 3: Rebatedores)
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    // Estado de pré-visualização ao vivo
    var previewTableTheme by remember { mutableStateOf(prefs.tableTheme) }
    var previewPuckSkin by remember { mutableStateOf(prefs.puckSkin) }
    var previewMalletSkin by remember { mutableStateOf(prefs.player1MalletSkin) }

    // Sincroniza pré-visualização quando as preferências são atualizadas externamente
    LaunchedEffect(prefs.tableTheme, prefs.puckSkin, prefs.player1MalletSkin) {
        previewTableTheme = prefs.tableTheme
        previewPuckSkin = prefs.puckSkin
        previewMalletSkin = prefs.player1MalletSkin
    }

    val bundleItems = remember {
        listOf(
            ThemeBundleItem(
                id = "classic",
                titleRes = R.string.theme_preset_classic,
                descRes = R.string.theme_preset_classic_desc,
                icon = Icons.Default.SportsHockey,
                primaryColor = NeonCyan,
                secondaryColor = NeonOrange,
                tableTheme = TableTheme.CLASSIC_NEON,
                puckSkin = PuckSkin.CLASSIC_PLASMA,
                malletSkin = MalletSkin.CLASSIC_NEON
            ),
            ThemeBundleItem(
                id = "cyber_soccer",
                titleRes = R.string.theme_preset_cyber_soccer,
                descRes = R.string.theme_preset_cyber_soccer_desc,
                icon = Icons.Default.SportsSoccer,
                primaryColor = NeonEmerald,
                secondaryColor = NeonRuby,
                tableTheme = TableTheme.CYBER_SOCCER,
                puckSkin = PuckSkin.CYBER_SOCCER_BALL,
                malletSkin = MalletSkin.MAGNETIC_STRIKER
            ),
            ThemeBundleItem(
                id = "neo_basketball",
                titleRes = R.string.theme_preset_neo_basketball,
                descRes = R.string.theme_preset_neo_basketball_desc,
                icon = Icons.Default.SportsBasketball,
                primaryColor = NeonAmber,
                secondaryColor = NeonCyan,
                tableTheme = TableTheme.NEO_BASKETBALL,
                puckSkin = PuckSkin.NEO_BASKETBALL,
                malletSkin = MalletSkin.DUNK_STRIKER
            ),
            ThemeBundleItem(
                id = "gravity_volley",
                titleRes = R.string.theme_preset_gravity_volley,
                descRes = R.string.theme_preset_gravity_volley_desc,
                icon = Icons.Default.SportsVolleyball,
                primaryColor = NeonViolet,
                secondaryColor = NeonMagenta,
                tableTheme = TableTheme.GRAVITY_VOLLEY,
                puckSkin = PuckSkin.GRAVITY_VOLLEY,
                malletSkin = MalletSkin.SPIKE_EMITTER
            ),
            ThemeBundleItem(
                id = "gridiron_football",
                titleRes = R.string.customization_custom_preset,
                descRes = R.string.customization_custom_preset,
                icon = Icons.Default.SportsFootball,
                primaryColor = Color(0xFF00E5FF),
                secondaryColor = Color(0xFFFFB300),
                tableTheme = TableTheme.CLASSIC_NEON,
                puckSkin = PuckSkin.CLASSIC_PLASMA,
                malletSkin = MalletSkin.CLASSIC_NEON,
                isComingSoon = true
            ),
            ThemeBundleItem(
                id = "cyber_baseball",
                titleRes = R.string.customization_custom_preset,
                descRes = R.string.customization_custom_preset,
                icon = Icons.Default.SportsBaseball,
                primaryColor = Color(0xFFE040FB),
                secondaryColor = Color(0xFF00E676),
                tableTheme = TableTheme.CLASSIC_NEON,
                puckSkin = PuckSkin.CLASSIC_PLASMA,
                malletSkin = MalletSkin.CLASSIC_NEON,
                isComingSoon = true
            )
        )
    }

    val tableItems = remember {
        listOf(
            TableItem(
                theme = TableTheme.CLASSIC_NEON,
                titleRes = R.string.theme_preset_classic,
                descRes = R.string.theme_preset_classic_desc,
                icon = Icons.Default.SportsHockey,
                primaryColor = NeonCyan,
                secondaryColor = NeonOrange
            ),
            TableItem(
                theme = TableTheme.CYBER_SOCCER,
                titleRes = R.string.theme_preset_cyber_soccer,
                descRes = R.string.theme_preset_cyber_soccer_desc,
                icon = Icons.Default.SportsSoccer,
                primaryColor = NeonEmerald,
                secondaryColor = NeonRuby
            ),
            TableItem(
                theme = TableTheme.NEO_BASKETBALL,
                titleRes = R.string.theme_preset_neo_basketball,
                descRes = R.string.theme_preset_neo_basketball_desc,
                icon = Icons.Default.SportsBasketball,
                primaryColor = NeonAmber,
                secondaryColor = NeonCyan
            ),
            TableItem(
                theme = TableTheme.GRAVITY_VOLLEY,
                titleRes = R.string.theme_preset_gravity_volley,
                descRes = R.string.theme_preset_gravity_volley_desc,
                icon = Icons.Default.SportsVolleyball,
                primaryColor = NeonViolet,
                secondaryColor = NeonMagenta
            )
        )
    }

    val puckItems = remember {
        listOf(
            PuckItem(
                skin = PuckSkin.CLASSIC_PLASMA,
                titleRes = R.string.skin_puck_classic,
                icon = Icons.Default.SportsHockey,
                primaryColor = NeonCyan
            ),
            PuckItem(
                skin = PuckSkin.CYBER_SOCCER_BALL,
                titleRes = R.string.skin_puck_soccer,
                icon = Icons.Default.SportsSoccer,
                primaryColor = NeonEmerald
            ),
            PuckItem(
                skin = PuckSkin.NEO_BASKETBALL,
                titleRes = R.string.skin_puck_basketball,
                icon = Icons.Default.SportsBasketball,
                primaryColor = NeonAmber
            ),
            PuckItem(
                skin = PuckSkin.GRAVITY_VOLLEY,
                titleRes = R.string.skin_puck_volley,
                icon = Icons.Default.SportsVolleyball,
                primaryColor = NeonViolet
            )
        )
    }

    val malletItems = remember {
        listOf(
            MalletItem(
                skin = MalletSkin.CLASSIC_NEON,
                titleRes = R.string.skin_mallet_classic,
                icon = Icons.Default.SportsHockey,
                primaryColor = NeonCyan
            ),
            MalletItem(
                skin = MalletSkin.MAGNETIC_STRIKER,
                titleRes = R.string.skin_mallet_soccer,
                icon = Icons.Default.SportsSoccer,
                primaryColor = NeonEmerald
            ),
            MalletItem(
                skin = MalletSkin.DUNK_STRIKER,
                titleRes = R.string.skin_mallet_basketball,
                icon = Icons.Default.SportsBasketball,
                primaryColor = NeonAmber
            ),
            MalletItem(
                skin = MalletSkin.SPIKE_EMITTER,
                titleRes = R.string.skin_mallet_volley,
                icon = Icons.Default.SportsVolleyball,
                primaryColor = NeonViolet
            )
        )
    }

    // Nome descritivo do kit atualmente ativo
    val currentActivePresetName = when {
        prefs.tableTheme == TableTheme.CLASSIC_NEON &&
                prefs.puckSkin == PuckSkin.CLASSIC_PLASMA &&
                prefs.player1MalletSkin == MalletSkin.CLASSIC_NEON -> stringResource(R.string.theme_preset_classic)
        prefs.tableTheme == TableTheme.CYBER_SOCCER &&
                prefs.puckSkin == PuckSkin.CYBER_SOCCER_BALL &&
                prefs.player1MalletSkin == MalletSkin.MAGNETIC_STRIKER -> stringResource(R.string.theme_preset_cyber_soccer)
        prefs.tableTheme == TableTheme.NEO_BASKETBALL &&
                prefs.puckSkin == PuckSkin.NEO_BASKETBALL &&
                prefs.player1MalletSkin == MalletSkin.DUNK_STRIKER -> stringResource(R.string.theme_preset_neo_basketball)
        prefs.tableTheme == TableTheme.GRAVITY_VOLLEY &&
                prefs.puckSkin == PuckSkin.GRAVITY_VOLLEY &&
                prefs.player1MalletSkin == MalletSkin.SPIKE_EMITTER -> stringResource(R.string.theme_preset_gravity_volley)
        else -> stringResource(R.string.customization_custom_preset)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B16))
            .safeDrawingPadding()
    ) {
        // 1. Barra de Navegação Superior (Padrão da Indústria)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x331E293B), CircleShape)
                    .border(1.dp, Color(0x66334155), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.customization_screen_title),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.settings_theme_active_indicator, currentActivePresetName),
                    color = Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 2. Vitrine de Pré-Visualização Dinâmica da Arena (Live Arena Preview)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .height(210.dp)
                .clip(CutCornerShape(12.dp))
                .background(Color(0xFF0B1220))
                .border(1.2.dp, Color(0xFF1E293B), CutCornerShape(12.dp))
        ) {
            ArenaLivePreview(
                tableTheme = previewTableTheme,
                puckSkin = previewPuckSkin,
                malletSkin = previewMalletSkin,
                modifier = Modifier.fillMaxSize()
            )

            // Distintivo "PRÉ-VISUALIZAÇÃO AO VIVO"
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color(0xCC070E1A), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF00E676), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.customization_live_preview),
                    color = Color(0xFFE2E8F0),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Barra de Categorias Segmentadas (Abas com Ícones Vetoriais)
        val categoryTitles = listOf(
            Pair(stringResource(R.string.customization_tab_bundles), Icons.Default.Layers),
            Pair(stringResource(R.string.customization_tab_tables), Icons.Default.ViewAgenda),
            Pair(stringResource(R.string.customization_tab_pucks), Icons.Default.Adjust),
            Pair(stringResource(R.string.customization_tab_mallets), Icons.Default.SportsEsports)
        )

        TabRow(
            selectedTabIndex = selectedCategoryIndex,
            containerColor = Color(0xFF0B1322),
            contentColor = Color(0xFF00E5FF),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                    height = 3.dp,
                    color = Color(0xFF00E5FF)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(CutCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E293B), CutCornerShape(8.dp))
        ) {
            categoryTitles.forEachIndexed { index, (title, icon) ->
                val isSelected = selectedCategoryIndex == index
                Tab(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedCategoryIndex = index
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Lista Modular de Itens da Categoria Selecionada
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedCategoryIndex) {
                0 -> {
                    // Categoria: CONJUNTOS COMPLETOS
                    items(bundleItems, key = { it.id }) { bundle ->
                        val isEquipped = !bundle.isComingSoon &&
                                prefs.tableTheme == bundle.tableTheme &&
                                prefs.puckSkin == bundle.puckSkin &&
                                prefs.player1MalletSkin == bundle.malletSkin

                        BundleCard(
                            bundle = bundle,
                            isEquipped = isEquipped,
                            onPreview = {
                                if (!bundle.isComingSoon) {
                                    previewTableTheme = bundle.tableTheme
                                    previewPuckSkin = bundle.puckSkin
                                    previewMalletSkin = bundle.malletSkin
                                }
                            },
                            onEquip = {
                                if (!bundle.isComingSoon) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    previewTableTheme = bundle.tableTheme
                                    previewPuckSkin = bundle.puckSkin
                                    previewMalletSkin = bundle.malletSkin
                                    coroutineScope.launch {
                                        preferencesRepo.applyThemePreset(bundle.tableTheme)
                                    }
                                }
                            }
                        )
                    }
                }
                1 -> {
                    // Categoria: MESAS
                    items(tableItems, key = { it.theme.name }) { tableItem ->
                        val isEquipped = prefs.tableTheme == tableItem.theme

                        SingleItemCard(
                            title = stringResource(tableItem.titleRes),
                            description = stringResource(tableItem.descRes),
                            icon = tableItem.icon,
                            primaryColor = tableItem.primaryColor,
                            secondaryColor = tableItem.secondaryColor,
                            isEquipped = isEquipped,
                            onPreview = {
                                previewTableTheme = tableItem.theme
                            },
                            onEquip = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                previewTableTheme = tableItem.theme
                                coroutineScope.launch {
                                    preferencesRepo.setTableTheme(tableItem.theme)
                                }
                            }
                        )
                    }
                }
                2 -> {
                    // Categoria: DISCOS
                    items(puckItems, key = { it.skin.name }) { puckItem ->
                        val isEquipped = prefs.puckSkin == puckItem.skin

                        SingleItemCard(
                            title = stringResource(puckItem.titleRes),
                            description = stringResource(R.string.theme_puck_label),
                            icon = puckItem.icon,
                            primaryColor = puckItem.primaryColor,
                            secondaryColor = Color.White,
                            isEquipped = isEquipped,
                            onPreview = {
                                previewPuckSkin = puckItem.skin
                            },
                            onEquip = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                previewPuckSkin = puckItem.skin
                                coroutineScope.launch {
                                    preferencesRepo.setPuckSkin(puckItem.skin)
                                }
                            }
                        )
                    }
                }
                3 -> {
                    // Categoria: REBATEDORES
                    items(malletItems, key = { it.skin.name }) { malletItem ->
                        val isEquipped = prefs.player1MalletSkin == malletItem.skin

                        SingleItemCard(
                            title = stringResource(malletItem.titleRes),
                            description = stringResource(R.string.theme_mallet_label),
                            icon = malletItem.icon,
                            primaryColor = malletItem.primaryColor,
                            secondaryColor = Color.White,
                            isEquipped = isEquipped,
                            onPreview = {
                                previewMalletSkin = malletItem.skin
                            },
                            onEquip = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                previewMalletSkin = malletItem.skin
                                coroutineScope.launch {
                                    preferencesRepo.setMalletSkinP1(malletItem.skin)
                                    preferencesRepo.setMalletSkinP2(malletItem.skin)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renderizador de Pré-Visualização Dinâmica em Canvas da Arena completa.
 */
@Composable
private fun ArenaLivePreview(
    tableTheme: TableTheme,
    puckSkin: PuckSkin,
    malletSkin: MalletSkin,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "previewPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "puckPulse"
    )

    val bgColor = when (tableTheme) {
        TableTheme.CYBER_SOCCER -> CyberTurfDark
        TableTheme.NEO_BASKETBALL -> CourtDark
        TableTheme.GRAVITY_VOLLEY -> VolleySpaceDark
        TableTheme.CLASSIC_NEON -> TableBgDark
    }

    val (p1Color, p1Rim) = when (malletSkin) {
        MalletSkin.MAGNETIC_STRIKER -> Pair(NeonEmerald, NeonEmeraldBright)
        MalletSkin.DUNK_STRIKER -> Pair(NeonAmber, NeonAmberBright)
        MalletSkin.SPIKE_EMITTER -> Pair(NeonViolet, NeonVioletBright)
        MalletSkin.CLASSIC_NEON -> Pair(NeonCyan, NeonCyanBright)
    }

    val (p2Color, p2Rim) = when (malletSkin) {
        MalletSkin.MAGNETIC_STRIKER -> Pair(NeonRuby, NeonRubyBright)
        MalletSkin.DUNK_STRIKER -> Pair(NeonCyan, NeonCyanBright)
        MalletSkin.SPIKE_EMITTER -> Pair(NeonMagenta, NeonMagentaBright)
        MalletSkin.CLASSIC_NEON -> Pair(NeonOrange, NeonOrangeBright)
    }

    val puckAccent = when (puckSkin) {
        PuckSkin.CYBER_SOCCER_BALL -> NeonEmerald
        PuckSkin.NEO_BASKETBALL -> NeonAmber
        PuckSkin.GRAVITY_VOLLEY -> NeonViolet
        PuckSkin.CLASSIC_PLASMA -> NeonCyan
    }

    Box(
        modifier = modifier
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val padding = 10f
            val availableW = w - padding * 2
            val availableH = h - padding * 2

            // Razão fixa do Air Hockey (1000 virtual units por 2000 virtual units)
            val scale = minOf(availableW / 1000f, availableH / 2000f)
            val tableW = 1000f * scale
            val tableH = 2000f * scale
            val left = (w - tableW) / 2f
            val top = (h - tableH) / 2f

            val goalW = 320f * scale
            val goalLeft = left + (tableW - goalW) / 2f

            // 1. Desenha a mesa
            when (tableTheme) {
                TableTheme.CYBER_SOCCER -> drawCyberSoccerTable(
                    left = left,
                    top = top,
                    width = tableW,
                    height = tableH,
                    scale = scale,
                    isExtendedHost = false,
                    isExtendedGuest = false,
                    isConnectedGuest = false
                )
                TableTheme.NEO_BASKETBALL -> drawNeoBasketballTable(
                    left = left,
                    top = top,
                    width = tableW,
                    height = tableH,
                    scale = scale,
                    isExtendedHost = false,
                    isExtendedGuest = false,
                    isConnectedGuest = false
                )
                TableTheme.GRAVITY_VOLLEY -> drawGravityVolleyTable(
                    left = left,
                    top = top,
                    width = tableW,
                    height = tableH,
                    scale = scale,
                    isExtendedHost = false,
                    isExtendedGuest = false,
                    isConnectedGuest = false
                )
                TableTheme.CLASSIC_NEON -> drawChamferedTable(
                    left = left,
                    top = top,
                    width = tableW,
                    height = tableH,
                    scale = scale,
                    isExtendedHost = false,
                    isExtendedGuest = false,
                    isConnectedGuest = false
                )
            }

            // 2. Desenha as metas
            when (tableTheme) {
                TableTheme.CYBER_SOCCER -> {
                    drawCyberSoccerGoal(goalLeft, goalW, top + tableH, scale, p1Color, isTop = false)
                    drawCyberSoccerGoal(goalLeft, goalW, top, scale, p2Color, isTop = true)
                }
                TableTheme.NEO_BASKETBALL -> {
                    drawBasketballGoal(goalLeft, goalW, top + tableH, scale, p1Color, isTop = false)
                    drawBasketballGoal(goalLeft, goalW, top, scale, p2Color, isTop = true)
                }
                TableTheme.GRAVITY_VOLLEY -> {
                    drawVolleyGoal(goalLeft, goalW, top + tableH, scale, p1Color, isTop = false)
                    drawVolleyGoal(goalLeft, goalW, top, scale, p2Color, isTop = true)
                }
                TableTheme.CLASSIC_NEON -> {
                    drawAngledGoal(goalLeft, goalW, top + tableH, scale, NeonCyan, isTop = false)
                    drawAngledGoal(goalLeft, goalW, top, scale, NeonOrange, isTop = true)
                }
            }

            // 3. Rebatedor P1 (Inferior)
            val p1X = left + tableW / 2f
            val p1Y = top + tableH * 0.78f
            val malletRadius = 64f * scale
            when (malletSkin) {
                MalletSkin.MAGNETIC_STRIKER -> drawCyberStrikerMallet(p1X, p1Y, malletRadius, p1Color, p1Rim, scale)
                MalletSkin.DUNK_STRIKER -> drawDunkStrikerMallet(p1X, p1Y, malletRadius, p1Color, p1Rim, scale)
                MalletSkin.SPIKE_EMITTER -> drawSpikeEmitterMallet(p1X, p1Y, malletRadius, p1Color, p1Rim, scale)
                MalletSkin.CLASSIC_NEON -> drawPseudo3DMallet(p1X, p1Y, malletRadius, p1Color, p1Rim, scale)
            }

            // 4. Rebatedor P2 (Superior)
            val p2X = left + tableW / 2f
            val p2Y = top + tableH * 0.22f
            when (malletSkin) {
                MalletSkin.MAGNETIC_STRIKER -> drawCyberStrikerMallet(p2X, p2Y, malletRadius, p2Color, p2Rim, scale)
                MalletSkin.DUNK_STRIKER -> drawDunkStrikerMallet(p2X, p2Y, malletRadius, p2Color, p2Rim, scale)
                MalletSkin.SPIKE_EMITTER -> drawSpikeEmitterMallet(p2X, p2Y, malletRadius, p2Color, p2Rim, scale)
                MalletSkin.CLASSIC_NEON -> drawPseudo3DMallet(p2X, p2Y, malletRadius, p2Color, p2Rim, scale)
            }

            // 5. Disco de Jogo (Centro com pulso animado)
            val puckX = left + tableW / 2f
            val puckY = top + tableH * 0.50f
            val puckRadius = 36f * scale * pulseScale
            when (puckSkin) {
                PuckSkin.CYBER_SOCCER_BALL -> drawCyberSoccerPuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.NEO_BASKETBALL -> drawNeoBasketballPuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.GRAVITY_VOLLEY -> drawGravityVolleyPuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.CLASSIC_PLASMA -> drawEnergyRingPuck(puckX, puckY, puckRadius, puckAccent, scale)
            }
        }
    }
}

/**
 * Card de Conjunto Temático Completo (Mesa + Disco + Rebatedor).
 */
@Composable
private fun BundleCard(
    bundle: ThemeBundleItem,
    isEquipped: Boolean,
    onPreview: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isEquipped) bundle.primaryColor else Color(0xFF1E293B)
    val containerBg = if (isEquipped) Color(0xFF111D33) else Color(0xFF0D1424)

    Card(
        shape = CutCornerShape(10.dp),
        border = BorderStroke(if (isEquipped) 1.5.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !bundle.isComingSoon, onClick = onPreview)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone do tema com fundo gradiente
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CutCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(bundle.primaryColor.copy(alpha = 0.35f), bundle.secondaryColor.copy(alpha = 0.15f))
                        )
                    )
                    .border(1.dp, bundle.primaryColor.copy(alpha = 0.5f), CutCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = bundle.icon,
                    contentDescription = null,
                    tint = bundle.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Textos informativos
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (bundle.isComingSoon) {
                            if (bundle.id == "gridiron_football") "Gridiron Sci-Fi" else "Cyber Diamond"
                        } else {
                            stringResource(bundle.titleRes)
                        },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (bundle.isComingSoon) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EM BREVE",
                            color = Color(0xFFFFB800),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0x33FFB800), RoundedCornerShape(4.dp))
                                .border(0.8.dp, Color(0x66FFB800), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (bundle.isComingSoon) {
                        if (bundle.id == "gridiron_football") {
                            "Arena de futebol americano com jardas neon e puck oval de precisão"
                        } else {
                            "Diamante holográfico de beisebol com bases dinâmicas e rebatedores slugger"
                        }
                    } else {
                        stringResource(bundle.descRes)
                    },
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Ação: Botão Equipado / Equipar
            if (bundle.isComingSoon) {
                Text(
                    text = "---",
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (isEquipped) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0x2800E5FF), CutCornerShape(6.dp))
                        .border(1.dp, Color(0xFF00E5FF), CutCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings_theme_equipped),
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onEquip,
                    shape = CutCornerShape(6.dp),
                    border = BorderStroke(1.dp, bundle.primaryColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = bundle.primaryColor.copy(alpha = 0.12f)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme_equip_action),
                        color = bundle.primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Card individual para Mesas, Discos e Rebatedores modulares.
 */
@Composable
private fun SingleItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    primaryColor: Color,
    secondaryColor: Color,
    isEquipped: Boolean,
    onPreview: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isEquipped) primaryColor else Color(0xFF1E293B)
    val containerBg = if (isEquipped) Color(0xFF111D33) else Color(0xFF0D1424)

    Card(
        shape = CutCornerShape(10.dp),
        border = BorderStroke(if (isEquipped) 1.5.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPreview)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CutCornerShape(8.dp))
                    .background(primaryColor.copy(alpha = 0.20f))
                    .border(1.dp, primaryColor.copy(alpha = 0.6f), CutCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isEquipped) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0x2800E5FF), CutCornerShape(6.dp))
                        .border(1.dp, Color(0xFF00E5FF), CutCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings_theme_equipped),
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onEquip,
                    shape = CutCornerShape(6.dp),
                    border = BorderStroke(1.dp, primaryColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = primaryColor.copy(alpha = 0.12f)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme_equip_action),
                        color = primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
