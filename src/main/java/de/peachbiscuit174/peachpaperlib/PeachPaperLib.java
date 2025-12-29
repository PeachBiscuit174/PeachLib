package de.peachbiscuit174.peachpaperlib;

import org.bukkit.plugin.java.JavaPlugin;

public final class PeachPaperLib extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
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
