package com.github.dcysteine.neicustomdiagram.generators.gregtech5.circuits;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.DisplayComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.util.gregtech5.GregTechRecipeUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

class RecipeHandler {

    private final CircuitLineHandler circuitLineHandler;

    private Set<ItemComponent> craftingTableOutputs;
    private Set<ItemComponent> assemblingMachineOutputs;
    private Set<ItemComponent> assemblingLineOutputs;
    private ImmutableListMultimap<ItemComponent, CircuitRecipe> circuitAssemblingMachineRecipes;

    RecipeHandler(CircuitLineHandler circuitLineHandler) {
        this.circuitLineHandler = circuitLineHandler;
    }

    /** This method must be called before any other methods are called. */
    @SuppressWarnings("unchecked")
    void initialize() {
        craftingTableOutputs = new HashSet<>();
        ((List<IRecipe>) CraftingManager.getInstance().getRecipeList()).stream()
                .filter(recipe -> recipe.getRecipeOutput() != null && recipe.getRecipeOutput().getItem() != null)
                .forEach(recipe -> craftingTableOutputs.add(ItemComponent.create(recipe.getRecipeOutput())));

        assemblingMachineOutputs = RecipeMaps.assemblerRecipes.getAllRecipes().stream()
                .flatMap(recipe -> GregTechRecipeUtil.buildComponentsFromItemOutputs(recipe).stream())
                .map(DisplayComponent::component).map(ItemComponent.class::cast).collect(Collectors.toSet());

        assemblingLineOutputs = RecipeMaps.assemblylineVisualRecipes.getAllRecipes().stream()
                .flatMap(recipe -> GregTechRecipeUtil.buildComponentsFromItemOutputs(recipe).stream())
                .map(DisplayComponent::component).map(ItemComponent.class::cast).collect(Collectors.toSet());

        Set<ItemComponent> allCircuits = circuitLineHandler.allCircuits();
        ListMultimap<ItemComponent, GTRecipe> circuitRecipes = MultimapBuilder.hashKeys().arrayListValues().build();
        for (GTRecipe recipe : RecipeMaps.circuitAssemblerRecipes.getAllRecipes()) {
            ItemComponent output = (ItemComponent) Iterables
                    .getOnlyElement(GregTechRecipeUtil.buildComponentsFromItemOutputs(recipe)).component();

            if (allCircuits.contains(output)) {
                circuitRecipes.put(output, recipe);
            }
        }

        ImmutableListMultimap.Builder<ItemComponent, CircuitRecipe> circuitAssemblingMachineRecipesBuilder = ImmutableListMultimap
                .builder();
        circuitRecipes.asMap().forEach(
                (key, value) -> circuitAssemblingMachineRecipesBuilder
                        .putAll(key, CircuitRecipe.buildCircuitRecipes(value)));
        circuitAssemblingMachineRecipes = circuitAssemblingMachineRecipesBuilder.build();
    }

    boolean hasCraftingTableRecipes(ItemComponent circuit) {
        return craftingTableOutputs.contains(circuit);
    }

    boolean hasAssemblingMachineRecipes(ItemComponent circuit) {
        return assemblingMachineOutputs.contains(circuit);
    }

    boolean hasAssemblingLineRecipes(ItemComponent circuit) {
        return assemblingLineOutputs.contains(circuit);
    }

    ImmutableList<CircuitRecipe> getCircuitAssemblingMachineRecipes(ItemComponent circuit) {
        return circuitAssemblingMachineRecipes.get(circuit);
    }
}
