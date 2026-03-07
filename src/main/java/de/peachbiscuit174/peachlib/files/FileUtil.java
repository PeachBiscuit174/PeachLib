package de.peachbiscuit174.peachlib.files;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;
import java.util.stream.Stream;

/**
 * Internal backend utility for common file and directory operations.
 * Includes downloading, hashing, atomic file saving, and backup rotation.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class FileUtil {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Recursively deletes a directory and all its contents.
     *
     * @param path The path to the file or directory to delete.
     * @throws IOException If an I/O error occurs.
     */
    public static void deleteRecursively(@NotNull Path path) throws IOException {
        if (!Files.exists(path)) return;

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Recursively copies a directory to a new location.
     *
     * @param source      The source directory.
     * @param destination The target directory.
     * @throws IOException If an I/O error occurs.
     */
    public static void copyRecursively(@NotNull Path source, @NotNull Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                Path targetDir = destination.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Calculates the total size of a file or directory in bytes.
     *
     * @param path The path to calculate the size for.
     * @return The size in bytes.
     * @throws IOException If an I/O error occurs.
     */
    public static long calculateSize(@NotNull Path path) throws IOException {
        if (!Files.exists(path)) return 0;

        if (Files.isRegularFile(path)) {
            return Files.size(path);
        }

        AtomicLong size = new AtomicLong(0);
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
        });
        return size.get();
    }

    /**
     * Atomically writes a string to a file.
     * Prevents file corruption if the server crashes during the write process.
     *
     * @param destination The file to write to.
     * @param content     The string content (e.g., JSON or YAML).
     * @throws IOException If an I/O error occurs.
     */
    public static void writeStringAtomically(@NotNull Path destination, @NotNull String content) throws IOException {
        Files.createDirectories(destination.getParent());
        Path tempFile = destination.getParent().resolve(destination.getFileName().toString() + ".tmp");
        Files.writeString(tempFile, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Reads the entire content of a file into a string using UTF-8 encoding.
     *
     * @param source The file to read.
     * @return The file content as a String.
     * @throws IOException If an I/O error occurs.
     */
    public static @NotNull String readString(@NotNull Path source) throws IOException {
        return Files.readString(source, StandardCharsets.UTF_8);
    }

    /**
     * Downloads a file securely while reporting the progress.
     *
     * @param fileUrl          The URL to download from.
     * @param destination      The local target path.
     * @param progressCallback A consumer receiving progress from 0.0 to 1.0 (or -1.0 if size is unknown).
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the thread is interrupted.
     */
    public static void downloadFileWithProgress(@NotNull String fileUrl, @NotNull Path destination, @NotNull DoubleConsumer progressCallback) throws IOException, InterruptedException {
        Files.createDirectories(destination.getParent());

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fileUrl)).GET().build();
        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Failed to download file. HTTP Status Code: " + response.statusCode());
        }

        long totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        long downloadedBytes = 0;

        try (InputStream is = response.body();
             OutputStream os = Files.newOutputStream(destination, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            byte[] buffer = new byte[65536];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                downloadedBytes += read;

                if (totalBytes > 0) {
                    progressCallback.accept((double) downloadedBytes / totalBytes);
                } else {
                    progressCallback.accept(-1.0);
                }
            }
        } catch (Exception e) {
            Files.deleteIfExists(destination);
            throw e;
        }
    }

    /**
     * Calculates the hash of a file.
     *
     * @param file      The file to hash.
     * @param algorithm The algorithm to use (e.g., "SHA-256", "MD5").
     * @return The hash as a lowercase hex string.
     * @throws IOException              If an I/O error occurs.
     * @throws NoSuchAlgorithmException If the requested algorithm does not exist.
     */
    public static @NotNull String calculateHash(@NotNull Path file, @NotNull String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[65536];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Cleans up old files in a directory, keeping only the newest specified amount.
     * Perfect for managing rotating backup archives.
     *
     * @param directory The directory containing the backups.
     * @param prefix    Only files starting with this prefix will be considered (e.g. "world_backup_"). Use "" for all files.
     * @param maxKeep   The maximum number of files to keep.
     * @throws IOException If an I/O error occurs during deletion or listing.
     */
    public static void cleanOldBackups(@NotNull Path directory, @NotNull String prefix, int maxKeep) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) return;

        try (Stream<Path> files = Files.list(directory)) {
            List<Path> sortedBackups = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(prefix))
                    .sorted(Comparator.comparingLong((Path p) -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).reversed()) // Sort descending (newest first)
                    .toList();

            for (int i = maxKeep; i < sortedBackups.size(); i++) {
                Files.deleteIfExists(sortedBackups.get(i));
            }
        }
    }

    /**
     * Appends a message to a log file, automatically adding a timestamp.
     * If the file or directory does not exist, it will be created.
     *
     * @param logFile The path to the log file.
     * @param message The message to log.
     * @throws IOException If an I/O error occurs.
     */
    public static void appendToLog(@NotNull Path logFile, @NotNull String message) throws IOException {
        if (logFile.getParent() != null) {
            Files.createDirectories(logFile.getParent());
        }

        // Generiert den Zeitstempel im Format [YYYY-MM-DD HH:mm:ss]
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String formattedMessage = "[" + timestamp + "] " + message + System.lineSeparator();

        Files.writeString(logFile, formattedMessage, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}