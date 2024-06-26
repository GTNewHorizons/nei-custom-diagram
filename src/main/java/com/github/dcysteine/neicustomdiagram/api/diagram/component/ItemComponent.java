package com.github.dcysteine.neicustomdiagram.api.diagram.component;

import java.util.Comparator;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.Interactable;
import com.github.dcysteine.neicustomdiagram.api.draw.Draw;
import com.github.dcysteine.neicustomdiagram.api.draw.Point;
import com.github.dcysteine.neicustomdiagram.main.config.ConfigOptions;
import com.google.auto.value.AutoValue;

import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;

@AutoValue
public abstract class ItemComponent implements Component {

    public static final Comparator<ItemComponent> COMPARATOR = Comparator
            .<ItemComponent, Integer>comparing(c -> Item.getIdFromItem(c.item())).thenComparing(ItemComponent::damage)
            .thenComparing(c -> c.nbtWrapper().orElse(null), ImmutableNbtWrapper.COMPARATOR);

    public static final int DEFAULT_STACK_SIZE = 1;

    /**
     * Helper method for retrieving the raw item damage of an {@code ItemStack}.
     *
     * <p>
     * This method is better than calling {@link ItemStack#getItemDamage()}, because that method could be overridden
     * with a custom implementation of {@link Item#getDamage(ItemStack)} and therefore not actually return the raw item
     * damage.
     */
    public static int getItemDamage(ItemStack itemStack) {
        return Items.feather.getDamage(itemStack);
    }

    public static ItemComponent create(Item item, int damage, Optional<NBTTagCompound> nbt) {
        if (item.isDamageable()) {
            return new AutoValue_ItemComponent(item, 0, nbt.map(ImmutableNbtWrapper::create));
        } else {
            return new AutoValue_ItemComponent(item, damage, nbt.map(ImmutableNbtWrapper::create));
        }
    }

    public static ItemComponent create(Item item, int damage) {
        return create(item, damage, Optional.empty());
    }

    /** NBT will be discarded. Use {@link #createWithNbt(ItemStack)} if you want NBT. */
    public static ItemComponent create(ItemStack itemStack) {
        return create(itemStack.getItem(), getItemDamage(itemStack));
    }

    public static ItemComponent createWithNbt(ItemStack itemStack) {
        return create(itemStack.getItem(), getItemDamage(itemStack), Optional.ofNullable(itemStack.stackTagCompound));
    }

    public static ItemComponent createWithNbt(ItemStack itemStack, NBTTagCompound nbt) {
        return create(itemStack.getItem(), getItemDamage(itemStack), Optional.of(nbt));
    }

    public static Optional<ItemComponent> create(Block block, int damage) {
        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            return Optional.empty();
        } else {
            return Optional.of(create(Item.getItemFromBlock(block), damage));
        }
    }

    public abstract Item item();

    public abstract int damage();

    @Override
    public abstract Optional<ImmutableNbtWrapper> nbtWrapper();

    public int itemId() {
        return Item.getIdFromItem(item());
    }

    public boolean hasWildcardDamage() {
        return damage() == OreDictionary.WILDCARD_VALUE;
    }

    @Override
    public ComponentType type() {
        return ComponentType.ITEM;
    }

    @Override
    public ItemComponent withNbt(NBTTagCompound nbt) {
        return create(item(), damage(), Optional.of(nbt));
    }

    @Override
    public ItemComponent withoutNbt() {
        return create(item(), damage(), Optional.empty());
    }

    @Override
    public ItemStack stack() {
        return stack(DEFAULT_STACK_SIZE);
    }

    @Override
    public ItemStack stack(int stackSize) {
        ItemStack itemStack = new ItemStack(item(), stackSize, damage());
        nbt().ifPresent(n -> itemStack.stackTagCompound = n);
        return itemStack;
    }

    @Override
    public String description() {
        if (ConfigOptions.SHOW_IDS.get()) {
            return String.format("%s (#%d/%d)", stack().getDisplayName(), itemId(), damage());
        } else {
            return stack().getDisplayName();
        }
    }

    @Override
    public void interact(Interactable.RecipeType recipeType) {
        ItemStack itemStack = stack();
        switch (recipeType) {
            case CRAFTING:
                GuiCraftingRecipe.openRecipeGui("item", itemStack);
                break;

            case USAGE:
                GuiUsageRecipe.openRecipeGui("item", itemStack);
                break;
        }
    }

    @Override
    public void draw(Point pos) {
        Draw.drawItem(stack(), pos);
    }

    @Override
    public final String toString() {
        return description();
    }

    @Override
    public int compareTo(Component other) {
        if (other == null) {
            return 1;
        }

        if (other instanceof ItemComponent) {
            return COMPARATOR.compare(this, (ItemComponent) other);
        } else {
            return type().compareTo(other.type());
        }
    }
}
