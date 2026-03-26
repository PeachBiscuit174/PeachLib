package de.peachbiscuit174.peachlib.items;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import de.peachbiscuit174.peachlib.api.items.ItemTagAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.ApiStatus;
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

    private ItemStack itemStack;
    private ItemMeta itemMeta;
    private int amount = 1;
    private int customModelDataLegacy = -1;
    private float customModelData = -1;
    private String item_tag;
    private String displayName = null;
    private List<Component> itemLore = new ArrayList<>();
    private List<String> item_tag_list = new ArrayList<>();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public ItemBuilder(@NotNull Material material) {
        if (material == Material.AIR) material = Material.STONE;
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder(@NotNull ItemStack itemStack) {
        if (itemStack.getType() == Material.AIR) itemStack = new ItemStack(Material.STONE);
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
    }

    private ItemBuilder(@NotNull ItemStack itemStack, int customModelDataint, float customModelDatafloat, String item_tag, List<String> item_tag_list, int amount, String displayName, List<Component> itemLore) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta().clone();
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

        if (item_tag_list != null && !item_tag_list.isEmpty()) {
            this.item_tag_list = new ArrayList<>(item_tag_list);
        }

        this.amount = amount;

        if (displayName != null) {
            this.displayName = displayName;
        }

        if (itemLore != null && !itemLore.isEmpty()) {
            this.itemLore = itemLore;
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
        return new ItemBuilder(this.buildWithoutData(), customModelDataLegacy, customModelData, item_tag, item_tag_list, amount, displayName, itemLore);
    }

    /**
     * Sets the Amount of the Item.
     * @param amount The Amount of the Item.
     * @return The current ItemBuilder instance.
     */
    public ItemBuilder setAmount(int amount) {
        if (amount >= 1 && amount <= 64) {
            this.amount = amount;
        }
        return this;
    }

    /**
     * Sets the display name of the item.
     *
     * @param displayName The name.
     * @return The current ItemBuilder instance.
     */
    public ItemBuilder setDisplayName(@NotNull String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Integrates the ItemLore class directly.
     *
     * @param itemLore The ItemLore instance to use.
     * @return The current ItemBuilder instance.
     */
    public ItemBuilder lore(@NotNull ItemLore itemLore) {
        List<Component> lore_list = itemLore.build();
        List<Component> finalLore = new ArrayList<>();
        for (Component lore : lore_list) {
            lore = lore.decoration(TextDecoration.ITALIC, false);
            finalLore.add(lore);
        }

        this.itemLore = finalLore;
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
        List<Component> finalLore = new ArrayList<>();
        for (Component lore : components) {
            lore = lore.decoration(TextDecoration.ITALIC, false);
            finalLore.add(lore);
        }

        this.itemLore = finalLore;
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
    @ApiStatus.Experimental
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
    @Deprecated
    public ItemBuilder setItemTag(@NotNull String item_tag) {
        this.item_tag = item_tag;
        return this;
    }

    /**
     * Adds a specific tag to the item for identification via the {@link ItemTagAPI}.
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
    public ItemBuilder addItemTag(@NotNull String item_tag) {
        if (!item_tag_list.contains(item_tag)) {
            item_tag_list.add(item_tag);
        }
        return this;
    }

    /**
     * Changes the underlying base {@link ItemStack} of this builder to a clone of the provided item.
     * <p>
     * <b>Important Behavior regarding Builder State:</b>
     * This method replaces the base item and its inherent {@link ItemMeta}.
     * <br><b>The following properties WILL BE OVERWRITTEN (lost)</b> if they were set prior to this call,
     * as they modify the underlying ItemMeta directly:
     * <ul>
     * <li>Enchantments added via {@link #enchant(Enchantment, int, boolean)}</li>
     * <li>ItemFlags added via {@link #flags(ItemFlag...)}</li>
     * <li>Unbreakable state set via {@link #unbreakable(boolean)}</li>
     * <li>Custom meta edits done via {@link #editMeta(Class, Consumer)}</li>
     * </ul>
     * <br><b>The following properties ARE PRESERVED</b> and will be applied to the new item upon {@link #build()}:
     * <ul>
     * <li>Item amount {@link #setAmount(int)}</li>
     * <li>Display name {@link #setDisplayName(String)}</li>
     * <li>Lore {@link #lore(List)} or {@link #lore(ItemLore)}</li>
     * <li>Custom Model Data (modern & legacy) {@link #setCustomModelData(float)}</li>
     * <li>Item Tags {@link #addItemTag(String)}</li>
     * </ul>
     * </p>
     *
     * @param itemStack The new {@link ItemStack} to base this builder on. Must not be null.
     * @return The current {@link ItemBuilder} instance for chaining.
     */
    public ItemBuilder changeItemStack(@NotNull ItemStack itemStack) {
        if (itemStack.getType() == Material.AIR) return this;
        this.itemStack = itemStack.clone();
        ItemMeta meta = this.itemStack.getItemMeta();
        this.itemMeta = (meta != null) ? meta.clone() : null;

        return this;
    }

    /**
     * Changes the underlying base material of this builder by creating a new {@link ItemStack}.
     * <p>
     * <b>Important Behavior regarding Builder State:</b>
     * This method replaces the base item and its inherent {@link ItemMeta}.
     * <br><b>The following properties WILL BE OVERWRITTEN (lost)</b> if they were set prior to this call,
     * as they modify the underlying ItemMeta directly:
     * <ul>
     * <li>Enchantments added via {@link #enchant(Enchantment, int, boolean)}</li>
     * <li>ItemFlags added via {@link #flags(ItemFlag...)}</li>
     * <li>Unbreakable state set via {@link #unbreakable(boolean)}</li>
     * <li>Custom meta edits done via {@link #editMeta(Class, Consumer)}</li>
     * </ul>
     * <br><b>The following properties ARE PRESERVED</b> and will be applied to the new item upon {@link #build()}:
     * <ul>
     * <li>Item amount {@link #setAmount(int)}</li>
     * <li>Display name {@link #setDisplayName(String)}</li>
     * <li>Lore {@link #lore(List)} or {@link #lore(ItemLore)}</li>
     * <li>Custom Model Data (modern & legacy) {@link #setCustomModelData(float)}</li>
     * <li>Item Tags {@link #addItemTag(String)}</li>
     * </ul>
     * </p>
     *
     * @param material The {@link Material} to create the new base item from. Must not be null.
     * @return The current {@link ItemBuilder} instance for chaining.
     */
    public ItemBuilder changeMaterial(@NotNull Material material) {
        if (material == Material.AIR) return this;
        if (this.itemStack.getType() == material) return this;
        this.itemStack = new ItemStack(material);
        ItemMeta meta = this.itemStack.getItemMeta();
        this.itemMeta = (meta != null) ? meta.clone() : null;

        return this;
    }

    /**
     * Builds the final ItemStack.
     *
     * @return The finished {@link ItemStack}.
     */
    public @NotNull ItemStack build() {
        ItemStack buildItemStack = itemStack.clone();
        ItemMeta buildItemMeta = (itemMeta != null) ? itemMeta.clone() : null;
        List<String> build_item_tag_list = new ArrayList<>(item_tag_list);

        if (buildItemMeta != null) {
            if (this.displayName != null) {
                buildItemMeta.displayName(MM.deserialize(this.displayName).decoration(TextDecoration.ITALIC, false));
            }

            if (!this.itemLore.isEmpty()) {
                buildItemMeta.lore(this.itemLore);
            }


            if (customModelData != -1) {
                buildItemStack.setItemMeta(buildItemMeta);
                if (!buildItemStack.hasData(DataComponentTypes.CUSTOM_MODEL_DATA)) {
                    buildItemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(customModelData).addFlag(true).build());
                }
            } else if (customModelDataLegacy != -1) {
                if (!buildItemMeta.hasCustomModelData()) {
                    buildItemMeta.setCustomModelData(customModelDataLegacy);
                }
                buildItemStack.setItemMeta(buildItemMeta);
            } else {
                buildItemStack.setItemMeta(buildItemMeta);
            }
        }

        if (item_tag != null) {
            if (!build_item_tag_list.contains(item_tag)) {
                build_item_tag_list.add(item_tag);
            }
        }

        if (!build_item_tag_list.isEmpty()) {
            for (String key : build_item_tag_list) {
                ItemTag.setItemTag(buildItemStack, key);
            }
        }

        if (amount >= 1 && amount <= 64) {
            buildItemStack.setAmount(amount);
        }

        return buildItemStack;
    }

    private @NotNull ItemStack buildWithoutData() {
        ItemStack buildItemStack = itemStack.clone();
        if (itemMeta != null) {
            buildItemStack.setItemMeta(itemMeta.clone());
        }

        return buildItemStack;
    }
}