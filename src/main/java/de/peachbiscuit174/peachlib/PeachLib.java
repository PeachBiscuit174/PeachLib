package de.peachbiscuit174.peachlib;

import de.peachbiscuit174.peachlib.Commands.PeachLibSettings;
import de.peachbiscuit174.peachlib.api.PeachLibAPI;
import de.peachbiscuit174.peachlib.configstuff.ConfigData;
import de.peachbiscuit174.peachlib.configstuff.CustomConfig;
import de.peachbiscuit174.peachlib.configstuff.SetupConfig;
import de.peachbiscuit174.peachlib.files.PeachFile;
import de.peachbiscuit174.peachlib.gui.GUIListener;
import de.peachbiscuit174.peachlib.other.*;
import de.peachbiscuit174.peachlib.scheduler.LibraryScheduler;
import de.peachbiscuit174.peachlib.updatecheck.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private static CustomConfig cfg;

    // --- INTERNAL WATCHER REGISTRY ---
    private static final Set<PeachFile> WATCHED_FILES = ConcurrentHashMap.newKeySet();

    /**
     * INTERNAL API - DO NOT USE!
     * Registers a PeachFile to be tracked for automatic watcher shutdown.
     */
    @ApiStatus.Internal
    public static void registerWatchedFile(@NotNull PeachFile file) {
        WATCHED_FILES.add(file);
    }

    /**
     * INTERNAL API - DO NOT USE!
     * Unregisters a PeachFile from the tracker.
     */
    @ApiStatus.Internal
    public static void unregisterWatchedFile(@NotNull PeachFile file) {
        WATCHED_FILES.remove(file);
    }

    /**
     * Private cleanup method.
     * Stops all active file watchers safely on plugin disable.
     */
    private void stopAllWatchers() {
        if (WATCHED_FILES.isEmpty()) return;
        for (PeachFile file : WATCHED_FILES) {
            file.stopWatching();
        }
        WATCHED_FILES.clear();
    }
    // ---------------------------------

    public static CustomConfig getCfg() {
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

        cfg = new CustomConfig("settings.yml");
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

        PeachLibAPI.init(this);

        if (ConfigData.isSyncTimeForDatabase()) {
            PeachLibAPI.getDataManager().getTimeProvider().syncBlocking();
        }

        if (ConfigData.isShutdownOnSyncFailure()) {
            if (!PeachLibAPI.getDataManager().getTimeProvider().isSynchronized()) {
                getLogger().severe("!!! CRITICAL TIME SYNC FAILED - SHUTTING DOWN SERVER !!!");
                Bukkit.shutdown();
                return; // Stop loading
            }
        }

        // Try to recover any crashed database tasks from the Write-Ahead-Log
        PeachLibAPI.getDataManager().recoverCrashLogs();

        getLogger().info("----------------------------------");
        getLogger().info("PeachLib has been loaded successfully.");
        getLogger().info("Version: " + getPluginMeta().getVersion());
        getLogger().info("Developed by PeachBiscuit174");
        getLogger().info("Thank you for using PeachLib (PL) :D");
        getLogger().info("----------------------------------");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        PeachLibAPI.getDataManager().shutdown();

        stopAllWatchers();

        if (scheduler != null) {
            scheduler.shutdown();
        }

        PeachLibAPI.shutdown();

        scheduler = null;
        updateChecker = null;
        metrics = null;
        cfg = null;
        getLogger().info("PeachLib disabled.");
        plugin = null;
    }
}