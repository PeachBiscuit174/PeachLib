package de.peachbiscuit174.peachpaperlib.api.managers;

import de.peachbiscuit174.peachpaperlib.api.items.CustomHeadsAPI;
import de.peachbiscuit174.peachpaperlib.items.ItemLore;

public class ItemsManager {

    public ItemLore getNewItemLore() {
        return new ItemLore();
    }

    private final CustomHeadsAPI headUtils = new CustomHeadsAPI();
    public CustomHeadsAPI getCustomHeadsAPI() {
        return headUtils;
    }
}