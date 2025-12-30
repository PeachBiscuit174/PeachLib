package de.peachbiscuit174.peachpaperlib.api;

import de.peachbiscuit174.peachpaperlib.api.managers.ItemsManager;

public class API {
    private static final ItemsManager itemsManager = new ItemsManager();

    public static ItemsManager getItemsManager() {
        return itemsManager;
    }


}