package de.peachbiscuit174.peachlib.gui;

import de.peachbiscuit174.peachlib.api.PeachLibAPI;
import de.peachbiscuit174.peachlib.items.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

    private boolean canItemsPlacedInGUI = false;
    private BiConsumer<InventoryCloseEvent, List<ItemStack>> onCloseAction = null;
    private BiConsumer<Integer, List<ItemStack>> onPageSwitchAction = null;
    private final Map<Integer, Consumer<List<ItemStack>>> specificPageActions = new HashMap<>();
    private final Map<UUID, Boolean> pageSwitching = new HashMap<>();

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

    /**
     * Sets whether players are allowed to place their own items into the GUI.
     *
     * @param value {@code true} to allow item placement, {@code false} to deny it.
     * @return The current instance for chaining.
     */
    public PaginatedGUI setCanItemsPlacedInGUI(boolean value) {
        this.canItemsPlacedInGUI = value;
        return this;
    }

    /**
     * Sets the action to be executed when the player completely closes the GUI.
     * <p>
     * Note: This event is only fired upon a real close (e.g., pressing ESC or using a close button).
     * It does not fire when a player switches between pages.
     * </p>
     *
     * @param onCloseAction A {@link BiConsumer} providing the {@link InventoryCloseEvent} and a List of {@link ItemStack}s placed by the player.
     * @return The current instance for chaining.
     */
    public PaginatedGUI setOnClose(BiConsumer<InventoryCloseEvent, List<ItemStack>> onCloseAction) {
        this.onCloseAction = onCloseAction;
        return this;
    }

    /**
     * Sets a general action to execute whenever the player switches away from ANY page.
     *
     * @param onPageSwitchAction A {@link BiConsumer} providing the page number (0-indexed) the player is leaving,
     * and a List of {@link ItemStack}s placed on that page.
     * @return The current instance for chaining.
     */
    public PaginatedGUI setOnPageSwitch(BiConsumer<Integer, List<ItemStack>> onPageSwitchAction) {
        this.onPageSwitchAction = onPageSwitchAction;
        return this;
    }

    /**
     * Sets a specific action to execute when the player switches away from a SPECIFIC page.
     * <p>
     * This action has priority and will override the general {@link #setOnPageSwitch} action for this specific page.
     * </p>
     *
     * @param page   The page number (0-indexed) this action applies to.
     * @param action A {@link Consumer} providing the List of {@link ItemStack}s placed on this page.
     * @return The current instance for chaining.
     */
    public PaginatedGUI setPageAction(int page, Consumer<List<ItemStack>> action) {
        this.specificPageActions.put(page, action);
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

        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        final int currentPage = page;

        // Title format: "Title (1/5)"
        String pageTitle = title + " <dark_gray>(" + (currentPage + 1) + "/" + Math.max(1, totalPages) + ")";
        InventoryGUI gui = new InventoryGUI(rows, pageTitle);

        gui.setCanItemsPlacedInGUI(this.canItemsPlacedInGUI);

        gui.setOnClose(event -> {
            boolean isSwitching = pageSwitching.getOrDefault(player.getUniqueId(), false);
            List<ItemStack> placedItems = gui.getPlayerPlacedItems();

            if (isSwitching) {
                if (specificPageActions.containsKey(currentPage)) {
                    specificPageActions.get(currentPage).accept(placedItems);
                } else if (onPageSwitchAction != null) {
                    onPageSwitchAction.accept(currentPage, placedItems);
                } else {
                    for (ItemStack item : placedItems) {
                        PeachLibAPI.getPlayerManager().getPlayerManagerAPI(player).giveOrDropItem(item);
                    }
                }
            } else {
                if (this.onCloseAction != null) {
                    this.onCloseAction.accept(event, placedItems);
                } else {
                    for (ItemStack item : placedItems) {
                        PeachLibAPI.getPlayerManager().getPlayerManagerAPI(player).giveOrDropItem(item);
                    }
                }
                pageSwitching.remove(player.getUniqueId());
            }
        });

        // 1. Fill Content
        int startIndex = currentPage * slotsPerPage;
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

        // "Previous" Button
        if (prevSlot >= 0 && currentPage > 0) {
            gui.setButton(toolbarStartIdx + prevSlot, new GUIButton(prevIcon.copy(), "prev_page", event -> {
                pageSwitching.put(player.getUniqueId(), true);
                this.open(player, currentPage - 1);
                pageSwitching.put(player.getUniqueId(), false);
            }));
        }

        // "Next" Button
        if (nextSlot >= 0 && currentPage < totalPages - 1) {
            gui.setButton(toolbarStartIdx + nextSlot, new GUIButton(nextIcon.copy(), "next_page", event -> {
                pageSwitching.put(player.getUniqueId(), true);
                this.open(player, currentPage + 1);
                pageSwitching.put(player.getUniqueId(), false);
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