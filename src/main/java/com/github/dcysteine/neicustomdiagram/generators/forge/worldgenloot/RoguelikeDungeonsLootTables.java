package com.github.dcysteine.neicustomdiagram.generators.forge.worldgenloot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.main.Lang;

import greymerk.roguelike.dungeon.Dungeon;
import greymerk.roguelike.treasure.loot.ILoot;
import greymerk.roguelike.treasure.loot.Loot;
import greymerk.roguelike.treasure.loot.WeightedRandomLoot;
import greymerk.roguelike.treasure.loot.provider.ItemBase;
import greymerk.roguelike.util.IWeighted;
import greymerk.roguelike.util.WeightedRandomizer;

/**
 * Extracts Roguelike Dungeons loot tables (Loot category × dungeon level) by reading the LootSettings providers via
 * reflection, since there is no public registry API.
 */
final class RoguelikeDungeonsLootTables {

    private static final int LEVELS = 5;

    private RoguelikeDungeonsLootTables() {}

    static List<LootTable> get() {
        ILoot lootProvider = Loot.getLoot();
        List<LootTable> result = new ArrayList<>();

        for (Loot lootType : Loot.values()) {
            for (int level = 0; level < LEVELS; level++) {
                IWeighted<ItemStack> provider = lootProvider.get(lootType, level);
                if (provider == null) continue;

                List<LootEntry> entries = new ArrayList<>();
                extractRecursive(provider, level, entries);

                if (!entries.isEmpty()) {
                    result.add(new LootTable(getLootLabel(lootType.name(), level), entries));
                }
            }
        }

        result.addAll(getConfigLootTables());
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<LootTable> getConfigLootTables() {
        try {
            if (Dungeon.settingsResolver == null) return Collections.emptyList();

            Field settingsField = Dungeon.settingsResolver.getClass().getDeclaredField("settings");
            settingsField.setAccessible(true);
            Map<String, ?> settingsMap = (Map<String, ?>) settingsField.get(Dungeon.settingsResolver);
            if (settingsMap.isEmpty()) return Collections.emptyList();

            List<LootTable> result = new ArrayList<>();
            for (Map.Entry<String, ?> entry : settingsMap.entrySet()) {
                extractDungeonConfigLoot(entry.getKey(), entry.getValue(), result);
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static void extractDungeonConfigLoot(String configKey, Object dungeonSettings, List<LootTable> out) {
        try {
            Object lootRuleManager = getField(dungeonSettings, "lootRules", Object.class);
            if (lootRuleManager == null) return;

            Field rulesField = lootRuleManager.getClass().getDeclaredField("rules");
            rulesField.setAccessible(true);
            List<?> rules = (List<?>) rulesField.get(lootRuleManager);
            if (rules.isEmpty()) return;

            Map<Integer, List<LootEntry>> byLevel = new TreeMap<>();
            for (Object rule : rules) {
                Object item = getField(rule, "item", Object.class);
                // ItemBase subclasses (ItemWeapon, ItemOre, etc.) are already captured
                // by the default Loot.getLoot() extraction — skip them here.
                if (item instanceof ItemBase) continue;
                int level = getIntField(rule, "level");
                extractRecursive(item, level, byLevel.computeIfAbsent(level, k -> new ArrayList<>()));
            }

            for (Map.Entry<Integer, List<LootEntry>> levelEntry : byLevel.entrySet()) {
                if (!levelEntry.getValue().isEmpty()) {
                    out.add(new LootTable(getLootLabel(configKey, levelEntry.getKey()), levelEntry.getValue()));
                }
            }
        } catch (Exception ignored) {}
    }

    private static String getLootLabel(String configKey, int level) {
        final String key = "roguelikedungeons.loot." + configKey.toLowerCase().replaceAll("[^a-z0-9]+", "_");
        final String lootName = Lang.FORGE_WORLDGEN_LOOT.canTranslate(key) ? Lang.FORGE_WORLDGEN_LOOT.trans(key)
                : configKey;
        return Lang.FORGE_WORLDGEN_LOOT.transf("roguelikedungeons.groupname", lootName, level);
    }

    private static void extractRecursive(Object obj, int level, List<LootEntry> out) {
        if (obj instanceof WeightedRandomLoot) {
            extractLeaf((WeightedRandomLoot) obj, out);
        } else if (obj instanceof WeightedRandomizer) {
            extractRandomizer((WeightedRandomizer<?>) obj, level, out);
        } else if (obj != null) {
            // ItemBase subclass — try to find a Map<Integer, WeightedRandomizer> field
            extractItemBase(obj, level, out);
        }
    }

    private static void extractRandomizer(WeightedRandomizer<?> randomizer, int level, List<LootEntry> out) {
        try {
            Field f = WeightedRandomizer.class.getDeclaredField("items");
            f.setAccessible(true);
            List<IWeighted<ItemStack>> items = (List<IWeighted<ItemStack>>) f.get(randomizer);
            for (IWeighted<ItemStack> item : items) {
                extractRecursive(item, level, out);
            }
        } catch (Exception ignored) {}
    }

    private static void extractItemBase(Object provider, int level, List<LootEntry> out) {
        for (Field f : provider.getClass().getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            try {
                Map<?, ?> map = (Map<?, ?>) f.get(provider);
                Object value = map.get(level);
                if (value instanceof IWeighted) {
                    extractRecursive(value, level, out);
                    return;
                }
            } catch (Exception ignored) {}
        }
    }

    private static void extractLeaf(WeightedRandomLoot leaf, List<LootEntry> out) {
        try {
            Item item = getField(leaf, "item", Item.class);
            Block block = getField(leaf, "block", Block.class);
            int damage = getIntField(leaf, "damage");
            int min = getIntField(leaf, "min");
            int max = getIntField(leaf, "max");
            int weight = getIntField(leaf, "weight");

            ItemStack stack = null;
            if (item != null) stack = new ItemStack(item, 1, damage);
            else if (block != null) stack = new ItemStack(block, 1, damage);

            if (stack != null && stack.getItem() != null) {
                out.add(new LootEntry(ItemComponent.create(stack), min, max, weight));
            }
        } catch (Exception ignored) {}
    }

    private static <T> T getField(Object obj, String name, Class<T> type) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return type.cast(f.get(obj));
    }

    private static int getIntField(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.getInt(obj);
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
