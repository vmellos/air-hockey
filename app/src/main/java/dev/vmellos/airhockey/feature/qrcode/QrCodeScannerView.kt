package dev.vmellos.airhockey.feature.qrcode

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import dev.vmellos.airhockey.AirHockeyApp

@Composable
fun QrCodeScannerView(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isSuccessDetected by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserAnimation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    var hasScanned = false
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                if (!hasScanned) {
                                    val qrText = scanBarcode(imageProxy)
                                    if (!qrText.isNullOrBlank()) {
                                        hasScanned = true
                                        isSuccessDetected = true
                                        try {
                                            AirHockeyApp.instance.audioEngine.playCountdownBeep(true)
                                        } catch (_: Exception) {}
                                        onQrCodeScanned(qrText)
                                    }
                                }
                                imageProxy.close()
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay com janela transparente e mira neon
        val frameSizePx = remember { 280.dp }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val boxSize = 280.dp.toPx()
            val left = (canvasW - boxSize) / 2f
            val top = (canvasH - boxSize) / 2f - 40.dp.toPx()

            // Fundo escurecido com recorte vazado no centro
            val clipPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, left + boxSize, top + boxSize),
                        cornerRadius = CornerRadius(24f, 24f)
                    )
                )
            }

            clipPath(clipPath, clipOp = ClipOp.Difference) {
                drawRect(Color(0x99000000))
            }

            // Laser animado de leitura
            val laserY = top + boxSize * laserYRatio
            val laserColor = if (isSuccessDetected) Color(0xFF10B981) else Color(0xFF00E5FF)
            drawLine(
                color = laserColor,
                start = Offset(left + 16f, laserY),
                end = Offset(left + boxSize - 16f, laserY),
                strokeWidth = 3f
            )

            // Cantos de mira neon
            val cornerLength = 36f
            val strokeW = 6f
            val cornerColor = if (isSuccessDetected) Color(0xFF10B981) else Color(0xFF00E5FF)

            // Top-Left
            drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), strokeW)
            drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), strokeW)

            // Top-Right
            drawLine(cornerColor, Offset(left + boxSize, top), Offset(left + boxSize - cornerLength, top), strokeW)
            drawLine(cornerColor, Offset(left + boxSize, top), Offset(left + boxSize, top + cornerLength), strokeW)

            // Bottom-Left
            drawLine(cornerColor, Offset(left, top + boxSize), Offset(left + cornerLength, top + boxSize), strokeW)
            drawLine(cornerColor, Offset(left, top + boxSize), Offset(left, top + boxSize - cornerLength), strokeW)

            // Bottom-Right
            drawLine(cornerColor, Offset(left + boxSize, top + boxSize), Offset(left + boxSize - cornerLength, top + boxSize), strokeW)
            drawLine(cornerColor, Offset(left + boxSize, top + boxSize), Offset(left + boxSize, top + boxSize - cornerLength), strokeW)
        }

        // Mensagens instrutivas abaixo da mira
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 320.dp, start = 24.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xCC0A0E1A), RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isSuccessDetected) Color(0xFF10B981) else Color(0xFF00E5FF),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (isSuccessDetected) "Código Detectado! Conectando..." else "Aponte para o Código QR da Mesa",
                    color = if (isSuccessDetected) Color(0xFF34D399) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enquadre o código do anfitrião dentro da mira",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Decodifica o código QR dos frames da câmera respeitando o rowStride e a rotação do sensor.
 */
private fun scanBarcode(imageProxy: ImageProxy): String? {
    return try {
        val yPlane = imageProxy.planes[0]
        val buffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride
        val width = imageProxy.width
        val height = imageProxy.height

        // Extrai o plano Y (luminância) respeitando rowStride
        val yData = ByteArray(width * height)
        if (rowStride == width && pixelStride == 1) {
            buffer.rewind()
            buffer.get(yData, 0, width * height)
        } else {
            for (row in 0 until height) {
                buffer.position(row * rowStride)
                if (pixelStride == 1) {
                    buffer.get(yData, row * width, width)
                } else {
                    for (col in 0 until width) {
                        yData[row * width + col] = buffer.get(row * rowStride + col * pixelStride)
                    }
                }
            }
        }

        // Aplica rotação necessária de acordo com o sensor do aparelho
        val rotation = imageProxy.imageInfo.rotationDegrees
        val source = when (rotation) {
            90 -> {
                val rotatedData = ByteArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotatedData[x * height + (height - y - 1)] = yData[x + y * width]
                    }
                }
                PlanarYUVLuminanceSource(rotatedData, height, width, 0, 0, height, width, false)
            }
            270 -> {
                val rotatedData = ByteArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotatedData[(width - x - 1) * height + y] = yData[x + y * width]
                    }
                }
                PlanarYUVLuminanceSource(rotatedData, height, width, 0, 0, height, width, false)
            }
            180 -> {
                val rotatedData = ByteArray(width * height)
                for (i in 0 until width * height) {
                    rotatedData[width * height - 1 - i] = yData[i]
                }
                PlanarYUVLuminanceSource(rotatedData, width, height, 0, 0, width, height, false)
            }
            else -> {
                PlanarYUVLuminanceSource(yData, width, height, 0, 0, width, height, false)
            }
        }

        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        val reader = MultiFormatReader()

        // 1. Tenta HybridBinarizer (ótimo para contrastes normais)
        try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            return reader.decode(binaryBitmap, hints).text
        } catch (_: Exception) {}

        // 2. Tenta GlobalHistogramBinarizer (ótimo para iluminação variável)
        try {
            val binaryBitmap = BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(source))
            return reader.decode(binaryBitmap, hints).text
        } catch (_: Exception) {}

        // 3. Tenta fonte invertida (caso o código esteja com contraste invertido)
        try {
            val invertedSource = source.invert()
            val binaryBitmap = BinaryBitmap(HybridBinarizer(invertedSource))
            return reader.decode(binaryBitmap, hints).text
        } catch (_: Exception) {}

        null
    } catch (_: Exception) {
        null
    }
}
