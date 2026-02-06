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
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        if (ItemTag.isItemTag(clickedItem, InventoryGUI.PROTECTED_TAG)) {
            event.setCancelled(true);
        }

        if (event.getInventory().getHolder() instanceof InventoryGUI gui) {

            String actionID = ItemTag.getItemStringTag(clickedItem, InventoryGUI.GUI_ID_TAG);
            if (actionID != null) {
                GUIButton button = gui.getButtonWithID(actionID);
                if (button != null) {
                    if (button.isGiveToPlayerOnClick()) {
                        PlayerManagerAPI api = new PlayerManagerAPI(player);

                        ItemStack cleanItem = button.getItemBuilder().build();
                        api.giveOrDropItem(cleanItem);
                    }

                    button.onClick(event);
                }
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