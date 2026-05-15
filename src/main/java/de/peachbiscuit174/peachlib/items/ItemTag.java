package de.peachbiscuit174.peachlib.items;

import de.peachbiscuit174.peachlib.PeachLib;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal utility class for managing PersistentDataContainer tags on ItemStacks.
 * * @author peachbiscuit174
 * @since 1.0.0
 */
public class ItemTag {

    /**
     * Creates a NamespacedKey based on the provided plugin instance.
     * * @param targetPlugin The plugin instance.
     * @param key The string key to normalize.
     * @return The generated NamespacedKey.
     * @throws IllegalArgumentException if the key is null or empty.
     */
    private static NamespacedKey getKey(Plugin targetPlugin, String key) {
        if (key == null || key.isEmpty() || key.isBlank()) {
            throw new IllegalArgumentException("Key must not be null or empty");
        }
        // Normalization during creation
        String normalizedKey = key.toLowerCase().replace(" ", "_");
        return new NamespacedKey(targetPlugin, normalizedKey);
    }

    /**
     * @deprecated Please use the method with the Plugin parameter to avoid namespace collisions.
     */
    @Deprecated
    private static NamespacedKey getKey(String key) {
        return getKey(PeachLib.getPlugin(), key);
    }

    // --- BOOLEAN TAGS ---

    public static ItemStack setItemTag(Plugin plugin, ItemStack itemStack, String key) {
        if (itemStack != null && itemStack.getType().isItem()) {
            if (key != null) {
                if (!isItemTag(plugin, itemStack, key)) {
                    ItemMeta meta = itemStack.getItemMeta();
                    if (meta != null) {
                        NamespacedKey namespacedKey = getKey(plugin, key);
                        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.BOOLEAN, true);
                        itemStack.setItemMeta(meta);
                    }
                }
            }
        }
        return itemStack;
    }

    @Deprecated
    public static ItemStack setItemTag(ItemStack itemStack, String key) {
        return setItemTag(PeachLib.getPlugin(), itemStack, key);
    }

    public static boolean isItemTag(Plugin plugin, ItemStack itemStack, String key) {
        boolean check = false;
        if (itemStack != null && itemStack.getType().isItem()) {
            if (key != null) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    NamespacedKey namespacedKey = getKey(plugin, key);
                    if (meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.BOOLEAN)) {
                        check = true;
                    }
                }
            }
        }
        return check;
    }

    @Deprecated
    public static boolean isItemTag(ItemStack itemStack, String key) {
        return isItemTag(PeachLib.getPlugin(), itemStack, key);
    }

    public static ItemStack removeItemTag(Plugin plugin, ItemStack itemStack, String key) {
        if (itemStack != null && itemStack.getType().isItem()) {
            if (key != null) {
                if (isItemTag(plugin, itemStack, key)) {
                    ItemMeta meta = itemStack.getItemMeta();
                    if (meta != null) {
                        NamespacedKey namespacedKey = getKey(plugin, key);
                        meta.getPersistentDataContainer().remove(namespacedKey);
                        itemStack.setItemMeta(meta);
                    }
                }
            }
        }
        return itemStack;
    }

    @Deprecated
    public static ItemStack removeItemTag(ItemStack itemStack, String key) {
        return removeItemTag(PeachLib.getPlugin(), itemStack, key);
    }

    // --- STRING TAGS ---

    public static ItemStack setItemStringTag(Plugin plugin, ItemStack itemStack, String key, String value) {
        if (itemStack == null || !itemStack.getType().isItem() || key == null || value == null) return itemStack;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = getKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    @Deprecated
    public static ItemStack setItemStringTag(ItemStack itemStack, String key, String value) {
        return setItemStringTag(PeachLib.getPlugin(), itemStack, key, value);
    }

    public static boolean isItemStringTag(Plugin plugin, ItemStack itemStack, String key) {
        boolean check = false;
        if (itemStack != null && itemStack.getType().isItem()) {
            if (key != null) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    NamespacedKey namespacedKey = getKey(plugin, key);
                    if (meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.STRING)) {
                        check = true;
                    }
                }
            }
        }
        return check;
    }

    @Deprecated
    public static boolean isItemStringTag(ItemStack itemStack, String key) {
        return isItemStringTag(PeachLib.getPlugin(), itemStack, key);
    }

    public static String getItemStringTag(Plugin plugin, ItemStack itemStack, String key) {
        if (itemStack == null || !itemStack.getType().isItem() || key == null) return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = getKey(plugin, key);
            if (meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.STRING)) {
                return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
            }
        }
        return null;
    }

    @Deprecated
    public static String getItemStringTag(ItemStack itemStack, String key) {
        return getItemStringTag(PeachLib.getPlugin(), itemStack, key);
    }

    // --- GENERAL UTILS ---

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
}