package de.peachbiscuit174.peachlib.other;

import de.peachbiscuit174.peachlib.PeachLib;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigData {

    private static final CustomConfig2 cfg_raw = PeachLib.getCfg();
    private static final FileConfiguration cfg = cfg_raw.getConfig();
    private static boolean autoUpdateStatus = true;

    public static void reloadData() {
        autoUpdateStatus = cfg.getBoolean("setting.auto_update");
    }

    public static boolean getAutoUpdateStatus() {
        return autoUpdateStatus;
    }

    public static void toggleAutoUpdateStatus() {
        if (autoUpdateStatus) {
            autoUpdateStatus = false;
            cfg.set("setting.auto_update", false);
        } else {
            autoUpdateStatus = true;
            cfg.set("setting.auto_update", true);
        }
        cfg_raw.save();
    }

    public static void setAutoUpdateStatus(boolean value) {
        if (value) {
            autoUpdateStatus = true;
            cfg.set("setting.auto_update", true);
        } else {
            autoUpdateStatus = false;
            cfg.set("setting.auto_update", false);
        }
        cfg_raw.save();
    }

}
