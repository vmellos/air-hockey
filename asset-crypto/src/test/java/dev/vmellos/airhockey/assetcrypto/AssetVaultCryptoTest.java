package dev.vmellos.airhockey.assetcrypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.AEADBadTagException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AssetVaultCryptoTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void encryptDecryptRoundTripPreservesExactBytes() throws Exception {
        byte[] masterKey = AssetKeyDerivation.deriveMasterKey("test-vault-secret-2026".getBytes(StandardCharsets.UTF_8));
        byte[] payload = buildSyntheticGlb("mesa_arcade_payload");

        byte[] encrypted = AssetVaultCipher.encrypt(masterKey, "models/mesa_arcade.glb", payload);
        assertTrue(AssetVaultFormat.hasValidMagic(encrypted));
        assertTrue(encrypted.length >= payload.length + AssetVaultFormat.MIN_CONTAINER_LENGTH_BYTES);

        byte[] decrypted = AssetVaultCipher.decrypt(masterKey, "models/mesa_arcade.glb", encrypted);
        assertArrayEquals(payload, decrypted);

        byte[] decryptedFromStream = AssetVaultCipher.decrypt(
            masterKey,
            "models/mesa_arcade.glb",
            new ByteArrayInputStream(encrypted)
        );
        assertArrayEquals(payload, decryptedFromStream);
    }

    @Test
    public void encryptedBlobRemovesOriginalHeadersAndCanonicalNames() throws Exception {
        byte[] masterKey = AssetKeyDerivation.deriveMasterKey("header-check-secret".getBytes(StandardCharsets.UTF_8));
        String[] signatures = new String[] { "glTF", "PNG", "RIFF", "OggS", "{\"asset\":" };

        for (String sig : signatures) {
            byte[] sigBytes = sig.getBytes(StandardCharsets.US_ASCII);
            byte[] payload = new byte[512];
            System.arraycopy(sigBytes, 0, payload, 0, sigBytes.length);
            Arrays.fill(payload, sigBytes.length, payload.length, (byte) 0x42);

            String canonicalPath = "models/boss_secret_" + sig.replaceAll("[^A-Za-z0-9]", "") + ".glb";
            byte[] encrypted = AssetVaultCipher.encrypt(masterKey, canonicalPath, payload);

            String asciiBlob = new String(encrypted, StandardCharsets.ISO_8859_1);
            assertFalse("Original signature must not appear in header: " + sig, startsWith(encrypted, sigBytes));
            assertFalse("glTF header leaked", asciiBlob.contains("glTF"));
            assertFalse("PNG header leaked", asciiBlob.contains("PNG"));
            assertFalse("RIFF header leaked", asciiBlob.contains("RIFF"));
            assertFalse("OggS header leaked", asciiBlob.contains("OggS"));
            assertFalse("JSON header leaked", asciiBlob.contains("{\"asset\""));
            assertFalse("Canonical path leaked", asciiBlob.contains("models/"));
            assertFalse("Original filename leaked", asciiBlob.contains("boss_secret"));
            assertFalse("Original extension leaked", asciiBlob.contains(".glb"));

            String obfuscated = AssetVaultFormat.obfuscatePath(canonicalPath);
            assertTrue("Obfuscated path must match lspk/<20-hex>.lpkg", obfuscated.matches("^lspk/[0-9a-f]{20}\\.lpkg$"));
        }
    }

    @Test
    public void deterministicIvPreservesReproducibilityAndNeverRepeatsAcrossContentOrPath() throws Exception {
        byte[] masterKey = AssetKeyDerivation.deriveMasterKey("iv-determinism-secret".getBytes(StandardCharsets.UTF_8));
        byte[] payloadA = buildSyntheticGlb("content-a");
        byte[] payloadB = buildSyntheticGlb("content-b");

        byte[] encA1 = AssetVaultCipher.encrypt(masterKey, "models/a.glb", payloadA);
        byte[] encA2 = AssetVaultCipher.encrypt(masterKey, "models/a.glb", payloadA);
        assertArrayEquals("Deterministic encryption must match for Gradle cache", encA1, encA2);

        byte[] encModifiedContent = AssetVaultCipher.encrypt(masterKey, "models/a.glb", payloadB);
        byte[] encDifferentPath = AssetVaultCipher.encrypt(masterKey, "models/b.glb", payloadA);

        byte[] ivA = extractIv(encA1);
        byte[] ivB = extractIv(encModifiedContent);
        byte[] ivPath = extractIv(encDifferentPath);
        assertFalse("IV must change when content changes", Arrays.equals(ivA, ivB));
        assertFalse("IV must change when canonical path changes", Arrays.equals(ivA, ivPath));
    }

    @Test
    public void rejectsTamperedBytesAnywhereInContainer() throws Exception {
        byte[] masterKey = AssetKeyDerivation.deriveMasterKey("tamper-secret".getBytes(StandardCharsets.UTF_8));
        byte[] payload = buildSyntheticGlb("tamper-check");
        byte[] encrypted = AssetVaultCipher.encrypt(masterKey, "models/disco_cyber.glb", payload);

        for (int pos : new int[] { 0, 7, 9, 10, 21, 22, encrypted.length / 2, encrypted.length - 1 }) {
            byte[] corrupted = encrypted.clone();
            corrupted[pos] ^= 0x01;
            try {
                AssetVaultCipher.decrypt(masterKey, "models/disco_cyber.glb", corrupted);
                fail("Expected tampering rejection at byte offset " + pos);
            } catch (GeneralSecurityException expected) {
                // Expected authentication/container rejection
            }
        }
    }

    @Test
    public void rejectsDecryptingBlobUnderDifferentAssetCanonicalPathAad() throws Exception {
        byte[] masterKey = AssetKeyDerivation.deriveMasterKey("aad-binding-secret".getBytes(StandardCharsets.UTF_8));
        byte[] payload = buildSyntheticGlb("aad-check");
        byte[] encrypted = AssetVaultCipher.encrypt(masterKey, "models/mesa_arcade.glb", payload);

        try {
            AssetVaultCipher.decrypt(masterKey, "models/mesa_cyber_pong.glb", encrypted);
            fail("Expected AEADBadTagException when swapping canonical path / AAD");
        } catch (AEADBadTagException expected) {
            // Expected GCM AAD authentication failure
        }
    }

    @Test
    public void packerCreatesObfuscatedFilesAndPrunesOrphansOnRenameOrDelete() throws Exception {
        Path rawDir = tempFolder.newFolder("raw_assets").toPath();
        Path outDir = tempFolder.newFolder("out_assets").toPath();
        byte[] masterKey = AssetKeyDerivation.deriveMasterKey("packer-secret".getBytes(StandardCharsets.UTF_8));

        Path modelsDir = rawDir.resolve("models");
        Files.createDirectories(modelsDir);
        Path asset1 = modelsDir.resolve("mesa_arcade.glb");
        Path asset2 = modelsDir.resolve("disco_cyber.glb");
        Files.write(asset1, buildSyntheticGlb("mesa"));
        Files.write(asset2, buildSyntheticGlb("disco"));

        AssetPacker.PackSummary first = AssetPacker.packDirectory(rawDir, outDir, masterKey);
        assertEquals(2, first.getPackedCount());
        assertEquals(0, first.getPrunedOrphansCount());

        Path obf1 = outDir.resolve(AssetVaultFormat.obfuscatePath("models/mesa_arcade.glb"));
        Path obf2 = outDir.resolve(AssetVaultFormat.obfuscatePath("models/disco_cyber.glb"));
        assertTrue(Files.isRegularFile(obf1));
        assertTrue(Files.isRegularFile(obf2));

        // Rename asset2 -> asset3 and verify old .lpkg is pruned
        Path asset3 = modelsDir.resolve("kiara_3d.glb");
        Files.move(asset2, asset3);
        AssetPacker.PackSummary second = AssetPacker.packDirectory(rawDir, outDir, masterKey);
        assertEquals(2, second.getPackedCount());
        assertEquals(1, second.getPrunedOrphansCount());
        assertFalse("Orphan .lpkg must be removed after rename", Files.exists(obf2));
        assertTrue(Files.isRegularFile(outDir.resolve(AssetVaultFormat.obfuscatePath("models/kiara_3d.glb"))));
    }

    @Test
    public void cliReadsSecretFromStdinAndEmitsObfuscatedKotlinKey() throws Exception {
        Path rawDir = tempFolder.newFolder("cli_raw").toPath();
        Path outAssets = tempFolder.newFolder("cli_assets").toPath();
        Path outKotlin = tempFolder.newFolder("cli_kotlin").toPath();

        Files.createDirectories(rawDir.resolve("models"));
        byte[] rawModel = buildSyntheticGlb("cli-model");
        Files.write(rawDir.resolve("models/piso_arena_cyber.glb"), rawModel);

        String secret = "super-private-vault-secret-xyz";
        InputStreamPreventer prev = new InputStreamPreventer(secret + "\n");
        java.io.InputStream originalIn = System.in;
        try {
            System.setIn(prev);
            AssetPackerCli.main(new String[] {
                "--input-dir", rawDir.toString(),
                "--output-assets-dir", outAssets.toString(),
                "--output-kotlin-dir", outKotlin.toString(),
                "--package", "dev.vmellos.airhockey.core.assets",
                "--class-name", "GeneratedAssetVaultKey",
                "--variant", "debug"
            });
        } finally {
            System.setIn(originalIn);
        }

        Path generatedKt = outKotlin.resolve("dev/vmellos/airhockey/core/assets/GeneratedAssetVaultKey.kt");
        assertTrue(Files.isRegularFile(generatedKt));
        String kotlinSource = Files.readString(generatedKt, StandardCharsets.UTF_8);
        assertFalse("Generated Kotlin must not embed plaintext secret", kotlinSource.contains(secret));
        assertTrue("Generated Kotlin must use uL.toLong() literals", kotlinSource.contains("uL.toLong()"));
        assertTrue("Generated Kotlin must use XOR masks", kotlinSource.contains(" xor "));
        assertTrue("Generated Kotlin must use bit rotations", kotlinSource.contains(".rotateRight("));

        byte[] expectedMasterKey = AssetKeyDerivation.deriveMasterKey(secret.getBytes(StandardCharsets.UTF_8));
        byte[] reconstructedFromFormula = evaluateGeneratedFormula(kotlinSource);
        assertArrayEquals(expectedMasterKey, reconstructedFromFormula);

        Path packed = outAssets.resolve(AssetVaultFormat.obfuscatePath("models/piso_arena_cyber.glb"));
        byte[] decrypted = AssetVaultCipher.decrypt(reconstructedFromFormula, "models/piso_arena_cyber.glb", Files.readAllBytes(packed));
        assertArrayEquals(rawModel, decrypted);
    }

    private static byte[] evaluateGeneratedFormula(String kotlinSource) {
        Pattern p = Pattern.compile(
            "val w(\\d) = \\(\\(0x([0-9A-F]{16})uL\\.toLong\\(\\) xor 0x([0-9A-F]{16})uL\\.toLong\\(\\)\\)\\.rotateRight\\((\\d+)\\) xor 0x([0-9A-F]{16})uL\\.toLong\\(\\)\\)"
        );
        Matcher m = p.matcher(kotlinSource);
        byte[] out = new byte[32];
        int count = 0;
        while (m.find()) {
            int idx = Integer.parseInt(m.group(1));
            long encoded = Long.parseUnsignedLong(m.group(2), 16);
            long maskB = Long.parseUnsignedLong(m.group(3), 16);
            int rot = Integer.parseInt(m.group(4));
            long maskA = Long.parseUnsignedLong(m.group(5), 16);
            long word = Long.rotateRight(encoded ^ maskB, rot) ^ maskA;
            for (int b = 0; b < 8; b++) {
                out[idx * 8 + b] = (byte) ((word >>> ((7 - b) * 8)) & 0xFFL);
            }
            count++;
        }
        assertEquals(4, count);
        return out;
    }

    private static byte[] buildSyntheticGlb(String tag) {
        byte[] header = new byte[] { 0x67, 0x6C, 0x54, 0x46, 0x02, 0x00, 0x00, 0x00 }; // glTF v2
        byte[] body = ("{\"asset\":{\"version\":\"2.0\",\"generator\":\"" + tag + "\"}}").getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[header.length + body.length];
        System.arraycopy(header, 0, out, 0, header.length);
        System.arraycopy(body, 0, out, header.length, body.length);
        return out;
    }

    private static byte[] extractIv(byte[] container) {
        int start = AssetVaultFormat.MAGIC_LENGTH_BYTES + AssetVaultFormat.VERSION_LENGTH_BYTES;
        return Arrays.copyOfRange(container, start, start + AssetVaultFormat.IV_LENGTH_BYTES);
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private static final class InputStreamPreventer extends ByteArrayInputStream {
        InputStreamPreventer(String s) {
            super(s.getBytes(StandardCharsets.UTF_8));
        }
    }
}
