package de.peachbiscuit174.peachlib.configstuff;

import de.peachbiscuit174.peachlib.PeachLib;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigData {

    private static final CustomConfig cfg_raw = PeachLib.getCfg();
    private static final FileConfiguration cfg = cfg_raw.getConfig();
    private static boolean autoUpdateStatus = true;
    private static boolean allowSnapshotUpdates = false;

    public static void reloadData() {
        autoUpdateStatus = cfg.getBoolean("setting.auto_update");
        allowSnapshotUpdates = cfg.getBoolean("setting.allow_snapshot_updates");
    }

    public static boolean getAutoUpdateStatus() {
        return autoUpdateStatus;
    }

    public static void toggleAutoUpdateStatus() {
        autoUpdateStatus = !autoUpdateStatus;
        cfg.set("setting.auto_update", autoUpdateStatus);
        cfg_raw.save();
    }

    public static void setAutoUpdateStatus(boolean value) {
        autoUpdateStatus = value;
        cfg.set("setting.auto_update", value);
        cfg_raw.save();
    }

    public static boolean isAllowSnapshotUpdates() {
        return allowSnapshotUpdates;
    }

    public static void toggleAllowSnapshotUpdates() {
        allowSnapshotUpdates = !allowSnapshotUpdates;
        cfg.set("setting.allow_snapshot_updates", allowSnapshotUpdates);
        cfg_raw.save();
    }

    public static void setAllowSnapshotUpdates(boolean value) {
        allowSnapshotUpdates = value;
        cfg.set("setting.allow_snapshot_updates", value);
        cfg_raw.save();
    }

}
