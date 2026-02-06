package de.peachbiscuit174.peachlib.gui;

import de.peachbiscuit174.peachlib.items.ItemBuilder;
import de.peachbiscuit174.peachlib.items.ItemTag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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
    private final Map<Integer, GUIButton> slotMap = new HashMap<>();
    private final Map<String, GUIButton> idMap = new HashMap<>();
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private String[] shape;
    private final Map<Character, GUIButton> charMapping = new HashMap<>();

    /**
     * The tag key used to identify items that should not be removed from the GUI.
     */
    public static final String PROTECTED_TAG = "gui_protected_pl";
    public static final String GUI_ID_TAG = "gui_action_id_pl";

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
     * Replaces existing buttons!
     *
     * @param slot   The inventory slot (0 to size-1).
     * @param button The {@link GUIButton} to place.
     * @return The current instance for fluent chaining.
     */
    public InventoryGUI setButton(int slot, @NotNull GUIButton button) {

        slotMap.put(slot, button);

        idMap.put(button.getActionId(), button);

        updateSlot(slot);
        return this;
    }

    /**
     * Remove a button from a specific slot and from the entire GUI
     * @param slot The Inventory Slot
     * @return The current instance for fluent chaining.
     */
    public InventoryGUI removeButton(int slot) {
        GUIButton btn = slotMap.remove(slot);
        if (btn != null) {
            idMap.remove(btn.getActionId()); // Auch aus ID-Map löschen
            inventory.setItem(slot, null);
        }
        return this;
    }

    /**
     * Moves a button from one to another slot, it overwrites existing buttons/placeholders!
     * @param fromSlot The Slot where the button is that should move.
     * @param toSlot The Slot where the button should move to.
     * @return The current instance for fluent chaining.
     */
    public InventoryGUI moveButton(int fromSlot, int toSlot) {
        GUIButton button = slotMap.get(fromSlot);

        if (button != null) {
            slotMap.remove(fromSlot);
            inventory.setItem(fromSlot, null);

            setButton(toSlot, button);
        }
        return this;
    }

    /**
     * Get a Button with it's actionID
     * @param actionId actionID from button to get.
     * @return The Requested Button or null if it not exist.
     */
    public @Nullable GUIButton getButtonWithID(@NotNull String actionId) {
        return idMap.get(actionId);
    }

    /**
     * Get all Buttons from the Map with the Buttons actionID's
     * @return All Buttons from idMap.
     */
    public Collection<GUIButton> getAllButtonsFromIdMap() {
        return idMap.values();
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
     * @return The current instance for fluent chaining.
     */
    public InventoryGUI updateSlot(int slot) {
        GUIButton button = slotMap.get(slot);
        if (button != null) {
            ItemStack itemStack = button.getItemBuilder().build();

            // Apply tags directly via ItemTag class to support multiple tags
            ItemTag.setItemTag(itemStack, PROTECTED_TAG);
            ItemTag.setItemStringTag(itemStack, GUI_ID_TAG, button.getActionId());

            inventory.setItem(slot, itemStack);
        }
        return this;
    }

    /**
     * Shorthand method to set a decorative item without a click action.
     *
     * @param slot    The slot to place the placeholder.
     * @param builder The ItemBuilder for the appearance.
     * @param id      The identification tag.
     * @return The current instance for fluent chaining.
     */
    public InventoryGUI setPlaceholder(int slot, @NotNull ItemBuilder builder, @NotNull String id) {
        setButton(slot, new GUIButton(builder, id, null));
        return this;
    }

    /**
     * Fills all currently empty slots in the inventory with a placeholder.
     *
     * @param builder The appearance of the filler item.
     * @param id      The identification tag for all filler items.
     * @return The current instance for fluent chaining.
     */
    public InventoryGUI fillEmptySlots(@NotNull ItemBuilder builder, @NotNull String id) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                setPlaceholder(i, builder.copy(), id);
            }
        }
        return this;
    }

    /**
     * Defines the visual layout of the GUI using a pattern of strings.
     * Each string represents a row in the inventory.
     *
     * @param rows The layout pattern (e.g., "#########", "#  X  #").
     * @return The current instance for fluent chaining.
     */
    public InventoryGUI shape(String... rows) {
        this.shape = rows;
        applyShape();
        return this;
    }

    /**
     * Maps a character used in the {@link #shape(String...)} method to a specific GUIButton.
     *
     * @param key    The character key used in the shape pattern.
     * @param button The button to be placed at every occurrence of the key.
     * @return The current instance for fluent chaining.
     */
    public InventoryGUI map(char key, GUIButton button) {
        this.charMapping.put(key, button);
        applyShape();
        return this;
    }

    /**
     * Maps a character used in the {@link #shape(String...)} method to a decorative
     * item (placeholder) without a click action.
     * <p>
     * This is a shorthand for {@link #map(char, GUIButton)} and is ideal for
     * background items or borders.
     * </p>
     *
     * @param key     The character key used in the shape pattern.
     * @param builder The {@link ItemBuilder} defining the appearance of the placeholder.
     * @param id      A unique identification tag for the item.
     * @return The current instance for fluent chaining.
     */
    public InventoryGUI mapPlaceholder(char key, @NotNull ItemBuilder builder, @NotNull String id) {
        this.charMapping.put(key, new GUIButton(builder, id, null));
        applyShape();
        return this;
    }

    /**
     * Internally processes the shape and mapping to populate the inventory with buttons.
     */
    private void applyShape() {
        if (shape == null || shape.length == 0) return;

        int maxRowsInInv = inventory.getSize() / 9;
        for (int row = 0; row < Math.min(maxRowsInInv, shape.length); row++) {
            String rowString = shape[row];
            for (int col = 0; col < Math.min(9, rowString.length()); col++) {
                char c = rowString.charAt(col);
                if (charMapping.containsKey(c)) {
                    int slot = (row * 9) + col;
                    setButton(slot, charMapping.get(c));
                }
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
    public Map<Integer, GUIButton> getButtons() { return slotMap; }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }
}