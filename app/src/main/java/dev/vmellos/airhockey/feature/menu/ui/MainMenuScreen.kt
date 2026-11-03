package dev.vmellos.airhockey.feature.menu.ui

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.ads.AdBanner
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.core.update.InAppUpdateStatus
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.core.util.findActivity

@Composable
fun MainMenuScreen(
    onStartSolo: () -> Unit,
    onStartSplit: () -> Unit,
    onStartExtended: () -> Unit,
    onStartConnected: () -> Unit,
    onStartAiVsAiLocal: (AiDifficulty, AiDifficulty) -> Unit = { _, _ -> },
    onStartAiVsAiExtended: (AiDifficulty, AiDifficulty) -> Unit = { _, _ -> },
    onOpenLeaderboard: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAiVsAiDialog by remember { mutableStateOf(false) }
    val inAppUpdateStatus by AirHockeyApp.instance.inAppUpdateEngine.status.collectAsState()

    val adManager = AirHockeyApp.instance.adManager
    val billingManager = AirHockeyApp.instance.billingManager
    val isAdsRemovedPermanently by billingManager.isAdsRemoved.collectAsState()
    val isAdFreeTemporarily by adManager.isAdFreeTemporarily.collectAsState()
    val isRewardedLoaded by adManager.isRewardedLoaded.collectAsState()
    val prefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val context = LocalContext.current
    val activity = context.findActivity()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        billingManager.events.collect { event ->
            when (event) {
                is dev.vmellos.airhockey.core.billing.BillingEvent.PurchaseSuccess -> {
                    android.widget.Toast.makeText(context, context.getString(R.string.remove_ads_feedback_success), android.widget.Toast.LENGTH_SHORT).show()
                }
                is dev.vmellos.airhockey.core.billing.BillingEvent.BillingError -> {
                    val msg = event.message?.takeIf { it.isNotBlank() } ?: context.getString(R.string.remove_ads_feedback_error)
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
    ) {
        // Imagem de Fundo com o Disco Flamejante
        Image(
            painter = painterResource(id = R.drawable.bg_fiery_puck),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Camada de Contraste / Scrim sutil para manter legibilidade sem apagar o fundo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66070B14),
                            Color(0x22070B14),
                            Color(0x77070B14)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Cabeçalho com Placar, Loja/Remover Anúncios, Recompensas e Configurações
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão Placar de Líderes
                IconButton(
                    onClick = onOpenLeaderboard,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xB3070D1A), CircleShape)
                        .border(1.2.dp, Color(0x66FFB800), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = stringResource(R.string.leaderboard_title),
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Ações da Direita
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isAdsRemovedPermanently && activity != null) {
                        IconButton(
                            onClick = { billingManager.launchPurchaseFlow(activity) },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xB3070D1A), CircleShape)
                                .border(1.2.dp, Color(0x6600E5FF), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = stringResource(R.string.remove_ads_menu_button),
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xB3070D1A), CircleShape)
                            .border(1.2.dp, Color(0x6600E5FF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.menu_settings),
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Título Neon Arcade Volumétrico com Arte Oficial
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.title_banner_championship),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(64.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }

            // Banner de Atualização Flexível Concluída
            if (inAppUpdateStatus is InAppUpdateStatus.Downloaded) {
                InAppUpdateDownloadedBanner(
                    onRestart = { AirHockeyApp.instance.inAppUpdateEngine.completeUpdate() },
                    onDismiss = { AirHockeyApp.instance.inAppUpdateEngine.dismissNotification() }
                )
            }

            // Cartões dos 5 Modos de Jogo no Padrão Arcade Glassmorphism
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                MenuModeCard(
                    title = stringResource(R.string.menu_mode_solo),
                    description = stringResource(R.string.menu_mode_solo_desc),
                    accentColor = Color(0xFF10B981),
                    hapticsEnabled = prefs.hapticsEnabled,
                    onClick = onStartSolo
                )

                MenuModeCard(
                    title = stringResource(R.string.menu_mode_connected),
                    description = stringResource(R.string.menu_mode_connected_desc),
                    accentColor = Color(0xFF00E5FF),
                    hapticsEnabled = prefs.hapticsEnabled,
                    onClick = onStartConnected
                )

                MenuModeCard(
                    title = stringResource(R.string.menu_mode_extended),
                    description = stringResource(R.string.menu_mode_extended_desc),
                    accentColor = Color(0xFF818CF8),
                    hapticsEnabled = prefs.hapticsEnabled,
                    onClick = onStartExtended
                )

                MenuModeCard(
                    title = stringResource(R.string.menu_mode_split),
                    description = stringResource(R.string.menu_mode_split_desc),
                    accentColor = Color(0xFFFF5722),
                    hapticsEnabled = prefs.hapticsEnabled,
                    onClick = onStartSplit
                )

                MenuModeCard(
                    title = stringResource(R.string.menu_mode_ai_vs_ai),
                    description = stringResource(R.string.menu_mode_ai_vs_ai_desc),
                    accentColor = Color(0xFFA855F7),
                    hapticsEnabled = prefs.hapticsEnabled,
                    onClick = { showAiVsAiDialog = true }
                )
            }

            if (showAiVsAiDialog) {
                AiVsAiConfigDialog(
                    initialDifficulty = prefs.aiDifficulty,
                    onDismiss = { showAiVsAiDialog = false },
                    onStartLocal = { d1, d2 ->
                        showAiVsAiDialog = false
                        onStartAiVsAiLocal(d1, d2)
                    },
                    onStartExtended = { d1, d2 ->
                        showAiVsAiDialog = false
                        onStartAiVsAiExtended(d1, d2)
                    },
                    onJoinAsGuest = {
                        showAiVsAiDialog = false
                        onStartExtended()
                    }
                )
            }

            // Anúncio em Banner não-invasivo
            AdBanner(
                adManager = adManager,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Rodapé elegante
            Text(
                text = "AIR HOCKEY ARCADE • v1.2",
                color = Color(0xFF475569),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun MenuModeCard(
    title: String,
    description: String,
    accentColor: Color,
    hapticsEnabled: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val cutShape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp)

    Card(
        onClick = {
            if (hapticsEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xD9080E1A)
        ),
        shape = cutShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.90f),
                        accentColor.copy(alpha = 0.35f)
                    )
                ),
                shape = cutShape
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            // Indicador luminoso neon vertical
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(26.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.35f)
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title.uppercase(),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Ícone arcade discreto de ação
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.70f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun InAppUpdateDownloadedBanner(
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E1B4B),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF818CF8)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Atualização Pronta",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Reinicie para aplicar a nova versão",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF818CF8)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Reiniciar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AiVsAiConfigDialog(
    initialDifficulty: dev.vmellos.airhockey.core.preferences.AiDifficulty,
    onDismiss: () -> Unit,
    onStartLocal: (dev.vmellos.airhockey.core.preferences.AiDifficulty, dev.vmellos.airhockey.core.preferences.AiDifficulty) -> Unit,
    onStartExtended: (dev.vmellos.airhockey.core.preferences.AiDifficulty, dev.vmellos.airhockey.core.preferences.AiDifficulty) -> Unit,
    onJoinAsGuest: () -> Unit
) {
    var isExtendedSelected by remember { mutableStateOf(false) }
    var diffBot1 by remember { mutableStateOf(initialDifficulty) }
    var diffBot2 by remember { mutableStateOf(initialDifficulty) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xF20B0F1C),
            shape = CutCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFF00E5FF),
                        Color(0xFFA855F7),
                        Color(0xFFFF5722)
                    )
                )
            ),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabeçalho com Ícone, Títulos e Botão Fechar (X)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x33A855F7),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66A855F7)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.ai_vs_ai_title),
                                color = Color(0xFFE2E8F0),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = stringResource(R.string.ai_vs_ai_subtitle),
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0x22FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Disposição da Mesa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 14.dp)
                            .background(Color(0xFFA855F7), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.table_layout),
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = { isExtendedSelected = false },
                        shape = CutCornerShape(10.dp),
                        color = if (!isExtendedSelected) Color(0x33A855F7) else Color(0xFF131A2A),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isExtendedSelected) Color(0xFFA855F7) else Color(0x22FFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.layout_local),
                            color = if (!isExtendedSelected) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }

                    Surface(
                        onClick = { isExtendedSelected = true },
                        shape = CutCornerShape(10.dp),
                        color = if (isExtendedSelected) Color(0x33A855F7) else Color(0xFF131A2A),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isExtendedSelected) Color(0xFFA855F7) else Color(0x22FFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.layout_extended),
                            color = if (isExtendedSelected) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Bot 1 (Ciano)
                Surface(
                    color = Color(0x1200E5FF),
                    shape = CutCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.bot_1_cyan),
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            dev.vmellos.airhockey.core.preferences.AiDifficulty.entries.forEach { d ->
                                val selected = diffBot1 == d
                                Surface(
                                    onClick = { diffBot1 = d },
                                    shape = CutCornerShape(6.dp),
                                    color = if (selected) Color(0xFF00E5FF) else Color(0x440B132B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) Color(0xFF00E5FF) else Color(0x2200E5FF)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = when (d) {
                                            dev.vmellos.airhockey.core.preferences.AiDifficulty.EASY -> stringResource(R.string.settings_ai_easy)
                                            dev.vmellos.airhockey.core.preferences.AiDifficulty.MEDIUM -> stringResource(R.string.settings_ai_medium)
                                            dev.vmellos.airhockey.core.preferences.AiDifficulty.HARD -> stringResource(R.string.settings_ai_hard)
                                        },
                                        color = if (selected) Color(0xFF0A0F1D) else Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card Bot 2 (Laranja)
                Surface(
                    color = Color(0x12FF5722),
                    shape = CutCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FF5722)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.bot_2_orange),
                                color = Color(0xFFFF5722),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            dev.vmellos.airhockey.core.preferences.AiDifficulty.entries.forEach { d ->
                                val selected = diffBot2 == d
                                Surface(
                                    onClick = { diffBot2 = d },
                                    shape = CutCornerShape(6.dp),
                                    color = if (selected) Color(0xFFFF5722) else Color(0x440B132B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) Color(0xFFFF5722) else Color(0x22FF5722)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = when (d) {
                                            dev.vmellos.airhockey.core.preferences.AiDifficulty.EASY -> stringResource(R.string.settings_ai_easy)
                                            dev.vmellos.airhockey.core.preferences.AiDifficulty.MEDIUM -> stringResource(R.string.settings_ai_medium)
                                            dev.vmellos.airhockey.core.preferences.AiDifficulty.HARD -> stringResource(R.string.settings_ai_hard)
                                        },
                                        color = if (selected) Color(0xFF0A0F1D) else Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botão de Iniciar Confronto com Gradiente Neon
                Button(
                    onClick = {
                        if (isExtendedSelected) {
                            onStartExtended(diffBot1, diffBot2)
                        } else {
                            onStartLocal(diffBot1, diffBot2)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    shape = CutCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFFA855F7),
                                    Color(0xFFFF5722)
                                )
                            ),
                            shape = CutCornerShape(12.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isExtendedSelected) {
                                stringResource(R.string.create_extended_table_host)
                            } else {
                                stringResource(R.string.start_showdown_local)
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                if (isExtendedSelected) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.extended_ai_guest_hint),
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onJoinAsGuest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.join_existing_as_guest),
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
