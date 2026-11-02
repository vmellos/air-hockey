package dev.vmellos.airhockey.feature.menu.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.ads.AdBanner
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.core.util.findActivity

@Composable
fun MainMenuScreen(
    onStartSolo: () -> Unit,
    onStartSplit: () -> Unit,
    onStartExtended: () -> Unit,
    onStartConnected: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        // Fundo com Grade Cibernética e Iluminação Neon Ambiente
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Gradiente radial Ciano no topo-esquerdo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1F00E5FF), Color.Transparent),
                    center = Offset(w * 0.2f, h * 0.15f),
                    radius = w * 0.7f
                ),
                radius = w * 0.7f,
                center = Offset(w * 0.2f, h * 0.15f)
            )

            // Gradiente radial Laranja na base-direita
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x18FF5722), Color.Transparent),
                    center = Offset(w * 0.8f, h * 0.8f),
                    radius = w * 0.7f
                ),
                radius = w * 0.7f,
                center = Offset(w * 0.8f, h * 0.8f)
            )

            // Grade cibernética sutil
            val gridStep = 44f
            var x = 0f
            while (x < w) {
                drawLine(
                    color = Color(0x0800E5FF),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = 0f
            while (y < h) {
                drawLine(
                    color = Color(0x0800E5FF),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }
        }

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
                        .background(Color(0xCC142038), CircleShape)
                        .border(1.dp, Color(0x33FFB800), CircleShape)
                        .size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = stringResource(R.string.leaderboard_title),
                        tint = Color(0xFFFFB800)
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
                                .background(Color(0xCC142038), CircleShape)
                                .border(1.dp, Color(0x3300E5FF), CircleShape)
                                .size(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = stringResource(R.string.remove_ads_menu_button),
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }

                    if (!isAdsRemovedPermanently && !isAdFreeTemporarily && isRewardedLoaded && activity != null) {
                        IconButton(
                            onClick = {
                                adManager.showRewarded(
                                    activity = activity,
                                    onRewardGranted = {},
                                    onDismiss = {}
                                )
                            },
                            modifier = Modifier
                                .background(Color(0xCC142038), CircleShape)
                                .border(1.dp, Color(0x33FF2A6D), CircleShape)
                                .size(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Redeem,
                                contentDescription = stringResource(R.string.ad_reward_button_desc),
                                tint = Color(0xFFFF2A6D)
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .background(Color(0xCC142038), CircleShape)
                            .border(1.dp, Color(0x3300E5FF), CircleShape)
                            .size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.menu_settings),
                            tint = Color(0xFF00E5FF)
                        )
                    }
                }
            }

            // Título Neon Arcade Volumétrico
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "AIR HOCKEY",
                    color = Color(0xFF00E5FF),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }

            // Cartões dos 4 Modos de Jogo no Padrão Arcade Glassmorphism
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MenuModeCard(
                    title = stringResource(R.string.menu_mode_connected),
                    description = stringResource(R.string.menu_mode_connected_desc),
                    badgeText = "WI-FI",
                    modeKey = "CONNECTED",
                    accentColor = Color(0xFF00E5FF),
                    hapticsEnabled = prefs.hapticsEnabled,
                    onClick = onStartConnected
                )

                MenuModeCard(
                    title = stringResource(R.string.menu_mode_extended),
                    description = stringResource(R.string.menu_mode_extended_desc),
                    badgeText = "2 TELAS",
                    modeKey = "EXTENDED",
                    accentColor = Color(0xFF818CF8),
                    hapticsEnabled = prefs.hapticsEnabled,
                    onClick = onStartExtended
                )

                MenuModeCard(
                    title = stringResource(R.string.menu_mode_split),
                    description = stringResource(R.string.menu_mode_split_desc),
                    badgeText = "1 TELA",
                    modeKey = "SPLIT",
                    accentColor = Color(0xFFFF5722),
                    hapticsEnabled = prefs.hapticsEnabled,
                    onClick = onStartSplit
                )

                MenuModeCard(
                    title = stringResource(R.string.menu_mode_solo),
                    description = stringResource(R.string.menu_mode_solo_desc),
                    badgeText = "VS IA",
                    modeKey = "SOLO",
                    accentColor = Color(0xFF10B981),
                    hapticsEnabled = prefs.hapticsEnabled,
                    onClick = onStartSolo
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
    badgeText: String,
    modeKey: String,
    accentColor: Color,
    hapticsEnabled: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        onClick = {
            if (hapticsEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xCC0D1527)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                color = accentColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            // Miniatura com Bordas Chanfradas da Mesa
            TableModeThumbnail(
                modeKey = modeKey,
                accentColor = accentColor,
                modifier = Modifier.size(width = 56.dp, height = 66.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.40f))
                    ) {
                        Text(
                            text = badgeText,
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Desenha a Miniatura da Mesa no Canvas demonstrando visualmente a arena de cada modo
 * com as novas Bordas Chanfradas Neon em 45 graus.
 */
@Composable
private fun TableModeThumbnail(
    modeKey: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(Color(0xFF090E1B), RoundedCornerShape(12.dp))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (modeKey) {
                "CONNECTED" -> {
                    // Dois aparelhos independentes com sinal sem fio
                    val phoneW = w * 0.38f
                    val phoneH = h * 0.88f
                    val topY = (h - phoneH) / 2f
                    val c = 4f

                    // Celular 1 (Esquerda - Ciano)
                    val p1Path = Path().apply {
                        moveTo(2f + c, topY)
                        lineTo(2f + phoneW - c, topY)
                        lineTo(2f + phoneW, topY + c)
                        lineTo(2f + phoneW, topY + phoneH - c)
                        lineTo(2f + phoneW - c, topY + phoneH)
                        lineTo(2f + c, topY + phoneH)
                        lineTo(2f, topY + phoneH - c)
                        lineTo(2f, topY + c)
                        close()
                    }
                    drawPath(p1Path, Color(0xFF142038))
                    drawPath(p1Path, Color(0xFF00E5FF), style = Stroke(width = 1.5f))
                    drawCircle(Color(0xFF00E5FF), radius = 3.5f, center = Offset(2f + phoneW / 2f, topY + phoneH * 0.78f))

                    // Celular 2 (Direita - Laranja)
                    val rightX = w - phoneW - 2f
                    val p2Path = Path().apply {
                        moveTo(rightX + c, topY)
                        lineTo(rightX + phoneW - c, topY)
                        lineTo(rightX + phoneW, topY + c)
                        lineTo(rightX + phoneW, topY + phoneH - c)
                        lineTo(rightX + phoneW - c, topY + phoneH)
                        lineTo(rightX + c, topY + phoneH)
                        lineTo(rightX, topY + phoneH - c)
                        lineTo(rightX, topY + c)
                        close()
                    }
                    drawPath(p2Path, Color(0xFF142038))
                    drawPath(p2Path, Color(0xFFFF5722), style = Stroke(width = 1.5f))
                    drawCircle(Color(0xFFFF5722), radius = 3.5f, center = Offset(rightX + phoneW / 2f, topY + phoneH * 0.78f))

                    // Ondas de sinal sem fio no centro
                    val midX = w / 2f
                    val midY = h / 2f
                    drawCircle(Color(0xFF00E5FF), radius = 2f, center = Offset(midX, midY))
                    drawArc(
                        color = Color(0xFF00E5FF),
                        startAngle = 135f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(midX - 7f, midY - 7f),
                        size = Size(14f, 14f),
                        style = Stroke(width = 1.5f)
                    )
                    drawArc(
                        color = Color(0xFF00E5FF),
                        startAngle = -45f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(midX - 7f, midY - 7f),
                        size = Size(14f, 14f),
                        style = Stroke(width = 1.5f)
                    )
                }

                "EXTENDED" -> {
                    // Dois aparelhos topo a topo formando uma única mesa contínua
                    val phoneW = w * 0.68f
                    val phoneH = (h * 0.46f)
                    val leftX = (w - phoneW) / 2f
                    val midY = h / 2f
                    val c = 4f

                    // Celular Superior (Laranja)
                    val topPath = Path().apply {
                        moveTo(leftX + c, 2f)
                        lineTo(leftX + phoneW - c, 2f)
                        lineTo(leftX + phoneW, 2f + c)
                        lineTo(leftX + phoneW, midY - 1f)
                        lineTo(leftX, midY - 1f)
                        lineTo(leftX, 2f + c)
                        close()
                    }
                    drawPath(topPath, Color(0xFF142038))
                    drawPath(topPath, Color(0xFFFF5722), style = Stroke(width = 1.5f))
                    drawCircle(Color(0xFFFF5722), radius = 3.5f, center = Offset(w / 2f, 11f))

                    // Celular Inferior (Ciano/Índigo)
                    val botPath = Path().apply {
                        moveTo(leftX, midY + 1f)
                        lineTo(leftX + phoneW, midY + 1f)
                        lineTo(leftX + phoneW, midY + phoneH - c)
                        lineTo(leftX + phoneW - c, midY + phoneH)
                        lineTo(leftX + c, midY + phoneH)
                        lineTo(leftX, midY + phoneH - c)
                        close()
                    }
                    drawPath(botPath, Color(0xFF142038))
                    drawPath(botPath, Color(0xFF818CF8), style = Stroke(width = 1.5f))
                    drawCircle(Color(0xFF818CF8), radius = 3.5f, center = Offset(w / 2f, h - 11f))

                    // Linha tracejada no vão de contato
                    drawLine(
                        color = Color(0xAAFFFFFF),
                        start = Offset(leftX - 3f, midY),
                        end = Offset(leftX + phoneW + 3f, midY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )

                    // Disco anelar cruzando o vão
                    drawCircle(Color(0xFF00E5FF), radius = 4f, center = Offset(w / 2f, midY))
                    drawCircle(Color(0xFF090E1B), radius = 2f, center = Offset(w / 2f, midY))
                }

                "SPLIT" -> {
                    // Um único aparelho na vertical dividido ao meio com cantos chanfrados
                    val phoneW = w * 0.74f
                    val phoneH = h * 0.94f
                    val leftX = (w - phoneW) / 2f
                    val topY = (h - phoneH) / 2f
                    val midY = topY + phoneH / 2f
                    val c = 5f

                    // Metade Superior (Laranja)
                    val topPath = Path().apply {
                        moveTo(leftX, midY)
                        lineTo(leftX, topY + c)
                        lineTo(leftX + c, topY)
                        lineTo(leftX + phoneW - c, topY)
                        lineTo(leftX + phoneW, topY + c)
                        lineTo(leftX + phoneW, midY)
                    }
                    drawPath(topPath, Color(0xFFFF5722), style = Stroke(width = 1.5f))

                    // Metade Inferior (Ciano)
                    val botPath = Path().apply {
                        moveTo(leftX, midY)
                        lineTo(leftX, topY + phoneH - c)
                        lineTo(leftX + c, topY + phoneH)
                        lineTo(leftX + phoneW - c, topY + phoneH)
                        lineTo(leftX + phoneW, topY + phoneH - c)
                        lineTo(leftX + phoneW, midY)
                    }
                    drawPath(botPath, Color(0xFF00E5FF), style = Stroke(width = 1.5f))

                    // Linha de centro e círculo central
                    drawLine(Color(0x66FFFFFF), Offset(leftX, midY), Offset(leftX + phoneW, midY), 1.2f)
                    drawCircle(Color(0x66FFFFFF), radius = 8f, center = Offset(w / 2f, midY), style = Stroke(1.2f))

                    // Rebatedor Superior (P2 Laranja)
                    drawCircle(Color(0xFFFF5722), radius = 4f, center = Offset(w / 2f, topY + 12f))

                    // Rebatedor Inferior (P1 Ciano)
                    drawCircle(Color(0xFF00E5FF), radius = 4f, center = Offset(w / 2f, topY + phoneH - 12f))

                    // Disco anelar
                    drawCircle(Color.White, radius = 3f, center = Offset(w / 2f + 7f, midY - 6f))
                }

                "SOLO" -> {
                    // Um único aparelho com IA no topo e humano na base
                    val phoneW = w * 0.74f
                    val phoneH = h * 0.94f
                    val leftX = (w - phoneW) / 2f
                    val topY = (h - phoneH) / 2f
                    val midY = topY + phoneH / 2f
                    val c = 5f

                    val tableBorder = Path().apply {
                        moveTo(leftX + c, topY)
                        lineTo(leftX + phoneW - c, topY)
                        lineTo(leftX + phoneW, topY + c)
                        lineTo(leftX + phoneW, topY + phoneH - c)
                        lineTo(leftX + phoneW - c, topY + phoneH)
                        lineTo(leftX + c, topY + phoneH)
                        lineTo(leftX, topY + phoneH - c)
                        lineTo(leftX, topY + c)
                        close()
                    }
                    drawPath(tableBorder, Color(0xFF10B981), style = Stroke(width = 1.5f))

                    // Linha de centro
                    drawLine(Color(0x6610B981), Offset(leftX, midY), Offset(leftX + phoneW, midY), 1.2f)
                    drawCircle(Color(0x6610B981), radius = 8f, center = Offset(w / 2f, midY), style = Stroke(1.2f))

                    // Rebatedor IA (Verde neon)
                    drawCircle(Color(0xFF10B981), radius = 4.5f, center = Offset(w / 2f, topY + 12f))

                    // Rebatedor Humano (Ciano)
                    drawCircle(Color(0xFF00E5FF), radius = 4.5f, center = Offset(w / 2f, topY + phoneH - 12f))

                    // Disco em movimento com rastro
                    drawCircle(Color(0x88FFFFFF), radius = 2f, center = Offset(w / 2f - 4f, midY + 8f))
                    drawCircle(Color.White, radius = 3f, center = Offset(w / 2f, midY + 3f))
                }
            }
        }
    }
}
