package dev.vmellos.airhockey.assetcrypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Authenticated AES-256-GCM encryption and decryption for the proprietary {@code .lpkg} container.
 */
public final class AssetVaultCipher {

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";

    private AssetVaultCipher() {}

    /**
     * Encrypts {@code plaintext} into the proprietary binary container bound to {@code canonicalPath}.
     */
    public static byte[] encrypt(byte[] masterKey, String canonicalPath, byte[] plaintext)
        throws GeneralSecurityException {
        AssetKeyDerivation.validateMasterKey(masterKey);
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext must not be null.");
        }
        String normalized = AssetVaultFormat.normalizeCanonicalPath(canonicalPath);
        byte[] subkey = AssetKeyDerivation.deriveSubkey(masterKey, normalized);
        byte[] iv = AssetKeyDerivation.deriveDeterministicIv(subkey, normalized, plaintext);
        byte[] aad = AssetKeyDerivation.aadBytes(normalized);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(subkey, KEY_ALGORITHM),
                new GCMParameterSpec(AssetVaultFormat.GCM_TAG_LENGTH_BITS, iv)
            );
            cipher.updateAAD(aad);
            byte[] ciphertextWithTag = cipher.doFinal(plaintext);

            byte[] magic = AssetVaultFormat.magicBytes();
            byte[] out = new byte[AssetVaultFormat.HEADER_LENGTH_BYTES + ciphertextWithTag.length];
            int offset = 0;
            System.arraycopy(magic, 0, out, offset, magic.length);
            offset += magic.length;
            out[offset++] = (byte) ((AssetVaultFormat.FORMAT_VERSION >>> 8) & 0xFF);
            out[offset++] = (byte) (AssetVaultFormat.FORMAT_VERSION & 0xFF);
            System.arraycopy(iv, 0, out, offset, iv.length);
            offset += iv.length;
            System.arraycopy(ciphertextWithTag, 0, out, offset, ciphertextWithTag.length);
            return out;
        } finally {
            Arrays.fill(subkey, (byte) 0);
            Arrays.fill(iv, (byte) 0);
            Arrays.fill(aad, (byte) 0);
        }
    }

    /**
     * Authenticates and decrypts an {@code .lpkg} container into a fresh plaintext {@code byte[]}.
     */
    public static byte[] decrypt(byte[] masterKey, String canonicalPath, byte[] containerBytes)
        throws GeneralSecurityException {
        AssetKeyDerivation.validateMasterKey(masterKey);
        if (containerBytes == null || containerBytes.length < AssetVaultFormat.MIN_CONTAINER_LENGTH_BYTES) {
            throw new GeneralSecurityException("Truncated or null asset vault container.");
        }

        byte[] expectedMagic = AssetVaultFormat.magicBytes();
        byte[] actualMagic = Arrays.copyOfRange(containerBytes, 0, AssetVaultFormat.MAGIC_LENGTH_BYTES);
        if (!MessageDigest.isEqual(expectedMagic, actualMagic)) {
            throw new GeneralSecurityException("Invalid asset vault container magic header.");
        }

        int versionOffset = AssetVaultFormat.MAGIC_LENGTH_BYTES;
        int version = ((containerBytes[versionOffset] & 0xFF) << 8)
            | (containerBytes[versionOffset + 1] & 0xFF);
        if (version != AssetVaultFormat.FORMAT_VERSION) {
            throw new GeneralSecurityException("Unsupported asset vault version: " + version);
        }

        int ivOffset = versionOffset + AssetVaultFormat.VERSION_LENGTH_BYTES;
        byte[] iv = Arrays.copyOfRange(containerBytes, ivOffset, ivOffset + AssetVaultFormat.IV_LENGTH_BYTES);
        int cipherOffset = AssetVaultFormat.HEADER_LENGTH_BYTES;
        int cipherLength = containerBytes.length - cipherOffset;

        String normalized = AssetVaultFormat.normalizeCanonicalPath(canonicalPath);
        byte[] subkey = AssetKeyDerivation.deriveSubkey(masterKey, normalized);
        byte[] aad = AssetKeyDerivation.aadBytes(normalized);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(subkey, KEY_ALGORITHM),
                new GCMParameterSpec(AssetVaultFormat.GCM_TAG_LENGTH_BITS, iv)
            );
            cipher.updateAAD(aad);
            return cipher.doFinal(containerBytes, cipherOffset, cipherLength);
        } finally {
            Arrays.fill(subkey, (byte) 0);
            Arrays.fill(iv, (byte) 0);
            Arrays.fill(aad, (byte) 0);
        }
    }

    /**
     * Reads all bytes from {@code containerStream}, decrypts in memory, and zeroes the intermediate
     * encrypted buffer before returning.
     */
    public static byte[] decrypt(byte[] masterKey, String canonicalPath, InputStream containerStream)
        throws IOException, GeneralSecurityException {
        if (containerStream == null) {
            throw new IllegalArgumentException("Container stream must not be null.");
        }
        byte[] encrypted = readAllBytes(containerStream);
        try {
            return decrypt(masterKey, canonicalPath, encrypted);
        } finally {
            Arrays.fill(encrypted, (byte) 0);
        }
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[16 * 1024];
        try {
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        } finally {
            Arrays.fill(chunk, (byte) 0);
        }
    }
}
