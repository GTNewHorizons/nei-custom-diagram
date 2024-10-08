package com.github.dcysteine.neicustomdiagram.util.gregtech5;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.Component;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;

public final class GregTechOreDictUtil {

    // Static class.
    private GregTechOreDictUtil() {}

    public static ItemComponent getComponent(ItemList item) {
        return ItemComponent.create(item.get(1));
    }

    public static Optional<ItemComponent> getComponent(OrePrefixes prefix, Materials material) {
        Optional<ItemStack> itemStackOptional = Optional.ofNullable(GTOreDictUnificator.get(prefix, material, 1));
        return itemStackOptional.map(ItemComponent::create);
    }

    public static List<ItemComponent> getAllComponents(OrePrefixes prefix, Materials material) {
        List<ItemStack> itemStacks = GTOreDictUnificator.getOres(prefix, material);
        return itemStacks.stream().map(ItemComponent::create).collect(Collectors.toList());
    }

    /**
     * Returns the unified version of {@code component}, or just returns {@code component} if it couldn't be unified or
     * isn't an item.
     */
    public static Component unify(Component component) {
        if (component.type() != Component.ComponentType.ITEM) {
            return component;
        }

        return ItemComponent.create(GTOreDictUnificator.get_nocopy((ItemStack) component.stack()));
    }

    /** Returns a list of everything that unifies into {@code component}. */
    public static List<Component> reverseUnify(Component component) {
        Component unified = unify(component);
        List<Component> results = new ArrayList<>();

        if (unified.type() == Component.ComponentType.ITEM) {
            GTOreDictUnificator.getNonUnifiedStacks(unified.stack())
                    .forEach(itemStack -> results.add(ItemComponent.create(itemStack)));
        } else {
            results.add(unified);
        }

        return results;
    }

    /**
     * GregTech doesn't handle getting item data for fluids or its own fluid display items, so this method will return
     * empty optional for such cases.
     *
     * <p>
     * If you need to get the material for a fluid, first get a filled cell for the fluid, and then call this method on
     * that filled cell.
     */
    public static Optional<ItemData> getItemData(Component component) {
        if (component.type() != Component.ComponentType.ITEM) {
            return Optional.empty();
        }
        return Optional.ofNullable(GTOreDictUnificator.getAssociation((ItemStack) component.stack()));
    }

    /**
     * Returns a list of GregTech associated items for {@code component} (including {@code component} itself), or a list
     * containing just {@code component} if it has no GregTech associated items.
     *
     * <p>
     * Use this method when matching diagrams by component.
     */
    public static List<Component> getAssociatedComponents(Component component) {
        List<Component> results = new ArrayList<>(reverseUnify(component));

        Optional<ItemData> itemDataOptional = getItemData(component);
        if (itemDataOptional.isPresent()) {
            ItemData itemData = itemDataOptional.get();
            itemData.mPrefix.mFamiliarPrefixes.forEach(
                    prefix -> getComponent(prefix, itemData.mMaterial.mMaterial)
                            .ifPresent(c -> results.addAll(reverseUnify(c))));
        }

        return results;
    }
}
