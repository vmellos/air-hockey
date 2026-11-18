package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.game.ui.KiaraBehaviorState
import dev.vmellos.airhockey.feature.game.ui.KiaraMascotController
import dev.vmellos.airhockey.feature.game.ui.MascotWaypoint
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    @Test
    fun kiaraGlb_containsAllFourAnimationsWithQuadrupedLimbsAndBakedFurTexture() {
        val glbFile = listOf(
            File("src/main/raw_assets/models/kiara_3d.glb"),
            File("app/src/main/raw_assets/models/kiara_3d.glb")
        ).firstOrNull { it.exists() }
        assertTrue("Expected kiara_3d.glb in raw_assets/models", glbFile != null && glbFile.exists())

        val bytes = glbFile!!.readBytes()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("Valid glTF magic", 0x46546C67, buf.getInt(0))
        val jsonChunkLen = buf.getInt(12)
        val jsonChunkType = buf.getInt(16)
        assertEquals("JSON chunk", 0x4E4F534A, jsonChunkType)

        val gltf = JSONObject(String(bytes, 20, jsonChunkLen, Charsets.UTF_8))

        // Extract BIN chunk start for sampler curve verification
        val binHeaderOffset = 20 + jsonChunkLen
        val binChunkOffset = binHeaderOffset + 8

        val materials = gltf.getJSONArray("materials")
        val matNames = (0 until materials.length()).map { materials.getJSONObject(it).getString("name") }.toSet()
        assertTrue(matNames.contains("Mat_Kiara_Fur"))
        assertTrue(matNames.contains("Mat_Kiara_Collar"))
        assertTrue(matNames.contains("Mat_Kiara_TagText"))

        val images = gltf.getJSONArray("images")
        assertTrue("Expected embedded fur texture image", images.length() >= 1)

        val nodes = gltf.getJSONArray("nodes")
        val animations = gltf.getJSONArray("animations")
        val accessors = gltf.getJSONArray("accessors")
        val bufferViews = gltf.getJSONArray("bufferViews")

        val animMap = (0 until animations.length()).associate {
            val animObj = animations.getJSONObject(it)
            animObj.getString("name") to animObj
        }
        val expectedAnims = listOf("Idle", "Walk", "LickPaw", "Swat")
        for (name in expectedAnims) {
            assertTrue("Missing animation $name", animMap.containsKey(name))
        }

        val requiredLimbs = listOf(
            "UpperArm_L", "UpperArm_R", "Forearm_L", "Forearm_R",
            "Thigh_L", "Thigh_R", "Shin_L", "Shin_R"
        )
        for (name in expectedAnims) {
            val anim = animMap.getValue(name)
            val channels = anim.getJSONArray("channels")
            val targetedNodes = (0 until channels.length()).map { idx ->
                val nodeIdx = channels.getJSONObject(idx).getJSONObject("target").getInt("node")
                nodes.getJSONObject(nodeIdx).optString("name", "")
            }.toSet()
            for (limb in requiredLimbs) {
                assertTrue("Animation $name missing channel for $limb", targetedNodes.contains(limb))
            }
        }

        // Verify Walk animation actually rotates all 4 upper limbs & forearms/shins (not frozen on 1 paw)
        val walkAnim = animMap.getValue("Walk")
        val walkChannels = walkAnim.getJSONArray("channels")
        val walkSamplers = walkAnim.getJSONArray("samplers")
        for (limb in listOf("UpperArm_L", "UpperArm_R", "Thigh_L", "Thigh_R")) {
            var maxDelta = 0f
            for (cIdx in 0 until walkChannels.length()) {
                val ch = walkChannels.getJSONObject(cIdx)
                val target = ch.getJSONObject("target")
                val nodeName = nodes.getJSONObject(target.getInt("node")).optString("name", "")
                if (nodeName == limb && target.getString("path") == "rotation") {
                    val sampler = walkSamplers.getJSONObject(ch.getInt("sampler"))
                    val acc = accessors.getJSONObject(sampler.getInt("output"))
                    val bv = bufferViews.getJSONObject(acc.getInt("bufferView"))
                    val count = acc.getInt("count")
                    val offset = binChunkOffset + bv.optInt("byteOffset", 0) + acc.optInt("byteOffset", 0)
                    val first = FloatArray(4) { buf.getFloat(offset + it * 4) }
                    for (k in 1 until count) {
                        for (comp in 0 until 4) {
                            val d = kotlin.math.abs(buf.getFloat(offset + (k * 4 + comp) * 4) - first[comp])
                            if (d > maxDelta) maxDelta = d
                        }
                    }
                }
            }
            assertTrue("Walk limb $limb should articulate across cycle (maxDelta=$maxDelta)", maxDelta > 0.05f)
        }
    }
}
