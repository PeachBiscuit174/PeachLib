package de.peachbiscuit174.peachlib.api.managers;

import de.peachbiscuit174.peachlib.api.language.LanguageAPI;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for handling plugin-specific language APIs.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class LanguageManager {

    private final Map<Plugin, LanguageAPI> apiInstances = new ConcurrentHashMap<>();

    /**
     * Retrieves the LanguageAPI instance for the specified plugin.
     * If it doesn't exist yet, it will be created and cached automatically.
     *
     * @param plugin The Bukkit plugin requesting the API.
     * @return The {@link LanguageAPI} instance for the plugin.
     */
    public LanguageAPI getLanguageAPI(@NotNull Plugin plugin) {
        return apiInstances.computeIfAbsent(plugin, LanguageAPI::new);
    }
}