package de.peachbiscuit174.peachpaperlib.api.managers;

import de.peachbiscuit174.peachpaperlib.api.player.PlayerManagerAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class PlayerManager {

    public PlayerManagerAPI getPlayerManagerAPI(@NotNull Player player) {
        return new PlayerManagerAPI(player);
    }

}
