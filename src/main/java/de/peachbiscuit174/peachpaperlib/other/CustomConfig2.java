package de.peachbiscuit174.peachpaperlib.other;

import de.peachbiscuit174.peachpaperlib.PeachPaperLib;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class CustomConfig2 {
    private File file;
    private FileConfiguration config;


    public CustomConfig2(String configName) {
        file = new File(PeachPaperLib.getPlugin().getDataFolder(), configName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void save() {
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
