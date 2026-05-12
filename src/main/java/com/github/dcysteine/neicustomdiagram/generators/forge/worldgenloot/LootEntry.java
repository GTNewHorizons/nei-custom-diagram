package com.github.dcysteine.neicustomdiagram.generators.forge.worldgenloot;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;

public record LootEntry(ItemComponent item, int min, int max, int weight) {}
