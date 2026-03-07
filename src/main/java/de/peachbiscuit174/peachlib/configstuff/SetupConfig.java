package de.peachbiscuit174.peachlib.configstuff;

import de.peachbiscuit174.peachlib.PeachLib;
import org.bukkit.configuration.file.FileConfiguration;

public class SetupConfig {

    private static final CustomConfig cfg_raw = PeachLib.getCfg();
    private static final FileConfiguration cfg = cfg_raw.getConfig();

    public static void setup() {
        if (!cfg.isSet("setting.auto_update")) {
            cfg.set("setting.auto_update", true);
        }

        if (!cfg.isSet("setting.allow_snapshot_updates")) {
            cfg.set("setting.allow_snapshot_updates", false);
        }

        cfg_raw.save();


    }

}
