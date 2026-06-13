package de.peachbiscuit174.peachlib.api;

import de.peachbiscuit174.peachlib.PeachLib;
import de.peachbiscuit174.peachlib.api.managers.*;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;

/**
 * The central entry point for the PeachLib API.
 * Access all managers and version information through this class.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class PeachLibAPI {

    // --- Managers ---
    private static ItemsManager itemsManager;
    private static SchedulerManager schedulerManager;
    private static PlayerManager playerManager;
    private static GUIManager guiManager;
    private static FileManager fileManager;
    private static LanguageManager languageManager;
    private static DataManager dataManager;

    // --- Versioning ---
    private static final String API_VERSION = "v1.0.0";

    @ApiStatus.Internal
    public static void init(PeachLib plugin) {
        itemsManager = new ItemsManager();
        schedulerManager = new SchedulerManager();
        playerManager = new PlayerManager();
        guiManager = new GUIManager();
        fileManager = new FileManager();
        languageManager = new LanguageManager();
        dataManager = new DataManager(plugin.getDataFolder());
    }

    @ApiStatus.Internal
    public static void shutdown() {
        itemsManager = null;
        schedulerManager = null;
        playerManager = null;
        guiManager = null;
        fileManager = null;
        languageManager = null;
        dataManager = null;
    }

    /**
     * Returns the current API Version as a String.
     *
     * @return available API Version string (e.g. "v1.0.0")
     */
    public static String getApiVersion() {
        return API_VERSION;
    }

    /**
     * Checks if the current API version is newer than or equal to the provided version string.
     * (Current Version >= Required Version)
     *
     * @param requiredVersion The minimum version required (e.g. "1.2.0").
     * @return true if the current version is greater than or equal to the required version.
     */
    public static boolean isVersionAtLeast(String requiredVersion) {
        return compareVersions(API_VERSION, requiredVersion) >= 0;
    }

    /**
     * Checks if the current API version is NOT newer than the provided version string.
     * (Current Version <= Max Version)
     * <p>
     * Useful for checking compatibility with older systems or deprecation.
     *
     * @param maxVersion The maximum version allowed (e.g. "1.5.0").
     * @return true if the current version is older than or equal to the max version.
     */
    public static boolean isVersionAtMost(String maxVersion) {
        return compareVersions(API_VERSION, maxVersion) <= 0;
    }

    // --- Internal Version Helpers ---

    /**
     * Compares two version strings.
     *
     * @return 1 if v1 > v2, -1 if v1 < v2, 0 if equal.
     */
    private static int compareVersions(String version1, String version2) {
        int[] v1 = parseVersion(version1);
        int[] v2 = parseVersion(version2);

        int length = Math.max(v1.length, v2.length);

        for (int i = 0; i < length; i++) {
            int part1 = i < v1.length ? v1[i] : 0;
            int part2 = i < v2.length ? v2[i] : 0;

            if (part1 < part2) {
                return -1;
            }
            if (part1 > part2) {
                return 1;
            }
        }
        return 0;
    }

    private static int[] parseVersion(String version) {
        // Removes non-numeric characters except dots (handles "v1.0" or "1.0-SNAPSHOT")
        String cleanVersion = version.split("-")[0].replaceAll("[^0-9.]", "");
        if (cleanVersion.isEmpty()) return new int[0];

        return Arrays.stream(cleanVersion.split("\\."))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    // --- Manager Getters ---

    public static ItemsManager getItemsManager() {
        return itemsManager;
    }

    public static SchedulerManager getSchedulerManager() {
        return schedulerManager;
    }

    public static PlayerManager getPlayerManager() {
        return playerManager;
    }

    public static GUIManager getGUIManager() {
        return guiManager;
    }

    public static FileManager getFileManager() {
        return fileManager;
    }

    public static LanguageManager getLanguageManager() {
        return languageManager;
    }

    public static DataManager getDataManager() {
        return dataManager;
    }
}