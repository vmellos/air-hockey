package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.game.ui.KiaraBehaviorState
import dev.vmellos.airhockey.feature.game.ui.KiaraMascotController
import dev.vmellos.airhockey.feature.game.ui.MascotWaypoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KiaraMascotControllerTest {

    @Test
    fun initialState_isLickingPawAtLeftMidfieldFloor() {
        val controller = KiaraMascotController()
        assertEquals(KiaraBehaviorState.LICKING_PAW, controller.state)
        assertEquals("LickPaw", controller.currentAnimation)
        assertEquals(-0.74f, controller.currentX, 0.01f)
        assertEquals(-0.15f, controller.currentZ, 0.01f)
        assertEquals(-0.045f, controller.currentY, 0.01f)
    }

    @Test
    fun stopTimerExpires_transitionsToWalking() {
        val testWaypoints = listOf(
            MascotWaypoint(x = 0f, z = 0f, isStopPoint = true, stopDurationSec = 1.0f, stopLookoutYaw = 45f, stopAnimation = "LickPaw"),
            MascotWaypoint(x = 1f, z = 0f, isStopPoint = false)
        )
        val controller = KiaraMascotController(testWaypoints)
        assertEquals(KiaraBehaviorState.LICKING_PAW, controller.state)

        // Pass 0.5s: still licking
        controller.update(0.5f)
        assertEquals(KiaraBehaviorState.LICKING_PAW, controller.state)
        assertEquals("LickPaw", controller.currentAnimation)

        // Pass another 0.6s (total 1.1s): timer expired, transitions to walking
        controller.update(0.6f)
        assertEquals(KiaraBehaviorState.WALKING, controller.state)
        assertEquals("Walk", controller.currentAnimation)
    }

    @Test
    fun walkingMovesTowardsNextWaypoint() {
        val testWaypoints = listOf(
            MascotWaypoint(x = 0f, z = 0f, isStopPoint = true, stopDurationSec = 0.1f),
            MascotWaypoint(x = 1f, z = 0f, isStopPoint = true, stopDurationSec = 2.0f)
        )
        val controller = KiaraMascotController(testWaypoints)
        controller.update(0.2f)

        assertEquals(KiaraBehaviorState.WALKING, controller.state)
        val startX = controller.currentX

        controller.update(0.5f)
        assertTrue("Mascot should advance along X", controller.currentX > startX)
    }

    @Test
    fun puckProximityDetection_triggersSwatStateAndAnimation() {
        val controller = KiaraMascotController()
        // Kiara is at (-0.74, -0.15). Place puck nearby at (-0.54, -0.15), dist = 0.20m < 0.38m
        controller.update(0.016f, puckX = -0.54f, puckZ = -0.15f)

        assertEquals(KiaraBehaviorState.SWATTING, controller.state)
        assertEquals("Swat", controller.currentAnimation)

        // After swat duration (1.0s), resumes walking
        controller.update(1.1f)
        assertEquals(KiaraBehaviorState.WALKING, controller.state)
        assertEquals("Walk", controller.currentAnimation)
    }

    @Test
    fun lerpAngle_handlesAngleWrapping() {
        val controller = KiaraMascotController()
        val interpolated = controller.lerpAngle(350f, 10f, 0.5f)
        assertEquals(0f, interpolated, 0.5f)
    }
}
