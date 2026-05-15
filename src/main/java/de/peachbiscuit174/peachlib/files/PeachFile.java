package de.peachbiscuit174.peachlib.files;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.peachbiscuit174.peachlib.PeachLib;
import de.peachbiscuit174.peachlib.api.PeachLibAPI;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The File wrapper API from PeachLib.
 * <p>
 * This class provides a fluent and highly optimized way to interact with files in Paper/Spigot plugins.
 * It combines synchronous and asynchronous I/O operations, Bukkit YAML support, GSON object serialization,
 * resource extraction, and a built-in file watcher for auto-reloading.
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * PeachFile config = new PeachFile(plugin, "config.yml")
 * .extractDefault(plugin, "config.yml", false);
 * YamlConfiguration yaml = config.getYaml();
 * }</pre>
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class PeachFile {

    private final Path path;

    /** * Global Gson instance configured for pretty-printing and safe HTML escaping.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Watcher Variables
    private WatchService watchService;
    private Thread watchThread;

    /**
     * Creates a new PeachFile from a string path.
     * @param filePath The absolute or relative file path.
     */
    public PeachFile(@NotNull String filePath) { this.path = Paths.get(filePath); }

    /**
     * Creates a new PeachFile from a standard Java File.
     * @param file The java.io.File object.
     */
    public PeachFile(@NotNull File file) { this.path = file.toPath(); }

    /**
     * Creates a new PeachFile from an NIO Path.
     * @param path The java.nio.file.Path object.
     */
    public PeachFile(@NotNull Path path) { this.path = path; }

    /**
     * Creates a new PeachFile directly inside a Bukkit plugin's data folder.
     * Automatically handles the plugin folder routing.
     * @param plugin The Bukkit Plugin instance.
     * @param relativePath The file name or relative path (e.g., "data/players.json").
     */
    public PeachFile(@NotNull Plugin plugin, @NotNull String relativePath) {
        this.path = plugin.getDataFolder().toPath().resolve(relativePath);
    }

    // --- AUTO-RELOAD (FILE WATCHER) ---

    /**
     * Starts a background watcher that listens for external changes to this file
     * (e.g., when a server admin edits the file via FTP or Notepad++).
     * <p>
     * <b>Important:</b> The provided action runs asynchronously. If you need to interact
     * with the Bukkit API inside the action, you must use the Bukkit Scheduler or LibraryScheduler to jump
     * back to the main thread.
     * <p>
     * <b>Failsafe:</b> This watcher is automatically tracked by PeachLib. While you should call
     * {@link #stopWatching()} when the watcher is no longer needed, PeachLib will automatically
     * shut it down during the server stop/reload phase to prevent zombie threads.
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * myFile.onChange(() -> {
     * plugin.getLogger().info("File changed! Reloading...");
     * Bukkit.getScheduler().runTask(plugin, () -> reloadConfig());
     * });
     * }</pre>
     *
     * @param action The Runnable to execute when a change is detected.
     */
    public void onChange(@NotNull Runnable action) {
        if (this.watchThread != null) return;

        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            Path parent = this.path.getParent();
            if (parent == null) return;

            parent.register(this.watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            this.watchThread = new Thread(() -> {
                long lastModifiedTime = 0; // Debounce timer to prevent double-firing

                try {
                    WatchKey key;
                    while ((key = this.watchService.take()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            Path changedFile = (Path) event.context();

                            if (changedFile.getFileName().equals(this.path.getFileName())) {
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastModifiedTime > 500) {
                                    lastModifiedTime = currentTime;
                                    action.run();
                                }
                            }
                        }
                        key.reset();
                    }
                } catch (InterruptedException | ClosedWatchServiceException ignored) {
                    // Thread was interrupted or watcher closed -> stop gracefully
                }
            }, "PeachFile-Watcher-" + getName());

            this.watchThread.setDaemon(true);
            this.watchThread.start();

            PeachLib.registerWatchedFile(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops the file watcher and cleans up resources.
     * <p>
     * It is recommended to call this method when you no longer need to track the file
     * (e.g., when a specific player leaves or a mini-game ends).
     * <p>
     * If the watcher is meant to run until the server stops, you don't need to call this manually.
     * PeachLib features an internal failsafe that automatically cleans up all active watchers
     * in its {@code onDisable()} method, guaranteeing no memory leaks occur.
     */
    public void stopWatching() {
        try {
            if (this.watchService != null) this.watchService.close();
            if (this.watchThread != null) this.watchThread.interrupt();
            this.watchService = null;
            this.watchThread = null;

            PeachLib.unregisterWatchedFile(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- ASYNCHRONOUS OPERATIONS (Anti-Lag) ---

    /**
     * Writes content to the file asynchronously on a separate thread to prevent server lag.
     * @param content The text content to write.
     * @return A CompletableFuture containing this PeachFile instance.
     */
    public CompletableFuture<PeachFile> writeAsync(@NotNull String content) {
        return CompletableFuture.supplyAsync(() -> {
            try { return write(content); }
            catch (IOException e) { throw new RuntimeException(e); }
        }, PeachLibAPI.getSchedulerManager().getScheduler()::runAsync);
    }

    /**
     * Reads the entire file asynchronously to prevent server lag.
     * @return A CompletableFuture containing the file content as a String.
     */
    public CompletableFuture<String> readAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try { return read(); }
            catch (IOException e) { throw new RuntimeException(e); }
        }, PeachLibAPI.getSchedulerManager().getScheduler()::runAsync);
    }

    // --- BASIC I/O ---

    /**
     * Writes text to the file. Overwrites any existing content.
     * Automatically creates parent directories if they are missing.
     * @param content The text to write.
     * @return This PeachFile instance for method chaining.
     * @throws IOException If an I/O error occurs.
     */
    public PeachFile write(@NotNull String content) throws IOException {
        createParentDirectories(this.path);
        Files.writeString(this.path, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return this;
    }

    /**
     * Appends text to the end of the file without overwriting existing content.
     * @param content The text to append.
     * @return This PeachFile instance for method chaining.
     * @throws IOException If an I/O error occurs.
     */
    public PeachFile append(@NotNull String content) throws IOException {
        createParentDirectories(this.path);
        Files.writeString(this.path, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return this;
    }

    /**
     * Reads the entire content of the file as a single String.
     * @return The file content.
     * @throws IOException If an I/O error occurs.
     */
    public String read() throws IOException { return Files.readString(this.path, StandardCharsets.UTF_8); }

    /**
     * Reads all lines of the file into a List of Strings.
     * @return A list containing all lines.
     * @throws IOException If an I/O error occurs.
     */
    public List<String> readLines() throws IOException { return Files.readAllLines(this.path, StandardCharsets.UTF_8); }

    // --- OBJECT SERIALIZATION (GSON) ---

    /**
     * Reads the file content and maps it directly to a Java Object using GSON.
     * <p><b>Example:</b>
     * <pre>{@code
     * PlayerStats stats = myFile.readObject(PlayerStats.class);
     * }</pre>
     * @param clazz The class to deserialize into.
     * @return The populated Java Object.
     * @throws IOException If an I/O error occurs.
     */
    public <T> T readObject(@NotNull Class<T> clazz) throws IOException { return GSON.fromJson(read(), clazz); }

    /**
     * Serializes a Java Object to pretty-printed JSON and saves it to the file.
     * @param object The object to serialize.
     * @return This PeachFile instance for method chaining.
     * @throws IOException If an I/O error occurs.
     */
    public PeachFile writeObject(@NotNull Object object) throws IOException { return write(GSON.toJson(object)); }

    // --- JSON PARSING ---

    /**
     * Parses the file content into a generic Gson JsonElement.
     * @return The parsed JsonElement.
     * @throws IOException If an I/O error occurs.
     */
    public JsonElement readJson() throws IOException { return JsonParser.parseString(read()); }

    /**
     * Parses the file content directly into a Gson JsonObject.
     * @return The parsed JsonObject.
     * @throws IOException If an I/O error occurs.
     */
    public JsonObject readJsonObject() throws IOException { return readJson().getAsJsonObject(); }

    /**
     * Parses the file content directly into a Gson JsonArray.
     * @return The parsed JsonArray.
     * @throws IOException If an I/O error occurs.
     */
    public JsonArray readJsonArray() throws IOException { return readJson().getAsJsonArray(); }

    // --- BUKKIT & RESOURCES ---

    /**
     * Extracts a default file from the plugin's jar (src/main/resources) to the plugin folder.
     * Uses Bukkit's native saveResource method.
     *
     * @param plugin       The Bukkit Plugin instance.
     * @param resourcePath The path inside the jar's resources folder (e.g., "messages.yml").
     * @param replace      Whether to overwrite the file if it already exists in the plugin folder.
     * @return This PeachFile instance for method chaining.
     */
    public PeachFile extractDefault(@NotNull Plugin plugin, @NotNull String resourcePath, boolean replace) {
        if (replace || !exists()) {
            try { createParentDirectories(this.path); } catch (IOException ignored) {}
            plugin.saveResource(resourcePath, replace);
        }
        return this;
    }

    /**
     * Loads this file as a Bukkit YamlConfiguration.
     * Returns an empty configuration if the file does not exist yet.
     * @return The YamlConfiguration object.
     */
    public YamlConfiguration getYaml() { return YamlConfiguration.loadConfiguration(this.path.toFile()); }

    /**
     * Saves a Bukkit YamlConfiguration back to this file.
     * @param yaml The YamlConfiguration to save.
     * @return This PeachFile instance for method chaining.
     * @throws IOException If an I/O error occurs.
     */
    public PeachFile saveYaml(@NotNull YamlConfiguration yaml) throws IOException {
        createParentDirectories(this.path);
        yaml.save(this.path.toFile());
        return this;
    }

    // --- SECURITY & UTILS (Hashing / Zipping) ---

    /**
     * Calculates the SHA-256 hash of this file.
     * Useful for verifying file integrity or checking for updates.
     * @return The SHA-256 hash as a hex string, or "error" if it fails.
     */
    public String getHash() {
        try { return FileUtil.calculateHash(this.path, "SHA-256"); }
        catch (Exception e) { return "error"; }
    }

    /**
     * Compresses this file into a ZIP archive at the specified destination.
     * @param destination The target path for the .zip file.
     * @throws IOException If an I/O error occurs.
     */
    public void zipTo(@NotNull Path destination) throws IOException { FileCompressor.compress(this.path, destination); }

    // --- FILE SYSTEM OPERATIONS ---

    /**
     * Copies this file to a new location. Replaces existing files at the target.
     * @param targetPath The destination file path.
     * @return A new PeachFile instance pointing to the copied file.
     * @throws IOException If an I/O error occurs.
     */
    public PeachFile copyTo(@NotNull String targetPath) throws IOException {
        Path target = Paths.get(targetPath);
        createParentDirectories(target);
        Files.copy(this.path, target, StandardCopyOption.REPLACE_EXISTING);
        return new PeachFile(target);
    }

    /**
     * Moves (renames) this file to a new location.
     * @param targetPath The destination file path.
     * @return A new PeachFile instance pointing to the moved file.
     * @throws IOException If an I/O error occurs.
     */
    public PeachFile moveTo(@NotNull String targetPath) throws IOException {
        Path target = Paths.get(targetPath);
        createParentDirectories(target);
        Files.move(this.path, target, StandardCopyOption.REPLACE_EXISTING);
        return new PeachFile(target);
    }

    /**
     * Creates an empty file if it does not already exist.
     * @return true if created, false if it already existed.
     * @throws IOException If an I/O error occurs.
     */
    public boolean createIfNotExist() throws IOException {
        if (!exists()) {
            createParentDirectories(this.path);
            Files.createFile(this.path);
            return true;
        }
        return false;
    }

    /** Deletes the file if it exists. @return true if deleted. */
    public boolean delete() throws IOException { return Files.deleteIfExists(this.path); }

    /** Clears the content of the file without deleting it. @return This instance. */
    public PeachFile clear() throws IOException { return write(""); }

    /** Checks if the file exists on the disk. @return true if it exists. */
    public boolean exists() { return Files.exists(this.path); }

    /** Gets the size of the file in bytes. @return file size, or 0 if it doesn't exist. */
    public long getSize() throws IOException { return !exists() ? 0 : Files.size(this.path); }

    /** Gets the last modified timestamp in milliseconds. */
    public long getLastModified() throws IOException { return !exists() ? 0 : Files.getLastModifiedTime(this.path).toMillis(); }

    /** Gets the name of the file including its extension (e.g., "config.yml"). */
    public String getName() { return this.path.getFileName().toString(); }

    /** Gets the file extension (e.g., "yml", "json"), or empty string if none. */
    public String getExtension() {
        String name = getName();
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(i + 1) : "";
    }

    // --- GETTERS & HELPERS ---

    /** @return The underlying java.io.File object. */
    public File getFile() { return this.path.toFile(); }

    /** @return The underlying java.nio.file.Path object. */
    public Path getPath() { return this.path; }

    /**
     * Gets the parent directory of this file.
     * @return A new PeachFile pointing to the parent directory, or null if there is no parent.
     */
    public PeachFile getParent() {
        Path parentPath = this.path.getParent();
        return parentPath != null ? new PeachFile(parentPath) : null;
    }

    /** Internal helper to ensure all parent directories exist before writing/moving. */
    private void createParentDirectories(Path target) throws IOException {
        if (target.getParent() != null) Files.createDirectories(target.getParent());
    }
}