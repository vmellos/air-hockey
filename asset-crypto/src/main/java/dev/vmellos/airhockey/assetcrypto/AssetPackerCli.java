package dev.vmellos.airhockey.assetcrypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * CLI entrypoint invoked by Gradle build tasks. Reads the vault secret exclusively from {@code stdin}
 * (never process arguments) and writes encrypted {@code .lpkg} assets and optional Kotlin key source.
 */
public final class AssetPackerCli {

    private AssetPackerCli() {}

    public static void main(String[] args) throws Exception {
        Path inputDir = null;
        Path outputAssetsDir = null;
        Path outputKotlinDir = null;
        String kotlinPackage = "dev.vmellos.airhockey.core.assets";
        String kotlinClassName = "GeneratedAssetVaultKey";
        String variantName = "default";

        for (int i = 0; i < args.length; i++) {
            String flag = args[i];
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for flag: " + flag);
            }
            String value = args[++i];
            switch (flag) {
                case "--input-dir":
                    inputDir = Paths.get(value);
                    break;
                case "--output-assets-dir":
                    outputAssetsDir = Paths.get(value);
                    break;
                case "--output-kotlin-dir":
                    outputKotlinDir = Paths.get(value);
                    break;
                case "--package":
                    kotlinPackage = value;
                    break;
                case "--class-name":
                    kotlinClassName = value;
                    break;
                case "--variant":
                    variantName = value;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported CLI argument: " + flag);
            }
        }

        if (inputDir == null || outputAssetsDir == null) {
            throw new IllegalArgumentException(
                "Usage: AssetPackerCli --input-dir <dir> --output-assets-dir <dir> "
                    + "[--output-kotlin-dir <dir> --package <pkg> --class-name <name> --variant <variant>]"
            );
        }

        byte[] secretBytes = readTrimmedSecretFromStdin(System.in);
        byte[] masterKey = null;
        try {
            masterKey = AssetKeyDerivation.deriveMasterKey(secretBytes);
            AssetPacker.packDirectory(inputDir, outputAssetsDir, masterKey);
            if (outputKotlinDir != null) {
                writeGeneratedKotlinKey(outputKotlinDir, kotlinPackage, kotlinClassName, variantName, masterKey);
            }
        } finally {
            Arrays.fill(secretBytes, (byte) 0);
            if (masterKey != null) {
                Arrays.fill(masterKey, (byte) 0);
            }
        }
    }

    static byte[] readTrimmedSecretFromStdin(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        try {
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            byte[] raw = out.toByteArray();
            int end = raw.length;
            while (end > 0 && (raw[end - 1] == '\n' || raw[end - 1] == '\r')) {
                end--;
            }
            if (end == 0) {
                Arrays.fill(raw, (byte) 0);
                throw new IllegalArgumentException("Vault secret read from stdin must not be empty.");
            }
            byte[] trimmed = Arrays.copyOf(raw, end);
            Arrays.fill(raw, (byte) 0);
            return trimmed;
        } finally {
            Arrays.fill(buf, (byte) 0);
        }
    }

    private static void writeGeneratedKotlinKey(
        Path outputKotlinDir,
        String packageName,
        String className,
        String variantName,
        byte[] masterKey
    ) throws IOException, GeneralSecurityException {
        Files.createDirectories(outputKotlinDir);
        Path packageDir = outputKotlinDir.resolve(packageName.replace('.', '/'));
        Files.createDirectories(packageDir);
        Path targetFile = packageDir.resolve(className + ".kt").toAbsolutePath().normalize();

        String source = KotlinKeyClassGenerator.generateSource(packageName, className, variantName, masterKey);
        byte[] desired = source.getBytes(StandardCharsets.UTF_8);
        if (!Files.isRegularFile(targetFile) || !Arrays.equals(Files.readAllBytes(targetFile), desired)) {
            Files.write(targetFile, desired);
        }

        List<Path> entries = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(outputKotlinDir)) {
            walk.forEach(entries::add);
        }
        entries.sort(Comparator.reverseOrder());
        Path normalizedRoot = outputKotlinDir.toAbsolutePath().normalize();
        for (Path entry : entries) {
            Path norm = entry.toAbsolutePath().normalize();
            if (norm.equals(normalizedRoot)) {
                continue;
            }
            if (Files.isRegularFile(entry) && !norm.equals(targetFile)) {
                Files.deleteIfExists(entry);
            } else if (Files.isDirectory(entry)) {
                try (Stream<Path> children = Files.list(entry)) {
                    if (children.findAny().isEmpty()) {
                        Files.deleteIfExists(entry);
                    }
                }
            }
        }
    }
}
