package com.github.dcysteine.neicustomdiagram.generators.gregtech5.materialtools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.github.dcysteine.neicustomdiagram.api.diagram.Diagram;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.DisplayComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Layout;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip;
import com.github.dcysteine.neicustomdiagram.main.Lang;
import com.github.dcysteine.neicustomdiagram.util.gregtech5.GregTechDiagramUtil;
import com.github.dcysteine.neicustomdiagram.util.gregtech5.GregTechOreDictUtil;
import com.google.common.collect.ImmutableList;
import com.ruling_0.materiallib.api.Material;

import gregtech.api.enums.OrePrefixes;
import gregtech.api.material.MaterialUtils;

class DiagramFactory {

    private enum MaterialPart {

        TOOL_HEADS(LayoutHandler.SlotGroupKeys.TOOL_PARTS, OrePrefixes.toolHeadSaw, OrePrefixes.toolHeadHammer,
                OrePrefixes.toolHeadFile, OrePrefixes.toolHeadDrill, OrePrefixes.toolHeadChainsaw,
                OrePrefixes.toolHeadWrench, OrePrefixes.toolHeadBuzzSaw),

        TURBINE_BLADE(LayoutHandler.SlotKeys.TURBINE_BLADE, OrePrefixes.turbineBlade);

        private static final MaterialPart[] VALUES = values();
        private final Layout.Key slotKey;
        private final ImmutableList<OrePrefixes> prefixes;

        MaterialPart(Layout.Key slotKey, OrePrefixes... prefixes) {
            this.slotKey = slotKey;
            this.prefixes = ImmutableList.copyOf(prefixes);
        }

        private void insertIntoSlot(Diagram.Builder builder, Material material) {
            if (prefixes.size() == 1) {
                builder.insertIntoSlot((Layout.SlotKey) slotKey, getPrefixComponents(prefixes, material));
            } else {
                builder.autoInsertIntoSlotGroup((Layout.SlotGroupKey) slotKey)
                        .insertEachSafe(getPrefixComponents(prefixes, material));
            }
        }
    }

    private final LayoutHandler layoutHandler;
    private final RecipeHandler recipeHandler;

    DiagramFactory(LayoutHandler layoutHandler, RecipeHandler recipeHandler) {
        this.layoutHandler = layoutHandler;
        this.recipeHandler = recipeHandler;
    }

    Diagram buildDiagram(Material material) {
        Diagram.Builder diagramBuilder = Diagram.builder().addAllLayouts(layoutHandler.requiredLayouts())
                .addAllOptionalLayouts(layoutHandler.optionalLayouts()).addInteractable(
                        GregTechDiagramUtil.buildMaterialInfoButton(LayoutHandler.MATERIAL_INFO_POSITION, material));

        GregTechOreDictUtil.getComponent(OrePrefixes.stick, MaterialUtils.handleMaterial(material)).ifPresent(
                handle -> diagramBuilder.autoInsertIntoSlotGroup(LayoutHandler.SlotGroupKeys.TOOL_PARTS)
                        .insertIntoNextSlot(
                                DisplayComponent.builder(handle)
                                        .setAdditionalTooltip(
                                                Tooltip.create(
                                                        Lang.GREGTECH_5_MATERIAL_TOOLS.trans("handlelabel"),
                                                        Tooltip.INFO_FORMATTING))
                                        .build()));

        diagramBuilder.autoInsertIntoSlotGroup(LayoutHandler.SlotGroupKeys.TOOLS)
                .insertEachGroupSafe(recipeHandler.getTools(material));
        diagramBuilder.autoInsertIntoSlotGroup(LayoutHandler.SlotGroupKeys.TURBINES)
                .insertEachGroupSafe(recipeHandler.getTurbines(material));
        diagramBuilder.autoInsertIntoSlotGroup(LayoutHandler.SlotGroupKeys.SCANNERS)
                .insertEachGroupSafe(recipeHandler.getScanners(material));
        diagramBuilder.autoInsertIntoSlotGroup(LayoutHandler.SlotGroupKeys.ELECTRIC_SCANNERS)
                .insertEachGroupSafe(recipeHandler.getElectricScanners(material));

        Arrays.stream(MaterialPart.VALUES).forEach(part -> part.insertIntoSlot(diagramBuilder, material));
        return diagramBuilder.build();
    }

    private static List<DisplayComponent> getPrefixComponents(ImmutableList<OrePrefixes> prefixes, Material material) {
        List<DisplayComponent> list = new ArrayList<>();
        for (OrePrefixes prefix : prefixes) {
            Optional<ItemComponent> componentOptional = GregTechOreDictUtil.getComponent(prefix, material);
            if (!componentOptional.isPresent()) {
                continue;
            }

            list.add(
                    DisplayComponent.builder(componentOptional.get()).setAdditionalTooltip(
                            Tooltip.create(
                                    Lang.GREGTECH_5_MATERIAL_TOOLS.transf("prefixlabel", prefix.getDefaultLocalName()),
                                    Tooltip.INFO_FORMATTING))
                            .build());
        }
        return list;
    }
}
