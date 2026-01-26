package de.peachbiscuit174.peachlib.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * A utility class for managing player-related actions,
 * providing support for modern Adventure Components and MiniMessage.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class PlayerManaging {

    private final Player player;

    /**
     * Constructs a new PlayerManaging instance for a specific player.
     * * @param player The Bukkit player to manage.
     */
    public PlayerManaging(@NotNull Player player) {
        this.player = player;
    }

    /**
     * Sends a message to the player using MiniMessage formatting.
     * * @param message The MiniMessage formatted string to send.
     */
    public void sendMessage(@NotNull String message) {
        player.sendMessage(parseComponent(message));
    }

    /**
     * Sets the display name of the player using MiniMessage formatting.
     * This updates the modern {@link Component} based display name.
     * * @param displayName The MiniMessage formatted string for the display name.
     */
    public void setDisplayName(@NotNull String displayName) {
        player.displayName(parseComponent(displayName));
    }

    /**
     * Sets the display name using legacy String format.
     * * @param displayNameLegacy The plain text or legacy formatted display name.
     * @deprecated Use {@link #setDisplayName(String)} for modern Component support.
     */
    @Deprecated
    public void setDisplayNameLegacy(@NotNull String displayNameLegacy) {
        player.setDisplayName(displayNameLegacy);
    }

    /**
     * Gets the current display name of the player as a Component.
     * * @return The {@link Component} representing the player's display name.
     */
    public Component getDisplayName() {
        return player.displayName();
    }

    /**
     * Gets the current display name of the player in legacy String format.
     * * @return The display name as a String.
     * @deprecated Use {@link #getDisplayName()} to work with modern Components.
     */
    @Deprecated
    public String getDisplayNameLegacy() {
        return player.getDisplayName();
    }

    /**
     * Attempts to add an ItemStack to the player's inventory.
     * If the inventory is full, the remaining items are dropped at the player's location.
     * * @param itemStack The {@link ItemStack} to give or drop.
     * @return {@code true} if all items were successfully added to the inventory;
     * {@code false} if any items had to be dropped on the ground.
     */
    public boolean giveOrDropItem(@NotNull ItemStack itemStack) {
        // addItem returns a map of items that didn't fit
        HashMap<Integer, ItemStack> remainingItems = player.getInventory().addItem(itemStack);

        if (!remainingItems.isEmpty()) {
            Location dropLocation = player.getLocation();
            World world = player.getWorld();

            for (ItemStack remaining : remainingItems.values()) {
                world.dropItem(dropLocation, remaining);
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * Internal helper method to parse a MiniMessage string into an Adventure Component.
     *
     * @param input The MiniMessage string to parse.
     * @return The parsed {@link Component}.
     */
    private static @NotNull Component parseComponent(@NotNull String input) {
        return MiniMessage.miniMessage().deserialize(input);
    }
}