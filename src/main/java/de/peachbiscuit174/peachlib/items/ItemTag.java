package de.peachbiscuit174.peachlib.items;


import de.peachbiscuit174.peachlib.PeachLib;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ItemTag {
    private static final Plugin plugin = PeachLib.getPlugin();
    private static final Map<String, NamespacedKey> keyCache = new ConcurrentHashMap<>();

    private static NamespacedKey getKey(String key) {
        return keyCache.computeIfAbsent(key, k ->
                new NamespacedKey(plugin, k.toLowerCase().replace(" ", "_"))
        );
    }

    public static ItemStack setItemTag (ItemStack itemStack, String key) {
        if (itemStack != null && itemStack.getType().isItem()) {
            if (key != null) {
                if (!isItemTag(itemStack, key)) {
                    ItemMeta meta = itemStack.getItemMeta();
                    if (meta != null) {
                        NamespacedKey namespacedKey = getKey(key);
                        if (namespacedKey != null) {
                            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.BOOLEAN, true);
                            itemStack.setItemMeta(meta);
                        }
                    }
                }
            }
        }
        return itemStack;
    }

    public static boolean isItemTag (ItemStack itemStack, String key) {
        boolean check = false;
        if (itemStack != null && itemStack.getType().isItem()) {
            if (key != null) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    NamespacedKey namespacedKey = getKey(key);
                    if (namespacedKey != null) {
                        boolean value = meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.BOOLEAN);
                        if (value) {
                            check = true;
                        }
                    }
                }
            }
        }
        return check;
    }

    public static List<NamespacedKey> getAllKeysfromItem(ItemStack itemStack) {
        List<NamespacedKey> return_list = new ArrayList<>();
        if (itemStack != null && itemStack.getType().isItem()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                return_list.addAll(meta.getPersistentDataContainer().getKeys());
            }
        }
        return return_list;
    }

    public static ItemStack removeItemTag(ItemStack itemStack, String key) {
        if (itemStack != null && itemStack.getType().isItem()) {
            if (key != null) {
                if (isItemTag(itemStack, key)) {
                    ItemMeta meta = itemStack.getItemMeta();
                    if (meta != null) {
                        NamespacedKey namespacedKey = getKey(key);
                        if (namespacedKey != null) {
                            meta.getPersistentDataContainer().remove(namespacedKey);
                            itemStack.setItemMeta(meta);
                        }
                    }
                }
            }
        }


        return itemStack;
    }

    public static ItemStack setItemStringTag(ItemStack itemStack, String key, String value) {
        if (itemStack == null || !itemStack.getType().isItem() || key == null || value == null) return itemStack;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = getKey(key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public static boolean isItemStringTag (ItemStack itemStack, String key) {
        boolean check = false;
        if (itemStack != null && itemStack.getType().isItem()) {
            if (key != null) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    NamespacedKey namespacedKey = getKey(key);
                    if (namespacedKey != null) {
                        boolean value = meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.STRING);
                        if (value) {
                            check = true;
                        }
                    }
                }
            }
        }
        return check;
    }


    public static String getItemStringTag(ItemStack itemStack, String key) {
        if (itemStack == null || !itemStack.getType().isItem() || key == null) return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = getKey(key);
            if (meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.STRING)) {
                return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
            }
        }
        return null;
    }
}
