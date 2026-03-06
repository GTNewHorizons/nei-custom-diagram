package com.github.dcysteine.neicustomdiagram.generators.enderstorage;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.github.dcysteine.neicustomdiagram.generators.enderstorage.chestoverview.EnderStorageChestOverview;
import com.github.dcysteine.neicustomdiagram.generators.enderstorage.tankoverview.EnderStorageTankOverview;
import com.github.dcysteine.neicustomdiagram.main.Registry;
import com.github.dcysteine.neicustomdiagram.mixin.GuiRecipe_Accessor;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import codechicken.enderstorage.event.EnderStorageStoredEvent;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.Recipe;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@EventBusSubscriber(side = Side.CLIENT)
public class StorageListener {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void clientStorageUpdate(EnderStorageStoredEvent event) {
        Recipe.RecipeId recipeId = GuiRecipe_Accessor.invokeGetCurrentRecipeId(Minecraft.getMinecraft().currentScreen);
        String currentId = recipeId == null ? "" : recipeId.getHandleName();
        String id;
        switch (event.type) {
            case EnderStorageStoredEvent.TYPE_ITEM:
                id = Registry.GROUP_ID_PREFIX + "enderstorage.chestoverview"
                        + (event.global ? EnderStorageChestOverview.LOOKUP_GLOBAL_CHESTS_SUFFIX
                                : EnderStorageChestOverview.LOOKUP_PERSONAL_CHESTS_SUFFIX);
                break;
            case EnderStorageStoredEvent.TYPE_LIQUID:
                id = Registry.GROUP_ID_PREFIX + "enderstorage.tankoverview"
                        + (event.global ? EnderStorageTankOverview.LOOKUP_GLOBAL_TANKS_SUFFIX
                                : EnderStorageTankOverview.LOOKUP_PERSONAL_TANKS_SUFFIX);
                break;
            default:
                id = "";
        }
        if (!id.isEmpty() && id.startsWith(currentId)) {
            GuiUsageRecipe.openRecipeGui(id, new ItemStack(Item.getItemById(0)));
        }
    }
}
