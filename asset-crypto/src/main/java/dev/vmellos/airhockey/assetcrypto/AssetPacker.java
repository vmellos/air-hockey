package dev.vmellos.airhockey.assetcrypto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Packages raw assets into deterministic {@code lspk/<20-hex>.lpkg} encrypted blobs and prunes
 * orphan outputs when assets are renamed or deleted.
 */
public final class AssetPacker {

    private AssetPacker() {}

    /** Summary of a vault packaging run. */
    public static final class PackSummary {
        private final int packedCount;
        private final int prunedOrphansCount;

        public PackSummary(int packedCount, int prunedOrphansCount) {
            this.packedCount = packedCount;
            this.prunedOrphansCount = prunedOrphansCount;
        }

        public int getPackedCount() {
            return packedCount;
        }

        public int getPrunedOrphansCount() {
            return prunedOrphansCount;
        }
    }

    /**
     * Encrypts every regular file under {@code inputDir} into {@code outputAssetsDir/lspk/*.lpkg}
     * and removes any orphan files under {@code outputAssetsDir}.
     */
    public static PackSummary packDirectory(Path inputDir, Path outputAssetsDir, byte[] masterKey)
        throws IOException, GeneralSecurityException {
        AssetKeyDerivation.validateMasterKey(masterKey);
        if (inputDir == null || !Files.isDirectory(inputDir)) {
            throw new IllegalArgumentException("Input raw assets directory does not exist: " + inputDir);
        }
        if (outputAssetsDir == null) {
            throw new IllegalArgumentException("Output assets directory must not be null.");
        }
        Files.createDirectories(outputAssetsDir);

        List<Path> sourceFiles = collectSortedRegularFiles(inputDir);
        Map<String, String> hashOwners = new HashMap<>();
        Set<Path> expectedOutputs = new HashSet<>();

        int packedCount = 0;
        for (Path sourceFile : sourceFiles) {
            String canonicalPath = AssetVaultFormat.normalizeCanonicalPath(
                inputDir.relativize(sourceFile).toString()
            );
            String obfuscatedRelative = AssetVaultFormat.obfuscatePath(canonicalPath);
            String existingOwner = hashOwners.putIfAbsent(obfuscatedRelative, canonicalPath);
            if (existingOwner != null) {
                throw new IllegalStateException(
                    "Obfuscated path hash collision between '" + existingOwner + "' and '" + canonicalPath + "'"
                );
            }

            Path outputFile = outputAssetsDir.resolve(obfuscatedRelative).normalize();
            expectedOutputs.add(outputFile.toAbsolutePath().normalize());
            Files.createDirectories(outputFile.getParent());

            byte[] plaintext = Files.readAllBytes(sourceFile);
            byte[] encrypted = null;
            try {
                encrypted = AssetVaultCipher.encrypt(masterKey, canonicalPath, plaintext);
                writeIfChanged(outputFile, encrypted);
                packedCount++;
            } finally {
                Arrays.fill(plaintext, (byte) 0);
                if (encrypted != null) {
                    Arrays.fill(encrypted, (byte) 0);
                }
            }
        }

        int pruned = pruneOrphans(outputAssetsDir, expectedOutputs);
        return new PackSummary(packedCount, pruned);
    }

    private static List<Path> collectSortedRegularFiles(Path inputDir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(inputDir)) {
            walk.filter(Files::isRegularFile).forEach(files::add);
        }
        files.sort(Comparator.comparing(p -> inputDir.relativize(p).toString().replace('\\', '/')));
        return files;
    }

    private static void writeIfChanged(Path outputFile, byte[] desired) throws IOException {
        if (Files.isRegularFile(outputFile)) {
            byte[] existing = Files.readAllBytes(outputFile);
            if (Arrays.equals(existing, desired)) {
                return;
            }
        }
        Files.write(outputFile, desired);
    }

    private static int pruneOrphans(Path outputAssetsDir, Set<Path> expectedOutputs) throws IOException {
        int pruned = 0;
        List<Path> allEntries = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(outputAssetsDir)) {
            walk.forEach(allEntries::add);
        }
        allEntries.sort(Comparator.reverseOrder());

        Path normalizedRoot = outputAssetsDir.toAbsolutePath().normalize();
        for (Path entry : allEntries) {
            Path normalizedEntry = entry.toAbsolutePath().normalize();
            if (normalizedEntry.equals(normalizedRoot)) {
                continue;
            }
            if (Files.isRegularFile(entry)) {
                if (!expectedOutputs.contains(normalizedEntry)) {
                    Files.deleteIfExists(entry);
                    pruned++;
                }
            } else if (Files.isDirectory(entry)) {
                try (Stream<Path> children = Files.list(entry)) {
                    if (children.findAny().isEmpty()) {
                        Files.deleteIfExists(entry);
                    }
                }
            }
        }
        return pruned;
    }
}
