package de.peachbiscuit174.peachlib.api.files;

import de.peachbiscuit174.peachlib.api.API;
import de.peachbiscuit174.peachlib.files.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleConsumer;

/**
 * API for asynchronous file and directory manipulation.
 * Includes safe file saving, reading, downloads, and backup management.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class FileUtilAPI {

    /**
     * Asynchronously deletes a directory and all its contents recursively.
     *
     * @param path The path to delete.
     * @return A {@link CompletableFuture} completing with true when finished.
     */
    public CompletableFuture<Boolean> deleteRecursivelyAsync(@NotNull Path path) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                FileUtil.deleteRecursively(path);
                future.complete(true);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously copies a directory and its contents recursively.
     *
     * @param source      The source directory.
     * @param destination The target directory.
     * @return A {@link CompletableFuture} completing with the destination path.
     */
    public CompletableFuture<Path> copyRecursivelyAsync(@NotNull Path source, @NotNull Path destination) {
        CompletableFuture<Path> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                FileUtil.copyRecursively(source, destination);
                future.complete(destination);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously calculates the total size of a file or directory.
     *
     * @param path The path to calculate.
     * @return A {@link CompletableFuture} completing with the size in bytes.
     */
    public CompletableFuture<Long> calculateSizeAsync(@NotNull Path path) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                long size = FileUtil.calculateSize(path);
                future.complete(size);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously writes a string to a file using an atomic move operation.
     * This prevents file corruption if the server crashes during the write process.
     *
     * @param destination The file to write to.
     * @param content     The string content (e.g., JSON or YAML string).
     * @return A {@link CompletableFuture} completing with true when safely written.
     */
    public CompletableFuture<Boolean> writeStringAtomicallyAsync(@NotNull Path destination, @NotNull String content) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                FileUtil.writeStringAtomically(destination, content);
                future.complete(true);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously reads the entire content of a file into a string.
     * Prevents server lag when reading large configuration or data files.
     *
     * @param source The file to read.
     * @return A {@link CompletableFuture} completing with the string content.
     */
    public CompletableFuture<String> readStringAsync(@NotNull Path source) {
        CompletableFuture<String> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                String content = FileUtil.readString(source);
                future.complete(content);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously downloads a file from the internet securely.
     *
     * @param url         The URL of the file.
     * @param destination The local path to save it to.
     * @return A {@link CompletableFuture} completing with the destination path.
     */
    public CompletableFuture<Path> downloadFileAsync(@NotNull String url, @NotNull Path destination) {
        return downloadFileWithProgressAsync(url, destination, progress -> {});
    }

    /**
     * Asynchronously downloads a file from the internet while reporting progress.
     *
     * @param url              The URL of the file.
     * @param destination      The local path to save it to.
     * @param progressCallback Reports download progress (0.0 to 1.0). Returns -1.0 if the server doesn't provide file size.
     * @return A {@link CompletableFuture} completing with the destination path.
     */
    public CompletableFuture<Path> downloadFileWithProgressAsync(@NotNull String url, @NotNull Path destination, @NotNull DoubleConsumer progressCallback) {
        CompletableFuture<Path> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                FileUtil.downloadFileWithProgress(url, destination, progressCallback);
                future.complete(destination);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously calculates the hash of a file.
     *
     * @param file The file to hash.
     * @return A {@link CompletableFuture} completing with the lowercase hex hash string.
     */
    public CompletableFuture<String> calculateSha256Async(@NotNull Path file) {
        CompletableFuture<String> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                String hash = FileUtil.calculateHash(file, "SHA-256");
                future.complete(hash);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously cleans up old backup files in a directory, keeping only the specified amount of the newest files.
     *
     * @param directory The folder containing the backups.
     * @param prefix    The prefix to filter specific backups (e.g. "world_"). Use "" to target all files.
     * @param maxKeep   The number of most recent files to keep.
     * @return A {@link CompletableFuture} completing with true when the cleanup is finished.
     */
    public CompletableFuture<Boolean> cleanOldBackupsAsync(@NotNull Path directory, @NotNull String prefix, int maxKeep) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                FileUtil.cleanOldBackups(directory, prefix, maxKeep);
                future.complete(true);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously appends a message to a log file with an automatic timestamp.
     * Perfect for tracking player actions, errors, or plugin events without lagging the server.
     *
     * @param logFile The log file path (e.g., plugin.getDataFolder().toPath().resolve("logs/actions.log")).
     * @param message The message to write.
     * @return A {@link CompletableFuture} completing with true when safely written.
     */
    public CompletableFuture<Boolean> appendLogAsync(@NotNull Path logFile, @NotNull String message) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                FileUtil.appendToLog(logFile, message);
                future.complete(true);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}