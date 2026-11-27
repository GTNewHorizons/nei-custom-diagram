package com.github.dcysteine.neicustomdiagram.generators.enderstorage;

import java.util.List;

import net.minecraft.client.Minecraft;

import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.Interactable;
import com.github.dcysteine.neicustomdiagram.generators.enderstorage.chestoverview.EnderStorageChestOverview;
import com.github.dcysteine.neicustomdiagram.generators.enderstorage.tankoverview.EnderStorageTankOverview;
import com.github.dcysteine.neicustomdiagram.main.Registry;
import com.github.dcysteine.neicustomdiagram.mixin.GuiRecipe_Accessor;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import codechicken.enderstorage.event.EnderStorageStoredEvent;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@EventBusSubscriber(side = Side.CLIENT)
public class StorageListener {

    public static final String REFRESH_VIEW_SUFFIX = "-refreshView";

    @SuppressWarnings("unchecked")
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void clientStorageUpdate(EnderStorageStoredEvent event) {
        String groupId;
        String suffix;
        switch (event.type) {
            case EnderStorageStoredEvent.TYPE_ITEM:
                groupId = Registry.GROUP_ID_PREFIX + "enderstorage.chestoverview";
                suffix = event.global ? EnderStorageChestOverview.LOOKUP_GLOBAL_CHESTS_SUFFIX
                        : EnderStorageChestOverview.LOOKUP_PERSONAL_CHESTS_SUFFIX;
                break;
            case EnderStorageStoredEvent.TYPE_LIQUID:
                groupId = Registry.GROUP_ID_PREFIX + "enderstorage.tankoverview";
                suffix = event.global ? EnderStorageTankOverview.LOOKUP_GLOBAL_TANKS_SUFFIX
                        : EnderStorageTankOverview.LOOKUP_PERSONAL_TANKS_SUFFIX;
                break;
            default:
                return;
        }

        // Only refresh the current tab, and only if it is ours.
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiRecipe<?>gui)
                || !(gui.getHandler() instanceof DiagramGroup group)
                || !group.info().groupId().equals(groupId)) {
            return;
        }

        Interactable.RecipeType recipeType = gui instanceof GuiUsageRecipe ? Interactable.RecipeType.USAGE
                : Interactable.RecipeType.CRAFTING;
        ((List<Object>) gui.currenthandlers)
                .set(gui.recipetype, group.loadDiagrams(groupId + suffix + REFRESH_VIEW_SUFFIX, recipeType));
        ((GuiRecipe_Accessor) gui).invokeSetRecipePage(gui.recipetype);
    }
}
