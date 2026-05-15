package com.github.dcysteine.neicustomdiagram.generators.forge.worldgenloot;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraftforge.common.ChestGenHooks;

import com.github.dcysteine.neicustomdiagram.api.diagram.Diagram;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGenerator;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroupInfo;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.Component;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.DisplayComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.AllDiagramsButton;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.CustomInteractable;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.Interactable;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Grid;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Layout;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.SlotGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Text;
import com.github.dcysteine.neicustomdiagram.api.diagram.matcher.CustomDiagramMatcher;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip;
import com.github.dcysteine.neicustomdiagram.main.Lang;
import com.github.dcysteine.neicustomdiagram.main.config.DiagramGroupVisibility;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIClientUtils;
import cpw.mods.fml.common.Loader;

/**
 * Generates diagrams showing world-gen chest loot tables registered via Forge's {@link ChestGenHooks}, plus
 * mod-specific compat for mods that bypass the registry.
 */
public final class ForgeWorldgenLoot implements DiagramGenerator {

    public static final ItemComponent ICON = ItemComponent.create(Item.getItemFromBlock(Blocks.chest), 0);

    private static final Layout.SlotGroupKey SLOT_GROUP_KEY = Layout.SlotGroupKey.create("items");
    private static final DecimalFormat CHANCE_FORMAT = new DecimalFormat("##0.##");

    private final DiagramGroupInfo info;

    private ImmutableListMultimap<ItemComponent, Diagram> itemToDiagrams;

    public ForgeWorldgenLoot(String groupId) {
        this.info = DiagramGroupInfo.builder(Lang.FORGE_WORLDGEN_LOOT.trans("groupname"), groupId, ICON, 2)
                .setDefaultVisibility(DiagramGroupVisibility.ALWAYS_SHOWN)
                .setDescription("This diagram displays world-gen loot tables registered via Forge ChestGenHooks.")
                .build();
    }

    @Override
    public DiagramGroupInfo info() {
        return info;
    }

    @Override
    public DiagramGroup generate() {
        List<LootTable> tables = collectTables();

        ImmutableList.Builder<Diagram> diagramsBuilder = ImmutableList.builder();
        ImmutableListMultimap.Builder<ItemComponent, Diagram> multimapBuilder = ImmutableListMultimap.builder();

        for (LootTable table : tables) {
            if (table.isEmpty()) continue;

            Diagram diagram = buildDiagram(table);
            diagramsBuilder.add(diagram);

            for (LootEntry entry : table.entries()) {
                multimapBuilder.put(entry.item(), diagram);
            }
        }

        itemToDiagrams = multimapBuilder.build();

        return new DiagramGroup(info, new CustomDiagramMatcher(diagramsBuilder.build(), this::getDiagrams));
    }

    private List<LootTable> collectTables() {
        List<LootTable> tables = new ArrayList<>();

        for (Map.Entry<String, ChestGenHooks> entry : getChestInfo().entrySet()) {
            String name = entry.getKey();

            if (Lang.FORGE_WORLDGEN_LOOT.canTranslate("forge.loot." + name)) {
                name = Lang.FORGE_WORLDGEN_LOOT.trans("forge.loot." + name);
            }
            LootTable table = LootTable.fromChestGenHooks(name, entry.getValue());
            if (table != null) tables.add(table);
        }

        if (Loader.isModLoaded("Roguelike")) {
            tables.addAll(RoguelikeDungeonsLootTables.get());
        }

        if (Loader.isModLoaded("TwilightForest")) {
            tables.addAll(TwilightForestLootTables.get());
        }

        return tables;
    }

    private List<Diagram> getDiagrams(Interactable.RecipeType unused, Component component) {
        if (component.type() != Component.ComponentType.ITEM) return ImmutableList.of();
        return itemToDiagrams.get((ItemComponent) component);
    }

    private Diagram buildDiagram(LootTable table) {
        Diagram.Builder builder = Diagram.builder().addLayout(buildLayout(table.name(), table.entries().size()));
        Diagram.Builder.SlotGroupAutoSubBuilder slots = builder.autoInsertIntoSlotGroup(SLOT_GROUP_KEY);

        for (LootEntry entry : table.entries()) {
            slots.insertIntoNextSlot(
                    DisplayComponent.builder(entry.item()).setStackSize(entry.max())
                            .setAdditionalTooltip(buildTooltip(entry, table.totalWeight())).build());
        }

        return builder.build();
    }

    private Tooltip buildTooltip(LootEntry entry, int totalWeight) {
        Tooltip.Builder builder = Tooltip.builder().setFormatting(Tooltip.INFO_FORMATTING);

        if (entry.min() != entry.max()) {
            builder.addTextLine(Lang.FORGE_WORLDGEN_LOOT.transf("amountrange", entry.min(), entry.max()));
        } else {
            builder.addTextLine(Lang.FORGE_WORLDGEN_LOOT.transf("amount", entry.max()));
        }

        if (totalWeight > 0) {
            double pct = (double) entry.weight() / totalWeight * 100.0;
            builder.addTextLine(Lang.FORGE_WORLDGEN_LOOT.transf("chance", CHANCE_FORMAT.format(pct)));
        }

        return builder.build();
    }

    private Layout buildLayout(String name, int itemCount) {
        final String croppedName = NEIClientUtils.cropText(GuiDraw.fontRenderer, name, Grid.TOTAL_WIDTH - 22);
        final String displayName = formatLootTableName(croppedName);
        final int displayNameWidth = GuiDraw.getStringWidth(croppedName);
        final int labelX = displayNameWidth > Grid.TOTAL_WIDTH - 40 ? 11 : 0;
        final Text labelText = Text.builder(displayName, Grid.GRID.grid(6, 0).translate(labelX, 6), Grid.Direction.N)
                .build();
        final int cols = 9;
        final int rows = Math.max(1, (itemCount + cols - 1) / cols);

        return Layout.builder().addInteractable(new AllDiagramsButton(info, Grid.GRID.grid(0, 0))).addInteractable(
                CustomInteractable.builder(labelText).setTooltip(Tooltip.create(name, Tooltip.SLOT_FORMATTING)).build())
                .putSlotGroup(
                        SLOT_GROUP_KEY,
                        SlotGroup.builder(cols, rows, Grid.GRID.grid(6, 2), Grid.Direction.S).build())
                .setPaddingBottom(4).build();
    }

    private String formatLootTableName(String name) {
        if (Lang.FORGE_WORLDGEN_LOOT.canTranslate("loottitleformat")) {
            return Lang.FORGE_WORLDGEN_LOOT.transf("loottitleformat", name);
        }
        return name;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ChestGenHooks> getChestInfo() {
        try {
            final Field f = ChestGenHooks.class.getDeclaredField("chestInfo");
            f.setAccessible(true);
            return (Map<String, ChestGenHooks>) f.get(null);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
