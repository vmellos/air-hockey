package dev.vmellos.airhockey.feature.game.ui

import com.google.android.filament.View
import kotlin.math.round

enum class NeonBloomBand {
    OFF,
    SOFT,
    NORMAL,
    VIBRANT
}

data class NeonBloomDisplayState(
    val sliderValue: Float,
    val percent: Int,
    val band: NeonBloomBand,
    val isControlEnabled: Boolean
)

/**
 * Calcula o estado de exibição da barra de Brilho do Neon sem sobrescrever a preferência salva.
 *
 * Quando [effectsEnabledInContext] é falso (ex.: Perfil de Efeitos Visuais em OFF durante partida),
 * o controle é apresentado bloqueado em 0% (`OFF`) mantendo intacto o valor salvo do usuário.
 */
fun resolveNeonBloomDisplayState(
    savedIntensity: Float,
    effectsEnabledInContext: Boolean
): NeonBloomDisplayState {
    val clamped = savedIntensity.coerceIn(0.0f, 0.10f)
    if (!effectsEnabledInContext) {
        return NeonBloomDisplayState(
            sliderValue = 0.0f,
            percent = 0,
            band = NeonBloomBand.OFF,
            isControlEnabled = false
        )
    }
    val pct = round((clamped / 0.10f) * 100f).toInt().coerceIn(0, 100)
    val band = when {
        pct == 0 -> NeonBloomBand.OFF
        pct <= 30 -> NeonBloomBand.SOFT
        pct <= 70 -> NeonBloomBand.NORMAL
        else -> NeonBloomBand.VIBRANT
    }
    return NeonBloomDisplayState(
        sliderValue = clamped,
        percent = pct,
        band = band,
        isControlEnabled = true
    )
}

/**
 * Configura uma [View] do Filament compartilhada entre a arena 3D em jogo e a vitrine de customização.
 *
 * Habilita resolução dinâmica adaptativa (desativada por padrão no SceneView por causa de streams AR)
 * e calibra o passe de Bloom HDR com menor consumo de largura de banda de GPU.
 */
fun configureFilamentArenaView(
    view: View,
    neonBloomIntensity: Float,
    bloomEnabledOverride: Boolean = true
) {
    view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
        enabled = true
        minScale = 0.65f
        maxScale = 1.0f
        sharpness = 0.9f
        homogeneousScaling = true
        quality = View.QualityLevel.LOW
    }
    updateFilamentArenaBloom(
        view = view,
        neonBloomIntensity = neonBloomIntensity,
        bloomEnabledOverride = bloomEnabledOverride
    )
}

/**
 * Atualiza em tempo real a intensidade de Bloom HDR conforme a Curva Gama de Bloom (ADR 0039)
 * e o estado de ativação do Perfil de Efeitos Visuais em partida.
 */
fun updateFilamentArenaBloom(
    view: View,
    neonBloomIntensity: Float,
    bloomEnabledOverride: Boolean = true
) {
    val normalizedBloom = (neonBloomIntensity / 0.10f).coerceIn(0.0f, 1.0f)
    val calibratedStrength = (normalizedBloom * normalizedBloom) * 0.22f
    view.bloomOptions = view.bloomOptions.apply {
        enabled = bloomEnabledOverride && neonBloomIntensity > 0.002f
        threshold = true
        highlight = 0.85f
        levels = 4
        resolution = 256
        strength = calibratedStrength
        blendMode = View.BloomOptions.BlendMode.ADD
    }
}
