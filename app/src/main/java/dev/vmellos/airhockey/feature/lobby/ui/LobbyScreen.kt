package dev.vmellos.airhockey.feature.lobby.ui

import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.core.ads.AdBanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.feature.customization.ui.PreMatchArenaSelectionScreen
import dev.vmellos.airhockey.feature.lobby.ConnectionStatus
import dev.vmellos.airhockey.feature.lobby.LobbyRole
import dev.vmellos.airhockey.feature.lobby.LobbyViewModel
import dev.vmellos.airhockey.feature.qrcode.QrCodeGenerator
import dev.vmellos.airhockey.feature.qrcode.QrCodeScannerView
import dev.vmellos.airhockey.feature.stage.ArenaConquestCatalog

@Composable
fun LobbyScreen(
    viewModel: LobbyViewModel,
    onBackToMenu: () -> Unit,
    onStartHostMatch: (() -> Unit)? = null,
    onStartAiVsAiLocal: ((dev.vmellos.airhockey.core.preferences.AiDifficulty, dev.vmellos.airhockey.core.preferences.AiDifficulty, dev.vmellos.airhockey.core.preferences.TableTheme, dev.vmellos.airhockey.core.preferences.ArenaDimension) -> Unit)? = null,
    onNavigateToStages: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputCode by remember { mutableStateOf("") }
    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var showCreateHostDialog by remember { mutableStateOf(false) }
    var showArenaSelection by remember { mutableStateOf(false) }
    var pendingHostMode by remember { mutableStateOf<String?>(null) }
    var pendingAiDiff1 by remember { mutableStateOf<dev.vmellos.airhockey.core.preferences.AiDifficulty?>(null) }
    var pendingAiDiff2 by remember { mutableStateOf<dev.vmellos.airhockey.core.preferences.AiDifficulty?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (showArenaSelection && pendingHostMode != null) {
        val mode = pendingHostMode!!
        val isAiLocal = mode == "AI_LOCAL"
        val confirmText = if (isAiLocal) {
            stringResource(R.string.pre_match_start_game)
        } else {
            stringResource(R.string.pre_match_confirm_host)
        }

        BackHandler {
            showArenaSelection = false
            showCreateHostDialog = true
        }

        val isExtended = mode == "EXTENDED" || mode == "AI_EXTENDED"

        PreMatchArenaSelectionScreen(
            confirmButtonText = confirmText,
            isSplitScreen = false,
            isExtendedTable = isExtended,
            onBack = {
                showArenaSelection = false
                showCreateHostDialog = true
            },
            onNavigateToStages = onNavigateToStages,
            onConfirm = { chosenTheme, chosenDimension ->
                showArenaSelection = false
                if (isAiLocal) {
                    val d1 = pendingAiDiff1 ?: dev.vmellos.airhockey.core.preferences.AiDifficulty.MEDIUM
                    val d2 = pendingAiDiff2 ?: dev.vmellos.airhockey.core.preferences.AiDifficulty.MEDIUM
                    onStartAiVsAiLocal?.invoke(d1, d2, chosenTheme, chosenDimension)
                } else {
                    val d1Str = pendingAiDiff1?.name ?: ""
                    val d2Str = pendingAiDiff2?.name ?: ""
                    viewModel.startAsHost(mode, d1Str, d2Str, chosenDimension.name, chosenTheme.name)
                }
            }
        )
        return
    }

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
                    Text(stringResource(R.string.settings_player_name_save), color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val defName = AirHockeyApp.instance.preferencesRepository.resetPlayerNameToDefault()
                                nameInput = defName
                            }
                        }
                    ) {
                        Text(stringResource(R.string.settings_player_name_reset), color = Color(0xFF94A3B8))
                    }
                    TextButton(onClick = { showNameDialog = false }) {
                        Text(stringResource(R.string.cancel), color = Color(0xFF94A3B8))
                    }
                }
            },
            containerColor = Color(0xFF1E293B)
        )
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_fiery_puck),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x88070B14),
                            Color(0x66070B14),
                            Color(0x99070B14)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = if (isLandscape) 700.dp else 560.dp)
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(horizontal = 20.dp, vertical = if (isLandscape) 8.dp else 20.dp)
            ) {
                // Linha 1: Botão Voltar e Título da Tela
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = handleBack,
                        modifier = Modifier
                            .size(if (isLandscape) 38.dp else 44.dp)
                            .background(Color(0xB3070D1A), CircleShape)
                            .border(1.2.dp, Color(0x6600E5FF), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(if (isLandscape) 18.dp else 20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    val titleText = when {
                        state.role == LobbyRole.NONE -> stringResource(R.string.menu_mode_network_centralized).uppercase()
                        state.role == LobbyRole.GUEST -> stringResource(R.string.lobby_title).uppercase()
                        state.targetMode == "CONNECTED" -> stringResource(R.string.lobby_title_connected)
                        state.targetMode == "AI_EXTENDED" || state.targetMode == "AI_CONNECTED" -> stringResource(R.string.lobby_title_ai_extended).uppercase()
                        else -> stringResource(R.string.lobby_title_extended)
                    }
                Text(
                    text = titleText,
                    color = Color.White,
                    fontSize = if (isLandscape) 17.sp else 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

            // Linha 2: Card / Chip com Nome do Dispositivo / Jogador com largura total
            Surface(
                color = Color(0xD9080E1A),
                shape = CutCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x4400E5FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        nameInput = state.selfPlayerName
                        showNameDialog = true
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = if (isLandscape) 6.dp else 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.lobby_device_player_title),
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = state.selfPlayerName.ifBlank { stringResource(R.string.player_1_default) },
                                color = Color.White,
                                fontSize = if (isLandscape) 14.sp else 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.lobby_edit_name),
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (state.role == LobbyRole.NONE) {
                    // Seleção de Papel (Anfitrião vs Convidado)
                    RoleSelectionCards(
                        onSelectHost = { showCreateHostDialog = true },
                        onSelectGuest = { viewModel.startAsGuest() }
                    )
                } else if (state.role == LobbyRole.HOST) {
                    val userPrefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = dev.vmellos.airhockey.core.preferences.UserPreferences())
                    val proceedAction = remember(state.targetMode, state.hostThemeName, state.hostArenaDimension, userPrefs.bezelGapMm, userPrefs.puckSpeed.maxSpeed, onStartHostMatch) {
                        {
                            if (onStartHostMatch != null) {
                                onStartHostMatch()
                            } else {
                                val gap = if (state.targetMode == "CONNECTED" || state.targetMode == "AI_CONNECTED") 0f else userPrefs.bezelGapMm
                                val theme = state.hostThemeName ?: userPrefs.tableTheme.name
                                val dim = state.hostArenaDimension ?: userPrefs.arenaDimension.name
                                viewModel.startHostMatch(gap, userPrefs.puckSpeed.maxSpeed, themeName = theme, arenaDimension = dim)
                            }
                        }
                    }

                    // Início automático e direto assim que o convidado conecta e valida o handshake
                    LaunchedEffect(state.role, state.connectionStatus) {
                        if (state.role == LobbyRole.HOST && state.connectionStatus == ConnectionStatus.CONNECTED) {
                            delay(350)
                            if (state.role == LobbyRole.HOST && state.connectionStatus == ConnectionStatus.CONNECTED) {
                                proceedAction()
                            }
                        }
                    }

                    // Tela do Dispositivo Anfitrião
                    HostLobbyView(
                        state = state,
                        onChangeMode = { showCreateHostDialog = true },
                        onProceed = proceedAction
                    )
                } else if (state.role == LobbyRole.GUEST) {
                    // Tela do Dispositivo Convidado
                    GuestLobbyView(
                        state = state,
                        inputCode = inputCode,
                        onInputCodeChange = { inputCode = it },
                        onConnectCode = { viewModel.connectByCode(inputCode) },
                        onConnectTable = { table, code -> viewModel.connectToTable(table.hostIp, code, table.mode) },
                        onRefreshDiscovery = { viewModel.refreshDiscovery() },
                        onScanQrClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                viewModel.setScanningQr(true)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AdBanner(
                adManager = AirHockeyApp.instance.adManager,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }

        if (showCreateHostDialog) {
            val userPrefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = dev.vmellos.airhockey.core.preferences.UserPreferences())
            HostCreateModeDialog(
                initialDifficulty = userPrefs.aiDifficulty,
                onDismiss = { showCreateHostDialog = false },
                onSelectExtended = {
                    showCreateHostDialog = false
                    pendingHostMode = "EXTENDED"
                    pendingAiDiff1 = null
                    pendingAiDiff2 = null
                    showArenaSelection = true
                },
                onSelectConnected = {
                    showCreateHostDialog = false
                    pendingHostMode = "CONNECTED"
                    pendingAiDiff1 = null
                    pendingAiDiff2 = null
                    showArenaSelection = true
                },
                onSelectAiExtended = { d1, d2 ->
                    showCreateHostDialog = false
                    pendingHostMode = "AI_EXTENDED"
                    pendingAiDiff1 = d1
                    pendingAiDiff2 = d2
                    showArenaSelection = true
                },
                onSelectAiLocal = onStartAiVsAiLocal?.let {
                    { d1, d2 ->
                        showCreateHostDialog = false
                        pendingHostMode = "AI_LOCAL"
                        pendingAiDiff1 = d1
                        pendingAiDiff2 = d2
                        showArenaSelection = true
                    }
                }
            )
        }
    }
}
}

@Composable
private fun RoleSelectionCards(
    onSelectHost: () -> Unit,
    onSelectGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val hostShape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp)
    val guestShape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp)

    val hostCard = @Composable { cardModifier: Modifier ->
        Card(
            onClick = onSelectHost,
            colors = CardDefaults.cardColors(containerColor = Color(0xD9080E1A)),
            shape = hostShape,
            modifier = cardModifier
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.90f),
                            Color(0xFF00E5FF).copy(alpha = 0.35f)
                        )
                    ),
                    shape = hostShape
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (isLandscape) 12.dp else 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(36.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF00E5FF).copy(alpha = 0.35f))
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.lobby_role_host).uppercase(),
                        color = Color.White,
                        fontSize = if (isLandscape) 15.sp else 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.lobby_role_host_desc),
                        color = Color(0xFF94A3B8),
                        fontSize = if (isLandscape) 11.5.sp else 12.5.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF).copy(alpha = 0.70f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    val guestCard = @Composable { cardModifier: Modifier ->
        Card(
            onClick = onSelectGuest,
            colors = CardDefaults.cardColors(containerColor = Color(0xD9080E1A)),
            shape = guestShape,
            modifier = cardModifier
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFF5722).copy(alpha = 0.90f),
                            Color(0xFFFF5722).copy(alpha = 0.35f)
                        )
                    ),
                    shape = guestShape
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (isLandscape) 12.dp else 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(36.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFFFF5722), Color(0xFFFF5722).copy(alpha = 0.35f))
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.lobby_role_guest).uppercase(),
                        color = Color.White,
                        fontSize = if (isLandscape) 15.sp else 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.lobby_role_guest_desc),
                        color = Color(0xFF94A3B8),
                        fontSize = if (isLandscape) 11.5.sp else 12.5.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFFFF5722).copy(alpha = 0.70f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (isLandscape) {
        // Layout Lado a Lado no Modo Paisagem (ADR 0037)
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            hostCard(Modifier.weight(1f))
            guestCard(Modifier.weight(1f))
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            hostCard(Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            guestCard(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HostLobbyView(
    state: dev.vmellos.airhockey.feature.lobby.LobbyState,
    onChangeMode: (() -> Unit)? = null,
    onProceed: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val (hostModeColor, hostModeLabel, hostModeIcon) = when (state.targetMode) {
        "CONNECTED" -> Triple(Color(0xFF00E5FF), stringResource(R.string.lobby_mode_badge_connected), Icons.Default.Wifi)
        "AI_EXTENDED", "AI_CONNECTED" -> Triple(Color(0xFFA855F7), stringResource(R.string.lobby_mode_badge_ai), Icons.Default.SmartToy)
        else -> Triple(Color(0xFF818CF8), stringResource(R.string.lobby_mode_badge_extended), Icons.Default.PhoneAndroid)
    }

    val userPrefs by AirHockeyApp.instance.preferencesRepository.userPreferencesFlow.collectAsState(initial = dev.vmellos.airhockey.core.preferences.UserPreferences())
    val arenaTier = remember(userPrefs.tableTheme) { ArenaConquestCatalog.getTierForTheme(userPrefs.tableTheme) }

    val qrBitmap = remember(state.qrContent) {
        if (state.qrContent.isNotBlank()) QrCodeGenerator.generateQrBitmap(state.qrContent, 400) else null
    }

    val badgesRow = @Composable {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = hostModeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, hostModeColor.copy(alpha = 0.5f)),
                modifier = Modifier.then(
                    if (onChangeMode != null) Modifier.clickable { onChangeMode() } else Modifier
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(hostModeIcon, contentDescription = null, tint = hostModeColor, modifier = Modifier.size(16.dp))
                    Text(
                        text = hostModeLabel.uppercase(),
                        color = hostModeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (onChangeMode != null) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.content_desc_change_mode),
                            tint = hostModeColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            if (arenaTier != null) {
                Surface(
                    color = arenaTier.primaryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, arenaTier.primaryColor.copy(alpha = 0.5f)),
                    modifier = Modifier.then(
                        if (onChangeMode != null) Modifier.clickable { onChangeMode() } else Modifier
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(arenaTier.icon, contentDescription = null, tint = arenaTier.primaryColor, modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(arenaTier.titleRes).uppercase(),
                            color = arenaTier.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    }

    val tableCodeSection = @Composable {
        Text(
            text = stringResource(R.string.lobby_host_table_code),
            color = Color(0xFF94A3B8),
            fontSize = if (isLandscape) 12.sp else 14.sp
        )
        Text(
            text = state.tableCode,
            color = Color(0xFF00E5FF),
            fontSize = if (isLandscape) 32.sp else 44.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = if (isLandscape) 4.sp else 6.sp
        )
    }

    val qrSection = @Composable {
        if (qrBitmap != null) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(if (isLandscape) 130.dp else 190.dp)
            ) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = stringResource(R.string.content_desc_qr_code),
                    modifier = Modifier.fillMaxSize().padding(if (isLandscape) 8.dp else 12.dp)
                )
            }
        }

        if (state.hostIp.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.lobby_table_ip_label),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Text(
                    text = "${state.hostIp}:8888",
                    color = Color(0xFF00E5FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    val wifiAndStatusSection = @Composable {
        val wifiCardShape = CutCornerShape(12.dp)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xD9080E1A)),
            shape = wifiCardShape,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x3300E5FF), wifiCardShape)
        ) {
            Column(modifier = Modifier.padding(if (isLandscape) 10.dp else 14.dp)) {
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

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.lobby_network_same_wifi),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                    shape = CutCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0x6600E5FF)),
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 16.dp))

        if (state.connectionStatus == ConnectionStatus.CONNECTED) {
            Text(
                text = stringResource(R.string.lobby_status_connected),
                color = Color(0xFF10B981),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onProceed,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = CutCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.lobby_btn_start_match), color = Color(0xFF0A0E1A), fontWeight = FontWeight.Bold)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.lobby_status_waiting_guest),
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                badgesRow()
                Spacer(modifier = Modifier.height(6.dp))
                tableCodeSection()
                Spacer(modifier = Modifier.height(8.dp))
                qrSection()
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                wifiAndStatusSection()
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            badgesRow()
            Spacer(modifier = Modifier.height(10.dp))
            tableCodeSection()
            Spacer(modifier = Modifier.height(12.dp))
            qrSection()
            Spacer(modifier = Modifier.height(12.dp))
            wifiAndStatusSection()
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
    onRefreshDiscovery: () -> Unit = {},
    onScanQrClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var selectedTableForPin by remember { mutableStateOf<dev.vmellos.airhockey.feature.network.DiscoveredTable?>(null) }
    var pinDialogInput by remember { mutableStateOf("") }

    val tableForPin = selectedTableForPin
    if (tableForPin != null) {
        val dialogShape = CutCornerShape(16.dp)
        AlertDialog(
            onDismissRequest = { selectedTableForPin = null },
            shape = dialogShape,
            modifier = Modifier.border(1.dp, Color(0x6600E5FF), dialogShape),
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
                        shape = CutCornerShape(8.dp),
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
                    shape = CutCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text(stringResource(R.string.lobby_connect_btn), color = Color(0xFF0A0E1A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTableForPin = null }) {
                    Text(stringResource(R.string.cancel), color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xF2080E1A)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.connectionStatus == ConnectionStatus.CONNECTED) {
            val connectedShape = CutCornerShape(14.dp)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xD9064E3B)),
                shape = connectedShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x6634D399), connectedShape)
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
                }
            }
            return
        }

        val displayErrorText = when {
            state.errorMessageRes != null -> {
                if (state.errorMessageArgs.isNotEmpty()) {
                    stringResource(state.errorMessageRes, *state.errorMessageArgs.toTypedArray())
                } else {
                    stringResource(state.errorMessageRes)
                }
            }
            state.errorMessage != null -> state.errorMessage
            else -> null
        }

        if (displayErrorText != null) {
            val errorShape = CutCornerShape(12.dp)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xD93B1E1E)),
                shape = errorShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .border(1.dp, Color(0x66FF5252), errorShape)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = displayErrorText, color = Color(0xFFFFCDD2), fontSize = 13.sp)
                }
            }
        }

        if (state.connectionStatus == ConnectionStatus.CONNECTING) {
            val connectingShape = CutCornerShape(12.dp)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xD9080E1A)),
                shape = connectingShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, Color(0x66FF5722), connectingShape)
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

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onScanQrClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                    shape = CutCornerShape(10.dp),
                    modifier = Modifier.weight(0.4f)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.lobby_scan_qr),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(0.6f)
                ) {
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = onInputCodeChange,
                        placeholder = { Text(stringResource(R.string.lobby_code_or_ip_placeholder), color = Color(0xFF64748B), fontSize = 12.sp) },
                        singleLine = true,
                        shape = CutCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConnectCode,
                        enabled = inputCode.length >= 4,
                        shape = CutCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        Text(stringResource(R.string.lobby_connect_btn), color = Color(0xFF0A0E1A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        } else {
            // Botão de Escanear QR Code
            Button(
                onClick = onScanQrClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                shape = CutCornerShape(10.dp),
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
                    placeholder = { Text(stringResource(R.string.lobby_code_or_ip_placeholder), color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = true,
                    shape = CutCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onConnectCode,
                    enabled = inputCode.length >= 4,
                    shape = CutCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text(stringResource(R.string.lobby_connect_btn), color = Color(0xFF0A0E1A), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.lobby_discovered_tables),
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = onRefreshDiscovery,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.lobby_refresh_btn),
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(state.discoveredTables) { table ->
                    val (modeColor, modeLabel, modeIcon) = when (table.mode) {
                        "CONNECTED" -> Triple(Color(0xFF00E5FF), stringResource(R.string.lobby_mode_badge_connected), Icons.Default.Wifi)
                        "AI_EXTENDED" -> Triple(Color(0xFFA855F7), stringResource(R.string.lobby_mode_badge_ai), Icons.Default.SmartToy)
                        else -> Triple(Color(0xFF818CF8), stringResource(R.string.lobby_mode_badge_extended), Icons.Default.PhoneAndroid)
                    }
                    val tableShape = CutCornerShape(12.dp)
                    Card(
                        onClick = {
                            pinDialogInput = ""
                            selectedTableForPin = table
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xD9080E1A)),
                        shape = tableShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, modeColor.copy(alpha = 0.5f), tableShape)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = modeColor.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, modeColor.copy(alpha = 0.4f)),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(modeIcon, contentDescription = null, tint = modeColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    val title = if (table.hostName.isNotBlank()) {
                                        stringResource(R.string.lobby_table_by, table.hostName)
                                    } else {
                                        table.name
                                    }
                                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = modeColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = modeLabel.uppercase(),
                                                color = modeColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(table.hostIp, color = Color(0xFF64748B), fontSize = 11.sp)
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.lobby_table_private_badge), tint = Color(0xFFFF5722), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.lobby_table_private_badge),
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

@Composable
fun HostCreateModeDialog(
    initialDifficulty: dev.vmellos.airhockey.core.preferences.AiDifficulty,
    onDismiss: () -> Unit,
    onSelectExtended: () -> Unit,
    onSelectConnected: () -> Unit,
    onSelectAiExtended: (dev.vmellos.airhockey.core.preferences.AiDifficulty, dev.vmellos.airhockey.core.preferences.AiDifficulty) -> Unit,
    onSelectAiLocal: ((dev.vmellos.airhockey.core.preferences.AiDifficulty, dev.vmellos.airhockey.core.preferences.AiDifficulty) -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showAiConfig by remember { mutableStateOf(false) }
    var diffBot1 by remember { mutableStateOf(initialDifficulty) }
    var diffBot2 by remember { mutableStateOf(initialDifficulty) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xF20B0F1C),
            shape = CutCornerShape(20.dp),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF818CF8),
                        Color(0xFFA855F7)
                    )
                )
            ),
            shadowElevation = 24.dp,
            modifier = Modifier
                .widthIn(max = if (isLandscape) 640.dp else 480.dp)
                .fillMaxWidth(0.92f)
                .padding(vertical = if (isLandscape) 8.dp else 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isLandscape) 14.dp else 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header com Botão Fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.lobby_select_host_mode_title),
                            color = Color(0xFF00E5FF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (showAiConfig) stringResource(R.string.lobby_configure_bots_subtitle) else stringResource(R.string.lobby_select_host_mode_subtitle),
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 16.dp))

                if (!showAiConfig) {
                    // Opção 1: Mesa Estendida
                    HostModeOptionCard(
                        title = stringResource(R.string.menu_mode_extended),
                        description = stringResource(R.string.menu_mode_extended_desc),
                        icon = Icons.Default.PhoneAndroid,
                        accentColor = Color(0xFF818CF8),
                        onClick = onSelectExtended
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opção 2: Mesa Conectada
                    HostModeOptionCard(
                        title = stringResource(R.string.menu_mode_connected),
                        description = stringResource(R.string.menu_mode_connected_desc),
                        icon = Icons.Default.Wifi,
                        accentColor = Color(0xFF00E5FF),
                        onClick = onSelectConnected
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opção 3: Duelo IA x IA
                    HostModeOptionCard(
                        title = stringResource(R.string.menu_mode_ai_vs_ai),
                        description = stringResource(R.string.menu_mode_ai_vs_ai_desc),
                        icon = Icons.Default.SmartToy,
                        accentColor = Color(0xFFA855F7),
                        onClick = { showAiConfig = true }
                    )
                } else {
                    // Painel de Configuração dos Bots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showAiConfig = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color(0xFF00E5FF))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.lobby_bots_difficulty_title),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

                    if (isLandscape) {
                        // Disposição lado a lado das dificuldades dos bots em modo paisagem
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.bot_1_cyan),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                DifficultySelectorRow(
                                    selected = diffBot1,
                                    activeColor = Color(0xFF00E5FF),
                                    onSelect = { diffBot1 = it }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.bot_2_orange),
                                    color = Color(0xFFFF5722),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                DifficultySelectorRow(
                                    selected = diffBot2,
                                    activeColor = Color(0xFFFF5722),
                                    onSelect = { diffBot2 = it }
                                )
                            }
                        }
                    } else {
                        // Bot 1 (Ciano)
                        Text(
                            text = stringResource(R.string.bot_1_cyan),
                            color = Color(0xFF00E5FF),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        DifficultySelectorRow(
                            selected = diffBot1,
                            activeColor = Color(0xFF00E5FF),
                            onSelect = { diffBot1 = it }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bot 2 (Laranja)
                        Text(
                            text = stringResource(R.string.bot_2_orange),
                            color = Color(0xFFFF5722),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        DifficultySelectorRow(
                            selected = diffBot2,
                            activeColor = Color(0xFFFF5722),
                            onSelect = { diffBot2 = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 20.dp))

                    // Botão Primário: Criar Mesa em 2 Celulares
                    Button(
                        onClick = { onSelectAiExtended(diffBot1, diffBot2) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                        shape = CutCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.lobby_host_ai_vs_ai_extended_btn).uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    if (onSelectAiLocal != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { onSelectAiLocal(diffBot1, diffBot2) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.lobby_host_ai_vs_ai_local_btn),
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HostModeOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    val cardShape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xD9080E1A)),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.85f),
                        accentColor.copy(alpha = 0.25f)
                    )
                ),
                shape = cardShape
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(36.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.35f))
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

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
private fun DifficultySelectorRow(
    selected: dev.vmellos.airhockey.core.preferences.AiDifficulty,
    activeColor: Color,
    onSelect: (dev.vmellos.airhockey.core.preferences.AiDifficulty) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dev.vmellos.airhockey.core.preferences.AiDifficulty.entries.forEach { diff ->
            val isSelected = diff == selected
            val label = when (diff) {
                dev.vmellos.airhockey.core.preferences.AiDifficulty.EASY -> stringResource(R.string.settings_ai_easy)
                dev.vmellos.airhockey.core.preferences.AiDifficulty.MEDIUM -> stringResource(R.string.settings_ai_medium)
                dev.vmellos.airhockey.core.preferences.AiDifficulty.HARD -> stringResource(R.string.settings_ai_hard)
            }
            Surface(
                color = if (isSelected) activeColor.copy(alpha = 0.22f) else Color(0x331E293B),
                shape = CutCornerShape(6.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.2.dp else 1.dp,
                    color = if (isSelected) activeColor else Color(0x3364748B)
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(diff) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = label.uppercase(),
                        color = if (isSelected) activeColor else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
