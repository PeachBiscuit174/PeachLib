package de.peachbiscuit174.peachpaperlib.other;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.LocalDate;

/**
 * All-in-one class that manages holiday greetings with dynamic placeholders.
 * Handles configuration, date calculation, and automatic year replacement.
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class HolidayGreetingListener implements Listener {

    private final CustomConfig2 configWrapper;
    private final FileConfiguration config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Constructor: Initializes the config and sets default holiday messages.
     */
    public HolidayGreetingListener() {
        this.configWrapper = new CustomConfig2("holidays.yml");
        this.config = configWrapper.getConfig();

        setupDefaults();
    }

    /**
     * Adds default messages with the %year% placeholder to the config.
     */
    private void setupDefaults() {
        config.addDefault("enabled", true);

        // Added %year% placeholder for automatic updates
        config.addDefault("messages.new_year", "<yellow><bold>Happy New Year %year%!</bold></yellow> Welcome to a fresh start!");
        config.addDefault("messages.valentines_day", "<red>Happy Valentine's Day %year%!</red> <dark_red>❤</dark_red>");
        config.addDefault("messages.st_patricks_day", "<green>Happy St. Patrick's Day %year%!</green> <dark_green>☘</dark_green>");
        config.addDefault("messages.easter", "<green>Happy Easter %year%!</green> Have a wonderful day!");
        config.addDefault("messages.halloween", "<gold>Happy Halloween %year%!</gold> <dark_purple>☠</dark_purple>");
        config.addDefault("messages.christmas", "<red>Merry Christmas %year%!</red> <green>Enjoy the holidays!</green>");
        config.addDefault("messages.new_year_eve", "<gold>Happy New Year's Eve %year%!</gold> Celebrate safely!");

        config.options().copyDefaults(true);
        configWrapper.save();
    }

    /**
     * Handles the player join event and processes placeholders.
     * @param event The PlayerJoinEvent.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        LocalDate today = LocalDate.now();
        String holidayKey = getHolidayKey(today);

        if (holidayKey != null) {
            String message = config.getString("messages." + holidayKey);
            if (message != null && !message.isEmpty()) {
                // Automatically replace %year% with the current year (e.g., 2025)
                String processedMessage = message.replace("%year%", String.valueOf(today.getYear()));

                player.sendMessage(miniMessage.deserialize(processedMessage));
            }
        }
    }

    /**
     * Identifies the holiday based on month and day.
     * @param date The current date.
     * @return The message key for the config.
     */
    private String getHolidayKey(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 1 && day == 1) return "new_year";
        if (month == 2 && day == 14) return "valentines_day";
        if (month == 3 && day == 17) return "st_patricks_day";
        if (isEasterSunday(date)) return "easter";
        if (month == 10 && day == 31) return "halloween";
        if (month == 12 && (day >= 24 && day <= 26)) return "christmas";
        if (month == 12 && day == 31) return "new_year_eve";

        return null;
    }

    /**
     * Gauss's Easter Algorithm to determine Easter Sunday.
     * @param date The current date.
     * @return true if it is Easter Sunday.
     */
    private boolean isEasterSunday(LocalDate date) {
        int year = date.getYear();
        int a = year % 19;
        int b = year % 4;
        int c = year % 7;
        int k = year / 100;
        int p = (13 + 8 * k) / 25;
        int q = k / 4;
        int m = (15 - p + k - q) % 30;
        int n = (4 + k - q) % 7;
        int d = (19 * a + m) % 30;
        int e = (2 * b + 4 * c + 6 * d + n) % 7;
        int easterDay = 22 + d + e;

        LocalDate easterDate = (easterDay > 31)
                ? LocalDate.of(year, 4, easterDay - 31)
                : LocalDate.of(year, 3, easterDay);

        return date.equals(easterDate);
    }
}