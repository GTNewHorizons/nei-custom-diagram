package com.github.dcysteine.neicustomdiagram.generators.gregtech5.oreprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGenerator;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroupInfo;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.matcher.ComponentDiagramMatcher;
import com.github.dcysteine.neicustomdiagram.main.Lang;
import com.github.dcysteine.neicustomdiagram.main.Logger;
import com.github.dcysteine.neicustomdiagram.main.Registry;
import com.github.dcysteine.neicustomdiagram.util.DiagramUtil;
import com.github.dcysteine.neicustomdiagram.util.bartworks.BartWorksOreDictUtil;
import com.github.dcysteine.neicustomdiagram.util.gregtech5.GregTechOreDictUtil;
import com.google.common.collect.ImmutableList;

import bartworks.system.material.Werkstoff;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.common.blocks.GTBlockOre;
import gtPlusPlus.core.block.base.BlockBaseOre;
import gtPlusPlus.core.material.Material;

/** Generates ore processing diagrams for GregTech ores. */
public final class GregTechOreProcessing implements DiagramGenerator {

    public static final ItemComponent ICON = GregTechOreDictUtil.getAllComponents(OrePrefixes.ore, Materials.Aluminium)
            .stream().filter(GregTechOreProcessing::isGregTechOreBlock).findFirst().get();

    private static final ImmutableList<OrePrefixes> OTHER_ORE_PREFIXES = ImmutableList.of(
            OrePrefixes.oreBlackgranite,
            OrePrefixes.oreRedgranite,
            OrePrefixes.oreMarble,
            OrePrefixes.oreBasalt,
            OrePrefixes.oreNetherrack,
            OrePrefixes.oreNether,
            OrePrefixes.oreDense,
            OrePrefixes.oreRich,
            OrePrefixes.oreNormal,
            OrePrefixes.oreSmall,
            OrePrefixes.orePoor,
            OrePrefixes.oreEndstone,
            OrePrefixes.oreEnd);

    private final DiagramGroupInfo info;

    private final LabelHandler labelHandler;
    private final LayoutHandler layoutHandler;
    private final RecipeHandler recipeHandler;

    public GregTechOreProcessing(String groupId) {
        this.info = DiagramGroupInfo.builder(Lang.GREGTECH_5_ORE_PROCESSING.trans("groupname"), groupId, ICON, 1)
                // We'll always insert the ore block itself, so require at least 2
                // components to be inserted to be non-empty.
                .setEmptyDiagramPredicate(DiagramUtil.buildEmptyDiagramPredicate(2))
                .setDescription("This diagram displays GregTech ore processing products.").build();

        this.labelHandler = new LabelHandler();
        this.layoutHandler = new LayoutHandler(this.info, this.labelHandler);
        this.recipeHandler = new RecipeHandler();
    }

    @Override
    public DiagramGroupInfo info() {
        return info;
    }

    @Override
    public DiagramGroup generate() {
        labelHandler.initialize();
        layoutHandler.initialize();
        recipeHandler.initialize();

        ComponentDiagramMatcher.Builder matcherBuilder = ComponentDiagramMatcher.builder();

        for (Materials material : Materials.getAll()) {
            if (!material.hasOresItems()) continue;

            List<ItemComponent> rawOres = GregTechOreDictUtil.getAllComponents(OrePrefixes.ore, material);
            if (rawOres.isEmpty()) {
                continue;
            }

            OTHER_ORE_PREFIXES
                    .forEach(prefix -> rawOres.addAll(GregTechOreDictUtil.getAllComponents(prefix, material)));

            Optional<ItemComponent> trueRawOre = GregTechOreDictUtil.getComponent(OrePrefixes.rawOre, material);

            buildDiagram(matcherBuilder, rawOres, trueRawOre);
        }

        if (Registry.ModDependency.BARTWORKS.isLoaded()) {
            for (Werkstoff werkstoff : Werkstoff.werkstoffHashSet) {
                Optional<ItemComponent> rawOre = BartWorksOreDictUtil.getComponent(OrePrefixes.ore, werkstoff);
                Optional<ItemComponent> trueRawOre = BartWorksOreDictUtil.getComponent(OrePrefixes.rawOre, werkstoff);
                if (!rawOre.isPresent()) {
                    continue;
                }

                List<ItemComponent> rawOres = new ArrayList<>();
                rawOres.add(rawOre.get());

                OTHER_ORE_PREFIXES.forEach(
                        prefix -> BartWorksOreDictUtil.getComponent(prefix, werkstoff).ifPresent(rawOres::add));

                buildDiagram(matcherBuilder, rawOres, trueRawOre);
            }
        }

        if (Registry.ModDependency.GT_PLUS_PLUS.isLoaded()) {
            for (Material material : Material.mMaterialMap) {
                ItemStack ore = material.getOre(1);
                if (ore == null || !(Block.getBlockFromItem(ore.getItem()) instanceof BlockBaseOre)) {
                    // Skip non-GT++ ore blocks to avoid duplicate diagrams.
                    continue;
                }

                buildDiagram(matcherBuilder, ImmutableList.of(ItemComponent.create(ore)), Optional.ofNullable(null));
            }
        }

        return new DiagramGroup(info, matcherBuilder.build());
    }

    private void buildDiagram(ComponentDiagramMatcher.Builder matcherBuilder, List<ItemComponent> rawOres,
            Optional<ItemComponent> trueRawOre) {
        DiagramBuilder diagramBuilder = new DiagramBuilder(
                layoutHandler,
                labelHandler,
                recipeHandler,
                rawOres,
                trueRawOre);
        diagramBuilder.buildDiagram(matcherBuilder);

        Logger.GREGTECH_5_ORE_PROCESSING.debug("Generated diagram [{}]", rawOres.get(0));
    }

    static boolean isGregTechOreBlock(ItemComponent itemComponent) {
        Block block = Block.getBlockFromItem(itemComponent.item());
        return block instanceof GTBlockOre;
    }
}
