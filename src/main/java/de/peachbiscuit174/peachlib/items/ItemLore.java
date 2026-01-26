package de.peachbiscuit174.peachlib.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


/**
 * A helper class to easily build item lore (descriptions) using MiniMessage.
 * <p>
 * This class uses method chaining to allow for a fluent API experience.
 * </p>
 *
 * @author peachbiscuit174
 * @version 1.0.0
 */
public class ItemLore {

    private final List<Component> lines = new ArrayList<>();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * Adds a new line to the lore.
     *
     * @param text The text to add (supports MiniMessage tags).
     * @return This ItemLore instance for method chaining.
     */
    public @NotNull ItemLore add(@NotNull String text) {
        this.lines.add(MM.deserialize(text));
        return this;
    }

    /**
     * Adds an empty line to the lore for spacing.
     *
     * @return This ItemLore instance for method chaining.
     */
    public @NotNull ItemLore space() {
        this.lines.add(Component.empty());
        return this;
    }

    /**
     * Builds the final list of components.
     *
     * @return A list of {@link Component} representing the lore.
     */
    public @NotNull List<Component> build() {
        return new ArrayList<>(this.lines);
    }
}
