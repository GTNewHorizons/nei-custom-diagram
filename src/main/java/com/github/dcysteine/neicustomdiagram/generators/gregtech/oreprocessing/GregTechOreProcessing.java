package com.github.dcysteine.neicustomdiagram.generators.gregtech.oreprocessing;

import com.github.bartimaeusnek.bartworks.system.material.Werkstoff;
import com.github.dcysteine.neicustomdiagram.api.Lang;
import com.github.dcysteine.neicustomdiagram.api.Logger;
import com.github.dcysteine.neicustomdiagram.api.Registry;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGenerator;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroupInfo;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.Component;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.matcher.ComponentDiagramMatcher;
import com.github.dcysteine.neicustomdiagram.util.bartworks.BartWorksOreDictUtils;
import com.github.dcysteine.neicustomdiagram.util.gregtech.GregTechOreDictUtils;
import com.google.common.collect.ImmutableList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import net.minecraft.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Generates ore processing diagrams for GregTech ores. */
public final class GregTechOreProcessing implements DiagramGenerator {
    public static final ItemComponent ICON =
            GregTechOreDictUtils.getComponent(OrePrefixes.ore, Materials.Aluminium)
                    .orElse(ItemComponent.create(Block.getBlockFromName("iron_ore"), 0).get());

    private static final ImmutableList<OrePrefixes> OTHER_ORE_PREFIXES = ImmutableList.of(
            OrePrefixes.oreBlackgranite, OrePrefixes.oreRedgranite, OrePrefixes.oreMarble,
            OrePrefixes.oreBasalt, OrePrefixes.oreNetherrack, OrePrefixes.oreNether,
            OrePrefixes.oreDense, OrePrefixes.oreRich, OrePrefixes.oreNormal, OrePrefixes.oreSmall,
            OrePrefixes.orePoor, OrePrefixes.oreEndstone, OrePrefixes.oreEnd);

    private final DiagramGroupInfo info;
    private final LabelHandler labelHandler;
    private final LayoutHandler layoutHandler;

    public GregTechOreProcessing(String groupId) {
        this.info =
                DiagramGroupInfo.create(
                        Lang.GREGTECH_ORE_PROCESSING.trans("groupname"),
                        groupId, ICON, 1);

        this.labelHandler = new LabelHandler();
        this.layoutHandler = new LayoutHandler(this.info, this.labelHandler);
    }

    public DiagramGroupInfo info() {
        return info;
    }

    public DiagramGroup generate() {
        labelHandler.initialize();
        layoutHandler.initialize();

        ComponentDiagramMatcher.Builder matcherBuilder = ComponentDiagramMatcher.builder();

        for (Materials material : Materials.getAll()) {
            if ((material.mTypes & 8) == 0) {
                // Bit 3 is the flag controlling whether ores get generated.
                // So if it's off, skip this material.
                continue;
            }

            List<Component> rawOres =
                    GregTechOreDictUtils.getAllComponents(OrePrefixes.ore, material);
            if (rawOres.isEmpty()) {
                continue;
            }

            OTHER_ORE_PREFIXES.forEach(
                    prefix ->
                            rawOres.addAll(
                                    GregTechOreDictUtils.getAllComponents(prefix, material)));

            buildDiagram(matcherBuilder, rawOres);
        }

        if (Registry.ModIds.isModLoaded(Registry.ModIds.BARTWORKS)) {
            for (Werkstoff werkstoff : Werkstoff.werkstoffHashSet) {
                Optional<ItemComponent> rawOre =
                        BartWorksOreDictUtils.getComponent(OrePrefixes.ore, werkstoff);
                if (!rawOre.isPresent()) {
                    continue;
                }

                List<Component> rawOres = new ArrayList<>();
                rawOres.add(rawOre.get());

                OTHER_ORE_PREFIXES.forEach(
                        prefix ->
                                BartWorksOreDictUtils.getComponent(prefix, werkstoff)
                                        .ifPresent(rawOres::add));

                buildDiagram(matcherBuilder, rawOres);
            }
        }

        return new DiagramGroup(info, matcherBuilder.build());
    }

    private void buildDiagram(
            ComponentDiagramMatcher.Builder matcherBuilder, List<Component> rawOres) {
        DiagramBuilder diagramBuilder = new DiagramBuilder(layoutHandler, labelHandler, rawOres);
        diagramBuilder.buildDiagram(matcherBuilder);

        Logger.GREGTECH_ORE_PROCESSING.debug("Generated diagram [{}]", rawOres.get(0));
    }
}