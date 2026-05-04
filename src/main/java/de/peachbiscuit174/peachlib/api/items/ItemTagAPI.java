package de.peachbiscuit174.peachlib.api.items;

import de.peachbiscuit174.peachlib.items.ItemTag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * API for managing PersistentDataContainer tags on ItemStacks.
 * * @author peachbiscuit174
 * @since 1.0.0
 */
public class ItemTagAPI {

    /**
     * Assigns a specific identification tag to the given {@link ItemStack}.
     * <p>
     * If the item does not already possess this tag, it will be added.
     * If it already exists, it remains unchanged.
     * </p>
     *
     * @param itemStack    The {@link ItemStack} to which the tag should be applied.
     * @param item_tag_key The unique string identifier to set as a tag.
     * @return The modified {@link ItemStack} containing the new tag.
     */
    public ItemStack setItemTag(@NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.setItemTag(itemStack, item_tag_key);
    }

    /**
     * Checks whether the specified {@link ItemStack} contains a particular identification tag.
     *
     * @param itemStack    The {@link ItemStack} to inspect.
     * @param item_tag_key The tag key to look for.
     * @return {@code true} if the item has the specified tag; {@code false} otherwise.
     */
    public boolean hasItemTag(@NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.isItemTag(itemStack, item_tag_key);
    }

    /**
     * Removes a specific identification tag from the given {@link ItemStack}.
     * <p>
     * If the tag does not exist on the item, no changes will be made.
     * </p>
     *
     * @param itemStack    The {@link ItemStack} from which the tag should be removed.
     * @param item_tag_key The tag key to be deleted.
     * @return The {@link ItemStack} without the specified tag.
     */
    public ItemStack removeItemTag(@NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.removeItemTag(itemStack, item_tag_key);
    }

    public ItemStack setItemStringTag(@NotNull ItemStack itemStack, @NotNull String item_tag_key, @NotNull String value) {
        return ItemTag.setItemStringTag(itemStack, item_tag_key, value);
    }

    public boolean isItemStringTag(@NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.isItemStringTag(itemStack, item_tag_key);
    }

    public String getItemStringTag(@NotNull ItemStack itemStack, @NotNull String item_tag_key) {
        return ItemTag.getItemStringTag(itemStack, item_tag_key);
    }
}