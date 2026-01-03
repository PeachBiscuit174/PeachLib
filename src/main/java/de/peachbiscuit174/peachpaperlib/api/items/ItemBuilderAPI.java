package de.peachbiscuit174.peachpaperlib.api.items;

import de.peachbiscuit174.peachpaperlib.items.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class ItemBuilderAPI {

    /**
     * Starts a new ItemBuilder for the given Material.
     *
     * @param material The material to start with.
     * @return A new {@link ItemBuilder} instance.
     */
    public @NotNull ItemBuilder builder(@NotNull Material material) {
        return new ItemBuilder(material);
    }

    /**
     * Starts an ItemBuilder based on an existing ItemStack (clones it).
     *
     * @param itemStack The stack to copy.
     * @return A new {@link ItemBuilder} instance.
     */
    public @NotNull ItemBuilder builder(@NotNull ItemStack itemStack) {
        return new ItemBuilder(itemStack);
    }
}
