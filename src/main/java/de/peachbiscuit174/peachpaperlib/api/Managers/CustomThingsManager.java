package de.peachbiscuit174.peachpaperlib.api.Managers;

import de.peachbiscuit174.peachpaperlib.api.CustomThings.CustomHeadsAPI;
import de.peachbiscuit174.peachpaperlib.custom.ItemLore;

public class CustomThingsManager {

    public ItemLore getNewItemLore() {
        return new ItemLore();
    }

    private final CustomHeadsAPI headUtils = new CustomHeadsAPI();
    public CustomHeadsAPI getCustomHeadUtils() {
        return headUtils;
    }
}