package de.peachbiscuit174.peachlib.files;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.zip.*;

/**
 * Internal backend utility for handling maximum level file compression and secure decompression.
 * Includes absolute protection against Zip Slip and Zip Bomb vulnerabilities using atomic extraction.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class FileCompressor {

    private static final long MAX_UNCOMPRESSED_SIZE = 1024L * 1024L * 1024L; // 1 GB max limit
    private static final int MAX_ENTRIES = 10000; // Max 10.000 files in a single zip

    /**
     * Compresses a single file using the highest possible Deflater compression level (Level 9).
     *
     * @param source      The path to the original file.
     * @param destination The path where the compressed file should be saved.
     * @throws IOException If an I/O error occurs reading or writing.
     */
    public static void compress(@NotNull Path source, @NotNull Path destination) throws IOException {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try (InputStream in = Files.newInputStream(source);
             OutputStream out = Files.newOutputStream(destination, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             DeflaterOutputStream defOut = new DeflaterOutputStream(out, deflater, 65536)) {
            in.transferTo(defOut);
        } finally {
            deflater.end();
        }
    }

    /**
     * Decompresses a previously compressed single file.
     *
     * @param source      The path to the compressed file.
     * @param destination The path where the original file should be restored.
     * @throws IOException If an I/O error occurs reading or writing.
     */
    public static void decompress(@NotNull Path source, @NotNull Path destination) throws IOException {
        Inflater inflater = new Inflater();
        try (InputStream in = Files.newInputStream(source);
             InflaterInputStream infIn = new InflaterInputStream(in, inflater, 65536);
             OutputStream out = Files.newOutputStream(destination, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            infIn.transferTo(out);
        } finally {
            inflater.end();
        }
    }

    /**
     * Zips an entire directory with a custom filter and reports the progress back via a callback.
     * Highly recommended for large backups to provide feedback to the server admin.
     *
     * @param sourceDirectory  The directory to compress.
     * @param targetZipFile    The target zip file.
     * @param filter           A Predicate to filter files (true = keep, false = ignore).
     * @param progressCallback A consumer receiving a double from 0.0 to 1.0 representing the progress.
     * @throws IOException If an I/O error occurs.
     */
    public static void zipDirectoryWithProgress(@NotNull Path sourceDirectory, @NotNull Path targetZipFile, @NotNull Predicate<Path> filter, @NotNull DoubleConsumer progressCallback) throws IOException {
        long[] sizeData = {0};
        Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                return filter.test(dir) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
            }
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                if (filter.test(file)) sizeData[0] += attrs.size();
                return FileVisitResult.CONTINUE;
            }
        });

        long exactTotalSize = sizeData[0];
        long[] processedSize = {0};

        try (OutputStream fos = Files.newOutputStream(targetZipFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            zos.setLevel(Deflater.BEST_COMPRESSION);

            Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                    return filter.test(dir) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    if (!filter.test(file)) return FileVisitResult.CONTINUE;

                    Path targetFile = sourceDirectory.relativize(file);
                    zos.putNextEntry(new ZipEntry(targetFile.toString().replace("\\", "/")));

                    try (InputStream in = Files.newInputStream(file)) {
                        byte[] buffer = new byte[65536];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            zos.write(buffer, 0, read);
                            processedSize[0] += read;

                            if (exactTotalSize > 0) {
                                progressCallback.accept((double) processedSize[0] / exactTotalSize);
                            } else {
                                progressCallback.accept(1.0);
                            }
                        }
                    }
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Unzips a file ATOMICALLY and SECURELY into a target directory.
     * Extracts to a temporary folder first. If ANY security constraint is violated (Zip Bomb / Zip Slip),
     * the extraction aborts and leaves no trace in the target directory.
     *
     * @param zipFile         The malicious or safe zip file to extract.
     * @param targetDirectory The directory to extract to.
     * @throws IOException       If an I/O error occurs.
     * @throws SecurityException If a malicious zip structure is detected.
     */
    public static void unzipSecurely(@NotNull Path zipFile, @NotNull Path targetDirectory) throws IOException {
        Path normalizedTargetDir = targetDirectory.toAbsolutePath().normalize();
        Files.createDirectories(normalizedTargetDir);
        Path tempDir = Files.createTempDirectory(normalizedTargetDir.getParent(), "peachlib_unzip_temp_");

        try {
            int entriesCount = 0;
            long totalUncompressedBytes = 0;

            try (InputStream fis = Files.newInputStream(zipFile);
                 ZipInputStream zis = new ZipInputStream(fis)) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    entriesCount++;
                    if (entriesCount > MAX_ENTRIES) {
                        throw new SecurityException("Zip Bomb detected: Too many entries (Max: " + MAX_ENTRIES + ")");
                    }

                    Path resolvedPath = tempDir.resolve(entry.getName()).normalize();
                    if (!resolvedPath.startsWith(tempDir)) {
                        throw new SecurityException("Zip Slip vulnerability detected: " + entry.getName());
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(resolvedPath);
                    } else {
                        Files.createDirectories(resolvedPath.getParent());
                        try (OutputStream out = Files.newOutputStream(resolvedPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                            byte[] buffer = new byte[65536];
                            int bytesRead;

                            while ((bytesRead = zis.read(buffer)) != -1) {
                                totalUncompressedBytes += bytesRead;
                                if (totalUncompressedBytes > MAX_UNCOMPRESSED_SIZE) {
                                    throw new SecurityException("Zip Bomb detected: Uncompressed size exceeds limit.");
                                }
                                out.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }
            FileUtil.copyRecursively(tempDir, normalizedTargetDir);
        } finally {
            FileUtil.deleteRecursively(tempDir);
        }
    }
}