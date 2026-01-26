package de.peachbiscuit174.peachlib;

import de.peachbiscuit174.peachlib.gui.GUIListener;
import de.peachbiscuit174.peachlib.other.HolidayGreetingListener;
import de.peachbiscuit174.peachlib.other.ReloadSafetyListener;
import de.peachbiscuit174.peachlib.scheduler.LibraryScheduler;
import de.peachbiscuit174.peachlib.updatecheck.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class of the PeachLib.
 * Handles the initialization of all managers, metrics, and the update system.
 * @author peachbiscuit174
 * @since 1.0.0
 */
public final class PeachLib extends JavaPlugin {
    private static Plugin plugin;
    private static UpdateChecker updateChecker;
    private static LibraryScheduler scheduler;
    private Metrics metrics;

    public static LibraryScheduler getScheduler() {
        return scheduler;
    }

    public static Plugin getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;

        int pluginId = 29074;
        metrics = new Metrics(this, pluginId);

        scheduler = new LibraryScheduler(plugin);
        updateChecker = new UpdateChecker(this);
        updateChecker.bootstrap();
        new ReloadSafetyListener(this, scheduler);
        Bukkit.getServer().getPluginManager().registerEvents(new GUIListener(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new HolidayGreetingListener(), this);

        getLogger().info("----------------------------------");
        getLogger().info("PeachLib has been loaded successfully.");
        getLogger().info("Version: " + getPluginMeta().getVersion());
        getLogger().info("Developed by PeachBiscuit174");
        getLogger().info("Thank you for using PeachLib (PPL) :D");
        getLogger().info("----------------------------------");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (scheduler != null) {
            scheduler.shutdown();
        }

        scheduler = null;
        updateChecker = null;
        metrics = null;
        getLogger().info("PeachLib disabled.");
        plugin = null;
    }
}
