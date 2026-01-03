package de.peachbiscuit174.peachpaperlib;

import de.peachbiscuit174.peachpaperlib.other.HolidayGreetingListener;
import de.peachbiscuit174.peachpaperlib.updatecheck.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author peachbiscuit174
 * @since 1.0.0
 */
public final class PeachPaperLib extends JavaPlugin {
    private static Plugin plugin;
    private static UpdateChecker updateChecker;

    public static Plugin getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;

        int pluginId = 28644;
        Metrics metrics = new Metrics(this, pluginId);

        updateChecker = new UpdateChecker(this);
        updateChecker.bootstrap();
        getServer().getPluginManager().registerEvents(new HolidayGreetingListener(), this);

        getLogger().info("PeachPaperLib has been loaded successfully.");
        getLogger().info("Version: " + getPluginMeta().getVersion());
        getLogger().info("Developed by PeachBiscuit174");
        getLogger().info("Thank you for using PeachPaperLib (PPL) :D");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("PeachPaperLib disabled.");
    }
}
