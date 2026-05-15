package de.peachbiscuit174.peachlib.api.items;

import de.peachbiscuit174.peachlib.items.ItemTag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * API for managing PersistentDataContainer tags on ItemStacks.
 * * @author peachbiscuit174
 * @since 1.0.0
 */
public class ItemTagAPI {

    // --- BOOLEAN TAGS ---

    /**
     * Assigns a specific identification tag to the given {@link ItemStack} bound to the specified plugin.
     * <p>
     * If the item does not already possess this tag, it will be added.
     * If it already exists, it remains unchanged.
     * </p>
     *
     * @param plugin       The plugin instance to bind the namespace to.
     * @param itemStack    The {@link ItemStack} to which the tag should be applied.
     * @param item_tag_key The unique string identifier to set as a tag.
     * @return The modified {@link ItemStack} containing the new tag.
     */
    public ItemStack setItemTag(@NotNull Plugin plugin, @NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.setItemTag(plugin, itemStack, item_tag_key);
    }

    /**
     * @deprecated Pass your plugin instance to avoid namespace collisions. Use {@link #setItemTag(Plugin, ItemStack, String)}
     */
    @Deprecated
    public ItemStack setItemTag(@NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.setItemTag(itemStack, item_tag_key);
    }

    /**
     * Checks whether the specified {@link ItemStack} contains a particular identification tag for the specified plugin.
     *
     * @param plugin       The plugin instance.
     * @param itemStack    The {@link ItemStack} to inspect.
     * @param item_tag_key The tag key to look for.
     * @return {@code true} if the item has the specified tag; {@code false} otherwise.
     */
    public boolean hasItemTag(@NotNull Plugin plugin, @NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.isItemTag(plugin, itemStack, item_tag_key);
    }

    /**
     * @deprecated Pass your plugin instance to avoid namespace collisions. Use {@link #hasItemTag(Plugin, ItemStack, String)}
     */
    @Deprecated
    public boolean hasItemTag(@NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.isItemTag(itemStack, item_tag_key);
    }

    /**
     * Removes a specific identification tag from the given {@link ItemStack} bound to the specified plugin.
     * <p>
     * If the tag does not exist on the item, no changes will be made.
     * </p>
     *
     * @param plugin       The plugin instance.
     * @param itemStack    The {@link ItemStack} from which the tag should be removed.
     * @param item_tag_key The tag key to be deleted.
     * @return The {@link ItemStack} without the specified tag.
     */
    public ItemStack removeItemTag(@NotNull Plugin plugin, @NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.removeItemTag(plugin, itemStack, item_tag_key);
    }

    /**
     * @deprecated Pass your plugin instance to avoid namespace collisions. Use {@link #removeItemTag(Plugin, ItemStack, String)}
     */
    @Deprecated
    public ItemStack removeItemTag(@NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.removeItemTag(itemStack, item_tag_key);
    }


    // --- STRING TAGS ---

    /**
     * Assigns a specific string identification tag to the given {@link ItemStack} bound to the specified plugin.
     * * @param plugin       The plugin instance.
     * @param itemStack    The {@link ItemStack} to modify.
     * @param item_tag_key The unique string identifier.
     * @param value        The string value to save.
     * @return The modified {@link ItemStack}.
     */
    public ItemStack setItemStringTag(@NotNull Plugin plugin, @NotNull ItemStack itemStack, @NotNull String item_tag_key, @NotNull String value) {
        return ItemTag.setItemStringTag(plugin, itemStack, item_tag_key, value);
    }

    /**
     * @deprecated Pass your plugin instance to avoid namespace collisions. Use {@link #setItemStringTag(Plugin, ItemStack, String, String)}
     */
    @Deprecated
    public ItemStack setItemStringTag(@NotNull ItemStack itemStack, @NotNull String item_tag_key, @NotNull String value) {
        return ItemTag.setItemStringTag(itemStack, item_tag_key, value);
    }

    /**
     * Checks whether the specified {@link ItemStack} contains a string identification tag for the specified plugin.
     * * @param plugin       The plugin instance.
     * @param itemStack    The {@link ItemStack} to inspect.
     * @param item_tag_key The tag key to look for.
     * @return {@code true} if the item has the specified string tag; {@code false} otherwise.
     */
    public boolean isItemStringTag(@NotNull Plugin plugin, @NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.isItemStringTag(plugin, itemStack, item_tag_key);
    }

    /**
     * @deprecated Pass your plugin instance to avoid namespace collisions. Use {@link #isItemStringTag(Plugin, ItemStack, String)}
     */
    @Deprecated
    public boolean isItemStringTag(@NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.isItemStringTag(itemStack, item_tag_key);
    }

    /**
     * Retrieves the value of a specific string identification tag from the given {@link ItemStack}.
     * * @param plugin       The plugin instance.
     * @param itemStack    The {@link ItemStack} to inspect.
     * @param item_tag_key The tag key to look for.
     * @return The string value of the tag, or null if it doesn't exist.
     */
    public String getItemStringTag(@NotNull Plugin plugin, @NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.getItemStringTag(plugin, itemStack, item_tag_key);
    }

    /**
     * @deprecated Pass your plugin instance to avoid namespace collisions. Use {@link #getItemStringTag(Plugin, ItemStack, String)}
     */
    @Deprecated
    public String getItemStringTag(@NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.getItemStringTag(itemStack, item_tag_key);
    }
}