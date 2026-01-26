package de.peachbiscuit174.peachlib.api.player;

import de.peachbiscuit174.peachlib.player.PlayerManaging;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class PlayerManagerAPI {
    private final PlayerManaging playerManaging;

    public PlayerManagerAPI (@NotNull Player player) {
        this.playerManaging = new PlayerManaging(player);
    }

    /**
     * Sends a message to the player using MiniMessage formatting.
     * * @param message The MiniMessage formatted string to send.
     */
    public void sendMessage(@NotNull String message) {
        playerManaging.sendMessage(message);
    }

    /**
     * Sets the display name of the player using MiniMessage formatting.
     * This updates the modern {@link Component} based display name.
     * * @param displayName The MiniMessage formatted string for the display name.
     */
    public void setDisplayName(@NotNull String displayName) {
        playerManaging.setDisplayName(displayName);
    }

    /**
     * Sets the display name using legacy String format.
     * * @param displayNameLegacy The plain text or legacy formatted display name.
     * @deprecated Use {@link #setDisplayName(String)} for modern Component support.
     */
    @Deprecated
    public void setDisplayNameLegacy(@NotNull String displayNameLegacy) {
        playerManaging.setDisplayNameLegacy(displayNameLegacy);
    }

    /**
     * Gets the current display name of the player as a Component.
     * * @return The {@link Component} representing the player's display name.
     */
    public Component getDisplayName() {
        return playerManaging.getDisplayName();
    }

    /**
     * Gets the current display name of the player in legacy String format.
     * * @return The display name as a String.
     * @deprecated Use {@link #getDisplayName()} to work with modern Components.
     */
    @Deprecated
    public String getDisplayNameLegacy() {
        return playerManaging.getDisplayNameLegacy();
    }

    /**
     * Attempts to add an ItemStack to the player's inventory.
     * If the inventory is full, the remaining items are dropped at the player's location.
     * * @param itemStack The {@link ItemStack} to give or drop.
     * @return {@code true} if all items were successfully added to the inventory;
     * {@code false} if any items had to be dropped on the ground.
     */
    public boolean giveOrDropItem(@NotNull ItemStack itemStack) {
        return playerManaging.giveOrDropItem(itemStack);
    }

}
