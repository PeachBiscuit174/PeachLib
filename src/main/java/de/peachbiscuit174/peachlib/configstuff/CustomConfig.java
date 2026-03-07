package de.peachbiscuit174.peachlib.configstuff;

import de.peachbiscuit174.peachlib.PeachLib;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Level;

/**
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class CustomConfig {
    private File file;
    private FileConfiguration config;

    public CustomConfig(String configName) {
        this("", configName);
    }

    public CustomConfig(String folderName, String configName) {
        File ordner = folderName.isEmpty() ?
                PeachLib.getPlugin().getDataFolder() :
                new File(PeachLib.getPlugin().getDataFolder(), folderName);

        if (!ordner.exists()) {
            ordner.mkdirs();
        }

        file = new File(ordner, configName);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                PeachLib.getPlugin().getLogger().log(Level.SEVERE, "Could not create config file: " + file.getName(), e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getName() {
        return file.getName();
    }

    public void save() {
        try {
            config.save(file);
        } catch (Exception e) {
            PeachLib.getPlugin().getLogger().log(Level.SEVERE, "Could not save config file: " + file.getName(), e);
        }
    }
}