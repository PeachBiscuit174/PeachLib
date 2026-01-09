package de.peachbiscuit174.peachpaperlib.gui;

import de.peachbiscuit174.peachpaperlib.items.ItemBuilder;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Represents a clickable or decorative element within an {@link InventoryGUI}.
 * * @author peachbiscuit174
 * @since 1.0.0
 */
public class GUIButton {
    private ItemBuilder itemBuilder;
    private final String actionId;
    private final Consumer<InventoryClickEvent> clickAction;
    private boolean giveToPlayerOnClick = false;

    /**
     * Constructs a new GUIButton.
     *
     * @param itemBuilder The builder defining the item's appearance.
     * @param actionId    A unique identification tag for this button.
     * @param clickAction The action to execute on click. Use {@code null} for placeholders.
     */
    public GUIButton(@NotNull ItemBuilder itemBuilder, @NotNull String actionId, @Nullable Consumer<InventoryClickEvent> clickAction) {
        this.itemBuilder = itemBuilder;
        this.actionId = actionId;
        this.clickAction = clickAction;
    }

    /**
     * Sets whether the item should be given to the player's inventory when clicked.
     * <p>
     * Uses the {@code giveOrDropItem} logic from PlayerManagerAPI. The item given
     * will be a clean version without GUI protection tags.
     * </p>
     *
     * @param giveToPlayer True if the player should receive the item.
     * @return The current GUIButton instance for method chaining.
     */
    public GUIButton giveToPlayerOnClick(boolean giveToPlayer) {
        this.giveToPlayerOnClick = giveToPlayer;
        return this;
    }

    /**
     * @return True if this button is configured to give its item to the player.
     */
    public boolean isGiveToPlayerOnClick() {
        return giveToPlayerOnClick;
    }

    /**
     * Returns a <b>copy</b> of the current ItemBuilder.
     *
     * @return A deep copy of the underlying {@link ItemBuilder}.
     */
    public @NotNull ItemBuilder getItemBuilder() {
        return itemBuilder.copy();
    }

    /**
     * Updates the ItemBuilder for this button.
     *
     * @param itemBuilder The new ItemBuilder to use.
     */
    public void setItemBuilder(@NotNull ItemBuilder itemBuilder) {
        this.itemBuilder = itemBuilder;
    }

    public String getActionId() {
        return actionId;
    }

    /**
     * Executes the assigned click action.
     *
     * @param event The {@link InventoryClickEvent} triggered by the player.
     */
    public void onClick(InventoryClickEvent event) {
        if (clickAction != null) {
            clickAction.accept(event);
        }
    }
}