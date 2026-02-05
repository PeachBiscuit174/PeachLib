package de.peachbiscuit174.peachlib.other;

import de.peachbiscuit174.peachlib.PeachLib;
import org.bukkit.configuration.file.FileConfiguration;

public class SetupConfig {

    private static final CustomConfig2 cfg_raw = PeachLib.getCfg();
    private static final FileConfiguration cfg = cfg_raw.getConfig();

    public static void setup() {
        if (!cfg.isSet("setting.auto_update")) {
            cfg.set("setting.auto_update", true);
        }
        cfg_raw.save();


    }

}
