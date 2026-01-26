package de.peachbiscuit174.peachlib.other;

import de.peachbiscuit174.peachlib.PeachLib;
import de.peachbiscuit174.peachlib.scheduler.LibraryScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Prevents the unsafe usage of /reload and forces a clean server restart instead.
 * Kicks all players with a warning message to ensure data integrity.
 */
public class ReloadSafetyListener implements Listener {

    private final LibraryScheduler scheduler;

    public ReloadSafetyListener(Plugin plugin, LibraryScheduler scheduler) {
        this.scheduler = scheduler;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // --- PLAYER COMMANDS ---
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();

        // Check for /reload or /rl
        if (message.equals("/reload") || message.equals("/rl") ||
                message.startsWith("/reload ") || message.startsWith("/rl ")) {

            // SECURITY CHECK: Only allow if player actually has permission to reload
            if (!event.getPlayer().hasPermission("bukkit.command.reload")) {
                return; // Let the server handle the "No Permission" message normally
            }

            event.setCancelled(true); // Stop the actual reload
            performSafeRestart(event.getPlayer().getName());
        }
    }

    // --- CONSOLE COMMANDS ---
    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsoleCommand(ServerCommandEvent event) {
        String message = event.getCommand().toLowerCase();

        if (message.equals("reload") || message.equals("rl") ||
                message.startsWith("reload ") || message.startsWith("rl ")) {

            event.setCancelled(true);
            performSafeRestart("Console");
        }
    }

    /**
     * Kicks all players and shuts down the server.
     *
     * @param initiator The name of the person who triggered the restart.
     */
    private void performSafeRestart(String initiator) {
        PeachLib.getPlugin().getLogger().warning("!!! UNSAFE RELOAD ATTEMPTED BY " + initiator + " !!!");
        PeachLib.getPlugin().getLogger().warning("Initiating safe shutdown to prevent plugin corruption...");

        // Message to display on the kick screen
        Component kickReason = Component.text()
                .append(Component.text("⚠ SERVER RESTART ⚠", NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("The server is restarting to apply changes safely.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("Please do not use /reload, as it breaks plugins.", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("You can rejoin in a few moments!", NamedTextColor.GREEN))
                .build();

        // Kick all players immediately
        Bukkit.getOnlinePlayers().forEach(player -> player.kick(kickReason));

        // Use our custom Scheduler to delay shutdown slightly (1 second)
        // This ensures the kick packets have time to reach the clients before the server dies.
        scheduler.runSyncDelayed(() -> {
            Bukkit.shutdown();
        }, 1, TimeUnit.SECONDS);
    }
}