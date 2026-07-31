package com.github.dcysteine.neicustomdiagram.util.gregtech5;

import net.minecraft.init.Items;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.CustomInteractable;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.Interactable;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.ComponentLabel;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip;
import com.github.dcysteine.neicustomdiagram.api.draw.Point;
import com.github.dcysteine.neicustomdiagram.main.Lang;
import com.ruling_0.materiallib.api.Material;

import gregtech.api.material.GTMaterialProperties;
import gregtech.api.material.MaterialUtils;

public final class GregTechDiagramUtil {

    public static final ItemComponent ICON = ItemComponent.create(Items.book, 0);

    // Static class.
    private GregTechDiagramUtil() {}

    public static Interactable buildMaterialInfoButton(Point pos, Material material) {
        Tooltip.Builder tooltipBuilder = Tooltip.builder()
                .addTextLine(GregTechFormatting.getMaterialDescription(material)).setFormatting(Tooltip.INFO_FORMATTING)
                .addTextLine(MaterialUtils.chemicalFormula(material));

        boolean radioactive = Boolean.TRUE.equals(material.getProperty(GTMaterialProperties.IS_RADIOACTIVE));
        float heatDamage = MaterialUtils.heatDamage(material);
        if (radioactive || heatDamage != 0) {
            tooltipBuilder.addSpacing().setFormatting(Tooltip.URGENT_FORMATTING);

            if (radioactive) {
                tooltipBuilder.addTextLine(Lang.GREGTECH_5_UTIL.trans("materialinforadioactive"));
            }
            if (heatDamage > 0) {
                tooltipBuilder.addTextLine(Lang.GREGTECH_5_UTIL.trans("materialinfohot"));
            } else if (heatDamage < 0) {
                tooltipBuilder.addTextLine(Lang.GREGTECH_5_UTIL.trans("materialinfocold"));
            }
        }

        return CustomInteractable.builder(ComponentLabel.create(ICON, pos)).setTooltip(tooltipBuilder.build()).build();
    }
}
