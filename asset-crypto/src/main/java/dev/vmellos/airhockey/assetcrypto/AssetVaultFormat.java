package dev.vmellos.airhockey.assetcrypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Binary container constants, canonical path normalization, and opaque path hashing for the
 * Air Hockey encrypted asset vault (.lpkg).
 */
public final class AssetVaultFormat {

    /** Custom 8-byte binary header (distinct from glTF, PNG, RIFF, OggS, ZIP, or JSON). */
    private static final byte[] MAGIC = new byte[] {
        (byte) 0xA9, 0x4C, 0x56, 0x31, 0x7F, (byte) 0xC2, 0x19, 0x5E
    };

    public static final int MAGIC_LENGTH_BYTES = MAGIC.length;
    public static final int VERSION_LENGTH_BYTES = 2;
    public static final int FORMAT_VERSION = 1;
    public static final int IV_LENGTH_BYTES = 12;
    public static final int GCM_TAG_LENGTH_BITS = 128;
    public static final int GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8;
    public static final int HEADER_LENGTH_BYTES = MAGIC_LENGTH_BYTES + VERSION_LENGTH_BYTES + IV_LENGTH_BYTES;
    public static final int MIN_CONTAINER_LENGTH_BYTES = HEADER_LENGTH_BYTES + GCM_TAG_LENGTH_BYTES;

    public static final String OBFUSCATED_DIR = "lspk";
    public static final String OBFUSCATED_EXTENSION = ".lpkg";
    private static final int OBFUSCATED_HEX_LENGTH = 20;

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private AssetVaultFormat() {}

    /** Returns a copy of the proprietary binary container magic header. */
    public static byte[] magicBytes() {
        return MAGIC.clone();
    }

    /** Checks whether {@code header} starts with the proprietary container magic bytes. */
    public static boolean hasValidMagic(byte[] data) {
        if (data == null || data.length < MAGIC_LENGTH_BYTES) {
            return false;
        }
        byte[] prefix = new byte[MAGIC_LENGTH_BYTES];
        System.arraycopy(data, 0, prefix, 0, MAGIC_LENGTH_BYTES);
        return MessageDigest.isEqual(MAGIC, prefix);
    }

    /**
     * Normalizes a canonical asset path (e.g. {@code models/mesa_arcade.glb}) to forward slashes
     * without leading slashes or traversal segments.
     */
    public static String normalizeCanonicalPath(String rawPath) {
        if (rawPath == null) {
            throw new IllegalArgumentException("Canonical asset path must not be null.");
        }
        String unified = rawPath.replace('\\', '/');
        while (unified.startsWith("/")) {
            unified = unified.substring(1);
        }
        if (unified.isEmpty() || unified.endsWith("/") || unified.contains("//")) {
            throw new IllegalArgumentException("Invalid canonical asset path: '" + rawPath + "'");
        }
        String[] segments = unified.split("/");
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("Invalid segment in canonical asset path: '" + rawPath + "'");
            }
        }
        return unified;
    }

    /**
     * Maps a canonical path to an opaque vault relative path {@code lspk/<20-hex>.lpkg} using SHA-256.
     */
    public static String obfuscatePath(String canonicalPath) {
        String normalized = normalizeCanonicalPath(canonicalPath);
        byte[] digest = sha256(normalized.getBytes(StandardCharsets.UTF_8));
        String hex = toLowerHex(digest);
        return OBFUSCATED_DIR + "/" + hex.substring(0, OBFUSCATED_HEX_LENGTH).toLowerCase(Locale.ROOT)
            + OBFUSCATED_EXTENSION;
    }

    /** Computes the SHA-256 digest of {@code data}. */
    public static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable in JVM", e);
        }
    }

    private static String toLowerHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            chars[i * 2] = HEX_DIGITS[v >>> 4];
            chars[i * 2 + 1] = HEX_DIGITS[v & 0x0F];
        }
        return new String(chars);
    }
}
