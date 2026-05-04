package de.peachbiscuit174.peachlib.api.managers;

import de.peachbiscuit174.peachlib.api.files.FileCompressionAPI;
import de.peachbiscuit174.peachlib.api.files.FileUtilAPI;
import de.peachbiscuit174.peachlib.files.PeachFile;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;

/**
 * Manager for file-related operations and utilities.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class FileManager {

    private final FileCompressionAPI fileCompressionAPI = new FileCompressionAPI();
    private final FileUtilAPI fileUtilAPI = new FileUtilAPI();

    /**
     * Gets the API responsible for asynchronous, secure file compression and decompression.
     *
     * @return The {@link FileCompressionAPI} instance.
     */
    public FileCompressionAPI getFileCompressionAPI() {
        return fileCompressionAPI;
    }

    /**
     * Gets the API responsible for asynchronous directory and file manipulations.
     *
     * @return The {@link FileUtilAPI} instance.
     */
    public FileUtilAPI getFileUtilAPI() {
        return fileUtilAPI;
    }

    /**
     * Wraps a string path into a convenient PeachFile API.
     *
     * @param filePath The path to the file.
     * @return A new {@link PeachFile} instance.
     */
    public PeachFile getPeachFile(@NotNull String filePath) {
        return new PeachFile(filePath);
    }

    /**
     * Wraps a standard Java File into a convenient PeachFile API.
     *
     * @param file The file object.
     * @return A new {@link PeachFile} instance.
     */
    public PeachFile getPeachFile(@NotNull File file) {
        return new PeachFile(file);
    }

    /**
     * Wraps a NIO Path into a convenient PeachFile API.
     *
     * @param path The path object.
     * @return A new {@link PeachFile} instance.
     */
    public PeachFile getPeachFile(@NotNull Path path) {
        return new PeachFile(path);
    }

    /**
     * Creates a PeachFile inside a specific plugin's data folder.
     *
     * @param plugin       The Bukkit Plugin.
     * @param relativePath The path relative to the plugin's folder.
     * @return A new {@link PeachFile} instance.
     */
    public PeachFile getPluginFile(@NotNull Plugin plugin, @NotNull String relativePath) {
        return new PeachFile(plugin, relativePath);
    }
}