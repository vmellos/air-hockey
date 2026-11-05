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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import dev.vmellos.airhockey.feature.customization.ArenaThemeCatalog
import dev.vmellos.airhockey.feature.game.ui.Arena2DRenderer
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
import dev.vmellos.airhockey.core.ads.AdBanner
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.TableTheme
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
                titleRes = R.string.theme_preset_gridiron,
                descRes = R.string.theme_preset_gridiron_desc,
                icon = Icons.Default.SportsFootball,
                primaryColor = GridironCyan,
                secondaryColor = GridironGold,
                tableTheme = TableTheme.GRIDIRON_FOOTBALL,
                puckSkin = PuckSkin.GRIDIRON_FOOTBALL,
                malletSkin = MalletSkin.LINEBACKER_STRIKER
            ),
            ThemeBundleItem(
                id = "cyber_baseball",
                titleRes = R.string.theme_preset_baseball,
                descRes = R.string.theme_preset_baseball_desc,
                icon = Icons.Default.SportsBaseball,
                primaryColor = BaseballRed,
                secondaryColor = BaseballCyan,
                tableTheme = TableTheme.CYBER_BASEBALL,
                puckSkin = PuckSkin.CYBER_BASEBALL,
                malletSkin = MalletSkin.SLUGGER_STRIKER
            ),
            ThemeBundleItem(
                id = "wasteland",
                titleRes = R.string.theme_preset_wasteland,
                descRes = R.string.theme_preset_wasteland_desc,
                icon = Icons.Default.Whatshot,
                primaryColor = WastelandFlame,
                secondaryColor = WastelandHazardYellow,
                tableTheme = TableTheme.WASTELAND_APOCALYPSE,
                puckSkin = PuckSkin.SAWBLADE,
                malletSkin = MalletSkin.RIPPER_STRIKER
            ),
            ThemeBundleItem(
                id = "synthwave",
                titleRes = R.string.theme_preset_synthwave,
                descRes = R.string.theme_preset_synthwave_desc,
                icon = Icons.Default.WbSunny,
                primaryColor = SynthPink,
                secondaryColor = SynthCyan,
                tableTheme = TableTheme.RETRO_SYNTHWAVE,
                puckSkin = PuckSkin.SYNTH_DISC,
                malletSkin = MalletSkin.OUTRUN_STRIKER
            ),
            ThemeBundleItem(
                id = "tokamak",
                titleRes = R.string.theme_preset_tokamak,
                descRes = R.string.theme_preset_tokamak_desc,
                icon = Icons.Default.Bolt,
                primaryColor = TokamakRadiation,
                secondaryColor = TokamakPlasma,
                tableTheme = TableTheme.TOKAMAK_REACTOR,
                puckSkin = PuckSkin.ATOMIC_CORE,
                malletSkin = MalletSkin.MAGNETIC_COIL
            ),
            ThemeBundleItem(
                id = "cyber_pong",
                titleRes = R.string.theme_preset_pong,
                descRes = R.string.theme_preset_pong_desc,
                icon = Icons.Default.VideogameAsset,
                primaryColor = PongGreen,
                secondaryColor = PongAmber,
                tableTheme = TableTheme.CYBER_PONG,
                puckSkin = PuckSkin.PONG_SQUARE,
                malletSkin = MalletSkin.PONG_PADDLE
            ),
            ThemeBundleItem(
                id = "colosseum",
                titleRes = R.string.theme_preset_colosseum,
                descRes = R.string.theme_preset_colosseum_desc,
                icon = Icons.Default.Shield,
                primaryColor = ColosseumGold,
                secondaryColor = ColosseumCrimson,
                tableTheme = TableTheme.CYBER_COLOSSEUM,
                puckSkin = PuckSkin.GLADIATOR_SHIELD,
                malletSkin = MalletSkin.SPARTAN_AEGIS
            ),
            ThemeBundleItem(
                id = "arcade_3d",
                titleRes = R.string.theme_preset_arcade_3d,
                descRes = R.string.theme_preset_arcade_3d_desc,
                icon = Icons.Default.Layers,
                primaryColor = NeonCyan,
                secondaryColor = NeonOrange,
                tableTheme = TableTheme.ARCADE_3D,
                puckSkin = PuckSkin.CLASSIC_PLASMA,
                malletSkin = MalletSkin.CLASSIC_NEON
            ),
            ThemeBundleItem(
                id = "ar_mode",
                titleRes = R.string.theme_preset_ar,
                descRes = R.string.theme_preset_ar_desc,
                icon = Icons.Default.ViewInAr,
                primaryColor = NeonCyan,
                secondaryColor = NeonEmerald,
                tableTheme = TableTheme.REALIDADE_AUMENTADA_AR,
                puckSkin = PuckSkin.CLASSIC_PLASMA,
                malletSkin = MalletSkin.CLASSIC_NEON
            )
        )
    }

    val tableItems = remember {
        listOf(
            TableItem(TableTheme.CLASSIC_NEON, R.string.theme_preset_classic, R.string.theme_preset_classic_desc, Icons.Default.SportsHockey, NeonCyan, NeonOrange),
            TableItem(TableTheme.CYBER_SOCCER, R.string.theme_preset_cyber_soccer, R.string.theme_preset_cyber_soccer_desc, Icons.Default.SportsSoccer, NeonEmerald, NeonRuby),
            TableItem(TableTheme.NEO_BASKETBALL, R.string.theme_preset_neo_basketball, R.string.theme_preset_neo_basketball_desc, Icons.Default.SportsBasketball, NeonAmber, NeonCyan),
            TableItem(TableTheme.GRAVITY_VOLLEY, R.string.theme_preset_gravity_volley, R.string.theme_preset_gravity_volley_desc, Icons.Default.SportsVolleyball, NeonViolet, NeonMagenta),
            TableItem(TableTheme.GRIDIRON_FOOTBALL, R.string.theme_preset_gridiron, R.string.theme_preset_gridiron_desc, Icons.Default.SportsFootball, GridironCyan, GridironGold),
            TableItem(TableTheme.CYBER_BASEBALL, R.string.theme_preset_baseball, R.string.theme_preset_baseball_desc, Icons.Default.SportsBaseball, BaseballRed, BaseballCyan),
            TableItem(TableTheme.WASTELAND_APOCALYPSE, R.string.theme_preset_wasteland, R.string.theme_preset_wasteland_desc, Icons.Default.Whatshot, WastelandFlame, WastelandHazardYellow),
            TableItem(TableTheme.RETRO_SYNTHWAVE, R.string.theme_preset_synthwave, R.string.theme_preset_synthwave_desc, Icons.Default.WbSunny, SynthPink, SynthCyan),
            TableItem(TableTheme.TOKAMAK_REACTOR, R.string.theme_preset_tokamak, R.string.theme_preset_tokamak_desc, Icons.Default.Bolt, TokamakRadiation, TokamakPlasma),
            TableItem(TableTheme.CYBER_PONG, R.string.theme_preset_pong, R.string.theme_preset_pong_desc, Icons.Default.VideogameAsset, PongGreen, PongAmber),
            TableItem(TableTheme.CYBER_COLOSSEUM, R.string.theme_preset_colosseum, R.string.theme_preset_colosseum_desc, Icons.Default.Shield, ColosseumGold, ColosseumCrimson),
            TableItem(TableTheme.ARCADE_3D, R.string.theme_preset_arcade_3d, R.string.theme_preset_arcade_3d_desc, Icons.Default.Layers, NeonCyan, NeonOrange),
            TableItem(TableTheme.REALIDADE_AUMENTADA_AR, R.string.theme_preset_ar, R.string.theme_preset_ar_desc, Icons.Default.ViewInAr, NeonCyan, NeonEmerald)
        )
    }

    val puckItems = remember {
        listOf(
            PuckItem(PuckSkin.CLASSIC_PLASMA, R.string.skin_puck_classic, Icons.Default.SportsHockey, NeonCyan),
            PuckItem(PuckSkin.CYBER_SOCCER_BALL, R.string.skin_puck_soccer, Icons.Default.SportsSoccer, NeonEmerald),
            PuckItem(PuckSkin.NEO_BASKETBALL, R.string.skin_puck_basketball, Icons.Default.SportsBasketball, NeonAmber),
            PuckItem(PuckSkin.GRAVITY_VOLLEY, R.string.skin_puck_volley, Icons.Default.SportsVolleyball, NeonViolet),
            PuckItem(PuckSkin.GRIDIRON_FOOTBALL, R.string.skin_puck_gridiron, Icons.Default.SportsFootball, GridironCyan),
            PuckItem(PuckSkin.CYBER_BASEBALL, R.string.skin_puck_baseball, Icons.Default.SportsBaseball, BaseballRed),
            PuckItem(PuckSkin.SAWBLADE, R.string.skin_puck_wasteland, Icons.Default.Whatshot, WastelandFlame),
            PuckItem(PuckSkin.SYNTH_DISC, R.string.skin_puck_synthwave, Icons.Default.WbSunny, SynthPink),
            PuckItem(PuckSkin.ATOMIC_CORE, R.string.skin_puck_tokamak, Icons.Default.Bolt, TokamakRadiation),
            PuckItem(PuckSkin.PONG_SQUARE, R.string.skin_puck_pong, Icons.Default.VideogameAsset, PongGreen),
            PuckItem(PuckSkin.GLADIATOR_SHIELD, R.string.skin_puck_colosseum, Icons.Default.Shield, ColosseumGold)
        )
    }

    val malletItems = remember {
        listOf(
            MalletItem(MalletSkin.CLASSIC_NEON, R.string.skin_mallet_classic, Icons.Default.SportsHockey, NeonCyan),
            MalletItem(MalletSkin.MAGNETIC_STRIKER, R.string.skin_mallet_soccer, Icons.Default.SportsSoccer, NeonEmerald),
            MalletItem(MalletSkin.DUNK_STRIKER, R.string.skin_mallet_basketball, Icons.Default.SportsBasketball, NeonAmber),
            MalletItem(MalletSkin.SPIKE_EMITTER, R.string.skin_mallet_volley, Icons.Default.SportsVolleyball, NeonViolet),
            MalletItem(MalletSkin.LINEBACKER_STRIKER, R.string.skin_mallet_gridiron, Icons.Default.SportsFootball, GridironCyan),
            MalletItem(MalletSkin.SLUGGER_STRIKER, R.string.skin_mallet_baseball, Icons.Default.SportsBaseball, BaseballRed),
            MalletItem(MalletSkin.RIPPER_STRIKER, R.string.skin_mallet_wasteland, Icons.Default.Whatshot, WastelandFlame),
            MalletItem(MalletSkin.OUTRUN_STRIKER, R.string.skin_mallet_synthwave, Icons.Default.WbSunny, SynthPink),
            MalletItem(MalletSkin.MAGNETIC_COIL, R.string.skin_mallet_tokamak, Icons.Default.Bolt, TokamakRadiation),
            MalletItem(MalletSkin.PONG_PADDLE, R.string.skin_mallet_pong, Icons.Default.VideogameAsset, PongGreen),
            MalletItem(MalletSkin.SPARTAN_AEGIS, R.string.skin_mallet_colosseum, Icons.Default.Shield, ColosseumGold)
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
        prefs.tableTheme == TableTheme.GRIDIRON_FOOTBALL &&
                prefs.puckSkin == PuckSkin.GRIDIRON_FOOTBALL &&
                prefs.player1MalletSkin == MalletSkin.LINEBACKER_STRIKER -> stringResource(R.string.theme_preset_gridiron)
        prefs.tableTheme == TableTheme.CYBER_BASEBALL &&
                prefs.puckSkin == PuckSkin.CYBER_BASEBALL &&
                prefs.player1MalletSkin == MalletSkin.SLUGGER_STRIKER -> stringResource(R.string.theme_preset_baseball)
        prefs.tableTheme == TableTheme.WASTELAND_APOCALYPSE &&
                prefs.puckSkin == PuckSkin.SAWBLADE &&
                prefs.player1MalletSkin == MalletSkin.RIPPER_STRIKER -> stringResource(R.string.theme_preset_wasteland)
        prefs.tableTheme == TableTheme.RETRO_SYNTHWAVE &&
                prefs.puckSkin == PuckSkin.SYNTH_DISC &&
                prefs.player1MalletSkin == MalletSkin.OUTRUN_STRIKER -> stringResource(R.string.theme_preset_synthwave)
        prefs.tableTheme == TableTheme.TOKAMAK_REACTOR &&
                prefs.puckSkin == PuckSkin.ATOMIC_CORE &&
                prefs.player1MalletSkin == MalletSkin.MAGNETIC_COIL -> stringResource(R.string.theme_preset_tokamak)
        prefs.tableTheme == TableTheme.CYBER_PONG &&
                prefs.puckSkin == PuckSkin.PONG_SQUARE &&
                prefs.player1MalletSkin == MalletSkin.PONG_PADDLE -> stringResource(R.string.theme_preset_pong)
        prefs.tableTheme == TableTheme.CYBER_COLOSSEUM &&
                prefs.puckSkin == PuckSkin.GLADIATOR_SHIELD &&
                prefs.player1MalletSkin == MalletSkin.SPARTAN_AEGIS -> stringResource(R.string.theme_preset_colosseum)
        prefs.tableTheme == TableTheme.ARCADE_3D &&
                prefs.puckSkin == PuckSkin.CLASSIC_PLASMA &&
                prefs.player1MalletSkin == MalletSkin.CLASSIC_NEON -> stringResource(R.string.theme_preset_arcade_3d)
        prefs.tableTheme == TableTheme.REALIDADE_AUMENTADA_AR &&
                prefs.puckSkin == PuckSkin.CLASSIC_PLASMA &&
                prefs.player1MalletSkin == MalletSkin.CLASSIC_NEON -> stringResource(R.string.theme_preset_ar)
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
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF64748B),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = title,
                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        val isEquipped = prefs.tableTheme == bundle.tableTheme &&
                                prefs.puckSkin == bundle.puckSkin &&
                                prefs.player1MalletSkin == bundle.malletSkin

                        BundleCard(
                            bundle = bundle,
                            isEquipped = isEquipped,
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

        Spacer(modifier = Modifier.height(6.dp))

        AdBanner(
            adManager = AirHockeyApp.instance.adManager,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )
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
    val containerBg = if (isEquipped) bundle.primaryColor.copy(alpha = 0.12f) else Color(0xFF0D1424)

    Card(
        shape = CutCornerShape(10.dp),
        border = BorderStroke(if (isEquipped) 1.5.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEquip)
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

            if (isEquipped) {
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
    onPreview: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isEquipped) primaryColor else Color(0xFF1E293B)
    val containerBg = if (isEquipped) primaryColor.copy(alpha = 0.12f) else Color(0xFF0D1424)

    Card(
        shape = CutCornerShape(10.dp),
        border = BorderStroke(if (isEquipped) 1.5.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEquip)
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

            if (isEquipped) {
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
