package com.github.dcysteine.neicustomdiagram.generators.forge.worldgenloot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;

import twilightforest.TFTreasure;
import twilightforest.TFTreasureItem;
import twilightforest.TFTreasureTable;

/**
 * Extracts Twilight Forest loot tables from the 22 static {@link TFTreasure} instances, producing one {@link LootTable}
 * per chest type per rarity tier (common / uncommon / rare / ultra-rare).
 */
final class TwilightForestLootTables {

    private static final Map<String, String> DISPLAY_NAMES = new LinkedHashMap<>();

    static {
        DISPLAY_NAMES.put("hill1", "TF: Hill (1)");
        DISPLAY_NAMES.put("hill2", "TF: Hill (2)");
        DISPLAY_NAMES.put("hill3", "TF: Hill (3)");
        DISPLAY_NAMES.put("hedgemaze", "TF: Hedge Maze");
        DISPLAY_NAMES.put("labyrinth_room", "TF: Labyrinth Room");
        DISPLAY_NAMES.put("labyrinth_deadend", "TF: Labyrinth Dead End");
        DISPLAY_NAMES.put("tower_room", "TF: Tower Room");
        DISPLAY_NAMES.put("tower_library", "TF: Tower Library");
        DISPLAY_NAMES.put("basement", "TF: Basement");
        DISPLAY_NAMES.put("labyrinth_vault", "TF: Labyrinth Vault");
        DISPLAY_NAMES.put("darktower_cache", "TF: Dark Tower Cache");
        DISPLAY_NAMES.put("darktower_key", "TF: Dark Tower Key");
        DISPLAY_NAMES.put("darktower_boss", "TF: Dark Tower Boss");
        DISPLAY_NAMES.put("tree_cache", "TF: Tree Cache");
        DISPLAY_NAMES.put("stronghold_cache", "TF: Stronghold Cache");
        DISPLAY_NAMES.put("stronghold_room", "TF: Stronghold Room");
        DISPLAY_NAMES.put("stronghold_boss", "TF: Stronghold Boss");
        DISPLAY_NAMES.put("aurora_cache", "TF: Aurora Cache");
        DISPLAY_NAMES.put("aurora_room", "TF: Aurora Room");
        DISPLAY_NAMES.put("aurora_boss", "TF: Aurora Boss");
        DISPLAY_NAMES.put("troll_garden", "TF: Troll Garden");
        DISPLAY_NAMES.put("troll_vault", "TF: Troll Vault");
    }

    private TwilightForestLootTables() {}

    static List<LootTable> get() {
        List<LootTable> result = new ArrayList<>();

        for (Map.Entry<String, String> nameEntry : DISPLAY_NAMES.entrySet()) {
            TFTreasure treasure = getTreasure(nameEntry.getKey());
            if (treasure == null) continue;

            String displayName = nameEntry.getValue();
            addTable(result, displayName + " / Common", treasure.common);
            addTable(result, displayName + " / Uncommon", treasure.uncommon);
            addTable(result, displayName + " / Rare", treasure.rare);
            addTable(result, displayName + " / Ultra-rare", treasure.ultrarare);
        }

        return result;
    }

    private static TFTreasure getTreasure(String fieldName) {
        try {
            Field f = TFTreasure.class.getDeclaredField(fieldName);
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
