package de.peachbiscuit174.peachlib.Commands;

import de.peachbiscuit174.peachlib.api.API;
import de.peachbiscuit174.peachlib.api.gui.InventoryGUIAPI;
import de.peachbiscuit174.peachlib.api.items.ItemBuilderAPI;
import de.peachbiscuit174.peachlib.api.player.PlayerManagerAPI;
import de.peachbiscuit174.peachlib.gui.GUIButton;
import de.peachbiscuit174.peachlib.gui.InventoryGUI;
import de.peachbiscuit174.peachlib.configstuff.ConfigData;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PeachLibSettings implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("Only Players can Execute this Command");
            return true;
        }

        InventoryGUIAPI guiApi = API.getGUIManager().getInventoryGUIAPI();
        ItemBuilderAPI itemBuilderAPI = API.getItemsManager().getNewItemBuilderAPI();
        PlayerManagerAPI playerManagerAPI = API.getPlayerManager().getPlayerManagerAPI(player);

        if (args.length == 0) {
            openSettingsGUI(player, guiApi, itemBuilderAPI);
            return true;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("toggleAutoUpdate")) {
                ConfigData.toggleAutoUpdateStatus();
                if (ConfigData.getAutoUpdateStatus()) {
                    playerManagerAPI.sendMessage("<green>Auto Update Activated :D");
                } else {
                    playerManagerAPI.sendMessage("<red>Auto Update Deactivated :(");
                }
            } else if (args[0].equalsIgnoreCase("reloadConfig")) {
                ConfigData.reloadData();
                playerManagerAPI.sendMessage("<green>Config reloaded :D");
            } else if (args[0].equalsIgnoreCase("toggleAllowSnapshotUpdates")) {
                ConfigData.toggleAllowSnapshotUpdates();
                if (ConfigData.isAllowSnapshotUpdates()) {
                    playerManagerAPI.sendMessage("<green>Allow Snapshot Updates Activated!");
                } else {
                    playerManagerAPI.sendMessage("<red>Allow Snapshot Updates Deactivated!");
                }
            } else {
                playerManagerAPI.sendMessage("<red>" + args[0] + " is not a valid input for 1 argument!");
            }
            return true;
        }

        if (args.length == 2) {
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
            } else if (arg1.equalsIgnoreCase("setAllowSnapshotUpdates")) {
                if (arg2.equalsIgnoreCase("true")) {
                    if (ConfigData.isAllowSnapshotUpdates()) {
                        playerManagerAPI.sendMessage("<green>Allow Snapshot Updates is already Active :D");
                    } else {
                        ConfigData.setAllowSnapshotUpdates(true);
                        playerManagerAPI.sendMessage("<green>Allow Snapshot Updates Activated :D");
                    }
                } else if (arg2.equalsIgnoreCase("false")) {
                    if (!ConfigData.isAllowSnapshotUpdates()) {
                        playerManagerAPI.sendMessage("<red>Allow Snapshot Updates is already Deactivated :(");
                    } else {
                        ConfigData.setAllowSnapshotUpdates(false);
                        playerManagerAPI.sendMessage("<red>Allow Snapshot Updates Deactivated :(");
                    }
                } else {
                    playerManagerAPI.sendMessage("<red>" + arg2 + " is not a valid input!");
                    playerManagerAPI.sendMessage("<green> Use 'true' or 'false' as argument!");
                }
            } else {
                playerManagerAPI.sendMessage("<red>" + arg1 + " is not a valid input for 2 arguments!");
            }
            return true;
        }

        playerManagerAPI.sendMessage("<red> Too many arguments!");
        return true;
    }

    private void openSettingsGUI(Player player, InventoryGUIAPI guiApi, ItemBuilderAPI itemBuilderAPI) {
        boolean autoupdateStatus = ConfigData.getAutoUpdateStatus();
        Material autoUpdateButtonMaterial = Material.WATER_BUCKET;
        String autoUpdateButtonDispayNameString = "<rainbow>Auto Update: <green>";
        if (!autoupdateStatus) {
            autoUpdateButtonMaterial = Material.BUCKET;
            autoUpdateButtonDispayNameString = "<rainbow>Auto Update: <red>";
        }
        GUIButton autoUpdateButton = guiApi.createButton(
                itemBuilderAPI.builder(autoUpdateButtonMaterial)
                        .setDisplayName(autoUpdateButtonDispayNameString + autoupdateStatus),
                "toggleAutoUpdate",
                e -> {
                    if (e.getInventory().getHolder() instanceof InventoryGUI gui) {
                        ConfigData.toggleAutoUpdateStatus();
                        GUIButton button = gui.getButtonWithID("toggleAutoUpdate");
                        boolean autoupdateStatus2 = ConfigData.getAutoUpdateStatus();
                        if (button != null) {
                            if (autoupdateStatus2) {
                                button.setItemBuilder(button.getItemBuilder().setDisplayName("<rainbow>Auto Update: <green>" + autoupdateStatus2).changeMaterial(Material.WATER_BUCKET));
                            } else {
                                button.setItemBuilder(button.getItemBuilder().setDisplayName("<rainbow>Auto Update: <red>" + autoupdateStatus2).changeMaterial(Material.BUCKET));
                            }
                            gui.updateSlot(e.getRawSlot());
                        }
                    }
                }
        );

        boolean isAllowSnapshotUpdates = ConfigData.isAllowSnapshotUpdates();
        Material allowSnapshotUpdatesButtonMaterial = Material.TORCH;
        String allowSnapshotUpdatesButtonDisplayNameString = "<red>Allow Snapshot Updates: <red>false";
        if (isAllowSnapshotUpdates) {
            allowSnapshotUpdatesButtonMaterial = Material.REDSTONE_TORCH;
            allowSnapshotUpdatesButtonDisplayNameString = "<red>Allow Snapshot Updates: <green>true";
        }
        GUIButton allowSnapshotUpdatesButton = guiApi.createButton(itemBuilderAPI.builder(allowSnapshotUpdatesButtonMaterial)
                .setDisplayName(allowSnapshotUpdatesButtonDisplayNameString).lore(API.getItemsManager().getNewItemLore()
                        .add("Applies only if Auto Update is enabled")
                        .space()
                        .add("Warning: Snapshots may be unstable and/or contain bugs!")), "toggleAllowSnapshotUpdates",
                e -> {
                    if (e.getInventory().getHolder() instanceof InventoryGUI gui) {
                        ConfigData.toggleAllowSnapshotUpdates();
                        GUIButton button = gui.getButtonWithID("toggleAllowSnapshotUpdates");
                        boolean isAllowSnapshotUpdates2 = ConfigData.isAllowSnapshotUpdates();
                        if (button != null) {
                            if (isAllowSnapshotUpdates2) {
                                button.setItemBuilder(button.getItemBuilder().setDisplayName("<red>Allow Snapshot Updates: <green>true").changeMaterial(Material.REDSTONE_TORCH));
                            } else {
                                button.setItemBuilder(button.getItemBuilder().setDisplayName("<red>Allow Snapshot Updates: <red>false").changeMaterial(Material.TORCH));
                            }
                            gui.updateSlot(e.getRawSlot());
                        }
                    }

        });

        guiApi.createGUI(1, "<red>Settings")
                .setButton(0, autoUpdateButton)
                .setButton(1, allowSnapshotUpdatesButton)
                .fillEmptySlots(itemBuilderAPI.builder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName(" "), "placeholder")
                .open(player);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        ArrayList<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("toggleAutoUpdate");
            list.add("setAutoUpdate");
            list.add("reloadConfig");
            list.add("toggleAllowSnapshotUpdates");
            list.add("setAllowSnapshotUpdates");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setAutoUpdate")) {
                list.add("true");
                list.add("false");
            } else if (args[0].equalsIgnoreCase("setAllowSnapshotUpdates")) {
                list.add("true");
                list.add("false");
            }

        }
        return StringUtil.copyPartialMatches(args[args.length - 1], list, new ArrayList<>());
    }
}