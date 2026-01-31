package de.peachbiscuit174.peachlib.gui;

import de.peachbiscuit174.peachlib.items.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A flexible system for multi-page inventories.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class PaginatedGUI {

    private final String title;
    private final int rows;
    private final List<GUIButton> contentButtons = new ArrayList<>();

    // Toolbar Configuration
    private final Map<Integer, GUIButton> customToolbarButtons = new HashMap<>();
    private boolean useToolbarBackground = true;

    // Slots (0-8 relative to the toolbar row)
    private int prevSlot = 0;
    private int nextSlot = 8;
    private int closeSlot = -1; // -1 = Disabled by default

    // Icons (Defaults in English)
    private ItemBuilder nextIcon = new ItemBuilder(Material.ARROW).setDisplayName("<green>Next Page »");
    private ItemBuilder prevIcon = new ItemBuilder(Material.ARROW).setDisplayName("<yellow>« Previous Page");
    private ItemBuilder closeIcon = new ItemBuilder(Material.BARRIER).setDisplayName("<red>Close");
    private ItemBuilder backgroundIcon = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName(" ");

    /**
     * Creates a new PaginatedGUI.
     *
     * @param rows             The total number of rows (must be 2-6).
     * (e.g., 6 rows = 5 content rows + 1 toolbar row).
     * @param titleMiniMessage The title of the inventory using MiniMessage formatting.
     */
    public PaginatedGUI(int rows, String titleMiniMessage) {
        // We need at least 2 rows (1 Content + 1 Toolbar)
        this.rows = Math.max(2, Math.min(6, rows));
        this.title = titleMiniMessage;
    }

    // --- Content Management ---

    /**
     * Adds a single button to the content list.
     * Items will be automatically distributed across pages.
     *
     * @param button The {@link GUIButton} to add.
     * @return The current instance for chaining.
     */
    public PaginatedGUI addButton(@NotNull GUIButton button) {
        this.contentButtons.add(button);
        return this;
    }

    /**
     * Adds a list of buttons to the content list.
     *
     * @param buttons The list of {@link GUIButton}s to add.
     * @return The current instance for chaining.
     */
    public PaginatedGUI addButtons(@NotNull List<GUIButton> buttons) {
        this.contentButtons.addAll(buttons);
        return this;
    }

    // --- Toolbar Configuration ---

    /**
     * Sets the slot positions for the navigation buttons in the toolbar.
     * Use {@code -1} to disable a specific button.
     *
     * @param prevSlot  Slot for "Previous Page" (0-8).
     * @param nextSlot  Slot for "Next Page" (0-8).
     * @param closeSlot Slot for "Close" (0-8). Default is -1 (disabled).
     * @return The current instance for chaining.
     */
    public PaginatedGUI setNavigationSlots(int prevSlot, int nextSlot, int closeSlot) {
        this.prevSlot = prevSlot;
        this.nextSlot = nextSlot;
        this.closeSlot = closeSlot;
        return this;
    }

    /**
     * Places a custom button in the toolbar.
     * <p>
     * This button will override the background item. However, if a navigation button
     * (Next/Prev) needs to be displayed at this slot, the navigation button takes priority.
     * </p>
     *
     * @param toolbarSlot The slot in the toolbar (0-8).
     * @param button      The {@link GUIButton} to place.
     * @return The current instance for chaining.
     */
    public PaginatedGUI setToolbarButton(int toolbarSlot, @NotNull GUIButton button) {
        if (toolbarSlot >= 0 && toolbarSlot <= 8) {
            this.customToolbarButtons.put(toolbarSlot, button);
        }
        return this;
    }

    /**
     * Toggles the filler item (background) of the toolbar on or off.
     *
     * @param enabled {@code true} to show the background item, {@code false} to leave empty slots.
     * @return The current instance for chaining.
     */
    public PaginatedGUI setToolbarBackgroundEnabled(boolean enabled) {
        this.useToolbarBackground = enabled;
        return this;
    }

    // --- Visual Customization ---

    /**
     * Sets the visual item for the "Next Page" button.
     * @param builder The {@link ItemBuilder} defining the icon.
     * @return The current instance.
     */
    public PaginatedGUI setNextIcon(@NotNull ItemBuilder builder) {
        this.nextIcon = builder;
        return this;
    }

    /**
     * Sets the visual item for the "Previous Page" button.
     * @param builder The {@link ItemBuilder} defining the icon.
     * @return The current instance.
     */
    public PaginatedGUI setPreviousIcon(@NotNull ItemBuilder builder) {
        this.prevIcon = builder;
        return this;
    }

    /**
     * Sets the visual item for the "Close" button.
     * @param builder The {@link ItemBuilder} defining the icon.
     * @return The current instance.
     */
    public PaginatedGUI setCloseIcon(@NotNull ItemBuilder builder) {
        this.closeIcon = builder;
        return this;
    }

    /**
     * Sets the visual item used as the toolbar background (filler).
     * @param builder The {@link ItemBuilder} defining the background item.
     * @return The current instance.
     */
    public PaginatedGUI setToolbarBackground(@NotNull ItemBuilder builder) {
        this.backgroundIcon = builder;
        return this;
    }

    // --- Logic ---

    /**
     * Opens the GUI for the specified player, starting at page 0.
     *
     * @param player The player to open the inventory for.
     */
    public void open(@NotNull Player player) {
        open(player, 0);
    }

    /**
     * Internal method to open a specific page.
     */
    private void open(@NotNull Player player, int page) {
        int contentRows = rows - 1;
        int slotsPerPage = contentRows * 9;

        int totalItems = contentButtons.size();
        int totalPages = (int) Math.ceil((double) totalItems / slotsPerPage);

        // Safety checks for page bounds
        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        // Title format: "Title (1/5)"
        String pageTitle = title + " <dark_gray>(" + (page + 1) + "/" + Math.max(1, totalPages) + ")";
        InventoryGUI gui = new InventoryGUI(rows, pageTitle);

        // 1. Fill Content
        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            // The slot in the GUI is relative to the start of the page content
            gui.setButton(i - startIndex, contentButtons.get(i));
        }

        // 2. Build Toolbar
        int toolbarStartIdx = contentRows * 9; // Absolute inventory slot where the toolbar begins

        // Step A: Background (if enabled)
        if (useToolbarBackground) {
            for (int i = 0; i < 9; i++) {
                gui.setPlaceholder(toolbarStartIdx + i, backgroundIcon.copy(), "toolbar_bg");
            }
        }

        // Step B: Custom Toolbar Buttons (Override Background)
        for (Map.Entry<Integer, GUIButton> entry : customToolbarButtons.entrySet()) {
            gui.setButton(toolbarStartIdx + entry.getKey(), entry.getValue());
        }

        // Step C: Navigation (Override Custom Buttons & Background where necessary)
        final int finalPage = page;

        // "Previous" Button
        if (prevSlot >= 0 && page > 0) {
            gui.setButton(toolbarStartIdx + prevSlot, new GUIButton(prevIcon.copy(), "prev_page", event -> {
                this.open(player, finalPage - 1);
            }));
        }

        // "Next" Button
        if (nextSlot >= 0 && page < totalPages - 1) {
            gui.setButton(toolbarStartIdx + nextSlot, new GUIButton(nextIcon.copy(), "next_page", event -> {
                this.open(player, finalPage + 1);
            }));
        }

        // "Close" Button (Always shown if slot is set)
        if (closeSlot >= 0) {
            gui.setButton(toolbarStartIdx + closeSlot, new GUIButton(closeIcon.copy(), "close_gui", event -> {
                player.closeInventory();
            }));
        }

        gui.open(player);
    }
}