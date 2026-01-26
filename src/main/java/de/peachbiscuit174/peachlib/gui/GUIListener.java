package de.peachbiscuit174.peachlib.gui;

import de.peachbiscuit174.peachlib.api.player.PlayerManagerAPI;
import de.peachbiscuit174.peachlib.items.ItemTag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles security and interaction logic for {@link InventoryGUI}.
 * Now integrates with {@link PlayerManagerAPI} to give items directly.
 */
public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();

        // Security check: Block movement if protected
        if (clickedItem != null && ItemTag.isItemTag(clickedItem, InventoryGUI.PROTECTED_TAG)) {
            event.setCancelled(true);
        }

        // GUI Logic
        if (event.getInventory().getHolder() instanceof InventoryGUI gui) {
            GUIButton button = gui.getButtons().get(event.getRawSlot());

            if (button != null) {
                // Feature: Automatically give item to player
                if (button.isGiveToPlayerOnClick()) {
                    PlayerManagerAPI api = new PlayerManagerAPI(player);
                    // Build a fresh item from the button's builder (clean, no GUI tags)
                    ItemStack cleanItem = button.getItemBuilder().build();
                    api.giveOrDropItem(cleanItem);
                }

                // Execute custom click logic
                button.onClick(event);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof InventoryGUI gui)) {
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < event.getInventory().getSize()) {
                event.setCancelled(true);
                break;
            }
        }
    }
}