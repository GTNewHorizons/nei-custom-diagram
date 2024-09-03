package com.github.dcysteine.neicustomdiagram.util.bartworks;

import java.util.Optional;

import net.minecraft.item.ItemStack;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;

import bartworks.system.material.Werkstoff;
import bartworks.system.material.WerkstoffLoader;
import gregtech.api.enums.OrePrefixes;

public final class BartWorksOreDictUtil {

    // Static class.
    private BartWorksOreDictUtil() {}

    public static Optional<ItemComponent> getComponent(OrePrefixes prefix, Werkstoff werkstoff) {
        if (!werkstoff.hasGenerationFeature(prefix)) {
            return Optional.empty();
        }

        Optional<ItemStack> itemStackOptional = Optional
                .ofNullable(WerkstoffLoader.getCorrespondingItemStackUnsafe(prefix, werkstoff, 1));
        return itemStackOptional.map(ItemComponent::create);
    }
}
