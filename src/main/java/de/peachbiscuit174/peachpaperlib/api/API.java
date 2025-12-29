package de.peachbiscuit174.peachpaperlib.api;

import de.peachbiscuit174.peachpaperlib.api.Managers.CustomThingsManager;

public class API {
    private static final CustomThingsManager customThingsManager = new CustomThingsManager();

    public static CustomThingsManager getCustomThingsManager() {
        return customThingsManager;
    }


}