package de.peachbiscuit174.peachlib.api.items;

import de.peachbiscuit174.peachlib.items.ItemSerializer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A utility API to serialize and deserialize ItemStacks to Base64 Strings.
 * Useful for storing items in databases or configuration files.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class ItemSerializerAPI {

    /**
     * Serializes an ItemStack to a raw byte array using Paper's native serialization.
     *
     * @param item The item to serialize.
     * @return The byte array representing the item.
     */
    public byte[] serializeToBytes(@NotNull ItemStack item) {
        return ItemSerializer.serializeToBytes(item);
    }

    /**
     * Deserializes a byte array back into an ItemStack.
     *
     * @param bytes The byte array.
     * @return The ItemStack.
     */
    public @NotNull ItemStack deserializeFromBytes(byte[] bytes) {
        return ItemSerializer.deserializeFromBytes(bytes);
    }

    /**
     * Serializes an ItemStack to a Base64 String (Readable text).
     * Ideal for saving in JSON or YAML.
     *
     * @param item The item to serialize.
     * @return A Base64 string representation.
     */
    public @NotNull String serializeToBase64(@NotNull ItemStack item) {
        return ItemSerializer.serializeToBase64(item);
    }

    /**
     * Deserializes a Base64 String back into an ItemStack.
     *
     * @param base64 The Base64 string.
     * @return The ItemStack, or null if the string is invalid.
     */
    public @Nullable ItemStack deserializeFromBase64(@NotNull String base64) {
        return ItemSerializer.deserializeFromBase64(base64);
    }
}