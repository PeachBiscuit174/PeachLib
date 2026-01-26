package de.peachbiscuit174.peachlib.api.managers;

import de.peachbiscuit174.peachlib.api.gui.InventoryGUIAPI;

public class GUIManager {

    private final InventoryGUIAPI inventoryGUIAPI = new InventoryGUIAPI();

    public InventoryGUIAPI getInventoryGUIAPI() {
        return inventoryGUIAPI;
    }

}
