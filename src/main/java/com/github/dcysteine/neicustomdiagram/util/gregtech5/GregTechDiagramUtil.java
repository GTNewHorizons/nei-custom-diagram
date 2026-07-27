package com.github.dcysteine.neicustomdiagram.util.gregtech5;

import net.minecraft.init.Items;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.CustomInteractable;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.Interactable;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.ComponentLabel;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip;
import com.github.dcysteine.neicustomdiagram.api.draw.Point;
import com.github.dcysteine.neicustomdiagram.main.Lang;

import gregtech.api.enums.Materials;
import gregtech.api.interfaces.IOreMaterial;
import gtPlusPlus.core.material.Material;

public final class GregTechDiagramUtil {

    public static final ItemComponent ICON = ItemComponent.create(Items.book, 0);

    // Static class.
    private GregTechDiagramUtil() {}

    public static Interactable buildMaterialInfoButton(Point pos, IOreMaterial material) {
        Tooltip.Builder tooltipBuilder = Tooltip.builder()
                .addTextLine(GregTechFormatting.getMaterialDescription(material)).setFormatting(Tooltip.INFO_FORMATTING)
                .addTextLine(getChemicalFormula(material));

        if (material instanceof Materials gtMaterial) {
            if (gtMaterial.mHeatDamage != 0) {
                tooltipBuilder.addSpacing().setFormatting(Tooltip.URGENT_FORMATTING);

                if (gtMaterial.mHeatDamage > 0) {
                    tooltipBuilder.addTextLine(Lang.GREGTECH_5_UTIL.trans("materialinfohot"));
                } else if (gtMaterial.mHeatDamage < 0) {
                    tooltipBuilder.addTextLine(Lang.GREGTECH_5_UTIL.trans("materialinfocold"));
                }
            }
        } else if (material instanceof Material gtppMaterial) {
            if (gtppMaterial.isRadioactive) {
                tooltipBuilder.addTextLine(Lang.GREGTECH_5_UTIL.trans("materialinforadioactive"));
            }
        }

        return CustomInteractable.builder(ComponentLabel.create(ICON, pos)).setTooltip(tooltipBuilder.build()).build();
    }

    private static String getChemicalFormula(IOreMaterial material) {
        if (material instanceof Materials gtMaterial) {
            return gtMaterial.getChemicalFormula();
        } else if (material instanceof Material gtppMaterial) {
            return gtppMaterial.chemicalFormula;
        }
        return "";
    }
}
