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
 * <li>Accurately resolves SNAPSHOT vs Release comparisons.</li>
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
            boolean isCurrentSnapshot = currentVersion.toUpperCase().contains("-SNAPSHOT");
            boolean allowSnapshots = isCurrentSnapshot || ConfigData.isAllowSnapshotUpdates();

            String bestCandidateVersion = currentVersion;
            JsonArray bestCandidateAssets = null;
            boolean foundBetterVersion = false;

            // Iterate through releases (GitHub returns them newest first)
            for (JsonElement element : releases) {
                JsonObject release = element.getAsJsonObject();
                String releaseVersion = release.get("tag_name").getAsString().replace("v", "").trim();
                boolean isRemoteSnapshot = releaseVersion.toUpperCase().contains("-SNAPSHOT");

                // GATEKEEPER: Ignore snapshots if they aren't allowed
                if (!allowSnapshots && isRemoteSnapshot) {
                    continue;
                }

                // 1. Version Check: Is this release strictly newer than our best candidate?
                if (!isNewerVersion(bestCandidateVersion, releaseVersion)) {
                    continue; // Skip if it's not better than what we already found/have
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
                    bestCandidateVersion = releaseVersion;
                    bestCandidateAssets = assets;
                    foundBetterVersion = true;
                }
            }

            // If we found a better version after checking all candidates, proceed
            if (foundBetterVersion && bestCandidateAssets != null) {
                this.latestCompatibleVersion = bestCandidateVersion;
                parseAssetsForDownload(bestCandidateAssets);
                handleDownloadProcess();
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Update check failed due to an exception.", e);
        }
    }

    /**
     * Checks if candidateVersion > currentVersion (Semantic Versioning with SNAPSHOT support).
     */
    private boolean isNewerVersion(String currentVersion, String candidateVersion) {
        String currentClean = currentVersion.split("-")[0];
        String candidateClean = candidateVersion.split("-")[0];

        int[] currentParts = parseVersionParts(currentClean);
        int[] candidateParts = parseVersionParts(candidateClean);

        int length = Math.max(currentParts.length, candidateParts.length);

        for (int i = 0; i < length; i++) {
            int current = (i < currentParts.length) ? currentParts[i] : 0;
            int candidate = (i < candidateParts.length) ? candidateParts[i] : 0;

            if (candidate > current) return true; // Candidate is newer
            if (candidate < current) return false; // Candidate is older
        }

        // The base numbers are exactly equal. Check for SNAPSHOT / Prerelease
        boolean currentIsSnapshot = currentVersion.toUpperCase().contains("-SNAPSHOT");
        boolean candidateIsSnapshot = candidateVersion.toUpperCase().contains("-SNAPSHOT");

        if (currentIsSnapshot && !candidateIsSnapshot) {
            return true; // Candidate is a full release, Current is a snapshot -> Candidate is BETTER
        } else if (!currentIsSnapshot && candidateIsSnapshot) {
            return false; // Candidate is snapshot, Current is full release -> Candidate is WORSE
        } else if (currentIsSnapshot && candidateIsSnapshot) {
            int currentSnap = getSnapshotNumber(currentVersion);
            int candidateSnap = getSnapshotNumber(candidateVersion);
            return candidateSnap > currentSnap;
        }

        return false; // Both are exactly the same
    }

    private int getSnapshotNumber(String version) {
        String upper = version.toUpperCase();
        int idx = upper.indexOf("-SNAPSHOT");
        if (idx == -1) return 0;

        String remainder = upper.substring(idx + 9).replaceAll("[^0-9]", "");
        if (remainder.isEmpty()) return 0;

        try {
            return Integer.parseInt(remainder);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int[] parseVersionParts(String cleanVersion) {
        if (cleanVersion == null || cleanVersion.isEmpty()) return new int[0];
        String[] parts = cleanVersion.split("\\.");
        int[] numbers = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {}
        }
        return numbers;
    }

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
     * Ensures Server Version >= Plugin API Version.
     */
    private boolean isApiVersionCompatible(String requiredApi) {
        if (requiredApi == null || requiredApi.trim().isEmpty()) return true;

        String serverVersion = Bukkit.getMinecraftVersion(); // e.g., "1.21.1"
        requiredApi = requiredApi.replace("v", "").trim();

        int[] serverParts = parseVersionParts(serverVersion);
        int[] apiParts = parseVersionParts(requiredApi);

        int length = Math.max(serverParts.length, apiParts.length);

        for (int i = 0; i < length; i++) {
            int sVer = (i < serverParts.length) ? serverParts[i] : 0;
            int aVer = (i < apiParts.length) ? apiParts[i] : 0;

            if (sVer > aVer) return true; // Server is explicitly NEWER than API -> Compatible!
            if (sVer < aVer) return false; // Server is OLDER than API -> Incompatible!
        }

        return true; // Exact match
    }

    private void handleDownloadProcess() {
        if (downloadUrl == null || remoteFileName == null) return;

        File updateFolder = Bukkit.getUpdateFolderFile();
        if (!updateFolder.exists() && !updateFolder.mkdirs()) return;

        File finalTargetFile = new File(updateFolder, remoteFileName);
        File tempFile = new File(updateFolder, remoteFileName + ".tmp");

        cleanupOldVersions(updateFolder);

        String remoteHash = fetchRemoteHash();
        if (remoteHash.isEmpty()) {
            plugin.getLogger().warning("Skipping update: No checksum found.");
            return;
        }

        if (finalTargetFile.exists()) {
            if (isValidFile(finalTargetFile, remoteHash)) {
                this.isUpdateAvailable = true;
                return;
            }
            try { Files.delete(finalTargetFile.toPath()); } catch (IOException ignored) {}
        }

        if (downloadAndVerify(tempFile, remoteHash)) {
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