package com.github.dcysteine.neicustomdiagram.generators.enderstorage;

import com.github.dcysteine.neicustomdiagram.generators.enderstorage.chestoverview.EnderStorageChestOverview;
import com.github.dcysteine.neicustomdiagram.generators.enderstorage.tankoverview.EnderStorageTankOverview;

import codechicken.enderstorage.event.EnderStorageStoredEvent;
import codechicken.nei.recipe.GuiUsageRecipe;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@EventBusSubscriber(side = Side.CLIENT)
public class StorageListener {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void clientStorageUpdate(EnderStorageStoredEvent event) {
        System.out.println("EnderStorageStoredEvent received: type=" + event.type + ", global=" + event.global);

        switch (event.type) {
            case EnderStorageStoredEvent.TYPE_ITEM:
                EnderStorageChestOverview.nextIsRemote = false;
                GuiUsageRecipe.openRecipeGui(
                        "enderstorage.chestoverview"
                                + (event.global ? EnderStorageChestOverview.LOOKUP_GLOBAL_CHESTS_SUFFIX
                                        : EnderStorageChestOverview.LOOKUP_PERSONAL_CHESTS_SUFFIX));
                break;
            case EnderStorageStoredEvent.TYPE_LIQUID:
                EnderStorageTankOverview.nextIsRemote = false;
                GuiUsageRecipe.openRecipeGui(
                        "enderstorage.tankoverview"
                                + (event.global ? EnderStorageTankOverview.LOOKUP_GLOBAL_TANKS_SUFFIX
                                        : EnderStorageTankOverview.LOOKUP_PERSONAL_TANKS_SUFFIX));
                break;
        }
    }
}
