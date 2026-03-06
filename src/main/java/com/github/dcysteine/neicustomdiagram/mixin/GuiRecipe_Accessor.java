package com.github.dcysteine.neicustomdiagram.mixin;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.Recipe.RecipeId;

@Mixin(value = GuiRecipe.class, remap = false)
public interface GuiRecipe_Accessor {

    @Invoker("getCurrentRecipeId")
    static RecipeId invokeGetCurrentRecipeId(GuiScreen gui) {
        throw new UnsupportedOperationException();
    }
}
