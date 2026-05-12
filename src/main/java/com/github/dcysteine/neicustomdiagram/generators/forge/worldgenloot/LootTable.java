package com.github.dcysteine.neicustomdiagram.generators.forge.worldgenloot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;

public record LootTable(String name, List<LootEntry> entries, int totalWeight) {

    public LootTable(String name, List<LootEntry> entries) {
        this(name, entries, entries.stream().mapToInt(LootEntry::weight).sum());
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public static LootTable fromChestGenHooks(String name, ChestGenHooks hooks) {
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
