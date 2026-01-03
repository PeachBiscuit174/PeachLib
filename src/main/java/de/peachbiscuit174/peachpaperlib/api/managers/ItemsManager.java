package de.peachbiscuit174.peachpaperlib.api.managers;

import de.peachbiscuit174.peachpaperlib.api.items.CustomHeadsAPI;
import de.peachbiscuit174.peachpaperlib.api.items.ItemBuilderAPI;
import de.peachbiscuit174.peachpaperlib.items.ItemLore;

/**
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class ItemsManager {

    private final ItemBuilderAPI itemBuilderAPI = new ItemBuilderAPI();
    public ItemBuilderAPI getNewItemBuilderAPI() {
        return itemBuilderAPI;
    }

    public ItemLore getNewItemLore() {
        return new ItemLore();
    }

    private final CustomHeadsAPI headUtils = new CustomHeadsAPI();
    public CustomHeadsAPI getCustomHeadsAPI() {
        return headUtils;
    }
}