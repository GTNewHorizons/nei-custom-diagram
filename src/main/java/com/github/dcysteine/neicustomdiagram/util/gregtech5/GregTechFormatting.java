package com.github.dcysteine.neicustomdiagram.util.gregtech5;

import net.minecraft.util.StatCollector;

import com.github.dcysteine.neicustomdiagram.main.config.ConfigOptions;
import com.ruling_0.materiallib.api.Material;

import gregtech.api.material.MaterialUtils;

public final class GregTechFormatting {

    // Static class.
    private GregTechFormatting() {}

    private static String getMaterialName(Material material) {
        String key = MaterialUtils.localizedNameKey(material);
        if (key != null && StatCollector.canTranslate(key)) {
            return StatCollector.translateToLocal(key);
        }
        return MaterialUtils.internalName(material);
    }

    public static String getMaterialDescription(Material material) {
        if (ConfigOptions.SHOW_IDS.get()) {
            return String.format("%s (#%d)", getMaterialName(material), MaterialUtils.oldSubId(material));
        } else {
            return getMaterialName(material);
        }
    }
}
