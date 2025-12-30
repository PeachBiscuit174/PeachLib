package de.peachbiscuit174.peachpaperlib.updatecheck;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker implements Listener {

    private final JavaPlugin plugin;
    private final String githubRepo = "PeachBiscuit174/PeachPaperLib";
    private String latestVersion;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialisiert den gesamten Update-Prozess.
     */
    public void bootstrap() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // 12h = 864.000 Ticks
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkUpdates, 0L, 864000L);
    }

    private void checkUpdates() {
        try {
            URL url = new URL("https://api.github.com/repos/" + githubRepo + "/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                this.latestVersion = json.get("tag_name").getAsString().replace("v", "");

                if (isUpdateAvailable()) {
                    plugin.getLogger().warning("A new version of PeachPaperLib is available: v" + latestVersion);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Update check failed: " + e.getMessage());
        }
    }

    private boolean isUpdateAvailable() {
        if (latestVersion == null) return false;
        String current = plugin.getPluginMeta().getVersion();
        return !current.equalsIgnoreCase(latestVersion);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.isOp()) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && isUpdateAvailable()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<newline><gold>🍑 PeachPaperLib</gold> <gray>»</gray> <yellow>New update available!</yellow><newline>" +
                                "<gray>Current: <red>v" + plugin.getPluginMeta().getVersion() + "</red> | Latest: <green>v" + latestVersion + "</green></gray><newline>" +
                                "<click:open_url:'https://github.com/" + githubRepo + "/releases'><underlined><aqua>Click here to download</aqua></underlined></click><newline>"
                ));
            }
        }, 40L);
    }
}