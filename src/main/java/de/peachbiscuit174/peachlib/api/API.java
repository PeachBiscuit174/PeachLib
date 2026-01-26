package de.peachbiscuit174.peachlib.api;

import de.peachbiscuit174.peachlib.api.managers.GUIManager;
import de.peachbiscuit174.peachlib.api.managers.ItemsManager;
import de.peachbiscuit174.peachlib.api.managers.PlayerManager;
import de.peachbiscuit174.peachlib.api.managers.SchedulerManager;

/**
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class API {
    private static final ItemsManager itemsManager = new ItemsManager();

    public static ItemsManager getItemsManager() {
        return itemsManager;
    }

    private static final SchedulerManager schedulerManager = new SchedulerManager();

    public static SchedulerManager getSchedulerManager() {
        return schedulerManager;
    }

    private static final PlayerManager playerManager = new PlayerManager();

    public static PlayerManager getPlayerManager() {
        return playerManager;
    }

    private static final GUIManager GUIManager = new GUIManager();

    public static GUIManager getGUIManager() {
        return GUIManager;
    }
}