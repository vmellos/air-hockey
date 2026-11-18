package dev.vmellos.airhockey.core.assets

import dev.vmellos.airhockey.assetcrypto.AssetPacker
import dev.vmellos.airhockey.assetcrypto.AssetVaultFormat
import dev.vmellos.airhockey.feature.game.ui.ArenaKitCatalog
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException

class ProtectedAssetLoaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun generatedVaultKeyReconstructs32ByteNonZeroKey() {
        val key1 = GeneratedAssetVaultKey.reconstructMasterKey()
        val key2 = GeneratedAssetVaultKey.reconstructMasterKey()
        assertEquals(32, key1.size)
        assertArrayEquals(key1, key2)
        assertTrue("Reconstructed master key must not be all zeros", key1.any { it.toInt() != 0 })
    }

    @Test
    fun protectedAssetLoaderDecryptsRealGlbInRamAndZeroesBufferImmediately() {
        val rawAssetsDir = resolveRawAssetsDir()
        val packedAssetsDir = tempFolder.newFolder("packed_assets")
        val masterKey = GeneratedAssetVaultKey.reconstructMasterKey()

        val summary = AssetPacker.packDirectory(rawAssetsDir.toPath(), packedAssetsDir.toPath(), masterKey)
        assertEquals(74, summary.packedCount)

        val loader = ProtectedAssetLoader(
            openObfuscatedAsset = { obfuscatedPath ->
                File(packedAssetsDir, obfuscatedPath).inputStream()
            }
        )

        val canonicalPath = ArenaKitCatalog.getKit(dev.vmellos.airhockey.core.preferences.TableTheme.CLASSIC_NEON).tableFullAsset
        val expectedRaw = File(rawAssetsDir, canonicalPath).readBytes()
        var capturedBuffer: ByteBuffer? = null
        var seenBytes: ByteArray? = null

        loader.withDecryptedBuffer(canonicalPath) { buffer ->
            capturedBuffer = buffer
            val copy = ByteArray(buffer.remaining())
            buffer.get(copy)
            seenBytes = copy
        }

        assertArrayEquals("Decrypted RAM payload must match original .glb", expectedRaw, seenBytes)
        val backingAfterReturn = capturedBuffer!!.array()
        assertTrue(
            "Temporary decrypted buffer must be zeroed immediately after consumer completes",
            backingAfterReturn.all { it.toInt() == 0 }
        )

        val obfuscatedRel = AssetVaultFormat.obfuscatePath(canonicalPath)
        val packedBytes = File(packedAssetsDir, obfuscatedRel).readBytes()
        assertTrue(AssetVaultFormat.hasValidMagic(packedBytes))
        assertFalse(
            "Encrypted .lpkg must not start with glTF magic",
            packedBytes.size >= 4 &&
                packedBytes[0] == 'g'.code.toByte() &&
                packedBytes[1] == 'l'.code.toByte() &&
                packedBytes[2] == 'T'.code.toByte() &&
                packedBytes[3] == 'F'.code.toByte()
        )
    }

    @Test
    fun protectedAssetLoaderRejectsTamperingAndSwappedAadPaths() {
        val rawAssetsDir = resolveRawAssetsDir()
        val packedAssetsDir = tempFolder.newFolder("tamper_packed")
        val masterKey = GeneratedAssetVaultKey.reconstructMasterKey()
        AssetPacker.packDirectory(rawAssetsDir.toPath(), packedAssetsDir.toPath(), masterKey)

        val pathA = "models/mesa_arcade.glb"
        val pathB = "models/disco_cyber.glb"
        val obfA = AssetVaultFormat.obfuscatePath(pathA)
        val obfB = AssetVaultFormat.obfuscatePath(pathB)

        // Attempt to load pathB using pathA's encrypted blob (simulating swapped blob / wrong AAD)
        val swappedLoader = ProtectedAssetLoader(
            openObfuscatedAsset = { requested ->
                val actual = if (requested == obfB) obfA else requested
                File(packedAssetsDir, actual).inputStream()
            }
        )
        try {
            swappedLoader.withDecryptedBuffer(pathB) { }
            fail("Expected AEADBadTagException when blob A is opened as canonical path B")
        } catch (_: AEADBadTagException) {
            // Expected AAD mismatch rejection
        }

        // Tamper 1 byte inside encrypted container
        val fileA = File(packedAssetsDir, obfA)
        val tampered = fileA.readBytes()
        tampered[tampered.size - 5] = (tampered[tampered.size - 5].toInt() xor 0xFF).toByte()
        fileA.writeBytes(tampered)

        val strictLoader = ProtectedAssetLoader(
            openObfuscatedAsset = { requested -> File(packedAssetsDir, requested).inputStream() }
        )
        try {
            strictLoader.withDecryptedBuffer(pathA) { }
            fail("Expected GeneralSecurityException when container bytes are tampered")
        } catch (_: GeneralSecurityException) {
            // Expected GCM tag failure
        }
    }

    private fun resolveRawAssetsDir(): File =
        File("src/main/raw_assets").let { if (it.exists()) it else File("app/src/main/raw_assets") }
}
