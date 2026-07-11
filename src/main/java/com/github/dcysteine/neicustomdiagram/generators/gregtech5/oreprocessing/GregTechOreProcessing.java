package com.github.dcysteine.neicustomdiagram.generators.gregtech5.oreprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGenerator;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroupInfo;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.matcher.ComponentDiagramMatcher;
import com.github.dcysteine.neicustomdiagram.main.Lang;
import com.github.dcysteine.neicustomdiagram.main.Logger;
import com.github.dcysteine.neicustomdiagram.main.Mods;
import com.github.dcysteine.neicustomdiagram.util.DiagramUtil;
import com.github.dcysteine.neicustomdiagram.util.bartworks.BartWorksOreDictUtil;
import com.github.dcysteine.neicustomdiagram.util.gregtech5.GregTechOreDictUtil;
import com.google.common.collect.ImmutableList;

import bartworks.system.material.Werkstoff;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.common.ores.GTOreAdapter;
import gregtech.common.ores.GTPPOreAdapter;
import gregtech.common.ores.OreInfo;
import gregtech.common.ores.OreManager;
import gtPlusPlus.core.block.base.BlockBaseOre;
import gtPlusPlus.core.material.Material;

/** Generates ore processing diagrams for GregTech ores. */
public final class GregTechOreProcessing implements DiagramGenerator {

    public static final ItemComponent ICON = computeIcon();

    /**
     * Class-init must never throw: the oredict lookup or the ML-ore-block filter could both come up empty depending on
     * load order or future material changes, so this falls back through progressively weaker guarantees, ending in a
     * vanilla item that is always present.
     */
    private static ItemComponent computeIcon() {
        List<ItemComponent> aluminiumOres = GregTechOreDictUtil.getAllComponents(OrePrefixes.ore, Materials.Aluminium);
        return aluminiumOres.stream().filter(GregTechOreProcessing::isGregTechOreBlock).findFirst()
                .or(() -> aluminiumOres.stream().findFirst())
                .orElseGet(() -> ItemComponent.create(new ItemStack(Blocks.iron_ore)));
    }

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

        if (Mods.BARTWORKS.isLoaded()) {
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

        if (Mods.GT_PLUS_PLUS.isLoaded()) {
            for (Material material : Material.mMaterialMap) {
                ItemStack ore = material.getOre(1);
                ItemStack rawOre = material.getRawOre(1);
                if (ore == null || !isGtPlusPlusOreBlock(ore)) {
                    // Skip non-GT++ ore blocks (e.g. materials merged into a vanilla GT material, whose ore is
                    // already covered by the Materials loop above) to avoid duplicate diagrams.
                    continue;
                }

                buildDiagram(
                        matcherBuilder,
                        ImmutableList.of(ItemComponent.create(ore)),
                        Optional.of(ItemComponent.create(rawOre)));
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
        try (OreInfo<?> oreInfo = OreManager.getOreInfo(itemComponent.stack())) {
            return oreInfo != null;
        }
    }

    /**
     * True for gtPlusPlus ore blocks specifically, whether backed by the legacy {@link BlockBaseOre} class or a
     * MaterialLib-migrated block. Materials merged into a vanilla GT material resolve through {@link GTOreAdapter}
     * instead and are intentionally excluded here (see {@link GTPPOreAdapter} class doc).
     */
    private static boolean isGtPlusPlusOreBlock(ItemStack ore) {
        Block block = Block.getBlockFromItem(ore.getItem());
        return GTPPOreAdapter.INSTANCE.supports(block, ItemComponent.getItemDamage(ore));
    }
}
