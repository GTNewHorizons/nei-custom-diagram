package com.github.dcysteine.neicustomdiagram.generators.forge.worldgenloot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;

final class LootTable {

    final String name;
    final List<LootEntry> entries;
    final int totalWeight;

    LootTable(String name, List<LootEntry> entries) {
        this.name = EnumChatFormatting.getTextWithoutFormattingCodes(name);
        this.entries = entries;
        this.totalWeight = entries.stream().mapToInt(e -> e.weight).sum();
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    static LootTable fromChestGenHooks(String name, ChestGenHooks hooks) {
        WeightedRandomChestContent[] contents = hooks.getItems(new Random());
        if (contents == null) return null;

        List<LootEntry> entries = new ArrayList<>();
        for (WeightedRandomChestContent c : contents) {
            if (c.theItemId == null) continue;
            entries.add(
                    new LootEntry(
                            ItemComponent.create(c.theItemId),
                            c.theMinimumChanceToGenerateItem,
                            c.theMaximumChanceToGenerateItem,
                            c.itemWeight));
        }
        return new LootTable(name, entries);
    }
}
