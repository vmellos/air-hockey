package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.game.ui.isHitSane
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirHockeyArenaARSceneTest {

    @Test
    fun testIsHitSaneWithFloorLevelPoint() {
        // Câmera na altura típica dos olhos/peito (1.4m), chão a 1.4m abaixo (-1.4m) e 1.2m à frente (-1.2m)
        val cx = 0f
        val cy = 1.4f
        val cz = 0f

        val tx = 0f
        val ty = 0f // Chão físico
        val tz = -1.2f // À frente

        assertTrue("Ponto no chão físico à frente deve ser considerado são", isHitSane(tx, ty, tz, cx, cy, cz))
    }

    @Test
    fun testIsHitSaneWithTableLevelPoint() {
        // Câmera segurada sentado em frente à mesa (50cm acima da mesa, 60cm à frente)
        val cx = 0f
        val cy = 0.5f
        val cz = 0f

        val tx = 0f
        val ty = 0f // Superfície da mesa
        val tz = -0.6f // À frente na mesa

        assertTrue("Ponto na mesa física em frente ao usuário deve ser considerado são", isHitSane(tx, ty, tz, cx, cy, cz))
    }

    @Test
    fun testIsHitSaneRejectsAstronomicalCoordinates() {
        // Coordenadas absurdas observadas no logcat causadas por divergência de rastreamento
        val cx = 0f
        val cy = 0f
        val cz = 0f

        assertFalse("Deve rejeitar coordenadas em escala de quilômetros",
            isHitSane(-37679.26f, -25422.191f, 147659.12f, cx, cy, cz))

        assertFalse("Deve rejeitar coordenadas com dezenas de milhões de metros",
            isHitSane(-1.9315104E7f, 7537474.0f, 6.403804E7f, cx, cy, cz))
    }

    @Test
    fun testIsHitSaneRejectsPointsInTheSkyOrCeiling() {
        val cx = 0f
        val cy = 0f
        val cz = 0f

        // Ponto 23 metros acima da câmera (no céu)
        assertFalse("Deve rejeitar ponto flutuando no céu/teto",
            isHitSane(9.124258f, 23.789276f, -28.498539f, cx, cy, cz))

        // Ponto 0.5m acima da câmera
        assertFalse("Deve rejeitar ponto acima da câmera",
            isHitSane(0f, 0.5f, -1.0f, cx, cy, cz))
    }

    @Test
    fun testIsHitSaneRejectsPointsTooCloseOrTooFar() {
        val cx = 0f
        val cy = 0f
        val cz = 0f

        // Menos de 35cm (dentro do próprio dispositivo)
        assertFalse("Deve rejeitar ponto muito próximo (<35cm)",
            isHitSane(0f, -0.1f, -0.1f, cx, cy, cz))

        // Mais de 4.0m de distância
        assertFalse("Deve rejeitar ponto muito distante (>4.0m)",
            isHitSane(0f, -0.5f, -5.0f, cx, cy, cz))
    }

    @Test
    fun testIsHitSaneRejectsNaNAndInfinite() {
        assertFalse("Deve rejeitar NaN", isHitSane(Float.NaN, -0.5f, -1.0f, 0f, 0f, 0f))
        assertFalse("Deve rejeitar Infinito", isHitSane(Float.POSITIVE_INFINITY, -0.5f, -1.0f, 0f, 0f, 0f))
    }
}
