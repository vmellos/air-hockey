package dev.vmellos.airhockey.feature.customization.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.R

data class LockedArenaInfo(
    val titleRes: Int,
    val unlockStage: Int
)

/**
 * Diálogo unificado de alerta para itens ou arenas temáticas bloqueadas.
 * Direciona o jogador para a conquista correspondente no Modo Fases.
 */
@Composable
fun LockedArenaDialog(
    info: LockedArenaInfo,
    onDismiss: () -> Unit,
    onNavigateToStages: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CutCornerShape(16.dp),
        containerColor = Color(0xF20B0F1C),
        titleContentColor = Color.White,
        textContentColor = Color(0xFF94A3B8),
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFFFB800),
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.arena_locked_dialog_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = stringResource(R.string.arena_locked_dialog_desc, info.unlockStage),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onNavigateToStages()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = CutCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.play_stages_mode).uppercase(),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.close),
                    color = Color(0xFF94A3B8)
                )
            }
        }
    )
}
