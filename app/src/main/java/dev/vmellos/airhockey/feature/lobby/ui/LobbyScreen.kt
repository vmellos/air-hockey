package dev.vmellos.airhockey.feature.lobby.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.feature.lobby.ConnectionStatus
import dev.vmellos.airhockey.feature.lobby.LobbyRole
import dev.vmellos.airhockey.feature.lobby.LobbyViewModel
import dev.vmellos.airhockey.feature.qrcode.QrCodeGenerator
import dev.vmellos.airhockey.feature.qrcode.QrCodeScannerView

@Composable
fun LobbyScreen(
    viewModel: LobbyViewModel,
    onNavigateToCalibration: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setScanningQr(true)
        }
    }

    if (state.isScanningQr) {
        Box(modifier = Modifier.fillMaxSize()) {
            QrCodeScannerView(
                onQrCodeScanned = { qrText ->
                    viewModel.handleQrScanResult(qrText)
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = { viewModel.setScanningQr(false) },
                modifier = Modifier
                    .padding(24.dp)
                    .background(Color(0x88000000), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBackToMenu) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.lobby_title),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (state.role == LobbyRole.NONE) {
            // Seleção de Papel (Anfitrião vs Convidado)
            RoleSelectionCards(
                onSelectHost = { viewModel.startAsHost() },
                onSelectGuest = { viewModel.startAsGuest() }
            )
        } else if (state.role == LobbyRole.HOST) {
            // Tela do Dispositivo Anfitrião
            HostLobbyView(
                state = state,
                onProceed = onNavigateToCalibration
            )
        } else if (state.role == LobbyRole.GUEST) {
            // Tela do Dispositivo Convidado
            GuestLobbyView(
                state = state,
                inputCode = inputCode,
                onInputCodeChange = { inputCode = it },
                onConnectCode = { viewModel.connectByCode(inputCode) },
                onConnectTable = { table -> viewModel.connectToTable(table.hostIp) },
                onScanQrClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        viewModel.setScanningQr(true)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onProceed = onNavigateToCalibration
            )
        }
    }
}

@Composable
private fun RoleSelectionCards(
    onSelectHost: () -> Unit,
    onSelectGuest: () -> Unit
) {
    Card(
        onClick = onSelectHost,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.lobby_role_host),
                color = Color(0xFF00E5FF),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.lobby_role_host_desc),
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Card(
        onClick = onSelectGuest,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.lobby_role_guest),
                color = Color(0xFFFF5722),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.lobby_role_guest_desc),
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun HostLobbyView(
    state: dev.vmellos.airhockey.feature.lobby.LobbyState,
    onProceed: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.lobby_host_table_code),
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )
        Text(
            text = state.tableCode,
            color = Color(0xFF00E5FF),
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 6.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // QR Code gerado offline
        val qrBitmap = remember(state.qrContent) {
            if (state.qrContent.isNotBlank()) QrCodeGenerator.generateQrBitmap(state.qrContent, 400) else null
        }

        if (qrBitmap != null) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(200.dp)
            ) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "Código QR da Mesa",
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.lobby_host_ip, state.hostIp),
            color = Color(0xFF64748B),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.connectionStatus == ConnectionStatus.CONNECTED) {
            Text(
                text = stringResource(R.string.lobby_status_connected),
                color = Color(0xFF10B981),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onProceed,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Avançar para Calibração", color = Color(0xFF0A0E1A), fontWeight = FontWeight.Bold)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.lobby_status_waiting_guest),
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun GuestLobbyView(
    state: dev.vmellos.airhockey.feature.lobby.LobbyState,
    inputCode: String,
    onInputCodeChange: (String) -> Unit,
    onConnectCode: () -> Unit,
    onConnectTable: (dev.vmellos.airhockey.feature.network.DiscoveredTable) -> Unit,
    onScanQrClick: () -> Unit,
    onProceed: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (state.connectionStatus == ConnectionStatus.CONNECTED) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.lobby_status_connected),
                        color = Color(0xFF34D399),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onProceed,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Avançar para Calibração", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        // Botão de Escanear QR Code
        Button(
            onClick = onScanQrClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.lobby_scan_qr), color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Digitação manual de código de 4 dígitos
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = inputCode,
                onValueChange = onInputCodeChange,
                placeholder = { Text(stringResource(R.string.lobby_enter_code_hint), color = Color(0xFF64748B)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onConnectCode,
                enabled = inputCode.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text(stringResource(R.string.lobby_connect_btn), color = Color(0xFF0A0E1A), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.lobby_discovered_tables),
            color = Color(0xFF94A3B8),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.discoveredTables.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                CircularProgressIndicator(color = Color(0xFFFF5722), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.lobby_no_tables_found),
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn {
                items(state.discoveredTables) { table ->
                    Card(
                        onClick = { onConnectTable(table) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF00E5FF))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(table.name, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(table.hostIp, color = Color(0xFF64748B), fontSize = 12.sp)
                                }
                            }
                            Text(
                                text = "#${table.tableCode}",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
