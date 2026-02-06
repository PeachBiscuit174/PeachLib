package de.peachbiscuit174.peachlib.configstuff;

import de.peachbiscuit174.peachlib.PeachLib;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class CustomConfig {
    private File file;
    private FileConfiguration config;


    public CustomConfig(String folderName, String configName) {

        File ordner = new File(PeachLib.getPlugin().getDataFolder(), folderName);
        if (!ordner.exists()) {
            ordner.mkdirs();
        }


        file = new File(ordner, configName);

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

    public String getName() {
        return file.getName();
    }

    public void save() {
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
