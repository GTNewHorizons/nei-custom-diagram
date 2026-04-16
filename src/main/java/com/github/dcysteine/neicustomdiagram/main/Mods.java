package com.github.dcysteine.neicustomdiagram.main;

import java.util.function.Predicate;

import cpw.mods.fml.common.Loader;

public enum Mods {

    ENDER_STORAGE("EnderStorage"),
    GTNH_CORE_MOD("dreamcraft"),
    // GregTech 5 shares a mod ID with GregTech 6, so we must also check the mod version.
    GREGTECH_5("gregtech", version -> !version.startsWith("GT6")),
    GREGTECH_6("gregtech", version -> version.startsWith("GT6")),
    // GregTech5 add-ons
    BARTWORKS("bartworks"),
    GT_PLUS_PLUS("miscutils"),
    DETRAV_SCANNER("detravscannermod");

    // If you're adding a new mod dependency here, don't forget to also add it to the list of
    // dependencies in NeiCustomDiagram.java (if necessary).

    public final String modID;
    public final Predicate<String> otherRequirement;
    private Boolean loaded;

    Mods(String modID) {
        this.modID = modID;
        this.otherRequirement = null;
    }

    Mods(String modID, Predicate<String> otherRequirement) {
        this.modID = modID;
        this.otherRequirement = otherRequirement;
    }

    public boolean isLoaded() {
        if (this.loaded != null) {
            return this.loaded;
        }

        if (!Loader.isModLoaded(modID)) {
            this.loaded = false;
        } else if (otherRequirement == null) {
            this.loaded = true;
        } else {
            String version = Loader.instance().getIndexedModList().get(modID).getVersion();
            this.loaded = otherRequirement.test(version);
        }

        return this.loaded;
    }
}
