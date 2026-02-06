package de.peachbiscuit174.peachlib;

import de.peachbiscuit174.peachlib.Commands.PeachLibSettings;
import de.peachbiscuit174.peachlib.configstuff.ConfigData;
import de.peachbiscuit174.peachlib.configstuff.CustomConfig2;
import de.peachbiscuit174.peachlib.configstuff.SetupConfig;
import de.peachbiscuit174.peachlib.gui.GUIListener;
import de.peachbiscuit174.peachlib.other.*;
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
    private static CustomConfig2 cfg;

    public static CustomConfig2 getCfg() {
        return cfg;
    }

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

        cfg = new CustomConfig2("settings.yml");
        SetupConfig.setup();
        ConfigData.reloadData();

        int pluginId = 29074;
        metrics = new Metrics(this, pluginId);

        scheduler = new LibraryScheduler(plugin);
        updateChecker = new UpdateChecker(this);
        updateChecker.bootstrap();
        new ReloadSafetyListener(this, scheduler);
        Bukkit.getServer().getPluginManager().registerEvents(new GUIListener(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new HolidayGreetingListener(), this);

        var cmd = getCommand("peachLibSettings");
        if (cmd != null) {
            cmd.setExecutor(new PeachLibSettings());
            getLogger().info("PeachLibSettings Command registriert :D");
        }

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
        cfg = null;
        getLogger().info("PeachLib disabled.");
        plugin = null;
    }
}
