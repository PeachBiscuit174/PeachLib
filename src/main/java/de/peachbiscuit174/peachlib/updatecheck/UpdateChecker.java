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
 * <li>Selects the HIGHEST version that is COMPATIBLE with the server.</li>
 * <li>Uses semantic version comparison for Plugin Version AND Minecraft API Version.</li>
 * <li>Handles complex scenarios like Server 1.21.11 vs API 1.21.4 correctly.</li>
 * </ul>
 * </p>
 *
 * @author peachbiscuit174
 * @since 1.2.0
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
     * Core Logic: Iterates through releases to find the BEST COMPATIBLE version.
     */
    private void checkUpdates() {
        if (!ConfigData.getAutoUpdateStatus()) return;

        try {
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

            // Variables to track the "Best Candidate" found so far
            String bestVersionFound = currentVersion;
            JsonArray bestAssets = null;
            boolean foundBetterVersion = false;

            // Iterate through releases
            for (JsonElement element : releases) {
                JsonObject release = element.getAsJsonObject();

                // Normalize version tag
                String releaseVersion = release.get("tag_name").getAsString().replace("v", "").trim();

                // 1. Version Check: Is this release strictly newer than our best candidate?
                // If we already found v2.0, we ignore v1.5 even if it's compatible.
                if (!isVersionGreater(bestVersionFound, releaseVersion)) {
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
                    isCompatible = checkMetadataCompatibility(metadataUrl);
                }

                // 4. Update Candidate if compatible
                if (isCompatible) {
                    bestVersionFound = releaseVersion;
                    bestAssets = assets;
                    foundBetterVersion = true;
                }
            }

            // If we found a better version after checking all candidates, proceed
            if (foundBetterVersion && bestAssets != null) {
                this.latestCompatibleVersion = bestVersionFound;
                parseAssetsForDownload(bestAssets);
                handleDownloadProcess();
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Update check failed due to an exception.", e);
        }
    }

    /**
     * Checks if remote version > base version (Semantic Versioning).
     */
    private boolean isVersionGreater(String base, String remote) {
        int[] baseParts = parseVersionParts(base);
        int[] remoteParts = parseVersionParts(remote);

        int length = Math.max(baseParts.length, remoteParts.length);

        for (int i = 0; i < length; i++) {
            int b = (i < baseParts.length) ? baseParts[i] : 0;
            int r = (i < remoteParts.length) ? remoteParts[i] : 0;

            if (r > b) return true;
            if (r < b) return false;
        }
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

                if (json.has("api_version")) {
                    String requiredApi = json.get("api_version").getAsString();
                    return isApiVersionCompatible(requiredApi);
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Parses assets to find the JAR and Checksum file.
     */
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

    /**
     * Strict Semantic Version Check for Minecraft API.
     * Handles: Server 1.21.11 vs API 1.21.4 correctly.
     */
    private boolean isApiVersionCompatible(String requiredApi) {
        if (requiredApi == null || requiredApi.isEmpty()) return true;

        String serverVersion = Bukkit.getMinecraftVersion(); // e.g., "1.21.11"

        int[] serverParts = parseVersionParts(serverVersion);
        int[] apiParts = parseVersionParts(requiredApi);

        int length = Math.max(serverParts.length, apiParts.length);

        for (int i = 0; i < length; i++) {
            int sVer = (i < serverParts.length) ? serverParts[i] : 0;
            int aVer = (i < apiParts.length) ? apiParts[i] : 0;

            if (sVer > aVer) return true; // Server is newer in this segment
            if (sVer < aVer) return false; // Server is older -> Incompatible
        }

        return true; // Versions are identical
    }

    /**
     * Helper: Splits "1.21.4" into [1, 21, 4].
     */
    private int[] parseVersionParts(String version) {
        if (version == null) return new int[0];
        String[] parts = version.split("[^0-9]+");
        int[] numbers = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                try {
                    numbers[i] = Integer.parseInt(parts[i]);
                } catch (NumberFormatException ignored) {}
            }
        }
        return numbers;
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
            // Verify internal plugin.yml
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