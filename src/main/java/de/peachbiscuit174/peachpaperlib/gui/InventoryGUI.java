package de.peachbiscuit174.peachpaperlib.gui;

import de.peachbiscuit174.peachpaperlib.items.ItemBuilder;
import de.peachbiscuit174.peachpaperlib.items.ItemTag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * A highly flexible GUI API for Paper plugins.
 * <p>
 * This class handles the creation of inventories based on rows and manages
 * {@link GUIButton} instances. It automatically applies protection tags to items
 * to prevent players from taking them out of the menu.
 * </p>
 * * <p><b>Example Usage:</b></p>
 * <pre>
 * InventoryGUI gui = new InventoryGUI(3, "&lt;gold&gt;Settings");
 * gui.setButton(13, new GUIButton(new ItemBuilder(Material.DIAMOND), "save", event -> {
 * event.getWhoClicked().sendMessage("Saved!");
 * }));
 * gui.open(player);
 * </pre>
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class InventoryGUI implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, GUIButton> buttons = new HashMap<>();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * The tag key used to identify items that should not be removed from the GUI.
     */
    public static final String PROTECTED_TAG = "gui_protected_ppl";

    /**
     * Creates a new InventoryGUI.
     *
     * @param rows             The number of rows (1-6).
     * @param titleMiniMessage The title using MiniMessage formatting.
     */
    public InventoryGUI(int rows, String titleMiniMessage) {
        int finalRows = Math.max(1, Math.min(6, rows));
        this.inventory = Bukkit.createInventory(this, finalRows * 9, MM.deserialize(titleMiniMessage));
    }

    /**
     * Places a button into a specific slot and applies the necessary tags.
     *
     * @param slot   The inventory slot (0 to size-1).
     * @param button The {@link GUIButton} to place.
     */
    public void setButton(int slot, @NotNull GUIButton button) {
        buttons.put(slot, button);
        updateSlot(slot);
    }

    /**
     * Synchronizes the visual ItemStack in the inventory with the current
     * state of the {@link GUIButton}'s ItemBuilder.
     * <p>
     * This method automatically applies the {@link #PROTECTED_TAG} and the
     * button's specific action ID using the {@link ItemTag} API.
     * </p>
     *
     * @param slot The slot to refresh.
     */
    public void updateSlot(int slot) {
        GUIButton button = buttons.get(slot);
        if (button != null) {
            ItemStack itemStack = button.getItemBuilder().build();

            // Apply tags directly via ItemTag class to support multiple tags
            ItemTag.setItemTag(itemStack, PROTECTED_TAG);
            ItemTag.setItemTag(itemStack, button.getActionId());

            inventory.setItem(slot, itemStack);
        }
    }

    /**
     * Shorthand method to set a decorative item without a click action.
     *
     * @param slot    The slot to place the placeholder.
     * @param builder The ItemBuilder for the appearance.
     * @param id      The identification tag.
     */
    public void setPlaceholder(int slot, @NotNull ItemBuilder builder, @NotNull String id) {
        setButton(slot, new GUIButton(builder, id, null));
    }

    /**
     * Fills all currently empty slots in the inventory with a placeholder.
     *
     * @param builder The appearance of the filler item.
     * @param id      The identification tag for all filler items.
     */
    public void fillEmptySlots(@NotNull ItemBuilder builder, @NotNull String id) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                setPlaceholder(i, builder.copy(), id);
            }
        }
    }

    /**
     * Opens the GUI for the specified player.
     *
     * @param player The player to open the inventory for.
     */
    public void open(@NotNull Player player) {
        player.openInventory(inventory);
    }

    /**
     * Returns a map containing all buttons registered in this GUI.
     *
     * @return A map of slots and their corresponding {@link GUIButton}.
     */
    public Map<Integer, GUIButton> getButtons() { return buttons; }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }
}