package com.github.dcysteine.neicustomdiagram.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import codechicken.nei.recipe.GuiRecipe;

@Mixin(value = GuiRecipe.class, remap = false)
public interface GuiRecipe_Accessor {

    @Invoker("setRecipePage")
    void invokeSetRecipePage(int idx);
}
