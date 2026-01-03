package de.peachbiscuit174.peachpaperlib.api.items;

import de.peachbiscuit174.peachpaperlib.items.CustomHeads;
import de.peachbiscuit174.peachpaperlib.items.ItemLore;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URL;

/**
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class CustomHeadsAPI {

    /**
     * Creates a player head with a texture from a Base64 string.
     *
     * @param base64_texture The Base64 encoded texture string.
     * @return A {@link ItemStack} of a player head with the specified texture.
     */
    public ItemStack getCustomHead(@NotNull String base64_texture) {
        return CustomHeads.getCustomHead(base64_texture);

    }

    /**
     * Creates a player head with a Base64 texture and a custom display name.
     *
     * @param base64_texture The Base64 encoded texture string.
     * @param display_name   The display name of the item (supports MiniMessage).
     * @return A player head with the specified texture and name.
     */
    public ItemStack getCustomHead(@NotNull String base64_texture, @NotNull String display_name) {
        return CustomHeads.getCustomHead(base64_texture, display_name);
    }

    /**
     * Creates a player head with a Base64 texture, display name, and lore.
     *
     * @param base64_texture The Base64 encoded texture string.
     * @param display_name   The display name of the item (supports MiniMessage).
     * @param lore           The {@link ItemLore} to be applied to the head.
     * @return A player head with the specified texture, name, and lore.
     */
    public ItemStack getCustomHead(@NotNull String base64_texture, @NotNull String display_name, @NotNull ItemLore lore) {
        return CustomHeads.getCustomHead(base64_texture, display_name, lore);
    }

    /**
     * Creates a player head from a texture URI.
     * The URL must point to the Minecraft texture server.
     *
     * @param uri The {@link URI} pointing to the skin texture.
     * @return A player head with the specified texture.
     */
    public ItemStack getCustomHead(@NotNull URI uri) {
        return CustomHeads.getCustomHead(uri);
    }

    /**
     * Creates a player head from a texture URI.
     * The URL must point to the Minecraft texture server.
     *
     * @param uri          The {@link URI} pointing to the skin texture.
     * @param display_name The display name of the item (supports MiniMessage).
     * @return A player head with the specified texture and name.
     */
    public ItemStack getCustomHead(@NotNull URI uri, @NotNull String display_name) {
        return CustomHeads.getCustomHead(uri, display_name);
    }

    /**
     * Creates a player head from a texture URI.
     * The URL must point to the Minecraft texture server.
     *
     * @param uri          The {@link URI} pointing to the skin texture.
     * @param display_name The display name of the item (supports MiniMessage).
     * @param lore         The {@link ItemLore} to be applied to the head.
     * @return A player head with the specified texture, name, and lore.
     */
    public ItemStack getCustomHead(@NotNull URI uri, @NotNull String display_name, @NotNull ItemLore lore) {
        return CustomHeads.getCustomHead(uri, display_name, lore);
    }

    /**
     * Creates a player head from a texture URL.
     * The URL must point to the Minecraft texture server.
     *
     * @param url The {@link URL} pointing to the skin texture.
     * @return A player head with the specified texture.
     */
    public ItemStack getCustomHead(@NotNull URL url) {
        return CustomHeads.getCustomHead(url);
    }

    /**
     * Creates a player head from a texture URL.
     * The URL must point to the Minecraft texture server.
     *
     * @param url          The {@link URL} pointing to the skin texture.
     * @param display_name The display name of the item (supports MiniMessage).
     * @return A player head with the specified texture and name.
     */
    public ItemStack getCustomHead(@NotNull URL url, @NotNull String display_name) {
        return CustomHeads.getCustomHead(url, display_name);
    }

    /**
     * Creates a player head from a texture URL.
     * The URL must point to the Minecraft texture server.
     *
     * @param url          The {@link URL} pointing to the skin texture.
     * @param display_name The display name of the item (supports MiniMessage).
     * @param lore         The {@link ItemLore} to be applied to the head.
     * @return A player head with the specified texture, name, and lore.
     */
    public ItemStack getCustomHead(@NotNull URL url, @NotNull String display_name, @NotNull ItemLore lore) {
        return CustomHeads.getCustomHead(url, display_name, lore);
    }

}