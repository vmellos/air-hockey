package dev.vmellos.airhockey.feature.stage.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vmellos.airhockey.AirHockeyApp
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.database.StageProgressEntity

@Composable
fun StageSelectionScreen(
    onSelectStage: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stageRepository = AirHockeyApp.instance.stageRepository
    val stages by stageRepository.stagesFlow.collectAsState(initial = emptyList())
    val totalStars by stageRepository.totalStarsFlow.collectAsState(initial = 0)

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

        // Camada de Contraste para legibilidade da grade
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x88070B14),
                            Color(0x55070B14),
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
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .safeDrawingPadding()
            ) {
            // Top Bar com HUD Arcade
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão Voltar HUD
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xB3070D1A), CircleShape)
                        .border(1.2.dp, Color(0x6600E5FF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.stage_back),
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Cápsula Central com Total de Estrelas
                Surface(
                    shape = CutCornerShape(10.dp),
                    color = Color(0xD9080E1A),
                    border = BorderStroke(1.2.dp, Color(0x66FFB800)),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "$totalStars",
                            color = Color(0xFFFFB800),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Espaçador para manter o contador de estrelas perfeitamente centralizado
                Spacer(modifier = Modifier.size(44.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Grade de 3 Colunas dos 100 Níveis
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = if (stages.isNotEmpty()) stages else (1..100).map {
                        StageProgressEntity(stageNumber = it, isUnlocked = it == 1)
                    },
                    key = { it.stageNumber }
                ) { stage ->
                    StageCard(
                        stage = stage,
                        onClick = {
                            if (stage.isUnlocked) {
                                onSelectStage(stage.stageNumber)
                            }
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
fun StageCard(
    stage: StageProgressEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = CutCornerShape(12.dp)

    if (stage.isUnlocked) {
        val isCompleted = stage.starsEarned > 0
        val accentColor = if (isCompleted) Color(0xFFFFB800) else Color(0xFF00E5FF)

        Box(
            modifier = modifier
                .aspectRatio(0.74f)
                .background(Color(0xD9080E1A), cardShape)
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.90f),
                            accentColor.copy(alpha = 0.35f)
                        )
                    ),
                    shape = cardShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Cápsula Superior de 3 Estrelas
                Surface(
                    shape = CutCornerShape(6.dp),
                    color = Color(0x660B132B),
                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..3) {
                            val isEarned = i <= stage.starsEarned
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isEarned) Color(0xFFFFB800) else Color(0xFF475569),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Número da Fase
                Text(
                    text = "${stage.stageNumber}",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    } else {
        // Cartão Bloqueado
        Box(
            modifier = modifier
                .aspectRatio(0.74f)
                .background(Color(0x99080E1A), cardShape)
                .border(1.dp, Color(0x22FFFFFF), cardShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = stringResource(R.string.stage_locked),
                tint = Color(0xFF475569),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
