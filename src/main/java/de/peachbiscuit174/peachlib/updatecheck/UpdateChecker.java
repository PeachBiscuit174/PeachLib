package de.peachbiscuit174.peachlib.updatecheck;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.peachbiscuit174.peachlib.PeachLib;
import de.peachbiscuit174.peachlib.configstuff.ConfigData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Robust and secure UpdateChecker for PeachLib.
 * <p>
 * <b>Security & Integrity Features:</b>
 * <ul>
 * <li><b>Atomic Downloads:</b> Downloads updates to a temporary file first (`.tmp`).
 * Only after successful SHA-256 verification and API compatibility checks is the file
 * atomically moved to the final `.jar` name. This prevents corrupt files from crashing the server on restart.</li>
 * <li><b>Strict API Checking:</b> Prevents downloading a version intended for a newer Minecraft version
 * than the one currently running.</li>
 * <li><b>GitHub Integration:</b> Fetches the latest release data directly from the GitHub REST API.</li>
 * </ul>
 * </p>
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class UpdateChecker implements Listener {

    private final JavaPlugin plugin;

    // The repository to check for updates (Format: "User/Repo")
    private final String githubRepo = "PeachBiscuit174/PeachLib";

    // Containers for update information fetched from the API
    private String latestVersion;
    private String downloadUrl;     // The direct link to the jar file
    private String checksumUrl;     // The direct link to the checksum.txt
    private String remoteFileName;  // The actual filename of the release asset

    // Volatile boolean to ensure thread visibility when accessed from the main thread (PlayerJoinEvent)
    private volatile boolean isUpdateAvailable = false;

    // HTTP Client optimized for reusing connections (Java 11+)
    private final HttpClient httpClient;

    /**
     * Constructor for the UpdateChecker.
     * Configures the HttpClient with timeouts to prevent thread hanging.
     *
     * @param plugin The main plugin instance.
     */
    public UpdateChecker(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;

        // We configure a connect timeout. If GitHub is down or slow,
        // we don't want the thread to hang indefinitely.
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    /**
     * Initializes the update checker lifecycle.
     * <p>
     * 1. Registers the event listener for join notifications.<br>
     * 2. Schedules the asynchronous update check task via the LibraryScheduler.
     * </p>
     */
    public void bootstrap() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Check if the custom scheduler is available to prevent NullPointerExceptions
        if (PeachLib.getScheduler() != null) {
            // Schedule the task: Starts immediately (0), repeats every 12 hours
            PeachLib.getScheduler().runAsyncRepeating(this::checkUpdates, 0L, 12L, TimeUnit.HOURS);
        } else {
            plugin.getLogger().severe("Could not start UpdateChecker: LibraryScheduler is null! Ensure correct initialization order in Main class.");
        }
    }

    /**
     * The core logic for checking updates.
     * <p>
     * This method runs asynchronously. It queries the GitHub API, compares versions,
     * and initiates the download process if a valid update is found.
     * </p>
     */
    private void checkUpdates() {
        if (!ConfigData.getAutoUpdateStatus()) return;
        try {
            // Build the HTTP request for the GitHub API
            // We use a specific User-Agent to comply with GitHub's API policy
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + githubRepo + "/releases/latest"))
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "PeachLib-UpdateChecker")
                    .timeout(java.time.Duration.ofSeconds(10)) // Fail fast if API is unreachable
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // If the API call fails (e.g., 404 Not Found, 403 Rate Limit), log it and exit
            if (response.statusCode() != 200) {
                plugin.getLogger().warning("Update check failed. HTTP Status Code: " + response.statusCode());
                return;
            }

            // Parse the JSON response
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            // Extract version from "tag_name" (e.g., "v1.0.5" -> "1.0.5")
            this.latestVersion = json.get("tag_name").getAsString().replace("v", "").trim();

            // Compare local version with remote version
            if (isNewerVersionAvailable()) {
                // Get the list of files (assets) attached to the release
                JsonArray assets = json.getAsJsonArray("assets");
                parseAssets(assets);

                // Check strict API compatibility (e.g., prevent installing a 1.21 plugin on a 1.20 server)
                String currentApiVersion = plugin.getPluginMeta().getAPIVersion();

                if (isApiVersionCompatible(currentApiVersion)) {
                    // Version is new AND compatible -> Start download logic
                    handleDownloadProcess();
                } else {
                    plugin.getLogger().info("Update check skipped: A new version is available (" + latestVersion + "), but it is not compatible with your server version.");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Update check failed due to an exception.", e);
        }
    }

    /**
     * Iterates through the release assets to find the JAR file and the Checksum file.
     *
     * @param assets The 'assets' JSON array from the GitHub response.
     */
    private void parseAssets(JsonArray assets) {
        this.downloadUrl = null;
        this.checksumUrl = null;

        for (int i = 0; i < assets.size(); i++) {
            JsonObject asset = assets.get(i).getAsJsonObject();
            String name = asset.get("name").getAsString();
            String url = asset.get("browser_download_url").getAsString(); // Exact download link

            // Identify the plugin JAR
            if (name.endsWith(".jar") && name.contains("PeachLib")) {
                this.downloadUrl = url;
                this.remoteFileName = name;
            }
            // Identify the checksum file (generated by the workflow)
            else if (name.endsWith("checksum.txt") || name.endsWith("sha256")) {
                this.checksumUrl = url;
            }
        }
    }

    /**
     * Compares the plugin.yml version with the GitHub release version.
     *
     * @return true if the remote version is different (assumed newer), false otherwise.
     */
    private boolean isNewerVersionAvailable() {
        String current = plugin.getPluginMeta().getVersion().trim();
        return !current.equalsIgnoreCase(latestVersion);
    }

    /**
     * Checks if the plugin's API version is compatible with the running server.
     *
     * @param apiVersion The API version string from the local plugin.yml.
     * @return true if compatible or if no API version is defined (legacy).
     */
    private boolean isApiVersionCompatible(String apiVersion) {
        if (apiVersion == null) return true; // Legacy support

        String serverVersion = Bukkit.getMinecraftVersion(); // e.g., "1.21.4"
        // Strict check: server version must start with the API version.
        // Example: Server "1.21.4" starts with API "1.21" -> Compatible.
        return serverVersion.startsWith(apiVersion);
    }

    /**
     * Manages the secure download process.
     * <p>
     * <b>Strategy:</b>
     * <ol>
     * <li>Downloads to a {@code .tmp} file first.</li>
     * <li>Verifies SHA-256 hash of the temp file.</li>
     * <li>Verifies the internal {@code plugin.yml} of the temp file.</li>
     * <li>Atomically moves (renames) the temp file to the final {@code .jar}.</li>
     * </ol>
     * </p>
     */
    private void handleDownloadProcess() {
        if (downloadUrl == null || remoteFileName == null) {
            plugin.getLogger().warning("New version detected, but no valid JAR file was found in the GitHub assets.");
            return;
        }

        // Bukkit defines a standard 'update' folder for automatic plugin updates on restart
        File updateFolder = Bukkit.getUpdateFolderFile();
        if (!updateFolder.exists()) {
            if (!updateFolder.mkdirs()) {
                plugin.getLogger().severe("Could not create update folder. Update aborted.");
                return;
            }
        }

        // The final destination file (e.g., plugins/update/PeachLib-1.0.1.jar)
        File finalTargetFile = new File(updateFolder, remoteFileName);

        // The temporary download file (e.g., plugins/update/PeachLib-1.0.1.jar.tmp)
        File tempFile = new File(updateFolder, remoteFileName + ".tmp");

        // Cleanup old versions of this library to prevent clutter
        cleanupOldVersions(updateFolder);

        // Fetch the remote SHA-256 hash for verification
        String remoteHash = fetchRemoteHash();
        if (remoteHash.isEmpty()) {
            plugin.getLogger().warning("Update verification failed: Checksum not found or empty on remote. Aborting for security.");
            return;
        }

        // Scenario: The server might have already downloaded the update but hasn't restarted yet.
        // If the file exists and is valid, we don't need to download it again.
        if (finalTargetFile.exists()) {
            if (isValidFile(finalTargetFile, remoteHash)) {
                this.isUpdateAvailable = true;
                return; // File is already ready
            }
            // If validation fails, the file is likely corrupt. Delete it.
            try { Files.delete(finalTargetFile.toPath()); } catch (IOException ignored) {}
        }

        // Perform the download to the TEMP file
        if (downloadAndVerify(tempFile, remoteHash)) {
            // Check API compatibility of the downloaded TEMP JAR
            if (verifyRemoteJarApi(tempFile)) {
                try {
                    // ATOMIC MOVE: Rename .tmp to .jar
                    // This is the most critical step. It ensures that the server never sees
                    // a partial or corrupt file in the update folder.
                    Files.move(tempFile.toPath(), finalTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

                    this.isUpdateAvailable = true;
                    plugin.getLogger().info("Successfully downloaded and verified update: " + remoteFileName);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to move update file into place: " + e.getMessage());
                    // Cleanup temp file on failure
                    try { Files.deleteIfExists(tempFile.toPath()); } catch (IOException ignored) {}
                }
            } else {
                plugin.getLogger().severe("Update rejected: Downloaded JAR API version is not compatible with this server.");
                try { Files.deleteIfExists(tempFile.toPath()); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Removes old versions of PeachLib from the update folder.
     *
     * @param updateFolder The directory where updates are stored.
     */
    private void cleanupOldVersions(File updateFolder) {
        File[] files = updateFolder.listFiles((dir, name) -> name.endsWith(".jar") && name.startsWith("PeachLib"));
        if (files != null) {
            for (File file : files) {
                // If it's a PeachLib jar but NOT the one we are currently handling, delete it.
                if (!file.getName().equals(remoteFileName)) {
                    try {
                        Files.deleteIfExists(file.toPath());
                    } catch (IOException ignored) {}
                }
            }
        }
    }

    /**
     * Checks if a local file is valid by verifying its hash and internal API version.
     *
     * @param file The file to check.
     * @param hash The expected SHA-256 hash.
     * @return true if valid.
     */
    private boolean isValidFile(File file, String hash) {
        return calculateSHA256(file).equalsIgnoreCase(hash) && verifyRemoteJarApi(file);
    }

    /**
     * Inspects the downloaded JAR file (without loading it) to read its plugin.yml.
     *
     * @param jarFile The downloaded JAR file.
     * @return true if compatible, false otherwise.
     */
    private boolean verifyRemoteJarApi(File jarFile) {
        try (ZipFile zip = new ZipFile(jarFile)) {
            ZipEntry entry = zip.getEntry("plugin.yml");
            if (entry == null) return false;

            try (InputStream is = zip.getInputStream(entry)) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(is));
                String apiVersion = yaml.getString("api-version");
                return isApiVersionCompatible(apiVersion);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read API version from downloaded JAR: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetches the SHA-256 hash from the remote checksum URL.
     *
     * @return The hash string, or an empty string on failure.
     */
    private String fetchRemoteHash() {
        if (checksumUrl == null) return "";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(checksumUrl))
                    .header("User-Agent", "PeachLib-UpdateChecker")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Logic: "hashvalue  filename" -> splits by whitespace -> takes first element "hashvalue"
                return response.body().trim().split("\\s+")[0];
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch checksum: " + e.getMessage());
        }
        return "";
    }

    /**
     * Downloads the file from the URL and verifies its integrity immediately.
     *
     * @param targetFile   The destination file on disk.
     * @param expectedHash The SHA-256 hash expected from the server.
     * @return true if download was successful AND hash matches.
     */
    private boolean downloadAndVerify(File targetFile, String expectedHash) {
        try {
            plugin.getLogger().info("Downloading update: " + remoteFileName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("User-Agent", "PeachLib-UpdateChecker")
                    .timeout(java.time.Duration.ofMinutes(2)) // Generous timeout for large files
                    .GET()
                    .build();

            // Stream response directly to file (reduces RAM usage for large files)
            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(targetFile.toPath()));

            if (response.statusCode() != 200) {
                plugin.getLogger().warning("Download failed with HTTP " + response.statusCode());
                Files.deleteIfExists(targetFile.toPath());
                return false;
            }

            // Verify integrity
            String localHash = calculateSHA256(targetFile);
            if (localHash.equalsIgnoreCase(expectedHash)) {
                return true;
            } else {
                plugin.getLogger().severe("Checksum mismatch! The downloaded file is corrupt or compromised.");
                Files.deleteIfExists(targetFile.toPath());
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Download error: " + e.getMessage());
            try { Files.deleteIfExists(targetFile.toPath()); } catch (IOException ignored) {}
            return false;
        }
    }

    /**
     * Calculates the SHA-256 hash of a local file.
     *
     * @param file The file to hash.
     * @return The hex string of the hash.
     */
    private String calculateSHA256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file.toPath())) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * Event Listener: Notifies OP players when they join if an update is pending.
     *
     * @param event The PlayerJoinEvent.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() && isUpdateAvailable) {
            Component message = MiniMessage.miniMessage().deserialize(
                    "<newline><gold>PeachLib</gold> <gray>»</gray> <green>A verified update is ready!</green><newline>" +
                            "<gray>Restart the server to apply the changes.</gray><newline>"
            );
            player.sendMessage(message);
        }
    }
}