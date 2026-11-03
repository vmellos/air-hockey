package dev.vmellos.airhockey.feature.lobby.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.feature.lobby.LobbyRole

@Composable
fun BezelCalibrationScreen(
    role: LobbyRole,
    initialGapMm: Float = 8.0f,
    isPeerReady: Boolean = false,
    isSelfReady: Boolean = false,
    onReadyClick: (Float) -> Unit,
    onBackToLobby: () -> Unit,
    modifier: Modifier = Modifier
) {
    var gapMm by remember { mutableFloatStateOf(initialGapMm) }

    BackHandler {
        onBackToLobby()
    }

    // Animação contínua do disco de teste transitando pelo vão físico
    val transition = rememberInfiniteTransition(label = "puck_calibration")
    val puckProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "puck_y"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Barra Superior
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBackToLobby) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.calibration_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Card didático com instrução clara
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.calibration_bezel_explanation),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visualizador animado com ilustração dos dois celulares topo-a-topo
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val gapPx = gapMm * 2.8f

                // Desenha silhuetas dos dois telefones
                val phoneW = (w * 0.58f).coerceAtMost(240f)
                val phoneLeft = (w - phoneW) / 2f

                // Telefone Superior (Jogador 2)
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(phoneLeft, 10f),
                    size = Size(phoneW, midY - gapPx / 2f - 10f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = Color(0xFFFF5722),
                    topLeft = Offset(phoneLeft, 10f),
                    size = Size(phoneW, midY - gapPx / 2f - 10f),
                    cornerRadius = CornerRadius(16f, 16f),
                    style = Stroke(width = 2.5f)
                )

                // Telefone Inferior (Jogador 1)
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(phoneLeft, midY + gapPx / 2f),
                    size = Size(phoneW, h - midY - gapPx / 2f - 10f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(phoneLeft, midY + gapPx / 2f),
                    size = Size(phoneW, h - midY - gapPx / 2f - 10f),
                    cornerRadius = CornerRadius(16f, 16f),
                    style = Stroke(width = 2.5f)
                )

                // Linha pontilhada no vão físico central
                drawLine(
                    color = Color(0x66FFFFFF),
                    start = Offset(phoneLeft - 20f, midY),
                    end = Offset(phoneLeft + phoneW + 20f, midY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Disco simulado transitando suavemente entre os dois aparelhos
                val puckY = (h * 0.18f) + (h * 0.64f) * puckProgress
                val inGap = puckY in (midY - gapPx / 2f)..(midY + gapPx / 2f)
                val puckAlpha = if (inGap) 0.35f else 1.0f

                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = puckAlpha * 0.4f),
                    radius = 26f,
                    center = Offset(w / 2f, puckY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = puckAlpha),
                    radius = 16f,
                    center = Offset(w / 2f, puckY)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Predefinições Rápidas (Chips)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetChip(
                label = stringResource(R.string.calibration_preset_thin),
                isSelected = gapMm in 3.5f..4.5f,
                onClick = { gapMm = 4.0f },
                modifier = Modifier.weight(1f)
            )
            PresetChip(
                label = stringResource(R.string.calibration_preset_standard),
                isSelected = gapMm in 7.5f..8.5f,
                onClick = { gapMm = 8.0f },
                modifier = Modifier.weight(1f)
            )
            PresetChip(
                label = stringResource(R.string.calibration_preset_thick),
                isSelected = gapMm in 13.5f..14.5f,
                onClick = { gapMm = 14.0f },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Valor numérico e Slider de Ajuste Fino
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.calibration_gap_label, gapMm),
                color = Color(0xFF00E5FF),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "0 mm — 20 mm",
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
        }

        Slider(
            value = gapMm,
            onValueChange = { gapMm = it },
            valueRange = 0f..20f,
            steps = 39,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF),
                inactiveTrackColor = Color(0xFF334155)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Botão de Ação (Pronto / Iniciar)
        val isGuest = role == LobbyRole.GUEST
        val buttonText = when {
            isGuest && isSelfReady -> "Aguardando anfitrião iniciar..."
            isGuest -> stringResource(R.string.calibration_ready)
            !isGuest && isPeerReady -> "Iniciar Partida (Ambos Prontos)"
            !isGuest -> "Iniciar Partida (Aguardando Jogador 2)"
            else -> stringResource(R.string.calibration_ready)
        }
        val buttonContainerColor = when {
            isGuest && isSelfReady -> Color(0xFF334155)
            !isGuest && isPeerReady -> Color(0xFF10B981)
            else -> Color(0xFF00E5FF)
        }
        val isEnabled = !(isGuest && isSelfReady)

        Button(
            onClick = { onReadyClick(gapMm) },
            enabled = isEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonContainerColor,
                disabledContainerColor = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isGuest && isSelfReady) {
                CircularProgressIndicator(
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = buttonText,
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = buttonText,
                    color = if (buttonContainerColor == Color(0xFF10B981)) Color.White else Color(0xFF0A0E1A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E293B),
                RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
