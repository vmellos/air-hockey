package dev.vmellos.airhockey.feature.customization.ui

import android.os.SystemClock
import android.view.MotionEvent
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import dev.vmellos.airhockey.feature.stage.ArenaConquestCatalog
import dev.romainguy.kotlin.math.Float3
import dev.vmellos.airhockey.feature.customization.ArenaThemeCatalog
import dev.vmellos.airhockey.feature.game.ui.Arena2DRenderer
import dev.vmellos.airhockey.feature.game.ui.configureFilamentArenaView
import dev.vmellos.airhockey.feature.game.ui.updateFilamentArenaBloom
import io.github.sceneview.Scene
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.ads.AdBanner
import dev.vmellos.airhockey.core.preferences.ArenaDimension
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.feature.game.ui.Theme3DAssetResolver
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.ui.*
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
    onNavigateToStages: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val preferencesRepo = AirHockeyApp.instance.preferencesRepository
    val prefs by preferencesRepo.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val stageRepo = AirHockeyApp.instance.stageRepository
    val unlockedThemes by stageRepo.unlockedThemesFlow.collectAsState(initial = setOf(TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D, TableTheme.KIARA_CAT))
    val unlockedPucks by stageRepo.unlockedPuckSkinsFlow.collectAsState(initial = setOf(PuckSkin.CLASSIC_PLASMA, PuckSkin.KIARA_PAW))
    val unlockedMallets by stageRepo.unlockedMalletSkinsFlow.collectAsState(initial = setOf(MalletSkin.CLASSIC_NEON, MalletSkin.NEKO_STRIKER))

    var lockedDialogInfo by remember { mutableStateOf<LockedArenaInfo?>(null) }

    // Estado da categoria selecionada (0: Conjuntos, 1: Mesas, 2: Discos, 3: Rebatedores)
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    // Estado de pré-visualização ao vivo
    var previewTableTheme by remember { mutableStateOf(prefs.tableTheme) }
    var previewPuckSkin by remember { mutableStateOf(prefs.puckSkin) }
    var previewMalletSkin by remember { mutableStateOf(prefs.player1MalletSkin) }
    var liveNeonBloomIntensity by remember(prefs.neonBloomIntensity) { mutableFloatStateOf(prefs.neonBloomIntensity) }

    // Sincroniza pré-visualização quando as preferências são atualizadas externamente
    LaunchedEffect(prefs.tableTheme, prefs.puckSkin, prefs.player1MalletSkin, prefs.neonBloomIntensity) {
        previewTableTheme = prefs.tableTheme
        previewPuckSkin = prefs.puckSkin
        previewMalletSkin = prefs.player1MalletSkin
        liveNeonBloomIntensity = prefs.neonBloomIntensity
    }

    val bundleItems = remember {
        ArenaConquestCatalog.ALL_THEMES.map { tier ->
            ThemeBundleItem(
                id = tier.theme.name.lowercase(),
                titleRes = tier.titleRes,
                descRes = tier.descRes,
                icon = tier.icon,
                primaryColor = tier.primaryColor,
                secondaryColor = tier.secondaryColor,
                tableTheme = tier.theme,
                puckSkin = tier.puckSkin,
                malletSkin = tier.malletSkin
            )
        }
    }

    val tableItems = remember {
        ArenaConquestCatalog.ALL_THEMES.map { tier ->
            TableItem(tier.theme, tier.titleRes, tier.descRes, tier.icon, tier.primaryColor, tier.secondaryColor)
        }
    }

    val puckItems = remember {
        ArenaConquestCatalog.ALL_THEMES.map { tier ->
            PuckItem(tier.puckSkin, tier.puckTitleRes, tier.icon, tier.primaryColor)
        }
    }

    val malletItems = remember {
        ArenaConquestCatalog.ALL_THEMES.map { tier ->
            MalletItem(tier.malletSkin, tier.malletTitleRes, tier.icon, tier.primaryColor)
        }
    }

    // Nome descritivo do kit atualmente ativo resolvido pelo Catálogo Canônico
    val currentActivePresetName = stringResource(
        ArenaConquestCatalog.resolveActivePresetTitleRes(
            prefs.tableTheme,
            prefs.puckSkin,
            prefs.player1MalletSkin
        )
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val is3d = prefs.arenaDimension == ArenaDimension.THREE_DIMENSIONAL

    val categoryTitles = remember {
        listOf(
            Pair(R.string.customization_tab_bundles, Icons.Default.Layers),
            Pair(R.string.customization_tab_tables, Icons.Default.ViewAgenda),
            Pair(R.string.customization_tab_pucks, Icons.Default.Adjust),
            Pair(R.string.customization_tab_mallets, Icons.Default.SportsEsports)
        )
    }

    val previewBoxContent = @Composable { boxModifier: Modifier ->
        Box(
            modifier = boxModifier
                .clip(CutCornerShape(12.dp))
                .background(Color(0xFF0B1220))
                .border(1.2.dp, Color(0xFF1E293B), CutCornerShape(12.dp))
        ) {
            if (is3d) {
                Arena3DLivePreview(
                    tableTheme = previewTableTheme,
                    puckSkin = previewPuckSkin,
                    malletSkin = previewMalletSkin,
                    neonBloomIntensity = liveNeonBloomIntensity,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ArenaLivePreview(
                    tableTheme = previewTableTheme,
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
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Opção 2D
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!is3d) Color(0xFF00E5FF) else Color.Transparent)
                        .clickable {
                            if (is3d) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    preferencesRepo.updateArenaDimension(ArenaDimension.TWO_DIMENSIONAL)
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.dimension_2d),
                        color = if (!is3d) Color(0xFF070B16) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Opção 3D
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (is3d) Color(0xFF00E5FF) else Color.Transparent)
                        .clickable {
                            if (!is3d) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    preferencesRepo.updateArenaDimension(ArenaDimension.THREE_DIMENSIONAL)
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.dimension_3d),
                        color = if (is3d) Color(0xFF070B16) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    val bloomControlsContent = @Composable {
        if (is3d) {
            Surface(
                color = Color(0xFF0C1322),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.neon_bloom_intensity_title),
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        val pct = kotlin.math.round((liveNeonBloomIntensity / 0.10f) * 100).toInt().coerceIn(0, 100)
                        val intensityLabel = when {
                            pct == 0 -> stringResource(R.string.neon_bloom_off)
                            pct <= 30 -> stringResource(R.string.neon_bloom_soft)
                            pct <= 70 -> stringResource(R.string.neon_bloom_normal)
                            else -> stringResource(R.string.neon_bloom_vibrant)
                        }
                        Text(
                            text = "$pct% • $intensityLabel",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = liveNeonBloomIntensity.coerceIn(0.0f, 0.10f),
                        onValueChange = { newVal ->
                            liveNeonBloomIntensity = newVal
                        },
                        onValueChangeFinished = {
                            coroutineScope.launch {
                                preferencesRepo.updateNeonBloomIntensity(liveNeonBloomIntensity)
                            }
                        },
                        valueRange = 0.0f..0.10f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )
                }
            }
        }
    }

    val headerContent = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isLandscape) 4.dp else 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0x331E293B), CircleShape)
                    .border(1.dp, Color(0x66334155), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.customization_screen_title),
                    color = Color.White,
                    fontSize = if (isLandscape) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.settings_theme_active_indicator, currentActivePresetName),
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    val tabRowContent = @Composable {
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
                .padding(horizontal = if (isLandscape) 4.dp else 16.dp)
                .clip(CutCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E293B), CutCornerShape(8.dp))
        ) {
            categoryTitles.forEachIndexed { index, (titleRes, icon) ->
                val isSelected = selectedCategoryIndex == index
                Tab(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedCategoryIndex = index
                    },
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(titleRes),
                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    val itemsListContent = @Composable { listModifier: Modifier ->
        LazyColumn(
            modifier = listModifier.padding(horizontal = if (isLandscape) 4.dp else 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (selectedCategoryIndex) {
                0 -> {
                    // Categoria: CONJUNTOS COMPLETOS
                    items(bundleItems, key = { it.id }) { bundle ->
                        val unlockStage = ArenaConquestCatalog.getUnlockStageForTheme(bundle.tableTheme)
                        val isLocked = unlockStage > 0 && !unlockedThemes.contains(bundle.tableTheme)
                        val isEquipped = !isLocked && prefs.tableTheme == bundle.tableTheme &&
                                prefs.puckSkin == bundle.puckSkin &&
                                prefs.player1MalletSkin == bundle.malletSkin

                        BundleCard(
                            bundle = bundle,
                            isEquipped = isEquipped,
                            isLocked = isLocked,
                            unlockStage = unlockStage,
                            onLockedClick = {
                                lockedDialogInfo = LockedArenaInfo(bundle.titleRes, unlockStage)
                            },
                            onPreview = {
                                previewTableTheme = bundle.tableTheme
                                previewPuckSkin = bundle.puckSkin
                                previewMalletSkin = bundle.malletSkin
                            },
                            onEquip = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                previewTableTheme = bundle.tableTheme
                                previewPuckSkin = bundle.puckSkin
                                previewMalletSkin = bundle.malletSkin
                                coroutineScope.launch {
                                    preferencesRepo.applyThemePreset(bundle.tableTheme)
                                }
                            }
                        )
                    }
                }
                1 -> {
                    // Categoria: MESAS
                    items(tableItems, key = { it.theme.name }) { tableItem ->
                        val unlockStage = ArenaConquestCatalog.getUnlockStageForTheme(tableItem.theme)
                        val isLocked = unlockStage > 0 && !unlockedThemes.contains(tableItem.theme)
                        val isEquipped = !isLocked && prefs.tableTheme == tableItem.theme

                        SingleItemCard(
                            title = stringResource(tableItem.titleRes),
                            description = stringResource(tableItem.descRes),
                            icon = tableItem.icon,
                            primaryColor = tableItem.primaryColor,
                            secondaryColor = tableItem.secondaryColor,
                            isEquipped = isEquipped,
                            isLocked = isLocked,
                            unlockStage = unlockStage,
                            onLockedClick = {
                                lockedDialogInfo = LockedArenaInfo(tableItem.titleRes, unlockStage)
                            },
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
                        val unlockStage = ArenaConquestCatalog.getUnlockStageForPuck(puckItem.skin)
                        val isLocked = unlockStage > 0 && !unlockedPucks.contains(puckItem.skin)
                        val isEquipped = !isLocked && prefs.puckSkin == puckItem.skin

                        SingleItemCard(
                            title = stringResource(puckItem.titleRes),
                            description = stringResource(R.string.theme_puck_label),
                            icon = puckItem.icon,
                            primaryColor = puckItem.primaryColor,
                            secondaryColor = Color.White,
                            isEquipped = isEquipped,
                            isLocked = isLocked,
                            unlockStage = unlockStage,
                            onLockedClick = {
                                lockedDialogInfo = LockedArenaInfo(puckItem.titleRes, unlockStage)
                            },
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
                        val unlockStage = ArenaConquestCatalog.getUnlockStageForMallet(malletItem.skin)
                        val isLocked = unlockStage > 0 && !unlockedMallets.contains(malletItem.skin)
                        val isEquipped = !isLocked && prefs.player1MalletSkin == malletItem.skin

                        SingleItemCard(
                            title = stringResource(malletItem.titleRes),
                            description = stringResource(R.string.theme_mallet_label),
                            icon = malletItem.icon,
                            primaryColor = malletItem.primaryColor,
                            secondaryColor = Color.White,
                            isEquipped = isEquipped,
                            isLocked = isLocked,
                            unlockStage = unlockStage,
                            onLockedClick = {
                                lockedDialogInfo = LockedArenaInfo(malletItem.titleRes, unlockStage)
                            },
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

    if (isLandscape) {
        // Layout Bicolunar na Customização em Modo Paisagem (ADR 0037)
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF070B16))
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Coluna Esquerda: Vitrine de Pré-Visualização 3D/2D Dinâmica + Dimensão + Bloom
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
                bloomControlsContent()
            }

            // Coluna Direita: Cabeçalho, Abas de Itens, Lista com Rolagem e Anúncio em Banner
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                headerContent()
                tabRowContent()
                Spacer(modifier = Modifier.height(6.dp))
                itemsListContent(Modifier.fillMaxWidth().weight(1f))
                AdBanner(
                    adManager = AirHockeyApp.instance.adManager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
                )
            }
        }
    } else {
        // Modo Retrato: Coluna Única Padrão
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF070B16))
                .safeDrawingPadding()
        ) {
            headerContent()

            previewBoxContent(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(210.dp)
            )

            dimensionSelectorContent()
            bloomControlsContent()

            Spacer(modifier = Modifier.height(8.dp))
            tabRowContent()
            Spacer(modifier = Modifier.height(8.dp))

            itemsListContent(Modifier.fillMaxWidth().weight(1f))

            Spacer(modifier = Modifier.height(4.dp))
            AdBanner(
                adManager = AirHockeyApp.instance.adManager,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }
    }

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

/**
 * Renderizador de Pré-Visualização Dinâmica 3D da Arena no Sceneview (Filament).
 * Renderiza em tempo real a mesa, disco e rebatedores selecionados sob a iluminação e bloom configurados.
 */
@Composable
internal fun Arena3DLivePreview(
    tableTheme: TableTheme,
    puckSkin: PuckSkin,
    malletSkin: MalletSkin,
    neonBloomIntensity: Float,
    isExtended: Boolean = false,
    modifier: Modifier = Modifier
) {
    val engine3d = rememberEngine()
    val modelLoader = rememberModelLoader(engine3d)
    val view3d = rememberView(engine3d) {
        SceneView.createView(engine3d).apply {
            configureFilamentArenaView(
                view = this,
                neonBloomIntensity = neonBloomIntensity,
                bloomEnabledOverride = true
            )
        }
    }

    LaunchedEffect(neonBloomIntensity) {
        updateFilamentArenaBloom(
            view = view3d,
            neonBloomIntensity = neonBloomIntensity,
            bloomEnabledOverride = true
        )
    }

    val tableAsset = remember(tableTheme) {
        Theme3DAssetResolver.getTable3DAsset(tableTheme, isExtended = false)
    }
    val puckAsset = remember(puckSkin, tableTheme) {
        Theme3DAssetResolver.getPuck3DAsset(puckSkin, tableTheme)
    }
    val p1MalletAsset = remember(malletSkin, tableTheme) {
        Theme3DAssetResolver.getMallet3DAsset(malletSkin, isPlayer1 = true, theme = tableTheme)
    }
    val p2MalletAsset = remember(malletSkin, tableTheme) {
        Theme3DAssetResolver.getMallet3DAsset(malletSkin, isPlayer1 = false, theme = tableTheme)
    }

    val tableInstance = remember(modelLoader, tableAsset) {
        try {
            modelLoader.createModelInstance(tableAsset)
        } catch (_: Exception) {
            null
        }
    }

    val puckInstance = remember(modelLoader, puckAsset) {
        try {
            modelLoader.createModelInstance(puckAsset)
        } catch (_: Exception) {
            null
        }
    }

    val p1Instance = remember(modelLoader, p1MalletAsset) {
        try {
            modelLoader.createModelInstance(p1MalletAsset)
        } catch (_: Exception) {
            null
        }
    }

    val p2Instance = remember(modelLoader, p2MalletAsset) {
        try {
            modelLoader.createModelInstance(p2MalletAsset)
        } catch (_: Exception) {
            null
        }
    }

    val mascotAsset = remember(tableTheme) {
        Theme3DAssetResolver.getMascot3DAsset(tableTheme)
    }
    val mascotInstance = remember(modelLoader, mascotAsset) {
        mascotAsset?.let { path ->
            try {
                modelLoader.createModelInstance(path)
            } catch (_: Exception) {
                null
            }
        }
    }
    val mascotNode = remember(mascotInstance) {
        mascotInstance?.let {
            ModelNode(it, autoAnimate = true).apply {
                position = Float3(-0.74f, -0.045f, -0.15f)
                rotation = Float3(0f, 90f, 0f)
                scale = Float3(1.0f, 1.0f, 1.0f)
            }
        }
    }

    val tableNode = remember(tableInstance) {
        tableInstance?.let {
            ModelNode(it).apply {
                position = Float3(0f, 0f, 0f)
            }
        }
    }

    val puckNode = remember(puckInstance) {
        puckInstance?.let {
            ModelNode(it).apply {
                position = Float3(0f, 0.0075f, 0f)
            }
        }
    }

    val p1Node = remember(p1Instance) {
        p1Instance?.let {
            ModelNode(it).apply {
                position = Float3(0f, 0.0f, 0.52f)
            }
        }
    }

    val p2Node = remember(p2Instance) {
        p2Instance?.let {
            ModelNode(it).apply {
                position = Float3(0f, 0.0f, -0.52f)
            }
        }
    }

    val cameraNode = rememberCameraNode(engine3d) {
        position = Float3(0f, 2.75f, 2.30f)
        rotation = Float3(-46f, 0f, 0f)
    }

    val cameraManipulator = rememberCameraManipulator(
        orbitHomePosition = Float3(0f, 2.75f, 2.30f),
        targetPosition = Float3(0f, 0f, 0f)
    )

    var lastTouchTime by remember { mutableLongStateOf(0L) }
    var isTouching by remember { mutableStateOf(false) }
    var currentRotationDegrees by remember { mutableFloatStateOf(0f) }
    var lastFrameNanos by remember { mutableLongStateOf(0L) }

    val arenaRootNode = remember(engine3d) {
        Node(engine3d)
    }

    LaunchedEffect(arenaRootNode, tableNode, puckNode, p1Node, p2Node, mascotNode) {
        arenaRootNode.clearChildNodes()
        listOfNotNull(tableNode, puckNode, p1Node, p2Node, mascotNode).forEach { node ->
            arenaRootNode.addChildNode(node)
        }
    }

    val childNodes = remember(arenaRootNode) {
        listOf(arenaRootNode)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine3d,
            modelLoader = modelLoader,
            view = view3d,
            cameraNode = cameraNode,
            cameraManipulator = cameraManipulator,
            childNodes = childNodes,
            onViewCreated = {
                cameraGestureDetector?.isPanEnabled = false
                setOnTouchListener { v, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    } else if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }
            },
            onTouchEvent = { event, _ ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        isTouching = true
                        lastTouchTime = SystemClock.uptimeMillis()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        isTouching = true
                        lastTouchTime = SystemClock.uptimeMillis()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isTouching = false
                        lastTouchTime = SystemClock.uptimeMillis()
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        if (event.pointerCount <= 1) {
                            isTouching = false
                        }
                        lastTouchTime = SystemClock.uptimeMillis()
                    }
                }
                false
            },
            onFrame = { frameTimeNanos ->
                val now = SystemClock.uptimeMillis()
                if (lastFrameNanos != 0L) {
                    val deltaSeconds = (frameTimeNanos - lastFrameNanos).coerceAtLeast(0L) / 1_000_000_000f
                    if (!isTouching && (now - lastTouchTime >= 3000L)) {
                        currentRotationDegrees = (currentRotationDegrees + deltaSeconds * 12.0f) % 360f
                        arenaRootNode.rotation = Float3(0f, currentRotationDegrees, 0f)
                    }
                }
                lastFrameNanos = frameTimeNanos
            }
        )
    }
}

/**
 * Renderizador de Pré-Visualização Dinâmica em Canvas da Arena completa.
 */
@Composable
internal fun ArenaLivePreview(
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

    val themeVisuals = remember(tableTheme) { ArenaThemeCatalog.getVisuals(tableTheme) }
    val bgColor = themeVisuals.backgroundColor

    val p1Visuals = remember(malletSkin) { ArenaThemeCatalog.getMalletSkinVisuals(malletSkin, isPlayer1 = true) }
    val p1Color = p1Visuals.color
    val p1Rim = p1Visuals.rimColor

    val p2Visuals = remember(malletSkin) { ArenaThemeCatalog.getMalletSkinVisuals(malletSkin, isPlayer1 = false) }
    val p2Color = p2Visuals.color
    val p2Rim = p2Visuals.rimColor

    val puckAccent = remember(puckSkin) { ArenaThemeCatalog.getPuckAccent(puckSkin) }

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

            // 1. Desenha a mesa completa
            Arena2DRenderer.run {
                drawTableSurface(tableTheme, left, top, tableW, tableH, scale)
            }

            // 2. Desenha as metas
            Arena2DRenderer.run {
                drawTableGoals(
                    theme = tableTheme,
                    goalLeft = goalLeft,
                    goalWidth = goalW,
                    topY = top,
                    bottomY = top + tableH,
                    scale = scale,
                    topGoalColor = p2Color,
                    bottomGoalColor = p1Color,
                    drawTopGoal = true
                )
            }

            // 3. Rebatedor P1 (Inferior)
            val p1X = left + tableW / 2f
            val p1Y = top + tableH * 0.78f
            val malletRadius = 64f * scale
            Arena2DRenderer.run {
                drawMalletSkin(malletSkin, p1X, p1Y, malletRadius, p1Color, p1Rim, scale)
            }

            // 4. Rebatedor P2 (Superior)
            val p2X = left + tableW / 2f
            val p2Y = top + tableH * 0.22f
            Arena2DRenderer.run {
                drawMalletSkin(malletSkin, p2X, p2Y, malletRadius, p2Color, p2Rim, scale)
            }

            // 5. Disco de Jogo (Centro com pulso animado)
            val puckX = left + tableW / 2f
            val puckY = top + tableH * 0.50f
            val puckRadius = 36f * scale * pulseScale
            when (puckSkin) {
                PuckSkin.CYBER_SOCCER_BALL -> drawCyberSoccerPuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.NEO_BASKETBALL -> drawNeoBasketballPuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.GRAVITY_VOLLEY -> drawGravityVolleyPuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.GRIDIRON_FOOTBALL -> drawGridironPuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.CYBER_BASEBALL -> drawCyberBaseballPuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.SAWBLADE -> drawSawbladePuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.SYNTH_DISC -> drawSynthwavePuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.ATOMIC_CORE -> drawAtomicCorePuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.PONG_SQUARE -> drawPongPuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.GLADIATOR_SHIELD -> drawGladiatorShieldPuck(puckX, puckY, puckRadius, puckAccent, scale)
                PuckSkin.KIARA_PAW -> drawKiaraPawPuck(puckX, puckY, puckRadius, puckAccent, scale)
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
    isLocked: Boolean = false,
    unlockStage: Int = 0,
    onLockedClick: (() -> Unit)? = null,
    onPreview: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isLocked -> Color(0x66FFB800)
        isEquipped -> bundle.primaryColor
        else -> Color(0xFF1E293B)
    }
    val containerBg = when {
        isLocked -> Color(0xFF0D1424)
        isEquipped -> bundle.primaryColor.copy(alpha = 0.12f)
        else -> Color(0xFF0D1424)
    }

    Card(
        shape = CutCornerShape(10.dp),
        border = BorderStroke(if (isEquipped || isLocked) 1.5.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onPreview()
                if (isLocked) {
                    onLockedClick?.invoke()
                } else {
                    onEquip()
                }
            }
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
                Text(
                    text = stringResource(bundle.titleRes),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = stringResource(bundle.descRes),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLocked) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x33FFB800), CircleShape)
                        .border(1.2.dp, Color(0x88FFB800), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (isEquipped) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(bundle.primaryColor.copy(alpha = 0.15f), CircleShape)
                        .border(1.5.dp, bundle.primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = bundle.primaryColor,
                        modifier = Modifier.size(18.dp)
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
    isLocked: Boolean = false,
    unlockStage: Int = 0,
    onLockedClick: (() -> Unit)? = null,
    onPreview: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isLocked -> Color(0x66FFB800)
        isEquipped -> primaryColor
        else -> Color(0xFF1E293B)
    }
    val containerBg = when {
        isLocked -> Color(0xFF0D1424)
        isEquipped -> primaryColor.copy(alpha = 0.12f)
        else -> Color(0xFF0D1424)
    }

    Card(
        shape = CutCornerShape(10.dp),
        border = BorderStroke(if (isEquipped || isLocked) 1.5.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onPreview()
                if (isLocked) {
                    onLockedClick?.invoke()
                } else {
                    onEquip()
                }
            }
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

            if (isLocked) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x33FFB800), CircleShape)
                        .border(1.2.dp, Color(0x88FFB800), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (isEquipped) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(primaryColor.copy(alpha = 0.15f), CircleShape)
                        .border(1.5.dp, primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
