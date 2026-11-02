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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.core.ads.AdBanner
import dev.vmellos.airhockey.core.util.findActivity
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.R

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
    val activity = LocalContext.current.findActivity()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Cabeçalho com Placar de Líderes, Remoção de Anúncios, Recompensas e Configurações
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão Placar de Líderes
            IconButton(
                onClick = onOpenLeaderboard,
                modifier = Modifier
                    .background(Color(0xFF1E293B), CircleShape)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = stringResource(R.string.leaderboard_title),
                    tint = Color(0xFFFFB800)
                )
            }

            // Ações da Direita (Remover Anúncios, Recompensa de Anúncio e Configurações)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isAdsRemovedPermanently && activity != null) {
                    IconButton(
                        onClick = { billingManager.launchPurchaseFlow(activity) },
                        modifier = Modifier
                            .background(Color(0xFF1E293B), CircleShape)
                            .size(44.dp)
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
                            .background(Color(0xFF1E293B), CircleShape)
                            .size(44.dp)
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
                        .background(Color(0xFF1E293B), CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.menu_settings),
                        tint = Color(0xFF00E5FF)
                    )
                }
            }
        }

        // Título Neon Arcade
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = "AIR HOCKEY",
                color = Color(0xFF00E5FF),
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Opções de Jogo com Miniaturas Neon da Mesa
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MenuModeCard(
                title = stringResource(R.string.menu_mode_connected),
                description = stringResource(R.string.menu_mode_connected_desc),
                modeKey = "CONNECTED",
                accentColor = Color(0xFF00E5FF),
                isHighlight = true,
                onClick = onStartConnected
            )

            MenuModeCard(
                title = stringResource(R.string.menu_mode_extended),
                description = stringResource(R.string.menu_mode_extended_desc),
                modeKey = "EXTENDED",
                accentColor = Color(0xFF818CF8),
                isHighlight = false,
                onClick = onStartExtended
            )

            MenuModeCard(
                title = stringResource(R.string.menu_mode_split),
                description = stringResource(R.string.menu_mode_split_desc),
                modeKey = "SPLIT",
                accentColor = Color(0xFFFF5722),
                isHighlight = false,
                onClick = onStartSplit
            )

            MenuModeCard(
                title = stringResource(R.string.menu_mode_solo),
                description = stringResource(R.string.menu_mode_solo_desc),
                modeKey = "SOLO",
                accentColor = Color(0xFF10B981),
                isHighlight = false,
                onClick = onStartSolo
            )
        }

        // Anúncio em Banner não-invasivo
        AdBanner(
            adManager = adManager,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Rodapé discreto
        Text(
            text = "v1.1 • P2P Wi-Fi / Hotspot",
            color = Color(0xFF475569),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun MenuModeCard(
    title: String,
    description: String,
    modeKey: String,
    accentColor: Color,
    isHighlight: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) Color(0xFF142038) else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHighlight) {
                    Modifier.border(2.dp, accentColor, RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            // Miniatura Ilustrativa da Mesa
            TableModeThumbnail(
                modeKey = modeKey,
                accentColor = accentColor,
                modifier = Modifier.size(width = 54.dp, height = 62.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
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
 * Desenha a Miniatura da Mesa no Canvas demonstrando visualmente a arena de cada modo.
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
            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (modeKey) {
                "CONNECTED" -> {
                    // Dois aparelhos independentes lado a lado com sinal sem fio
                    val phoneW = w * 0.38f
                    val phoneH = h * 0.85f
                    val topY = (h - phoneH) / 2f

                    // Celular 1 (Esquerda)
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(2f, topY),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0xFF00E5FF),
                        topLeft = Offset(2f, topY),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 1.5f)
                    )
                    // Linha central e rebatedor P1
                    drawLine(Color(0x5500E5FF), Offset(2f, topY + phoneH / 2f), Offset(2f + phoneW, topY + phoneH / 2f), 1f)
                    drawCircle(Color(0xFF00E5FF), radius = 3.5f, center = Offset(2f + phoneW / 2f, topY + phoneH * 0.78f))

                    // Celular 2 (Direita)
                    val rightX = w - phoneW - 2f
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(rightX, topY),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0xFFFF5722),
                        topLeft = Offset(rightX, topY),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 1.5f)
                    )
                    // Linha central e rebatedor P2
                    drawLine(Color(0x55FF5722), Offset(rightX, topY + phoneH / 2f), Offset(rightX + phoneW, topY + phoneH / 2f), 1f)
                    drawCircle(Color(0xFFFF5722), radius = 3.5f, center = Offset(rightX + phoneW / 2f, topY + phoneH * 0.78f))

                    // Ondas sem fio no centro
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
                    // Dois aparelhos topo a topo formando uma mesa contínua
                    val phoneW = w * 0.65f
                    val phoneH = (h * 0.46f)
                    val leftX = (w - phoneW) / 2f
                    val midY = h / 2f

                    // Celular Superior (P2)
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(leftX, 2f),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0xFFFF5722),
                        topLeft = Offset(leftX, 2f),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(Color(0xFFFF5722), radius = 3.5f, center = Offset(w / 2f, 12f))

                    // Celular Inferior (P1)
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(leftX, midY + 1f),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0xFF818CF8),
                        topLeft = Offset(leftX, midY + 1f),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(Color(0xFF818CF8), radius = 3.5f, center = Offset(w / 2f, h - 12f))

                    // Linha pontilhada no vão físico de contato
                    drawLine(
                        color = Color(0xAAFFFFFF),
                        start = Offset(leftX - 3f, midY),
                        end = Offset(leftX + phoneW + 3f, midY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )

                    // Disco brilhante cruzando a junção dos aparelhos
                    drawCircle(Color(0xFF00E5FF), radius = 4f, center = Offset(w / 2f, midY))
                    drawCircle(Color.White, radius = 2.5f, center = Offset(w / 2f, midY))
                }

                "SPLIT" -> {
                    // Um único aparelho na vertical dividido ao meio
                    val phoneW = w * 0.72f
                    val phoneH = h * 0.92f
                    val leftX = (w - phoneW) / 2f
                    val topY = (h - phoneH) / 2f
                    val midY = topY + phoneH / 2f

                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(leftX, topY),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = Color(0xFFFF5722),
                        topLeft = Offset(leftX, topY),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 1.5f)
                    )

                    // Linha de centro e círculo central
                    drawLine(Color(0x66FF5722), Offset(leftX, midY), Offset(leftX + phoneW, midY), 1.5f)
                    drawCircle(Color(0x66FF5722), radius = 8f, center = Offset(w / 2f, midY), style = Stroke(1.2f))

                    // Rebatedor Superior (P2 Laranja)
                    drawCircle(Color(0xFFFF5722), radius = 4f, center = Offset(w / 2f, topY + 14f))

                    // Rebatedor Inferior (P1 Azul)
                    drawCircle(Color(0xFF00E5FF), radius = 4f, center = Offset(w / 2f, topY + phoneH - 14f))

                    // Disco no campo
                    drawCircle(Color.White, radius = 3f, center = Offset(w / 2f + 8f, midY - 6f))
                }

                "SOLO" -> {
                    // Um único aparelho com IA no topo e humano na base
                    val phoneW = w * 0.72f
                    val phoneH = h * 0.92f
                    val leftX = (w - phoneW) / 2f
                    val topY = (h - phoneH) / 2f
                    val midY = topY + phoneH / 2f

                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(leftX, topY),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = Color(0xFF10B981),
                        topLeft = Offset(leftX, topY),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 1.5f)
                    )

                    // Linha de centro
                    drawLine(Color(0x6610B981), Offset(leftX, midY), Offset(leftX + phoneW, midY), 1.5f)
                    drawCircle(Color(0x6610B981), radius = 8f, center = Offset(w / 2f, midY), style = Stroke(1.2f))

                    // Rebatedor IA (Verde neon com quadrado interno simbolizando CPU)
                    drawCircle(Color(0xFF10B981), radius = 4.5f, center = Offset(w / 2f, topY + 14f))

                    // Rebatedor Humano (Ciano)
                    drawCircle(Color(0xFF00E5FF), radius = 4.5f, center = Offset(w / 2f, topY + phoneH - 14f))

                    // Disco em movimento com rastro
                    drawCircle(Color(0x88FFFFFF), radius = 2f, center = Offset(w / 2f - 4f, midY + 8f))
                    drawCircle(Color.White, radius = 3f, center = Offset(w / 2f, midY + 3f))
                }
            }
        }
    }
}
