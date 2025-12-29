package de.peachbiscuit174.peachpaperlib.custom;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URL;
import java.util.UUID;


/**
 * Utility class for creating custom player heads using various texture sources.
 * <p>
 * This class supports textures from Base64 strings, URLs, and URIs, and
 * integrates seamlessly with MiniMessage for formatting.
 * </p>
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class CustomHeads {

    /**
     * Creates a player head with a texture from a Base64 string.
     *
     * @param base64_texture The Base64 encoded texture string.
     * @return A {@link ItemStack} of a player head with the specified texture.
     */
    public static ItemStack getCustomHead(@NotNull String base64_texture) {

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (base64_texture == null) return head;
        if (meta == null) return head;

        PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID());
        ProfileProperty profileProperty = new ProfileProperty("textures", base64_texture);
        playerProfile.setProperty(profileProperty);

        meta.setPlayerProfile(playerProfile);
        head.setItemMeta(meta);

        return head;

    }

    /**
     * Creates a player head with a Base64 texture and a custom display name.
     *
     * @param base64_texture The Base64 encoded texture string.
     * @param display_name   The display name of the item (supports MiniMessage).
     * @return A player head with the specified texture and name.
     */
    public static ItemStack getCustomHead(@NotNull String base64_texture, @NotNull String display_name) {
        ItemStack head = getCustomHead(base64_texture);
        head = renameItem(head, display_name, null);
        return head;
    }

    /**
     * Creates a player head with a Base64 texture, display name, and lore.
     *
     * @param base64_texture The Base64 encoded texture string.
     * @param display_name   The display name of the item (supports MiniMessage).
     * @param lore           The {@link ItemLore} to be applied to the head.
     * @return A player head with the specified texture, name, and lore.
     */
    public static ItemStack getCustomHead(@NotNull String base64_texture, @NotNull String display_name, @NotNull ItemLore lore) {
        ItemStack head = getCustomHead(base64_texture);
        head = renameItem(head, display_name, lore);
        return head;
    }

    /**
     * Creates a player head from a texture URI.
     *
     * @param uri The {@link URI} pointing to the skin texture.
     * @return A player head with the specified texture.
     */
    public static ItemStack getCustomHead(@NotNull URI uri) {

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (uri == null) return head;
        if (meta == null) return head;

        PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID());
        PlayerTextures playerTextures = playerProfile.getTextures();

        URL url = null;
        if (playerTextures == null) return head;
        try {

             url = uri.toURL();

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (url == null) return head;

        playerTextures.setSkin(url);
        playerProfile.setTextures(playerTextures);
        meta.setPlayerProfile(playerProfile);
        head.setItemMeta(meta);

        return head;


    }

    /**
     * Creates a player head from a texture URI.
     *
     * @param uri The {@link URI} pointing to the skin texture.
     * @param display_name   The display name of the item (supports MiniMessage).
     * @return A player head with the specified texture and name.
     */
    public static ItemStack getCustomHead(@NotNull URI uri, @NotNull String display_name) {
        ItemStack head = getCustomHead(uri);
        head = renameItem(head, display_name, null);
        return head;
    }

    /**
     * Creates a player head from a texture URI.
     *
     * @param uri The {@link URI} pointing to the skin texture.
     * @param display_name   The display name of the item (supports MiniMessage).
     * @param lore           The {@link ItemLore} to be applied to the head.
     * @return A player head with the specified texture, name, and lore.
     */
    public static ItemStack getCustomHead(@NotNull URI uri, @NotNull String display_name, @NotNull ItemLore lore) {
        ItemStack head = getCustomHead(uri);
        head = renameItem(head, display_name, lore);
        return head;
    }

    /**
     * Creates a player head from a texture URL.
     *
     * @param url The {@link URL} pointing to the skin texture.
     * @return A player head with the specified texture.
     */
    public static ItemStack getCustomHead(@NotNull URL url) {

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (url == null) return head;
        if (meta == null) return head;

        PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID());
        PlayerTextures playerTextures = playerProfile.getTextures();

        if (playerTextures == null) return head;

        playerTextures.setSkin(url);
        playerProfile.setTextures(playerTextures);
        meta.setPlayerProfile(playerProfile);
        head.setItemMeta(meta);

        return head;


    }

    /**
     * Creates a player head from a texture URL.
     *
     * @param url The {@link URL} pointing to the skin texture.
     * @param display_name   The display name of the item (supports MiniMessage).
     * @return A player head with the specified texture and name.
     */
    public static ItemStack getCustomHead(@NotNull URL url, @NotNull String display_name) {
        ItemStack head = getCustomHead(url);
        head = renameItem(head, display_name, null);
        return head;
    }

    /**
     * Creates a player head from a texture URL.
     *
     * @param url The {@link URL} pointing to the skin texture.
     * @param display_name   The display name of the item (supports MiniMessage).
     * @param lore           The {@link ItemLore} to be applied to the head.
     * @return A player head with the specified texture, name, and lore.
     */
    public static ItemStack getCustomHead(@NotNull URL url, @NotNull String display_name, @NotNull ItemLore lore) {
        ItemStack head = getCustomHead(url);
        head = renameItem(head, display_name, lore);
        return head;
    }

    /**
     * Internal helper to apply display name and lore to an item.
     *
     * @param item         The item to modify.
     * @param display_name The new display name (can be null).
     * @param lore         The new lore (can be null).
     * @return The modified {@link ItemStack}.
     */
    private static ItemStack renameItem(@NotNull ItemStack item, @Nullable String display_name, @Nullable ItemLore lore) {

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (display_name != null) {
            meta.displayName(parseComponent(display_name));
        }

        if (lore != null) {
            meta.lore(lore.build());
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Internal helper
     * Parses a string into an Adventure Component using MiniMessage.
     *
     * @param input The string to parse.
     * @return The parsed {@link Component}.
     */
    private static @NotNull Component parseComponent(@NotNull String input) {
        return MiniMessage.miniMessage().deserialize(input);
    }




}
