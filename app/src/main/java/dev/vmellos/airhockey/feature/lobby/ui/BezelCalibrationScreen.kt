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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    // Animação cíclica do disco de teste transitando pelo vão
    val transition = rememberInfiniteTransition(label = "puck_calibration")
    val puckProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "puck_y"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBackToLobby) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.calibration_title),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.calibration_instruction),
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Visualizador de calibração em tempo real
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

                // Linha de borda superior onde os aparelhos se tocam
                val edgeY = if (role == LobbyRole.HOST) h - 10f else 10f
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(0f, edgeY),
                    end = Offset(w, edgeY),
                    strokeWidth = 4f
                )

                // Posição vertical simulada do disco de teste
                val puckY = if (role == LobbyRole.HOST) {
                    (h * 0.4f) + (h * 0.6f + gapMm * 10f) * puckProgress
                } else {
                    (h * 0.6f) - (h * 0.6f + gapMm * 10f) * puckProgress
                }

                // Desenha o disco de teste
                if (puckY in -50f..(h + 50f)) {
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 28f,
                        center = Offset(w / 2f, puckY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 20f,
                        center = Offset(w / 2f, puckY)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.calibration_gap_label, gapMm),
            color = Color(0xFF00E5FF),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

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

        Spacer(modifier = Modifier.height(16.dp))

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
                .height(52.dp)
        ) {
            if (isGuest && isSelfReady) {
                CircularProgressIndicator(
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = buttonText,
                    color = Color(0xFF94A3B8),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = buttonText,
                    color = if (buttonContainerColor == Color(0xFF10B981)) Color.White else Color(0xFF0A0E1A),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
