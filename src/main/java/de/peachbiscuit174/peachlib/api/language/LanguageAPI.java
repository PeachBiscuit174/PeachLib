package de.peachbiscuit174.peachlib.api.language;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.peachbiscuit174.peachlib.files.PeachFile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * API for managing multi-language messages, prefixes, and file modifications.
 * <p>
 * Features an automatic 1-hour expiration cache to optimize RAM and CPU usage
 * during message retrieval.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class LanguageAPI {

    private final Plugin plugin;
    private String prefix = "";
    private String defaultLocale = "en";

    private final Map<String, PeachFile> localeFiles = new HashMap<>();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Cache<String, String> messageCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    /**
     * Creates a new LanguageAPI instance for a specific plugin.
     *
     * @param plugin The Bukkit plugin instance owning this API.
     */
    public LanguageAPI(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets the global prefix to be prepended to all messages retrieved via {@link #getPrefixedMessage}.
     * Supports MiniMessage formatting tags.
     *
     * @param prefix The prefix string.
     * @return The current instance for fluent chaining.
     */
    public LanguageAPI setPrefix(@NotNull String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * Sets the default locale to fall back to if a requested message key is missing
     * in the target language file.
     *
     * @param defaultLocale The default locale code (e.g., "en").
     * @return The current instance for fluent chaining.
     */
    public LanguageAPI setDefaultLocale(@NotNull String defaultLocale) {
        this.defaultLocale = defaultLocale;
        return this;
    }

    /**
     * Loads a language file and registers it under the specified locale code.
     * If the file does not exist in the data folder, it will be extracted from the plugin's resources.
     * If it doesn't exist in the resources either, it will be registered virtually and created upon the first save.
     *
     * @param localeCode The locale identifier (e.g., "en", "de").
     * @param fileName   The path and name of the file relative to the plugin's data folder.
     * @return The current instance for fluent chaining.
     */
    public LanguageAPI loadLocale(@NotNull String localeCode, @NotNull String fileName) {
        PeachFile file = new PeachFile(plugin, fileName);
        file.extractDefault(plugin, fileName, false);

        localeFiles.put(localeCode.toLowerCase(), file);

        if (file.exists()) {
            plugin.getLogger().info("Loaded language file [" + localeCode + "]: " + fileName);
        } else {
            plugin.getLogger().info("Registered virtual language file (will be created on first save) [" + localeCode + "]: " + fileName);
        }

        return this;
    }

    /**
     * Overwrites the existing language file on the server with the default one from the plugin's resources.
     * <p>
     * WARNING: This will permanently delete any custom translations or modifications
     * made by the server owner! Use with extreme caution.
     *
     * @param localeCode The locale identifier (e.g., "en", "de").
     * @param fileName   The path and name of the file relative to the plugin's data folder.
     * @return The current instance for fluent chaining.
     */
    public LanguageAPI loadLocaleAndOverwriteWithDefault(@NotNull String localeCode, @NotNull String fileName) {
        PeachFile file = new PeachFile(plugin, fileName);
        String lowerLocale = localeCode.toLowerCase();

        file.extractDefault(plugin, fileName, true);
        localeFiles.put(lowerLocale, file);

        plugin.getLogger().warning("OVERWROTE language file [" + localeCode + "] with default from jar! All custom changes were reset.");

        messageCache.asMap().keySet().removeIf(cacheKey -> cacheKey.startsWith(lowerLocale + ":"));

        return this;
    }

    /**
     * Compares the current language file on the server with the default file from the plugin's jar.
     * Any missing keys are automatically added to the server's file, while existing
     * custom translations are preserved safely.
     * <p>
     * Perfect for plugin updates where new messages were introduced.
     *
     * @param localeCode The locale identifier (e.g., "en", "de").
     * @param fileName   The path and name of the file inside the plugin's jar.
     * @return The current instance for fluent chaining.
     */
    public LanguageAPI updateLocaleWithDefaults(@NotNull String localeCode, @NotNull String fileName) {
        String lowerLocale = localeCode.toLowerCase();
        PeachFile file = localeFiles.get(lowerLocale);

        if (file == null) {
            plugin.getLogger().warning("Cannot update locale '" + localeCode + "'. It must be loaded first!");
            return this;
        }

        java.io.InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream == null) {
            plugin.getLogger().warning("Cannot update locale. Default file '" + fileName + "' not found in the jar!");
            return this;
        }

        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(defaultStream, java.nio.charset.StandardCharsets.UTF_8)
        );
        YamlConfiguration currentConfig = file.getYaml();

        boolean fileWasUpdated = false;

        for (String key : defaultConfig.getKeys(true)) {
            if (!defaultConfig.isConfigurationSection(key) && !currentConfig.isSet(key)) {
                currentConfig.set(key, defaultConfig.get(key));

                messageCache.invalidate(lowerLocale + ":" + key);

                fileWasUpdated = true;
            }
        }

        if (fileWasUpdated) {
            try {
                file.saveYaml(currentConfig);
                plugin.getLogger().info("Successfully updated language file [" + localeCode + "] with new keys from the jar.");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save updated language file for locale '" + localeCode + "'", e);
            }
        }

        return this;
    }

    /**
     * Updates an existing entry or creates a new one in the specified language file,
     * saves the file to disk, and invalidates the affected key in the RAM cache.
     * <p>
     * If the requested language file does not yet exist physically on the server
     * (e.g., it was virtually registered via {@link #loadLocale(String, String)}),
     * this initial save operation will automatically generate the file on disk.
     *
     * @param localeCode The locale identifier.
     * @param key        The configuration key to update or create.
     * @param value      The new value to set.
     */
    public void setAndSaveEntry(@NotNull String localeCode, @NotNull String key, @NotNull String value) {
        String lowerLocale = localeCode.toLowerCase();
        PeachFile file = localeFiles.get(lowerLocale);

        if (file != null) {
            YamlConfiguration config = file.getYaml();
            config.set(key, value);

            try {
                file.saveYaml(config);
                messageCache.invalidate(lowerLocale + ":" + key);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save language file for locale '" + localeCode + "'", e);
            }
        } else {
            plugin.getLogger().warning("Cannot save entry. Locale '" + localeCode + "' is not loaded.");
        }
    }

    /**
     * Retrieves the raw string from the requested locale file using lazy loading and caching.
     * Falls back to the default locale if the key is missing in the target locale.
     *
     * @param localeCode The locale identifier. If null, the default locale is used.
     * @param key        The configuration key to retrieve.
     * @return The unparsed string, or an error message if the key is completely missing.
     */
    public @NotNull String getRawString(@Nullable String localeCode, @NotNull String key) {
        String targetLocale = (localeCode != null && localeFiles.containsKey(localeCode.toLowerCase()))
                ? localeCode.toLowerCase()
                : this.defaultLocale.toLowerCase();

        String cacheKey = targetLocale + ":" + key;

        String cachedMessage = messageCache.getIfPresent(cacheKey);
        if (cachedMessage != null) return cachedMessage;

        String resultMessage = null;
        PeachFile requestedFile = localeFiles.get(targetLocale);

        if (requestedFile != null) {
            YamlConfiguration config = requestedFile.getYaml();
            if (config.isSet(key)) resultMessage = config.getString(key);
        }

        if (resultMessage == null && !targetLocale.equals(this.defaultLocale.toLowerCase())) {
            PeachFile defaultFile = localeFiles.get(this.defaultLocale.toLowerCase());
            if (defaultFile != null) {
                YamlConfiguration defaultConfig = defaultFile.getYaml();
                if (defaultConfig.isSet(key)) resultMessage = defaultConfig.getString(key);
            }
        }

        if (resultMessage == null) return "<red>Missing key: " + key + "</red>";

        messageCache.put(cacheKey, resultMessage);
        return resultMessage;
    }

    /**
     * Retrieves a fully parsed MiniMessage component, automatically prepending the plugin's prefix.
     * Alternating placeholders are supported to dynamically replace text.
     *
     * @param localeCode   The locale identifier. If null, the default locale is used.
     * @param key          The configuration key to retrieve.
     * @param placeholders An alternating sequence of placeholder keys and values (e.g., "player", "Notch").
     * @return The parsed {@link Component} including the prefix and applied placeholders.
     */
    public @NotNull Component getPrefixedMessage(@Nullable String localeCode, @NotNull String key, @NotNull String... placeholders) {
        String rawMessage = getRawString(localeCode, key);
        return parseString(this.prefix + rawMessage, placeholders);
    }

    /**
     * Retrieves a fully parsed MiniMessage component WITHOUT the plugin's prefix.
     * Alternating placeholders are supported to dynamically replace text.
     * <p>
     * Ideal for GUIs, ActionBars, or Titles where a chat prefix is visually disruptive.
     *
     * @param localeCode   The locale identifier. If null, the default locale is used.
     * @param key          The configuration key to retrieve.
     * @param placeholders An alternating sequence of placeholder keys and values (e.g., "player", "Notch").
     * @return The parsed {@link Component} without the prefix, applying placeholders.
     */
    public @NotNull Component getMessage(@Nullable String localeCode, @NotNull String key, @NotNull String... placeholders) {
        String rawMessage = getRawString(localeCode, key);
        return parseString(rawMessage, placeholders);
    }

    /**
     * Internal helper to parse MiniMessage strings with simple placeholders.
     *
     * @param text         The raw MiniMessage string.
     * @param placeholders The array of alternating keys and values.
     * @return The parsed {@link Component}.
     */
    private @NotNull Component parseString(@NotNull String text, @NotNull String... placeholders) {
        if (placeholders.length == 0) return MM.deserialize(text);

        if (placeholders.length % 2 != 0) {
            plugin.getLogger().warning("Invalid placeholder arguments. Must be key-value pairs.");
            return MM.deserialize(text);
        }

        List<TagResolver> resolvers = new ArrayList<>();
        for (int i = 0; i < placeholders.length; i += 2) {
            String pKey = placeholders[i];
            String pValue = placeholders[i + 1];
            resolvers.add(Placeholder.parsed(pKey, pValue != null ? pValue : "null"));
        }

        return MM.deserialize(text, TagResolver.resolver(resolvers));
    }
}