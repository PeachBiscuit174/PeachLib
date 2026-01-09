package de.peachbiscuit174.peachpaperlib.updatecheck;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * UpdateChecker for PeachPaperLib.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class UpdateChecker implements Listener {

    private final JavaPlugin plugin;
    private final String githubRepo = "PeachBiscuit174/PeachPaperLib";
    private String latestVersion;
    private String downloadUrl;
    private String checksumUrl;
    private String remoteFileName;
    private boolean isUpdateAvailable = false;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void bootstrap() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Check asynchronously every 12 hours
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkUpdates, 0L, 864000L);
    }

    private void checkUpdates() {
        try {
            URL url = new URL("https://api.github.com/repos/" + githubRepo + "/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                this.latestVersion = json.get("tag_name").getAsString().replace("v", "").trim();

                // PHASE 1: Only proceed if the version on GitHub is different from installed
                if (isNewerVersionAvailable()) {
                    JsonArray assets = json.getAsJsonArray("assets");
                    for (int i = 0; i < assets.size(); i++) {
                        JsonObject asset = assets.get(i).getAsJsonObject();
                        String name = asset.get("name").getAsString();

                        if (name.endsWith(".jar") && name.contains("PeachPaperLib")) {
                            this.downloadUrl = asset.get("browser_download_url").getAsString();
                            this.remoteFileName = name;
                        } else if (name.equals("checksum.txt")) {
                            this.checksumUrl = asset.get("browser_download_url").getAsString();
                        }
                    }

                    // Proceed only if current server matches current plugin API
                    if (isApiVersionMatchingExactly(plugin.getPluginMeta().getAPIVersion())) {
                        handleDownloadProcess();
                    } else {
                        plugin.getLogger().info("Update check skipped: Your current server version does not match your current plugin API.");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Update check failed: " + e.getMessage());
        }
    }

    private boolean isNewerVersionAvailable() {
        String current = plugin.getPluginMeta().getVersion().trim();
        return !current.equalsIgnoreCase(latestVersion);
    }

    private boolean isApiVersionMatchingExactly(String apiVersion) {
        String serverVersion = Bukkit.getMinecraftVersion();
        return apiVersion != null && apiVersion.equals(serverVersion);
    }

    private void handleDownloadProcess() {
        if (downloadUrl == null || remoteFileName == null) return;

        File updateFolder = new File(plugin.getDataFolder().getParentFile(), "update");
        if (!updateFolder.exists()) updateFolder.mkdirs();

        File targetFile = new File(updateFolder, remoteFileName);

        // PHASE 2: Delete outdated PeachPaperLib files in /update
        File[] files = updateFolder.listFiles((dir, name) -> name.endsWith(".jar") && name.startsWith("PeachPaperLib"));
        if (files != null) {
            for (File file : files) {
                if (!file.getName().equals(remoteFileName)) file.delete();
            }
        }

        String remoteHash = fetchRemoteHash();
        if (remoteHash.isEmpty()) {
            plugin.getLogger().warning("Update verification failed: checksum.txt not found on GitHub.");
            return;
        }

        // PHASE 3: Check if file already exists and is valid
        if (targetFile.exists()) {
            if (calculateSHA256(targetFile).equalsIgnoreCase(remoteHash) && verifyRemoteJarApi(targetFile)) {
                this.isUpdateAvailable = true;
                return;
            }
            targetFile.delete();
        }

        // PHASE 4: Download and double-verify (Hash & API)
        if (downloadAndVerify(targetFile, remoteHash)) {
            if (verifyRemoteJarApi(targetFile)) {
                this.isUpdateAvailable = true;
                plugin.getLogger().info("Successfully downloaded and verified: " + remoteFileName);
            } else {
                plugin.getLogger().severe("Update rejected: Downloaded JAR API version does not match your server.");
                targetFile.delete();
            }
        }
    }

    private boolean verifyRemoteJarApi(File jarFile) {
        try (ZipFile zip = new ZipFile(jarFile)) {
            ZipEntry entry = zip.getEntry("plugin.yml");
            if (entry == null) return false;
            try (InputStream is = zip.getInputStream(entry)) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(is));
                return isApiVersionMatchingExactly(yaml.getString("api-version"));
            }
        } catch (IOException e) {
            return false;
        }
    }

    private String fetchRemoteHash() {
        if (checksumUrl == null) return "";
        try (InputStream in = new URL(checksumUrl).openStream();
             Scanner scanner = new Scanner(in)) {
            if (scanner.hasNext()) return scanner.next().trim();
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    private boolean downloadAndVerify(File targetFile, String expectedHash) {
        try {
            plugin.getLogger().info("Downloading update: " + remoteFileName);
            URL url = new URL(downloadUrl);
            try (InputStream in = url.openStream();
                 ReadableByteChannel rbc = Channels.newChannel(in);
                 FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }

            String localHash = calculateSHA256(targetFile);
            if (localHash.equalsIgnoreCase(expectedHash)) {
                return true;
            } else {
                plugin.getLogger().severe("Checksum mismatch! The file is corrupt. Deleting...");
                if (targetFile.exists()) targetFile.delete();
                return false;
            }
        } catch (Exception e) {
            if (targetFile.exists()) targetFile.delete();
            return false;
        }
    }

    private String calculateSHA256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = new FileInputStream(file)) {
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
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<newline><gold>PeachPaperLib</gold> <gray>»</gray> <green>A verified update is ready!</green><newline>" +
                            "<gray>Restart the server to apply the changes.</gray><newline>"
            ));
        }
    }
}