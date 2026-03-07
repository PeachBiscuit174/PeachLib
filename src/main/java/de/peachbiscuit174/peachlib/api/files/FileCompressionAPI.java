package de.peachbiscuit174.peachlib.api.files;

import de.peachbiscuit174.peachlib.api.API;
import de.peachbiscuit174.peachlib.files.FileCompressor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;

/**
 * API for asynchronous, secure file and directory compression.
 * Includes native protections against malicious zip files and progress tracking.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class FileCompressionAPI {

    /**
     * Asynchronously compresses a single file using maximum lossless compression.
     *
     * @param source      The file to compress.
     * @param destination The target path for the compressed file.
     * @return A {@link CompletableFuture} completing with the destination path.
     */
    public CompletableFuture<Path> compressSingleFileAsync(@NotNull Path source, @NotNull Path destination) {
        CompletableFuture<Path> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                FileCompressor.compress(source, destination);
                future.complete(destination);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously decompresses a single file.
     *
     * @param source      The compressed file to read.
     * @param destination The target path to extract to.
     * @return A {@link CompletableFuture} completing with the destination path.
     */
    public CompletableFuture<Path> decompressSingleFileAsync(@NotNull Path source, @NotNull Path destination) {
        CompletableFuture<Path> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                FileCompressor.decompress(source, destination);
                future.complete(destination);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously zips an entire directory with maximum compression.
     * Includes all files within the directory.
     *
     * @param sourceDirectory The directory to compress.
     * @param targetZipFile   The target zip file.
     * @return A {@link CompletableFuture} completing with the zip file path.
     */
    public CompletableFuture<Path> zipDirectoryAsync(@NotNull Path sourceDirectory, @NotNull Path targetZipFile) {
        return zipDirectoryAsync(sourceDirectory, targetZipFile, path -> true);
    }

    /**
     * Asynchronously zips an entire directory with maximum compression, applying a custom filter.
     *
     * @param sourceDirectory The directory to compress.
     * @param targetZipFile   The target zip file.
     * @param filter          A predicate that returns true to include a file/folder, or false to skip it.
     * @return A {@link CompletableFuture} completing with the zip file path.
     */
    public CompletableFuture<Path> zipDirectoryAsync(@NotNull Path sourceDirectory, @NotNull Path targetZipFile, @NotNull Predicate<Path> filter) {
        return zipDirectoryWithProgressAsync(sourceDirectory, targetZipFile, filter, progress -> {});
    }

    /**
     * Asynchronously zips an entire directory with maximum compression, applying a custom filter,
     * and reports the progress back in real-time.
     *
     * @param sourceDirectory  The folder to zip.
     * @param targetZipFile    The destination .zip file.
     * @param filter           A predicate to include/exclude files.
     * @param progressCallback A callback that receives a value between 0.0 and 1.0 representing completion.
     * @return A {@link CompletableFuture} completing with the zip file path.
     */
    public CompletableFuture<Path> zipDirectoryWithProgressAsync(@NotNull Path sourceDirectory, @NotNull Path targetZipFile, @NotNull Predicate<Path> filter, @NotNull DoubleConsumer progressCallback) {
        CompletableFuture<Path> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                FileCompressor.zipDirectoryWithProgress(sourceDirectory, targetZipFile, filter, progressCallback);
                future.complete(targetZipFile);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously unzips a file securely.
     * Contains built-in absolute protection against Zip Slip and Zip Bomb vulnerabilities.
     *
     * @param zipFile         The zip file to extract.
     * @param targetDirectory The destination folder.
     * @return A {@link CompletableFuture} completing with the target directory path.
     */
    public CompletableFuture<Path> unzipSecurelyAsync(@NotNull Path zipFile, @NotNull Path targetDirectory) {
        CompletableFuture<Path> future = new CompletableFuture<>();
        API.getSchedulerManager().getScheduler().runAsync(() -> {
            try {
                FileCompressor.unzipSecurely(zipFile, targetDirectory);
                future.complete(targetDirectory);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}