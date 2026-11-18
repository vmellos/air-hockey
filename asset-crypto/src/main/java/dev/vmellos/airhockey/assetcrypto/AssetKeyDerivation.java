package dev.vmellos.airhockey.assetcrypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Deterministic HMAC-SHA256 key derivation, per-asset subkeys, deterministic 96-bit GCM IVs, and
 * AAD binding shared by the build packer and runtime loader.
 */
public final class AssetKeyDerivation {

    public static final int KEY_LENGTH_BYTES = 32;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final byte[] MASTER_DOMAIN_KEY =
        "airhockey.asset.vault.master.v1".getBytes(StandardCharsets.UTF_8);

    private AssetKeyDerivation() {}

    /** Derives a 256-bit vault master key from the raw secret bytes read via stdin or config. */
    public static byte[] deriveMasterKey(byte[] secretBytes) throws GeneralSecurityException {
        if (secretBytes == null || secretBytes.length == 0) {
            throw new IllegalArgumentException("Vault secret must not be empty.");
        }
        return hmacSha256(MASTER_DOMAIN_KEY, secretBytes);
    }

    /** Derives a unique 256-bit AES subkey bound to {@code canonicalPath}. */
    public static byte[] deriveSubkey(byte[] masterKey, String canonicalPath)
        throws GeneralSecurityException {
        validateMasterKey(masterKey);
        String normalized = AssetVaultFormat.normalizeCanonicalPath(canonicalPath);
        byte[] info = ("airhockey.asset.subkey.v1:" + normalized).getBytes(StandardCharsets.UTF_8);
        return hmacSha256(masterKey, info);
    }

    /**
     * Derives a deterministic 96-bit GCM IV from {@code HMAC-SHA256(subkey, path + sha256(content))},
     * preserving Gradle incremental/build cache reproducibility without reusing {@code (key, IV)}.
     */
    public static byte[] deriveDeterministicIv(byte[] subkey, String canonicalPath, byte[] plaintext)
        throws GeneralSecurityException {
        if (subkey == null || subkey.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("Subkey must be 32 bytes.");
        }
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext must not be null.");
        }
        String normalized = AssetVaultFormat.normalizeCanonicalPath(canonicalPath);
        byte[] pathPrefix = (normalized + "\u0000").getBytes(StandardCharsets.UTF_8);
        byte[] contentHash = AssetVaultFormat.sha256(plaintext);
        byte[] macInput = new byte[pathPrefix.length + contentHash.length];
        System.arraycopy(pathPrefix, 0, macInput, 0, pathPrefix.length);
        System.arraycopy(contentHash, 0, macInput, pathPrefix.length, contentHash.length);
        byte[] fullMac = hmacSha256(subkey, macInput);
        try {
            return Arrays.copyOf(fullMac, AssetVaultFormat.IV_LENGTH_BYTES);
        } finally {
            Arrays.fill(macInput, (byte) 0);
            Arrays.fill(fullMac, (byte) 0);
        }
    }

    /** Builds the GCM Additional Authenticated Data (AAD) bound to {@code canonicalPath}. */
    public static byte[] aadBytes(String canonicalPath) {
        String normalized = AssetVaultFormat.normalizeCanonicalPath(canonicalPath);
        return ("airhockey.asset.aad.v1:" + normalized).getBytes(StandardCharsets.UTF_8);
    }

    static byte[] hmacSha256(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
        return mac.doFinal(data);
    }

    static void validateMasterKey(byte[] masterKey) {
        if (masterKey == null || masterKey.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("Master key must be 32 bytes.");
        }
    }
}
