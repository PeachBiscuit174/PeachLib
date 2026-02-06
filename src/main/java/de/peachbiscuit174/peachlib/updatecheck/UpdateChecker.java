package de.peachbiscuit174.peachlib.updatecheck;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
 * Robust and secure UpdateChecker for PeachLib with Metadata Support.
 * <p>
 * <b>Logic:</b>
 * <ul>
 * <li>Checks the last 10 releases from GitHub.</li>
 * <li>Compares SemVer versions correctly (e.g. 1.10 > 1.9).</li>
 * <li>Downloads 'metadata.json' first to verify compatibility (API version).</li>
 * <li>Prevents incompatible updates (e.g., 1.21 plugin on 1.20 server).</li>
 * <li>Performs atomic download & verification of the JAR.</li>
 * </ul>
 * </p>
 *
 * @author peachbiscuit174
 * @since 1.1.0
 */
public class UpdateChecker implements Listener {

    private final JavaPlugin plugin;
    private final String githubRepo = "PeachBiscuit174/PeachLib";

    // Information about the pending update (if found)
    private String latestCompatibleVersion;
    private String downloadUrl;
    private String checksumUrl;
    private String remoteFileName;
    private volatile boolean isUpdateAvailable = false;

    private final HttpClient httpClient;

    public UpdateChecker(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    public void bootstrap() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        if (PeachLib.getScheduler() != null) {
            PeachLib.getScheduler().runAsyncRepeating(this::checkUpdates, 0L, 12L, TimeUnit.HOURS);
        } else {
            plugin.getLogger().severe("Could not start UpdateChecker: LibraryScheduler is null!");
        }
    }

    /**
     * Core Logic: Iterates through releases to find the newest COMPATIBLE version.
     */
    private void checkUpdates() {
        if (!ConfigData.getAutoUpdateStatus()) return;

        try {
            // Request the list of releases (not just latest)
            // per_page=10 means we check the last 10 releases
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + githubRepo + "/releases?per_page=10"))
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "PeachLib-UpdateChecker")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                plugin.getLogger().warning("Update check failed. HTTP Status Code: " + response.statusCode());
                return;
            }

            JsonArray releases = JsonParser.parseString(response.body()).getAsJsonArray();
            String currentVersion = plugin.getPluginMeta().getVersion().trim();

            // Iterate through releases (GitHub usually returns them newest first, but we check logic anyway)
            for (JsonElement element : releases) {
                JsonObject release = element.getAsJsonObject();

                // Remove "v" prefix from tag (e.g. "v1.2.0" -> "1.2.0")
                String releaseVersion = release.get("tag_name").getAsString().replace("v", "").trim();

                // 1. Version Comparison Logic (SemVer)
                // If the remote release is NOT newer than our current version, we skip it.
                // This handles cases like: Current=1.2.0, Remote=1.1.0 -> Skip.
                if (!isRemoteNewer(currentVersion, releaseVersion)) {
                    continue;
                }

                // 2. Parse Assets to find metadata.json
                JsonArray assets = release.getAsJsonArray("assets");
                String metadataUrl = null;

                for (JsonElement assetElem : assets) {
                    JsonObject asset = assetElem.getAsJsonObject();
                    if (asset.get("name").getAsString().equals("metadata.json")) {
                        metadataUrl = asset.get("browser_download_url").getAsString();
                        break;
                    }
                }

                // 3. Verify Metadata (Compatibility Check)
                boolean isCompatible = false;
                if (metadataUrl != null) {
                    // Modern Way: Check metadata.json
                    isCompatible = checkMetadataCompatibility(metadataUrl);
                } else {
                    // Fallback: If no metadata exists (legacy release), we assume strict mode -> Skip
                    continue;
                }

                if (isCompatible) {
                    // WE FOUND A WINNER!
                    // This is a newer version AND it is compatible with our server.
                    this.latestCompatibleVersion = releaseVersion;
                    parseAssetsForDownload(assets); // Extract JAR & Checksum URLs

                    // Start download logic immediately and stop searching
                    handleDownloadProcess();
                    return;
                }

                // If not compatible, loop continues to the next release.
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Update check failed due to an exception.", e);
        }
    }

    /**
     * Checks if the remote version is strictly greater than the current version.
     * Handles Semantic Versioning (e.g., 1.10.0 > 1.9.0).
     * * @param current The locally installed version.
     * @param remote The version found on GitHub.
     * @return true if remote > current.
     */
    private boolean isRemoteNewer(String current, String remote) {
        // Strip non-numeric characters except dots (removes -SNAPSHOT, v, etc.)
        // Example: "1.0.0-SNAPSHOT15" -> "1.0.015" (Wait, strictly regex replaces all non digits/dots)
        // Better strategy: Split by non-digit separators first.

        String[] cParts = current.split("[^0-9]+");
        String[] rParts = remote.split("[^0-9]+");

        int length = Math.max(cParts.length, rParts.length);

        for (int i = 0; i < length; i++) {
            int c = i < cParts.length && !cParts[i].isEmpty() ? Integer.parseInt(cParts[i]) : 0;
            int r = i < rParts.length && !rParts[i].isEmpty() ? Integer.parseInt(rParts[i]) : 0;

            if (r > c) return true;  // Remote is newer in this segment
            if (r < c) return false; // Remote is older in this segment
        }

        // Versions are numerically equal (e.g. 1.0 vs 1.0.0)
        return false;
    }

    /**
     * Downloads and parses metadata.json in memory to check API version.
     */
    private boolean checkMetadataCompatibility(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

                // Get API Version from Metadata
                if (json.has("api_version")) {
                    String requiredApi = json.get("api_version").getAsString();
                    return isApiVersionCompatible(requiredApi);
                }
            }
        } catch (Exception ignored) {
            // On error, assume incompatible
        }
        return false;
    }

    private void parseAssetsForDownload(JsonArray assets) {
        this.downloadUrl = null;
        this.checksumUrl = null;

        for (JsonElement assetElem : assets) {
            JsonObject asset = assetElem.getAsJsonObject();
            String name = asset.get("name").getAsString();
            String url = asset.get("browser_download_url").getAsString();

            if (name.endsWith(".jar") && name.contains("PeachLib")) {
                this.downloadUrl = url;
                this.remoteFileName = name;
            } else if (name.endsWith("checksum.txt")) {
                this.checksumUrl = url;
            }
        }
    }

    private boolean isApiVersionCompatible(String apiVersion) {
        if (apiVersion == null || apiVersion.isEmpty()) return true;
        String serverVersion = Bukkit.getMinecraftVersion(); // e.g., "1.21.4"
        // Check if server version starts with the API version (e.g. 1.21.4 starts with 1.21)
        return serverVersion.startsWith(apiVersion);
    }

    private void handleDownloadProcess() {
        if (downloadUrl == null || remoteFileName == null) return;

        File updateFolder = Bukkit.getUpdateFolderFile();
        if (!updateFolder.exists() && !updateFolder.mkdirs()) return;

        File finalTargetFile = new File(updateFolder, remoteFileName);
        File tempFile = new File(updateFolder, remoteFileName + ".tmp");

        cleanupOldVersions(updateFolder);

        // Fetch Remote Checksum
        String remoteHash = fetchRemoteHash();
        if (remoteHash.isEmpty()) {
            plugin.getLogger().warning("Skipping update: No checksum found.");
            return;
        }

        // Check if file already exists
        if (finalTargetFile.exists()) {
            if (isValidFile(finalTargetFile, remoteHash)) {
                this.isUpdateAvailable = true;
                return;
            }
            try { Files.delete(finalTargetFile.toPath()); } catch (IOException ignored) {}
        }

        // Download & Verify
        if (downloadAndVerify(tempFile, remoteHash)) {
            // Double Check: Verify internal plugin.yml of the downloaded JAR just to be safe
            if (verifyRemoteJarApi(tempFile)) {
                try {
                    Files.move(tempFile.toPath(), finalTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    this.isUpdateAvailable = true;
                    plugin.getLogger().info("Update ready: " + latestCompatibleVersion + " (Compatible with " + Bukkit.getMinecraftVersion() + ")");
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to install update: " + e.getMessage());
                }
            } else {
                plugin.getLogger().warning("Downloaded JAR internal API mismatch. Aborting.");
                try { Files.deleteIfExists(tempFile.toPath()); } catch (IOException ignored) {}
            }
        }
    }

    private void cleanupOldVersions(File updateFolder) {
        File[] files = updateFolder.listFiles((dir, name) -> name.endsWith(".jar") && name.startsWith("PeachLib"));
        if (files != null) {
            for (File file : files) {
                if (!file.getName().equals(remoteFileName)) {
                    try { Files.deleteIfExists(file.toPath()); } catch (IOException ignored) {}
                }
            }
        }
    }

    private boolean isValidFile(File file, String hash) {
        return calculateSHA256(file).equalsIgnoreCase(hash) && verifyRemoteJarApi(file);
    }

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
            return false;
        }
    }

    private String fetchRemoteHash() {
        if (checksumUrl == null) return "";
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(checksumUrl)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body().trim().split("\\s+")[0];
            }
        } catch (Exception ignored) {}
        return "";
    }

    private boolean downloadAndVerify(File targetFile, String expectedHash) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).GET().build();
            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(targetFile.toPath()));

            if (response.statusCode() != 200) {
                Files.deleteIfExists(targetFile.toPath());
                return false;
            }

            String localHash = calculateSHA256(targetFile);
            if (localHash.equalsIgnoreCase(expectedHash)) {
                return true;
            } else {
                plugin.getLogger().severe("Checksum mismatch on update download.");
                Files.deleteIfExists(targetFile.toPath());
                return false;
            }
        } catch (Exception e) {
            try { Files.deleteIfExists(targetFile.toPath()); } catch (IOException ignored) {}
            return false;
        }
    }

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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() && isUpdateAvailable) {
            Component message = MiniMessage.miniMessage().deserialize(
                    "<newline><gold>PeachLib</gold> <gray>»</gray> <green>A verified update is ready!</green><newline>" +
                            "<gray>Version: <yellow>" + latestCompatibleVersion + "</yellow></gray><newline>" +
                            "<gray>Restart the server to apply the changes.</gray><newline>"
            );
            player.sendMessage(message);
        }
    }
}