package dev.vmellos.airhockey.feature.lobby.ui

import dev.vmellos.airhockey.AirHockeyApp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    var inputCode by remember { mutableStateOf("") }
    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.lobby_edit_name), color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 24) nameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = nameInput.trim()
                        if (newName.isNotBlank()) {
                            scope.launch {
                                AirHockeyApp.instance.preferencesRepository.setPlayerName(newName)
                            }
                        }
                        showNameDialog = false
                    }
                ) {
                    Text("Salvar", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    androidx.compose.runtime.LaunchedEffect(state.navigateToCalibrationTrigger) {
        if (state.navigateToCalibrationTrigger > 0L) {
            onNavigateToCalibration()
        }
    }

    val handleBack = {
        if (state.isScanningQr) {
            viewModel.setScanningQr(false)
        } else if (state.role != LobbyRole.NONE) {
            viewModel.stopAll()
        } else {
            onBackToMenu()
        }
    }

    BackHandler {
        handleBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setScanningQr(false)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setScanningQr(true)
        }
    }

    val permissionsToRequest = remember {
        mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            if (Build.VERSION.SDK_INT >= 34) {
                // Permissão de proteção de rede local introduzida no Android 14+/15/16/17
                add("android.permission.ACCESS_LOCAL_NETWORK")
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }.toTypedArray()
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val missing = permissionsToRequest.filter { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionsLauncher.launch(missing.toTypedArray())
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = handleBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                val titleText = if (state.targetMode == "CONNECTED") {
                    stringResource(R.string.lobby_title_connected)
                } else {
                    stringResource(R.string.lobby_title)
                }
                Text(
                    text = titleText,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Chip com Nome do Jogador
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.clickable {
                    nameInput = state.selfPlayerName
                    showNameDialog = true
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.selfPlayerName.ifBlank { "Jogador" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (state.role == LobbyRole.NONE) {
            // Seleção de Papel (Anfitrião vs Convidado)
            RoleSelectionCards(
                onSelectHost = { viewModel.startAsHost() },
                onSelectGuest = { viewModel.startAsGuest() }
            )
        } else if (state.role == LobbyRole.HOST) {
            val userPrefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = dev.vmellos.airhockey.core.preferences.UserPreferences())
            // Tela do Dispositivo Anfitrião
            HostLobbyView(
                state = state,
                onProceed = {
                    if (state.targetMode == "CONNECTED") {
                        viewModel.startHostMatch(0f, userPrefs.puckSpeed.maxSpeed)
                    } else {
                        viewModel.notifyNavigateToCalibration()
                        onNavigateToCalibration()
                    }
                }
            )
        } else if (state.role == LobbyRole.GUEST) {
            // Tela do Dispositivo Convidado
            GuestLobbyView(
                state = state,
                inputCode = inputCode,
                onInputCodeChange = { inputCode = it },
                onConnectCode = { viewModel.connectByCode(inputCode) },
                onConnectTable = { table, code -> viewModel.connectToTable(table.hostIp, code) },
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
    val context = LocalContext.current

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
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 6.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // QR Code gerado offline
        val qrBitmap = remember(state.qrContent) {
            if (state.qrContent.isNotBlank()) QrCodeGenerator.generateQrBitmap(state.qrContent, 400) else null
        }

        if (qrBitmap != null) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(190.dp)
            ) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "Código QR da Mesa",
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                )
            }
        }

        if (state.hostIp.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "IP da Mesa: ",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
                Text(
                    text = "${state.hostIp}:8888",
                    color = Color(0xFF00E5FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Card de Instruções e Status da Rede Wi-Fi
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (state.isWifiConnected) Color(0xFF00E5FF) else Color(0xFFFF9800),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val networkLabel = if (state.wifiSsid.isNotBlank()) {
                        stringResource(R.string.lobby_network_ssid, state.wifiSsid)
                    } else if (state.isWifiConnected) {
                        stringResource(R.string.lobby_network_wifi_connected, state.hostIp)
                    } else {
                        stringResource(R.string.lobby_network_no_wifi)
                    }
                    Text(
                        text = networkLabel,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.lobby_network_same_wifi),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent("android.settings.TETHER_SETTINGS").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(fallback)
                            } catch (_: Exception) {}
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.WifiTethering,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.lobby_network_open_hotspot),
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.connectionStatus == ConnectionStatus.CONNECTED) {
            Text(
                text = stringResource(R.string.lobby_status_connected),
                color = Color(0xFF10B981),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onProceed,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                val buttonText = if (state.targetMode == "CONNECTED") {
                    stringResource(R.string.lobby_btn_start_match)
                } else {
                    "Avançar para Calibração"
                }
                Text(buttonText, color = Color(0xFF0A0E1A), fontWeight = FontWeight.Bold)
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
    onConnectTable: (dev.vmellos.airhockey.feature.network.DiscoveredTable, String) -> Unit,
    onScanQrClick: () -> Unit,
    onProceed: () -> Unit
) {
    var selectedTableForPin by remember { mutableStateOf<dev.vmellos.airhockey.feature.network.DiscoveredTable?>(null) }
    var pinDialogInput by remember { mutableStateOf("") }

    val tableForPin = selectedTableForPin
    if (tableForPin != null) {
        AlertDialog(
            onDismissRequest = { selectedTableForPin = null },
            title = {
                Text(
                    text = stringResource(R.string.lobby_pin_dialog_title),
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    val hostName = tableForPin.hostName.ifBlank { "Anfitrião" }
                    Text(
                        text = stringResource(R.string.lobby_table_by, hostName),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.lobby_pin_dialog_desc),
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = pinDialogInput,
                        onValueChange = { if (it.length <= 6) pinDialogInput = it },
                        placeholder = { Text(stringResource(R.string.lobby_pin_dialog_hint), color = Color(0xFF64748B)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val entered = pinDialogInput.trim()
                        if (entered.isNotBlank()) {
                            onConnectTable(tableForPin, entered)
                            selectedTableForPin = null
                        }
                    },
                    enabled = pinDialogInput.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text(stringResource(R.string.lobby_connect_btn), color = Color(0xFF0A0E1A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTableForPin = null }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

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
                    if (state.targetMode == "CONNECTED") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF34D399),
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.lobby_waiting_host_start),
                                color = Color(0xFFA7F3D0),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Button(
                            onClick = onProceed,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Avançar para Calibração", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            return
        }

        if (state.errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1E1E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = state.errorMessage, color = Color(0xFFFFCDD2), fontSize = 13.sp)
                }
            }
        }

        if (state.connectionStatus == ConnectionStatus.CONNECTING) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(20.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF5722), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = stringResource(R.string.lobby_status_connecting),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
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

        // Digitação manual de código de 4 dígitos ou endereço IP direto
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = inputCode,
                onValueChange = onInputCodeChange,
                placeholder = { Text("Código (4 dígitos) ou IP do Host", color = Color(0xFF64748B), fontSize = 13.sp) },
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
                        onClick = {
                            pinDialogInput = ""
                            selectedTableForPin = table
                        },
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
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF00E5FF))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    val title = if (table.hostName.isNotBlank()) {
                                        stringResource(R.string.lobby_table_by, table.hostName)
                                    } else {
                                        table.name
                                    }
                                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                                    val modeLabel = if (table.mode == "CONNECTED") {
                                        stringResource(R.string.menu_mode_connected)
                                    } else {
                                        stringResource(R.string.menu_mode_extended)
                                    }
                                    Text("${table.hostIp} • $modeLabel", color = Color(0xFF64748B), fontSize = 12.sp)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = "Privada", tint = Color(0xFFFF5722), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Privada",
                                    color = Color(0xFFFF5722),
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
}
