package com.github.dcysteine.neicustomdiagram.generators.forge.worldgenloot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.main.Lang;

import twilightforest.TFTreasure;
import twilightforest.TFTreasureItem;
import twilightforest.TFTreasureTable;

/**
 * Extracts Twilight Forest loot tables from the 22 static {@link TFTreasure} instances, producing one {@link LootTable}
 * per chest type per rarity tier (common / uncommon / rare / ultra-rare).
 */
final class TwilightForestLootTables {

    private static final String[] DISPLAY_NAMES = { "hill1", "hill2", "hill3", "hedgemaze", "labyrinth_room",
            "labyrinth_deadend", "tower_room", "tower_library", "basement", "labyrinth_vault", "darktower_cache",
            "darktower_key", "darktower_boss", "tree_cache", "stronghold_cache", "stronghold_room", "stronghold_boss",
            "aurora_cache", "aurora_room", "aurora_boss", "troll_garden", "troll_vault" };

    private TwilightForestLootTables() {}

    static List<LootTable> get() {
        List<LootTable> result = new ArrayList<>();

        for (String chestKey : DISPLAY_NAMES) {
            TFTreasure treasure = getTreasure(chestKey);
            if (treasure == null) continue;

            addTable(result, getChestLabel(chestKey, "common"), treasure.common);
            addTable(result, getChestLabel(chestKey, "uncommon"), treasure.uncommon);
            addTable(result, getChestLabel(chestKey, "rare"), treasure.rare);
            addTable(result, getChestLabel(chestKey, "ultrarare"), treasure.ultrarare);
        }

        return result;
    }

    private static String getChestLabel(String chestKey, String levelKey) {
        final String loot = Lang.FORGE_WORLDGEN_LOOT.trans("twilightforest.loot." + chestKey);
        final String level = Lang.FORGE_WORLDGEN_LOOT.trans("twilightforest.level." + levelKey);

        return Lang.FORGE_WORLDGEN_LOOT.transf("twilightforest.groupname", loot, level);
    }

    private static TFTreasure getTreasure(String chestKey) {
        try {
            Field f = TFTreasure.class.getDeclaredField(chestKey);
            return (TFTreasure) f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static void addTable(List<LootTable> result, String name, TFTreasureTable table) {
        if (table == null || table.isEmpty()) return;

        List<TFTreasureItem> items = getList(table);
        if (items == null || items.isEmpty()) return;

        List<LootEntry> entries = new ArrayList<>();
        for (TFTreasureItem item : items) {
            ItemStack stack = getItemStack(item);
            int rarity = item.getRarity();
            if (stack == null || stack.getItem() == null) continue;
            // stackSize in TFTreasureItem is the max quantity (getItemStack randomises 1..stackSize)
            entries.add(new LootEntry(ItemComponent.create(stack), 1, stack.stackSize, rarity));
        }

        if (!entries.isEmpty()) {
            result.add(new LootTable(name, entries));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<TFTreasureItem> getList(TFTreasureTable table) {
        try {
            Field f = TFTreasureTable.class.getDeclaredField("list");
            f.setAccessible(true);
            return (List<TFTreasureItem>) f.get(table);
        } catch (Exception e) {
            return null;
        }
    }

    private static ItemStack getItemStack(TFTreasureItem item) {
        try {
            Field f = TFTreasureItem.class.getDeclaredField("itemStack");
            f.setAccessible(true);
            return (ItemStack) f.get(item);
        } catch (Exception e) {
            return null;
        }
    }
}
