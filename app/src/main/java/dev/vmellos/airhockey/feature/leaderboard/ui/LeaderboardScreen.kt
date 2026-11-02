package dev.vmellos.airhockey.feature.leaderboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.database.MatchRecordEntity
import dev.vmellos.airhockey.core.database.PlayerProfileStatsEntity
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

    val topElo by repo.topPlayersByRating.collectAsState(initial = emptyList())
    val topMulti by repo.topMultiplayerPlayers.collectAsState(initial = emptyList())
    val topSolo by repo.topSoloPlayers.collectAsState(initial = emptyList())
    val recentMatches by repo.recentMatches.collectAsState(initial = emptyList())

    val defaultPlayerName = stringResource(R.string.settings_player_name_default)
    val selfName = userPrefs.playerName.ifBlank { defaultPlayerName }
    val selfStats by repo.getPlayerStats(selfName).collectAsState(initial = null)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = remember {
        listOf(
            LeaderboardTabItem(R.string.leaderboard_tab_elo, Icons.Default.EmojiEvents),
            LeaderboardTabItem(R.string.leaderboard_tab_extended, Icons.Default.SportsEsports),
            LeaderboardTabItem(R.string.leaderboard_tab_solo, Icons.Default.SmartToy),
            LeaderboardTabItem(R.string.leaderboard_tab_history, Icons.Default.History)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B18))
            .safeDrawingPadding()
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.leaderboard_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Card do Perfil Local
        val rating = selfStats?.ratingElo ?: 1000
        val wins = selfStats?.totalWins ?: 0
        val matches = selfStats?.totalMatches ?: 0
        val streak = selfStats?.currentWinStreak ?: 0
        val winRate = if (matches > 0) ((wins.toDouble() / matches) * 100).toInt() else 0

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10172A)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
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
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.leaderboard_win_streak, streak),
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.leaderboard_elo_value, rating),
                        color = Color(0xFFFFB800),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(R.string.leaderboard_win_rate, wins, matches, winRate),
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color(0xFF0B1021),
            contentColor = Color(0xFF00E5FF),
            edgePadding = 16.dp,
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

        // Tab Content
        when (selectedTabIndex) {
            0 -> PlayerListContent(players = topElo, metricLabel = stringResource(R.string.leaderboard_metric_elo))
            1 -> PlayerListContent(players = topMulti, metricLabel = stringResource(R.string.leaderboard_metric_multi_wins))
            2 -> PlayerListContent(players = topSolo, metricLabel = stringResource(R.string.leaderboard_metric_solo_wins))
            3 -> RecentMatchesContent(matches = recentMatches)
        }
    }
}

@Composable
private fun PlayerListContent(
    players: List<PlayerProfileStatsEntity>,
    metricLabel: String
) {
    if (players.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.leaderboard_empty_players),
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
        itemsIndexed(players) { index, player ->
            PlayerRankCard(rank = index + 1, player = player, metricLabel = metricLabel)
        }
    }
}

@Composable
private fun PlayerRankCard(
    rank: Int,
    player: PlayerProfileStatsEntity,
    metricLabel: String
) {
    val medalColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color(0xFF1E293B)
    }

    val medalTextColor = if (rank <= 3) Color(0xFF0F172A) else Color(0xFF94A3B8)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1426)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (rank <= 3) 1.dp else 0.5.dp,
                color = if (rank <= 3) medalColor else Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Rank Badge (Numeric circle)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(medalColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = medalTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.playerName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                val diff = player.goalDifferential
                val diffStr = if (diff > 0) "+$diff" else "$diff"
                Text(
                    text = stringResource(
                        R.string.leaderboard_player_record,
                        player.totalWins,
                        player.totalMatches - player.totalWins,
                        diffStr
                    ),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val valueText = when (metricLabel) {
                    stringResource(R.string.leaderboard_metric_elo) -> "${player.ratingElo}"
                    stringResource(R.string.leaderboard_metric_multi_wins) -> "${player.multiplayerWins}"
                    stringResource(R.string.leaderboard_metric_solo_wins) -> "${player.soloWins}"
                    else -> "${player.ratingElo}"
                }
                Text(
                    text = valueText,
                    color = if (rank == 1) Color(0xFFFFD700) else Color(0xFF00E5FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = metricLabel,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun RecentMatchesContent(
    matches: List<MatchRecordEntity>
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
                fontSize = 14.sp
            )
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(matches) { match ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1426)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
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
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${match.playerAName} vs ${match.playerBName}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateFormat.format(Date(match.timestamp)),
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
                        val deltaColor = if (deltaA > 0) Color(0xFF10B981) else if (deltaA < 0) Color(0xFFEF4444) else Color(0xFF94A3B8)
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
    }
}
