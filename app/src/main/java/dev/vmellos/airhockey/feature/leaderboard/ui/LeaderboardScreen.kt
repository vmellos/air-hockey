package dev.vmellos.airhockey.feature.leaderboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsEsports
import android.widget.Toast
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.ads.AdBanner
import dev.vmellos.airhockey.core.database.MatchRecordEntity
import dev.vmellos.airhockey.core.database.PlayerProfileStatsEntity
import dev.vmellos.airhockey.core.playgames.PlayGamesAuthState
import dev.vmellos.airhockey.core.util.findActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class LeaderboardTabItem(
    val titleRes: Int,
    val icon: ImageVector
)

@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repo = AirHockeyApp.instance.leaderboardRepository
    val prefsRepo = AirHockeyApp.instance.preferencesRepository
    val userPrefs by prefsRepo.userPreferencesFlow.collectAsState(initial = dev.vmellos.airhockey.core.preferences.UserPreferences())

    val recentMatches by repo.recentMatches.collectAsState(initial = emptyList())

    val defaultPlayerName = stringResource(R.string.settings_player_name_default)
    val selfName = userPrefs.playerName.ifBlank { defaultPlayerName }
    val selfStats by repo.getPlayerStats(selfName).collectAsState(initial = null)

    val pgsAuth by AirHockeyApp.instance.playGamesManager.authState.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = remember {
        listOf(
            LeaderboardTabItem(R.string.stats_tab_overview, Icons.Default.EmojiEvents),
            LeaderboardTabItem(R.string.stats_tab_history, Icons.Default.History)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_fiery_puck),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x88070B14),
                            Color(0x66070B14),
                            Color(0x99070B14)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .safeDrawingPadding()
            ) {
                // Barra Superior
                val context = LocalContext.current
                val activity = context.findActivity()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
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
                        text = stringResource(R.string.leaderboard_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Abas de Seleção: Carreira & Histórico
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xD9080E1A),
                    contentColor = Color(0xFF00E5FF),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF00E5FF)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, tabItem ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = tabItem.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedTabIndex == index) Color(0xFF00E5FF) else Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = stringResource(tabItem.titleRes),
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTabIndex == index) Color(0xFF00E5FF) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        )
                    }
                }

                // Conteúdo da Aba
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTabIndex) {
                        0 -> CareerOverviewContent(
                            selfName = selfName,
                            stats = selfStats,
                            pgsAuth = pgsAuth,
                            recentMatches = recentMatches,
                            onOpenGlobalLeaderboards = {
                                activity?.let { act ->
                                    AirHockeyApp.instance.playGamesManager.showLeaderboards(act) { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onOpenAchievements = {
                                activity?.let { act ->
                                    AirHockeyApp.instance.playGamesManager.showAchievements(act) { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onNavigateToHistory = {
                                selectedTabIndex = 1
                            }
                        )
                        1 -> RecentMatchesContent(
                            matches = recentMatches,
                            selfName = selfName
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                AdBanner(
                    adManager = AirHockeyApp.instance.adManager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CareerOverviewContent(
    selfName: String,
    stats: PlayerProfileStatsEntity?,
    pgsAuth: PlayGamesAuthState,
    recentMatches: List<MatchRecordEntity>,
    onOpenGlobalLeaderboards: () -> Unit,
    onOpenAchievements: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rating = stats?.ratingElo ?: 1000
    val totalWins = stats?.totalWins ?: 0
    val soloWins = stats?.soloWins ?: 0
    val multiWins = stats?.multiplayerWins ?: 0
    val matches = stats?.totalMatches ?: 0
    val streak = stats?.currentWinStreak ?: 0
    val bestStreak = stats?.bestWinStreak ?: 0
    val winRate = stats?.winRatePercent ?: 0
    val goalsScored = stats?.goalsScored ?: 0
    val goalsConceded = stats?.goalsConceded ?: 0
    val diff = stats?.goalDifferential ?: 0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Card do Perfil do Jogador & Elo
        item {
            val profileShape = CutCornerShape(14.dp)
            Card(
                shape = profileShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xD9080E1A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, Color(0x6600E5FF), profileShape)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF0077B6)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = selfName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Status da Conexão Play Games
                            if (pgsAuth is PlayGamesAuthState.Authenticated) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.stats_status_connected, pgsAuth.player.displayName),
                                        color = Color(0xFF00E5FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.stats_status_offline),
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.leaderboard_elo_value, rating),
                            color = Color(0xFFFFB800),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = stringResource(R.string.leaderboard_metric_elo),
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 2. Card de Destaque dos Placares Globais da Google Play
        item {
            val pgsCardShape = CutCornerShape(12.dp)
            Card(
                shape = pgsCardShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xD908182B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, Color(0xFF00E5FF), pgsCardShape)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0x3300E5FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.stats_global_rankings_title),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.stats_global_rankings_desc),
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onOpenGlobalLeaderboards,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF080E1A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.stats_global_rankings_cta),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onOpenAchievements,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0x66FFB800)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFFB800)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFB800)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.stats_global_achievements_cta),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Grid 2x2 de Métricas de Desempenho
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Vitórias
                    StatMetricCard(
                        title = stringResource(R.string.stats_card_wins_title),
                        value = "$totalWins",
                        valueColor = Color(0xFF00E676),
                        subtitle = stringResource(R.string.stats_solo_vs_multi, soloWins, multiWins),
                        modifier = Modifier.weight(1f)
                    )
                    // Aproveitamento
                    StatMetricCard(
                        title = stringResource(R.string.stats_card_win_rate_title),
                        value = "$winRate%",
                        valueColor = Color(0xFF00E5FF),
                        subtitle = stringResource(R.string.leaderboard_win_rate, totalWins, matches, winRate),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Sequência Invicta
                    StatMetricCard(
                        title = stringResource(R.string.stats_card_streaks_title),
                        value = "$streak",
                        valueColor = Color(0xFFFFB800),
                        subtitle = stringResource(R.string.stats_current_vs_best, streak, bestStreak),
                        modifier = Modifier.weight(1f)
                    )
                    // Gols & Saldo
                    val diffStr = if (diff > 0) "+$diff" else "$diff"
                    val diffColor = if (diff > 0) Color(0xFF00E676) else if (diff < 0) Color(0xFFFF5252) else Color(0xFF00E5FF)
                    StatMetricCard(
                        title = stringResource(R.string.stats_card_goals_title),
                        value = diffStr,
                        valueColor = diffColor,
                        subtitle = stringResource(R.string.stats_goals_balance, goalsScored, goalsConceded),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Prévia das Últimas Partidas
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.stats_match_summary_title),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (recentMatches.isNotEmpty()) {
                    TextButton(onClick = onNavigateToHistory) {
                        Text(
                            text = stringResource(R.string.stats_view_all_history),
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (recentMatches.isEmpty()) {
            item {
                Card(
                    shape = CutCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xD9080E1A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x3300E5FF), CutCornerShape(10.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.leaderboard_empty_history),
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val previewMatches = recentMatches.take(3)
            items(previewMatches) { match ->
                MatchSummaryCard(match = match, selfName = selfName)
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    valueColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val cardShape = CutCornerShape(10.dp)
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xD9080E1A)),
        modifier = modifier
            .border(1.dp, Color(0x3300E5FF), cardShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF64748B),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentMatchesContent(
    matches: List<MatchRecordEntity>,
    selfName: String
) {
    if (matches.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.leaderboard_empty_history),
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(matches) { match ->
            MatchSummaryCard(match = match, selfName = selfName)
        }
    }
}

@Composable
private fun MatchSummaryCard(
    match: MatchRecordEntity,
    selfName: String
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val matchShape = CutCornerShape(12.dp)

    val isPlayerA = match.playerAName.equals(selfName, ignoreCase = true)
    val isWin = if (isPlayerA) match.winner == "PLAYER_A" else match.winner == "PLAYER_B"
    val isDraw = match.winner == "DRAW"

    val (resultLabelRes, resultColor, resultBg) = when {
        isDraw -> Triple(R.string.match_result_draw, Color(0xFF94A3B8), Color(0x2694A3B8))
        isWin -> Triple(R.string.match_result_win, Color(0xFF00E676), Color(0x2600E676))
        else -> Triple(R.string.match_result_loss, Color(0xFFFF5252), Color(0x26FF5252))
    }

    Card(
        shape = matchShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xD9080E1A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x3300E5FF), matchShape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge de Resultado (VITÓRIA / DERROTA / EMPATE)
                    Box(
                        modifier = Modifier
                            .background(resultBg, RoundedCornerShape(4.dp))
                            .border(0.8.dp, resultColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(resultLabelRes),
                            color = resultColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    val modeLabel = when (match.gameMode) {
                        "EXTENDED_TABLE", "EXTENDED_HOST", "EXTENDED_GUEST" -> stringResource(R.string.menu_mode_extended)
                        "SPLIT_SCREEN" -> stringResource(R.string.menu_mode_split)
                        "SOLO" -> {
                            val difficultyStr = when (match.aiDifficulty) {
                                "EASY" -> stringResource(R.string.settings_ai_easy)
                                "MEDIUM" -> stringResource(R.string.settings_ai_medium)
                                "HARD" -> stringResource(R.string.settings_ai_hard)
                                else -> match.aiDifficulty ?: ""
                            }
                            val soloBase = stringResource(R.string.menu_mode_solo)
                            if (difficultyStr.isNotBlank()) "$soloBase ($difficultyStr)" else soloBase
                        }
                        else -> match.gameMode
                    }
                    Text(
                        text = modeLabel,
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${match.playerAName} vs ${match.playerBName}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                val formattedDate = dateFormat.format(Date(match.timestamp))
                val durationText = if (match.durationSeconds > 0) " · ${match.durationSeconds}s" else ""
                Text(
                    text = "$formattedDate$durationText",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${match.playerAScore} - ${match.playerBScore}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                val deltaA = match.ratingDeltaA
                val deltaSign = if (deltaA >= 0) "+$deltaA" else "$deltaA"
                val deltaColor = if (deltaA > 0) Color(0xFF00E676) else if (deltaA < 0) Color(0xFFFF5252) else Color(0xFF94A3B8)
                Text(
                    text = stringResource(R.string.leaderboard_delta_elo, deltaSign),
                    color = deltaColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
