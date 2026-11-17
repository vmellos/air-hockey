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

data class FilamentArenaRenderPolicy(
    val bloomEnabled: Boolean,
    val bloomStrength: Float,
    val dynamicResolutionEnabled: Boolean
)

/**
 * Resolve a política de pós-processamento e escala da View 3D no Filament.
 *
 * A resolução dinâmica permanece desativada enquanto o Bloom HDR está ativo para evitar
 * vazamentos de halos neon nas bordas do viewport causados pelo downsampling em subescala.
 */
fun resolveFilamentArenaRenderPolicy(
    neonBloomIntensity: Float,
    bloomEnabledOverride: Boolean = true
): FilamentArenaRenderPolicy {
    val bloomActive = bloomEnabledOverride && neonBloomIntensity > 0.002f
    return FilamentArenaRenderPolicy(
        bloomEnabled = bloomActive,
        bloomStrength = computeCalibratedBloomStrength(neonBloomIntensity),
        dynamicResolutionEnabled = !bloomActive
    )
}

/**
 * Configura uma [View] do Filament compartilhada entre a arena 3D em jogo e a vitrine de customização.
 *
 * Calibra o passe de Bloom HDR e restringe a resolução dinâmica adaptativa aos cenários sem Bloom
 * ativo (ex.: `VisualEffectsProfile.OFF` ou `0%`), evitando vazamento de neon nas bordas da tela.
 */
fun configureFilamentArenaView(
    view: View,
    neonBloomIntensity: Float,
    bloomEnabledOverride: Boolean = true
) {
    view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
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
 * Calcula a força calibrada do Bloom HDR conforme a Curva Gama de Bloom (ADR 0039).
 */
fun computeCalibratedBloomStrength(neonBloomIntensity: Float): Float {
    val normalizedBloom = (neonBloomIntensity / 0.10f).coerceIn(0.0f, 1.0f)
    return (normalizedBloom * normalizedBloom) * 0.22f
}

/**
 * Atualiza em tempo real o Bloom HDR conforme a Curva Gama de Bloom (ADR 0039) e sincroniza
 * a resolução dinâmica para que permaneça desativada enquanto o Bloom estiver ativo.
 */
fun updateFilamentArenaBloom(
    view: View,
    neonBloomIntensity: Float,
    bloomEnabledOverride: Boolean = true
) {
    val policy = resolveFilamentArenaRenderPolicy(
        neonBloomIntensity = neonBloomIntensity,
        bloomEnabledOverride = bloomEnabledOverride
    )
    view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
        enabled = policy.dynamicResolutionEnabled
    }
    view.bloomOptions = view.bloomOptions.apply {
        enabled = policy.bloomEnabled
        threshold = true
        highlight = 0.85f
        levels = 4
        resolution = 256
        quality = View.QualityLevel.MEDIUM
        strength = policy.bloomStrength
        blendMode = View.BloomOptions.BlendMode.ADD
    }
}
