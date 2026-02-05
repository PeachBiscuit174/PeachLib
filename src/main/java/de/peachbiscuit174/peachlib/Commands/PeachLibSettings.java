package de.peachbiscuit174.peachlib.Commands;

import de.peachbiscuit174.peachlib.api.API;
import de.peachbiscuit174.peachlib.api.gui.InventoryGUIAPI;
import de.peachbiscuit174.peachlib.api.items.ItemBuilderAPI;
import de.peachbiscuit174.peachlib.api.player.PlayerManagerAPI;
import de.peachbiscuit174.peachlib.gui.GUIButton;
import de.peachbiscuit174.peachlib.gui.InventoryGUI;
import de.peachbiscuit174.peachlib.other.ConfigData;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PeachLibSettings implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (commandSender instanceof Player player) {
            InventoryGUIAPI guiApi = API.getGUIManager().getInventoryGUIAPI();
            ItemBuilderAPI itemBuilderAPI = API.getItemsManager().getNewItemBuilderAPI();
            PlayerManagerAPI playerManagerAPI = API.getPlayerManager().getPlayerManagerAPI(player);
            if (args.length == 0) {
                GUIButton autoUpdateButton = guiApi.createButton(itemBuilderAPI.builder(Material.WATER_BUCKET).setDisplayName("<red>Auto Update: <white>" + ConfigData.getAutoUpdateStatus()), "toggleAutoUpdate", e -> {
                    if (e.getInventory().getHolder() instanceof InventoryGUI gui) {
                        ConfigData.toggleAutoUpdateStatus();
                        gui.getButtons().get(e.getRawSlot()).setItemBuilder(gui.getButtons().get(e.getRawSlot()).getItemBuilder().setDisplayName("<red>Auto Update: <white>" + ConfigData.getAutoUpdateStatus()));
                        gui.updateSlot(e.getRawSlot());
                    }

                });
                guiApi.createGUI(1, "<red>Settings").setButton(0, autoUpdateButton).fillEmptySlots(itemBuilderAPI.builder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName(""), "placeholder").open(player);
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("toggleAutoUpdate")) {
                    ConfigData.toggleAutoUpdateStatus();
                    if (ConfigData.getAutoUpdateStatus()) {
                        playerManagerAPI.sendMessage("<green>Auto Update Activated :D");
                    } else {
                        playerManagerAPI.sendMessage("<red>Auto Update Deactivated :(");
                    }
                } else {
                    playerManagerAPI.sendMessage("<red>" + args[0] + " is not a valid input!");
                }
            } else if (args.length == 2) {
                String arg1 = args[0];
                String arg2 = args[1];
                if (arg1.equalsIgnoreCase("setAutoUpdate")) {
                    if (arg2.equalsIgnoreCase("true")) {
                        if (ConfigData.getAutoUpdateStatus()) {
                            playerManagerAPI.sendMessage("<green>Auto Update is already Active :D");
                        } else {
                            ConfigData.setAutoUpdateStatus(true);
                            playerManagerAPI.sendMessage("<green>Auto Update Activated :D");
                        }
                    } else if (arg2.equalsIgnoreCase("false")) {
                        if (!ConfigData.getAutoUpdateStatus()) {
                            playerManagerAPI.sendMessage("<red>Auto Update is already Deactivated :(");
                        } else {
                            ConfigData.setAutoUpdateStatus(false);
                            playerManagerAPI.sendMessage("<red>Auto Update Deactivated :(");
                        }
                    } else {
                        playerManagerAPI.sendMessage("<red>" + arg2 + " is not a valid input!");
                        playerManagerAPI.sendMessage("<green> Use 'true' or 'false' as argument!");
                    }
                } else {
                    playerManagerAPI.sendMessage("<red>" + arg1 + " is not a valid input!");
                }
            } else {
                playerManagerAPI.sendMessage("<red> To many arguments!");
            }
        } else {
            commandSender.sendMessage("Only Players can Execute this Command");
        }



        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {

        ArrayList<String> list = new ArrayList<>();
        if (args.length == 0) return list;
        if (args.length == 1) {
            list.add("toggleAutoUpdate");
            list.add("setAutoUpdate");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setAutoUpdate")) {
                list.add("true");
                list.add("false");
            }
        }

        ArrayList<String> list2 = new ArrayList<>();
        String aktuelles_argument = args[args.length - 1].toLowerCase();
        for (String string : list) {
            String string1 = string.toLowerCase();
            if (string1.startsWith(aktuelles_argument)) {
                list2.add(string);
            }
        }

        return list2;
    }
}
