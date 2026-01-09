package de.peachbiscuit174.peachpaperlib.api.gui;

import de.peachbiscuit174.peachpaperlib.gui.GUIButton;
import de.peachbiscuit174.peachpaperlib.gui.InventoryGUI;
import de.peachbiscuit174.peachpaperlib.items.ItemBuilder;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * API for creating and managing modern Inventory GUIs.
 * <p>
 * This API provides access to the {@link InventoryGUI} system, allowing for
 * row-based inventory creation, automated item protection, and easy item giving.
 * </p>
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class InventoryGUIAPI {

    /**
     * Creates a new InventoryGUI instance.
     *
     * @param rows             The number of rows for the inventory (1-6).
     * @param titleMiniMessage The title of the inventory (supports MiniMessage).
     * @return A new {@link InventoryGUI} instance.
     */
    public InventoryGUI createGUI(int rows, @NotNull String titleMiniMessage) {
        return new InventoryGUI(rows, titleMiniMessage);
    }

    /**
     * Creates a standard GUIButton with a specific action.
     *
     * @param itemBuilder The visual representation of the button.
     * @param actionId    A unique identifier for this button action.
     * @param clickAction The logic to execute when the button is clicked.
     * @return A new {@link GUIButton} instance.
     */
    public GUIButton createButton(@NotNull ItemBuilder itemBuilder, @NotNull String actionId, @Nullable Consumer<InventoryClickEvent> clickAction) {
        return new GUIButton(itemBuilder, actionId, clickAction);
    }

    /**
     * Creates a GUIButton that is automatically given to the player when clicked.
     * <p>
     * This uses the internal giveOrDrop logic to ensure the player receives
     * a clean version of the item (without GUI protection tags).
     * </p>
     *
     * @param itemBuilder The visual representation of the item.
     * @param actionId    A unique identifier for this button.
     * @param clickAction An optional additional action to execute.
     * @return A new {@link GUIButton} configured to give items on click.
     */
    public GUIButton createGiveawayButton(@NotNull ItemBuilder itemBuilder, @NotNull String actionId, @Nullable Consumer<InventoryClickEvent> clickAction) {
        return new GUIButton(itemBuilder, actionId, clickAction).giveToPlayerOnClick(true);
    }

    /**
     * Creates a placeholder button (no click action).
     *
     * @param itemBuilder The visual representation of the placeholder.
     * @param actionId    A unique identifier (e.g., "background").
     * @return A new decorative {@link GUIButton}.
     */
    public GUIButton createPlaceholder(@NotNull ItemBuilder itemBuilder, @NotNull String actionId) {
        return new GUIButton(itemBuilder, actionId, null);
    }
}