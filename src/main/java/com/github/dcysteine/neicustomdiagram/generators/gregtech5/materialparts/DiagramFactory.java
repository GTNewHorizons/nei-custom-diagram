package com.github.dcysteine.neicustomdiagram.generators.gregtech5.materialparts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.github.dcysteine.neicustomdiagram.api.diagram.Diagram;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.Component;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.DisplayComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.FluidComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.CustomInteractable;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.Interactable;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.ComponentLabel;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Layout;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.TooltipLine;
import com.github.dcysteine.neicustomdiagram.main.Lang;
import com.github.dcysteine.neicustomdiagram.util.gregtech5.GregTechDiagramUtil;
import com.github.dcysteine.neicustomdiagram.util.gregtech5.GregTechFluidDictUtil;
import com.github.dcysteine.neicustomdiagram.util.gregtech5.GregTechOreDictUtil;
import com.google.common.collect.ImmutableList;
import com.ruling_0.materiallib.api.Material;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.material.MaterialUtils;
import gregtech.api.material.MaterialUtils.CrackType;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

class DiagramFactory {

    private static final ComponentLabel BLAST_FURNACE_INFO_ICON = ComponentLabel.create(
            GregTechOreDictUtil.getComponent(ItemList.Machine_Multi_BlastFurnace),
            LayoutHandler.BLAST_FURNACE_INFO_POSITION);

    private enum MaterialPart {

        // spotless:off
        GEMS(LayoutHandler.SlotGroupKeys.GEMS, OrePrefixes.gemChipped, OrePrefixes.gemFlawed, OrePrefixes.gemFlawless, OrePrefixes.gemExquisite),
        GEM(LayoutHandler.SlotKeys.GEM, OrePrefixes.gem),
        LENS(LayoutHandler.SlotKeys.LENS, OrePrefixes.lens),

        NANITES(LayoutHandler.SlotKeys.NANITES, OrePrefixes.nanite),
        DUSTS(LayoutHandler.SlotGroupKeys.DUSTS, OrePrefixes.dust, OrePrefixes.dustSmall, OrePrefixes.dustTiny),

        HOT_INGOT(LayoutHandler.SlotKeys.HOT_INGOT, OrePrefixes.ingotHot),
        INGOTS(LayoutHandler.SlotGroupKeys.INGOTS, OrePrefixes.ingot, OrePrefixes.nugget, OrePrefixes.block),

        ALLOY_PLATE(LayoutHandler.SlotKeys.ALLOY_PLATE, OrePrefixes.plateAlloy),
        PLATES(LayoutHandler.SlotGroupKeys.PLATES, OrePrefixes.plate, OrePrefixes.foil, OrePrefixes.plateDense),
        MULTI_PLATES(LayoutHandler.SlotGroupKeys.MULTI_PLATES, OrePrefixes.plateDouble, OrePrefixes.plateTriple, OrePrefixes.plateQuadruple, OrePrefixes.plateQuintuple),
        SUPERDENSE_PLATES(LayoutHandler.SlotKeys.SUPERDENSE_PLATES, OrePrefixes.plateSuperdense),

        RODS(LayoutHandler.SlotGroupKeys.RODS, OrePrefixes.stick, OrePrefixes.stickLong),
        BOLTS(LayoutHandler.SlotGroupKeys.BOLTS, OrePrefixes.bolt, OrePrefixes.screw),
        RING(LayoutHandler.SlotKeys.RING, OrePrefixes.ring),
        ROUND(LayoutHandler.SlotKeys.ROUND, OrePrefixes.round),
        SPRINGS(LayoutHandler.SlotGroupKeys.SPRINGS, OrePrefixes.spring, OrePrefixes.springSmall),
        GEARS(LayoutHandler.SlotGroupKeys.GEARS, OrePrefixes.gearGt, OrePrefixes.gearGtSmall),
        ROTORS(LayoutHandler.SlotKeys.ROTOR, OrePrefixes.rotor),
        CASING(LayoutHandler.SlotKeys.CASING, OrePrefixes.itemCasing),
        BARS(LayoutHandler.SlotKeys.BARS, OrePrefixes.bars),
        FRAME_BOX(LayoutHandler.SlotKeys.FRAME_BOX, OrePrefixes.frameGt),
        SHEET_METAL(LayoutHandler.SlotKeys.SHEET_METAL, OrePrefixes.sheetmetal),
        BOLTED_CASING(LayoutHandler.SlotKeys.BOLTED_CASING, OrePrefixes.blockCasing),
        REBOLTED_CASING(LayoutHandler.SlotKeys.REBOLTED_CASING, OrePrefixes.blockCasingAdvanced),

        WIRES(LayoutHandler.SlotGroupKeys.WIRES, OrePrefixes.wireGt01, OrePrefixes.wireGt02, OrePrefixes.wireGt04, OrePrefixes.wireGt08, OrePrefixes.wireGt12, OrePrefixes.wireGt16),
        FINE_WIRE(LayoutHandler.SlotKeys.FINE_WIRE, OrePrefixes.wireFine),
        CABLES(LayoutHandler.SlotGroupKeys.CABLES, OrePrefixes.cableGt01, OrePrefixes.cableGt02, OrePrefixes.cableGt04, OrePrefixes.cableGt08, OrePrefixes.cableGt12, OrePrefixes.cableGt16),

        PIPES(LayoutHandler.SlotGroupKeys.PIPES, OrePrefixes.pipeTiny, OrePrefixes.pipeSmall, OrePrefixes.pipeMedium, OrePrefixes.pipeLarge, OrePrefixes.pipeHuge),
        SPECIAL_PIPES(LayoutHandler.SlotGroupKeys.SPECIAL_PIPES, OrePrefixes.pipeQuadruple, OrePrefixes.pipeNonuple, OrePrefixes.pipeRestrictiveTiny, OrePrefixes.pipeRestrictiveSmall, OrePrefixes.pipeRestrictiveMedium, OrePrefixes.pipeRestrictiveLarge, OrePrefixes.pipeRestrictiveHuge);
        // spotless:on

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
    private final HeatingCoilHandler heatingCoilHandler;
    private final RelatedMaterialsHandler relatedMaterialsHandler;
    private final HashMap<Material, Integer> materialEbfHeatMap;

    DiagramFactory(LayoutHandler layoutHandler, HeatingCoilHandler heatingCoilHandler,
            RelatedMaterialsHandler relatedMaterialsHandler) {
        this.layoutHandler = layoutHandler;
        this.heatingCoilHandler = heatingCoilHandler;
        this.relatedMaterialsHandler = relatedMaterialsHandler;
        this.materialEbfHeatMap = new HashMap<>();
    }

    void initialize() {
        for (GTRecipe ebfRecipe : RecipeMaps.blastFurnaceRecipes.getAllRecipes()) {
            for (ItemStack output : ebfRecipe.mOutputs) {
                ItemData outData = GTOreDictUnificator.getAssociation(output);
                if ((outData != null) && outData.hasValidMaterialData()
                        && outData.hasValidPrefixData()
                        && (outData.mPrefix == OrePrefixes.ingot || outData.mPrefix == OrePrefixes.ingotHot)) {
                    Material mat = outData.mMaterial.mMaterial;
                    int recipeHeat = ebfRecipe.mSpecialValue;
                    materialEbfHeatMap.compute(
                            mat,
                            (m, oldHeat) -> (oldHeat == null) ? recipeHeat : Math.min(recipeHeat, oldHeat));
                }
            }
        }
    }

    Diagram buildDiagram(Material material) {
        Diagram.Builder diagramBuilder = Diagram.builder().addAllLayouts(layoutHandler.requiredLayouts())
                .addAllOptionalLayouts(layoutHandler.optionalLayouts()).addInteractable(
                        GregTechDiagramUtil.buildMaterialInfoButton(LayoutHandler.MATERIAL_INFO_POSITION, material));

        buildBlastFurnaceInfoButton(material).ifPresent(diagramBuilder::addInteractable);

        diagramBuilder.autoInsertIntoSlotGroup(LayoutHandler.SlotGroupKeys.RELATED_MATERIALS)
                .insertEachSafe(relatedMaterialsHandler.getRelatedMaterialRepresentations(material));

        Diagram.Builder.SlotGroupAutoSubBuilder fluidsSlotBuilder = diagramBuilder
                .autoInsertIntoSlotGroup(LayoutHandler.SlotGroupKeys.FLUIDS);
        insertFluidIntoSlot(
                fluidsSlotBuilder,
                MaterialUtils.fluid(material, 1000),
                Lang.GREGTECH_5_MATERIAL_PARTS.trans("fluidtypefluid"));
        insertFluidIntoSlot(
                fluidsSlotBuilder,
                MaterialUtils.gas(material, 1000),
                Lang.GREGTECH_5_MATERIAL_PARTS.trans("fluidtypegas"));
        insertFluidIntoSlot(
                fluidsSlotBuilder,
                MaterialUtils.solid(material, 1000),
                Lang.GREGTECH_5_MATERIAL_PARTS.trans("fluidtypesolid"));
        insertFluidIntoSlot(
                fluidsSlotBuilder,
                MaterialUtils.molten(material, 1000),
                Lang.GREGTECH_5_MATERIAL_PARTS.trans("fluidtypemolten"));
        insertFluidIntoSlot(
                fluidsSlotBuilder,
                MaterialUtils.plasma(material, 1000),
                Lang.GREGTECH_5_MATERIAL_PARTS.trans("fluidtypeplasma"));

        Diagram.Builder.SlotGroupAutoSubBuilder hydroCrackedFluidsSlotBuilder = diagramBuilder
                .autoInsertIntoSlotGroup(LayoutHandler.SlotGroupKeys.HYDRO_CRACKED_FLUIDS);
        insertFluidIntoSlot(
                hydroCrackedFluidsSlotBuilder,
                cracked(material, CrackType.HYDRO, 0),
                Lang.GREGTECH_5_MATERIAL_PARTS.trans("fluidtypelightlyhydrocracked"));
        insertFluidIntoSlot(
                hydroCrackedFluidsSlotBuilder,
                cracked(material, CrackType.HYDRO, 1),
                Lang.GREGTECH_5_MATERIAL_PARTS.trans("fluidtypemoderatelyhydrocracked"));
        insertFluidIntoSlot(
                hydroCrackedFluidsSlotBuilder,
                cracked(material, CrackType.HYDRO, 2),
                Lang.GREGTECH_5_MATERIAL_PARTS.trans("fluidtypeseverelyhydrocracked"));

        Diagram.Builder.SlotGroupAutoSubBuilder steamCrackedFluidsSlotBuilder = diagramBuilder
                .autoInsertIntoSlotGroup(LayoutHandler.SlotGroupKeys.STEAM_CRACKED_FLUIDS);
        insertFluidIntoSlot(
                steamCrackedFluidsSlotBuilder,
                cracked(material, CrackType.STEAM, 0),
                Lang.GREGTECH_5_MATERIAL_PARTS.trans("fluidtypelightlysteamcracked"));
        insertFluidIntoSlot(
                steamCrackedFluidsSlotBuilder,
                cracked(material, CrackType.STEAM, 1),
                Lang.GREGTECH_5_MATERIAL_PARTS.trans("fluidtypemoderatelysteamcracked"));
        insertFluidIntoSlot(
                steamCrackedFluidsSlotBuilder,
                cracked(material, CrackType.STEAM, 2),
                Lang.GREGTECH_5_MATERIAL_PARTS.trans("fluidtypeseverelysteamcracked"));

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
                                    Lang.GREGTECH_5_MATERIAL_PARTS.transf("prefixlabel", prefix.getDefaultLocalName()),
                                    Tooltip.INFO_FORMATTING))
                            .build());
        }
        return list;
    }

    private static @Nullable FluidStack cracked(Material material, CrackType type, int severity) {
        Fluid fluid = MaterialUtils.crackedFluid(material, type, severity);
        return fluid == null ? null : new FluidStack(fluid, 1000);
    }

    private Optional<Interactable> buildBlastFurnaceInfoButton(Material material) {
        if (!MaterialUtils.blastFurnaceRequired(material)) {
            return Optional.empty();
        }

        Tooltip.Builder tooltipBuilder = Tooltip.builder().setFormatting(Tooltip.INFO_FORMATTING);
        int ebfTemp = materialEbfHeatMap.getOrDefault(material, MaterialUtils.blastFurnaceTemp(material));
        if (ebfTemp > 0) {
            tooltipBuilder.addTextLine(Lang.GREGTECH_5_MATERIAL_PARTS.transf("blastfurnaceinfotemp", ebfTemp))
                    .addSpacing().setFormatting(Tooltip.SLOT_FORMATTING)
                    .addTextLine(Lang.GREGTECH_5_MATERIAL_PARTS.trans("blastfurnaceinfocoils"))
                    .setFormatting(Tooltip.DEFAULT_FORMATTING);

            for (Map.Entry<Long, Component> entry : heatingCoilHandler.getHeatingCoils(ebfTemp).entrySet()) {
                long heat = entry.getKey();
                Component heatingCoil = entry.getValue();

                tooltipBuilder.addLine(
                        TooltipLine.builder().addComponentIcon(heatingCoil).addText(String.format("[%,dK]", heat))
                                .addComponentDescription(heatingCoil).build());
            }
        } else {
            tooltipBuilder.addTextLine(Lang.GREGTECH_5_MATERIAL_PARTS.trans("blastfurnaceinfo"));
        }

        return Optional
                .of(CustomInteractable.builder(BLAST_FURNACE_INFO_ICON).setTooltip(tooltipBuilder.build()).build());
    }

    private static void insertFluidIntoSlot(Diagram.Builder.SlotGroupAutoSubBuilder slotBuilder,
            @Nullable FluidStack fluid, String fluidType) {
        if (fluid == null) {
            return;
        }
        FluidComponent fluidComponent = FluidComponent.create(fluid);

        // Try to use a GregTech filled cell or fluid display item, since it's more convenient when
        // looking up recipes.
        slotBuilder.insertIntoNextSlot(
                DisplayComponent.builder(GregTechFluidDictUtil.getCellOrDisplayItem(fluidComponent))
                        .setAdditionalTooltip(
                                Tooltip.create(
                                        Lang.GREGTECH_5_MATERIAL_PARTS.transf("fluidtypelabel", fluidType),
                                        Tooltip.INFO_FORMATTING))
                        .build());
    }
}
