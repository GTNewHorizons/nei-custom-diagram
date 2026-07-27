package com.github.dcysteine.neicustomdiagram.util.gregtech5;

import com.github.dcysteine.neicustomdiagram.main.config.ConfigOptions;

import gregtech.api.enums.Materials;
import gregtech.api.interfaces.IOreMaterial;
import gtPlusPlus.core.material.Material;

public final class GregTechFormatting {

    // Static class.
    private GregTechFormatting() {}

    private static String getMaterialName(IOreMaterial material) {
        String fallback = "";
        if (material instanceof Materials gtMaterial) {
            fallback = gtMaterial.mName;
        } else if (material instanceof Material gtppMaterial) {
            fallback = gtppMaterial.getInternalName();
        }
        return material.getLocalizedName().equals("null") ? fallback : material.getLocalizedName();
    }

    public static String getMaterialDescription(IOreMaterial material) {
        // Only GT materials have IDs, GTPP materials do not
        if (ConfigOptions.SHOW_IDS.get() && material instanceof Materials gtMaterial) {
            return String.format("%s (#%d)", getMaterialName(material), gtMaterial.mMetaItemSubID);
        } else {
            return getMaterialName(material);
        }
    }
}
