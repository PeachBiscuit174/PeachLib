package de.peachbiscuit174.peachpaperlib.items;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import de.peachbiscuit174.peachpaperlib.api.items.ItemTagAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.http.annotation.Experimental;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A powerful builder for ItemStacks, integrating with ItemLore.
 * With MiniMessage support
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;
    private int customModelDataLegacy = -1;
    private float customModelData = -1;
    private String item_tag;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public ItemBuilder(@NotNull Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
    }

    private ItemBuilder(@NotNull ItemStack itemStack, int customModelDataint, float customModelDatafloat, String item_tag) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
        if (customModelDataint == -1 && customModelDatafloat != -1) {
            this.customModelData = customModelDatafloat;
        } else if (customModelDataint != -1 && customModelDatafloat == -1) {
            this.customModelDataLegacy = customModelDataint;
        } else if (customModelDataint != -1 && customModelDatafloat != -1) {
            this.customModelData = customModelDatafloat;
        }

        if (item_tag != null) {
            this.item_tag = item_tag;
        }

    }

    /**
     * Creates a deep copy of the current ItemBuilder state.
     * <p>
     * This is useful if you want to create multiple variations of an item
     * based on a single template.
     * </p>
     *
     * @return A new ItemBuilder instance with identical data.
     */
    public @NotNull ItemBuilder copy() {
        if (customModelData != -1 || customModelDataLegacy != -1 || item_tag != null) {
            return new ItemBuilder(this.buildWithoutData(), customModelDataLegacy, customModelData, item_tag);
        } else {
            return new ItemBuilder(this.build());
        }
    }

    /**
     * Sets the display name of the item.
     *
     * @param displayName The name.
     * @return The current ItemBuilder instance.
     */
    public ItemBuilder setDisplayName(@NotNull String displayName) {
        if (itemMeta != null) {
            itemMeta.displayName(MM.deserialize(displayName).decoration(TextDecoration.ITALIC, false));
        }
        return this;
    }

    /**
     * Integrates the ItemLore class directly.
     *
     * @param itemLore The ItemLore instance to use.
     * @return The current ItemBuilder instance.
     */
    public ItemBuilder lore(@NotNull ItemLore itemLore) {
        if (itemMeta != null) {
            List<Component> lore_list = itemLore.build();
            List<Component> finalLore = new ArrayList<>();
            for (Component lore : lore_list) {
                lore = lore.decoration(TextDecoration.ITALIC, false);
                finalLore.add(lore);
            }

            itemMeta.lore(finalLore);
        }
        return this;
    }

    /**
     * Sets the lore using a list of existing {@link Component}s.
     * <p>
     * Note: If you want to use MiniMessage formatting, the components should be
     * deserialized before passing them to this method.
     * </p>
     *
     * @param components A list of {@link Component}s to be used as lore.
     * @return The current ItemBuilder instance;
     */
    public ItemBuilder lore(@NotNull List<Component> components) {
        if (itemMeta != null) {
            List<Component> finalLore = new ArrayList<>();
            for (Component lore : components) {
                lore = lore.decoration(TextDecoration.ITALIC, false);
                finalLore.add(lore);
            }

            itemMeta.lore(finalLore);
        }
        return this;
    }

    /**
     *
     * @param enchantment The Enchantment to Enchant
     * @param level The Level of The Enchantment
     * @param ignoreLevelRestriction If the restriction of the Maximum Enchantment Level of Minecraft should be ignored
     @return The current ItemBuilder instance.
     */
    public ItemBuilder enchant(@NotNull Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
        if (itemMeta != null) {
            itemMeta.addEnchant(enchantment, level, ignoreLevelRestriction);
        }
        return this;
    }

    /**
     * Adds ItemFlags to the item.
     *
     * @param flags The flags to add.
     * @return The current ItemBuilder instance.
     */
    public ItemBuilder flags(@NotNull ItemFlag... flags) {
        if (itemMeta != null) {
            itemMeta.addItemFlags(flags);
        }
        return this;
    }

    /**
     * Sets whether the item should be unbreakable.
     *
     * @param unbreakable True if the item should not break.
     * @return The current ItemBuilder instance.
     */
    public ItemBuilder unbreakable(boolean unbreakable) {
        if (itemMeta != null) {
            itemMeta.setUnbreakable(unbreakable);
        }
        return this;
    }

    /**
     * Applies a custom model data ID to the item if it does not already have one.
     * <p>
     * This method utilizes the modern CustomModelDataComponent
     * The float value provided serves as the primary identifier for resource pack overrides.
     * This method should not be used when using the other deprecated setCustomModelData method from this ItemBuilder Instance
     * </p>
     * @param customModelData The numerical ID (int) to be used for the custom model.
     * @return The current ItemBuilder instance.
     */
    @Experimental
    public ItemBuilder setCustomModelData(float customModelData) {
        if (customModelData >= 0) {
            this.customModelData = customModelData;
        }
        return this;
    }

    /**
     * Applies a custom model data ID to the item if it does not already have one.
     * <p>
     * This method utilizes the deprecated setCustomModelData method
     * The int value provided serves as the primary identifier for resource pack overrides.
     * This method should not be used when using the other modern setCustomModelData method from this ItemBuilder Instance
     * </p>
     * @param customModelData The numerical ID (int) to be used for the custom model.
     * @return The current ItemBuilder instance.
     */
    @Deprecated
    public ItemBuilder setCustomModelData(int customModelData) {
        if (customModelData >= 0) {
            this.customModelDataLegacy = customModelData;
        }
        return this;
    }

    /**
     * Allows editing of specialized {@link ItemMeta} sub-interfaces (e.g., {@link org.bukkit.inventory.meta.LeatherArmorMeta},
     * {@link org.bukkit.inventory.meta.SkullMeta}, or {@link org.bukkit.inventory.meta.BookMeta}).
     * <p>
     * This method internally checks if the current ItemMeta is an instance of the provided class.
     * If compatible, the provided {@link Consumer} is executed with the casted meta object.
     * </p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * builder.editMeta(LeatherArmorMeta.class, meta -> meta.setColor(Color.RED));
     * </pre>
     *
     * @param <T>       The type of ItemMeta to be edited.
     * @param metaClass The class object of type T (e.g., {@code SkullMeta.class}).
     * @param consumer  A {@link Consumer} containing the logic to modify the meta.
     * @return The current {@link ItemBuilder} instance for method chaining.
     */
    public <T extends ItemMeta> ItemBuilder editMeta(@NotNull Class<T> metaClass, @NotNull Consumer<T> consumer) {
        if (metaClass.isInstance(itemMeta)) {
            consumer.accept(metaClass.cast(itemMeta));
        }
        return this;
    }

    /**
     * Assigns a specific tag to the item for identification via the {@link ItemTagAPI}.
     * <p>
     * If the item does not already possess this tag, it will be applied.
     * This is useful for filtering or identifying custom items programmatically.
     * </p>
     *
     * @param item_tag The unique string identifier to be assigned as a tag.
     * Must not be null (enforced by {@link NotNull}).
     * @return The current {@link ItemBuilder} instance to allow for method chaining (fluent API).
     * @see ItemTagAPI
     */
    public ItemBuilder setItemTag(@NotNull String item_tag) {
        this.item_tag = item_tag;
        return this;
    }

    /**
     * Builds the final ItemStack.
     *
     * @return The finished {@link ItemStack}.
     */
    public @NotNull ItemStack build() {
        if (customModelDataLegacy == -1 && customModelData != -1) {
            itemStack.setItemMeta(itemMeta);
            if (!itemStack.hasData(DataComponentTypes.CUSTOM_MODEL_DATA)) {
                itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(customModelData).addFlag(true).build());
            }
        } else if (customModelDataLegacy != -1 && customModelData == -1) {
            if (itemMeta != null) {
                if (!itemMeta.hasCustomModelData()) {
                    itemMeta.setCustomModelData(customModelDataLegacy);
                }
            }
            itemStack.setItemMeta(itemMeta);
        } else if (customModelDataLegacy != -1 && customModelData != -1) {
            itemStack.setItemMeta(itemMeta);
            if (!itemStack.hasData(DataComponentTypes.CUSTOM_MODEL_DATA)) {
                itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(customModelData).addFlag(true).build());
            }
        }

        if (item_tag != null) {
            ItemTag.setItemTag(itemStack, item_tag);
        }

        return itemStack;
    }

    private @NotNull ItemStack buildWithoutData() {
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }
}