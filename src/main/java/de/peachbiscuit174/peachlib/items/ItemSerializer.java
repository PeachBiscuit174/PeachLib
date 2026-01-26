package de.peachbiscuit174.peachlib.items;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

/**
 * A utility to serialize and deserialize ItemStacks to Base64 Strings.
 * Useful for storing items in databases or configuration files.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class ItemSerializer{

    /**
     * Serializes an ItemStack to a raw byte array using Paper's native serialization.
     *
     * @param item The item to serialize.
     * @return The byte array representing the item.
     */
    public static byte[] serializeToBytes(@NotNull ItemStack item) {
        return item.serializeAsBytes();
    }

    /**
     * Deserializes a byte array back into an ItemStack.
     *
     * @param bytes The byte array.
     * @return The ItemStack.
     */
    public static @NotNull ItemStack deserializeFromBytes(byte[] bytes) {
        return ItemStack.deserializeBytes(bytes);
    }

    /**
     * Serializes an ItemStack to a Base64 String (Readable text).
     * Ideal for saving in JSON or YAML.
     *
     * @param item The item to serialize.
     * @return A Base64 string representation.
     */
    public static @NotNull String serializeToBase64(@NotNull ItemStack item) {
        byte[] bytes = item.serializeAsBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Deserializes a Base64 String back into an ItemStack.
     *
     * @param base64 The Base64 string.
     * @return The ItemStack, or null if the string is invalid.
     */
    public static @Nullable ItemStack deserializeFromBase64(@NotNull String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(bytes);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}