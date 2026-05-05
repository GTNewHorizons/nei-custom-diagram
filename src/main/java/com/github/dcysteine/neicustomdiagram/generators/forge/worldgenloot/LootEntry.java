package com.github.dcysteine.neicustomdiagram.generators.forge.worldgenloot;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;

final class LootEntry {

    final ItemComponent item;
    final int min;
    final int max;
    final int weight;

    LootEntry(ItemComponent item, int min, int max, int weight) {
        this.item = item;
        this.min = min;
        this.max = max;
        this.weight = weight;
    }
}
