package dev.vmellos.airhockey.feature.menu.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Cabeçalho com botão de configurações no topo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
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

        // Título Neon Arcade
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Text(
                text = "AIR HOCKEY",
                color = Color(0xFF00E5FF),
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                color = Color(0xFF94A3B8),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Opções de Jogo
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MenuModeCard(
                title = stringResource(R.string.menu_mode_extended),
                description = stringResource(R.string.menu_mode_extended_desc),
                icon = Icons.Default.PhoneAndroid,
                accentColor = Color(0xFF00E5FF),
                isHighlight = true,
                onClick = onStartExtended
            )

            MenuModeCard(
                title = stringResource(R.string.menu_mode_split),
                description = stringResource(R.string.menu_mode_split_desc),
                icon = Icons.Default.Group,
                accentColor = Color(0xFFFF5722),
                isHighlight = false,
                onClick = onStartSplit
            )

            MenuModeCard(
                title = stringResource(R.string.menu_mode_solo),
                description = stringResource(R.string.menu_mode_solo_desc),
                icon = Icons.Default.Person,
                accentColor = Color(0xFF10B981),
                isHighlight = false,
                onClick = onStartSolo
            )
        }

        // Rodapé discreto
        Text(
            text = "v1.0 • P2P Wi-Fi / Hotspot",
            color = Color(0xFF475569),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun MenuModeCard(
    title: String,
    description: String,
    icon: ImageVector,
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
            modifier = Modifier.padding(20.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
