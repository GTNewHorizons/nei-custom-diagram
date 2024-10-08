package com.github.dcysteine.neicustomdiagram.generators.gregtech5.oreprocessing;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import com.github.dcysteine.neicustomdiagram.api.diagram.component.DisplayComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.main.Logger;
import com.github.dcysteine.neicustomdiagram.util.gregtech5.GregTechOreDictUtil;
import com.github.dcysteine.neicustomdiagram.util.gregtech5.GregTechRecipeUtil;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/** Class that caches GregTech recipe data and stores it in a map, for fast lookup later. */
class RecipeHandler {

    enum RecipeMap {

        MACERATOR(RecipeMaps.maceratorRecipes),
        ORE_WASHING_PLANT(RecipeMaps.oreWasherRecipes),
        THERMAL_CENTRIFUGE(RecipeMaps.thermalCentrifugeRecipes),
        SIFTER(RecipeMaps.sifterRecipes),
        CENTRIFUGE(RecipeMaps.centrifugeRecipes),

        BLAST_FURNACE(RecipeMaps.blastFurnaceRecipes),
        CHEMICAL_BATH(RecipeMaps.chemicalBathRecipes),
        CHEMICAL_REACTOR(RecipeMaps.chemicalReactorRecipes),
        ELECTROMAGNETIC_SEPARATOR(RecipeMaps.electroMagneticSeparatorRecipes),
        MIXER(RecipeMaps.mixerRecipes),
        AUTOCLAVE(RecipeMaps.autoclaveRecipes);

        final gregtech.api.recipe.RecipeMap<?> recipeMap;

        RecipeMap(gregtech.api.recipe.RecipeMap<?> recipeMap) {
            this.recipeMap = recipeMap;
        }
    }

    /**
     * Enum containing fluids that we will look up crushed ore recipes for in the chemical bath.
     *
     * <p>
     * These are the GregTech display item stack, and so are {@link ItemComponent}s rather than {@code FluidComponent}s.
     */
    enum ChemicalBathFluid {

        MERCURY(ItemComponent.create(GTUtility.getFluidDisplayStack(Materials.Mercury.mFluid))),

        SODIUM_PERSULFATE(ItemComponent.create(GTUtility.getFluidDisplayStack(Materials.SodiumPersulfate.mFluid)));

        final ItemComponent fluid;

        ChemicalBathFluid(ItemComponent fluid) {
            this.fluid = fluid;
        }

        /**
         * Note that the keys are GregTech fluid display items, not fluids. This is for convenience, because
         * {@link GregTechRecipeUtil} returns GregTech fluid display items (when possible).
         */
        static final ImmutableMap<ItemComponent, ChemicalBathFluid> VALUES_MAP = ImmutableMap
                .copyOf(Arrays.stream(values()).collect(Collectors.toMap(c -> c.fluid, Function.identity())));
    }

    /**
     * Helper class containing the input fluid amount, as well as the recipe outputs.
     *
     * <p>
     * This class is stored in {@code chemicalBathFluidData} below. Unlike {@code recipeData}, we cannot just map to
     * {@code ImmutableList<DisplayComponent>}, because we must also store the additional information of how much of the
     * chemical bath fluid is required as input.
     */
    @AutoValue
    public abstract static class ChemicalBathFluidRecipe {

        public static ChemicalBathFluidRecipe create(int inputFluidAmount, ImmutableList<DisplayComponent> outputs) {
            return new AutoValue_RecipeHandler_ChemicalBathFluidRecipe(inputFluidAmount, outputs);
        }

        public abstract int inputFluidAmount();

        public abstract ImmutableList<DisplayComponent> outputs();
    }

    /**
     * Map of recipe type to multimap. The multimaps are maps of input {@link ItemComponent}s to lists of
     * {@link DisplayComponent}s representing the outputs for each recipe for that input.
     *
     * <p>
     * We usually don't look up recipe outputs by fluid, so fluid inputs will be skipped when building this data. We
     * have separate data structures for the few fluid lookups that we do.
     */
    private final EnumMap<RecipeMap, SetMultimap<ItemComponent, ImmutableList<DisplayComponent>>> recipeData;

    /**
     * Map of fluid to multimap. The multimaps are maps of input {@link ItemComponent}s to
     * {@link ChemicalBathFluidRecipe}s containing the input fluid amount, as well as the chemical bath recipe outputs,
     * for the given chemical bath fluid and input item.
     */
    private final EnumMap<ChemicalBathFluid, SetMultimap<ItemComponent, ChemicalBathFluidRecipe>> chemicalBathFluidData;

    /** Map of smelting input to smelting output. */
    private final Map<ItemComponent, ItemComponent> furnaceData;

    RecipeHandler() {
        this.recipeData = new EnumMap<>(RecipeMap.class);
        this.chemicalBathFluidData = new EnumMap<>(ChemicalBathFluid.class);
        this.furnaceData = new HashMap<>();
    }

    /** This method must be called before any other methods are called. */
    @SuppressWarnings("unchecked")
    void initialize() {
        Arrays.stream(ChemicalBathFluid.values()).forEach(
                chemicalBathFluid -> chemicalBathFluidData
                        .put(chemicalBathFluid, MultimapBuilder.hashKeys().hashSetValues().build()));

        for (RecipeMap recipeMap : RecipeMap.values()) {
            SetMultimap<ItemComponent, ImmutableList<DisplayComponent>> multimap = MultimapBuilder.hashKeys()
                    .hashSetValues().build();
            recipeData.put(recipeMap, multimap);

            for (GTRecipe recipe : recipeMap.recipeMap.getAllRecipes()) {
                ImmutableList<DisplayComponent> outputs = ImmutableList
                        .copyOf(GregTechRecipeUtil.buildComponentsFromOutputs(recipe));

                Optional<ChemicalBathFluid> chemicalBathFluidOptional = Optional.empty();
                int inputFluidAmount = 0;
                if (recipeMap == RecipeMap.CHEMICAL_BATH) {
                    List<DisplayComponent> fluidInputs = GregTechRecipeUtil.buildComponents(recipe.mFluidInputs);

                    if (fluidInputs.size() != 1) {
                        Logger.GREGTECH_5_ORE_PROCESSING.warn(
                                "Found chemical bath recipe with {} fluids:\n[{}]\n ->\n[{}]",
                                fluidInputs.size(),
                                GregTechRecipeUtil.buildComponentsFromInputs(recipe),
                                GregTechRecipeUtil.buildComponentsFromOutputs(recipe));
                    } else {
                        DisplayComponent inputFluid = Iterables.getOnlyElement(fluidInputs);

                        if (inputFluid.stackSize().isPresent()) {
                            chemicalBathFluidOptional = Optional
                                    .ofNullable(ChemicalBathFluid.VALUES_MAP.get(inputFluid.component()));
                            inputFluidAmount = inputFluid.stackSize().get();
                        } else {
                            Logger.GREGTECH_5_ORE_PROCESSING.warn(
                                    "Found chemical bath recipe missing input fluid stack size:\n[{}]\n ->\n[{}]",
                                    GregTechRecipeUtil.buildComponentsFromInputs(recipe),
                                    GregTechRecipeUtil.buildComponentsFromOutputs(recipe));
                        }
                    }
                }

                for (ItemStack itemStack : recipe.mInputs) {
                    if (itemStack == null) {
                        continue;
                    }

                    ItemComponent itemComponent = ItemComponent.create(GTOreDictUnificator.get_nocopy(itemStack));
                    multimap.put(itemComponent, outputs);

                    // Need an effectively final variable here so that we can reference it within
                    // the lambda.
                    final int finalInputFluidAmount = inputFluidAmount;
                    chemicalBathFluidOptional.ifPresent(
                            chemicalBathFluid -> chemicalBathFluidData.get(chemicalBathFluid).put(
                                    itemComponent,
                                    ChemicalBathFluidRecipe.create(finalInputFluidAmount, outputs)));
                }
            }
        }

        ((Map<ItemStack, ItemStack>) FurnaceRecipes.smelting().getSmeltingList())
                .forEach((key, value) -> furnaceData.put(ItemComponent.create(key), ItemComponent.create(value)));
    }

    /** The returned set is immutable! */
    Set<ImmutableList<DisplayComponent>> getRecipeOutputs(RecipeMap recipeMap, ItemComponent input) {
        return Multimaps.unmodifiableSetMultimap(recipeData.get(recipeMap))
                .get((ItemComponent) GregTechOreDictUtil.unify(input));
    }

    /**
     * Returns the unique recipe output for recipes including {@code input}, or empty optional if there were zero such
     * recipes, or if there were multiple recipes with differing outputs.
     *
     * <p>
     * Also will log each case where multiple differing recipe outputs were found.
     */
    Optional<ImmutableList<DisplayComponent>> getUniqueRecipeOutput(RecipeMap recipeMap, ItemComponent input) {
        Set<ImmutableList<DisplayComponent>> outputs = recipeData.get(recipeMap)
                .get((ItemComponent) GregTechOreDictUtil.unify(input));

        if (outputs.size() > 1) {
            Logger.GREGTECH_5_ORE_PROCESSING.warn("Found {} recipes: [{}] [{}]", outputs.size(), recipeMap, input);
            return Optional.empty();
        } else if (outputs.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Iterables.getOnlyElement(outputs));
    }

    /**
     * Returns the recipe data for chemical bath recipes including {@code chemicalBathFluid} and {@code input}, or empty
     * optional if there were zero such recipes, or if there were multiple recipes with differing outputs or input fluid
     * amount.
     *
     * <p>
     * Also will log each case where multiple differing recipes were found.
     */
    Optional<ChemicalBathFluidRecipe> getUniqueChemicalBathOutput(ChemicalBathFluid chemicalBathFluid,
            ItemComponent input) {
        Set<ChemicalBathFluidRecipe> outputs = chemicalBathFluidData.get(chemicalBathFluid).get(input);
        if (outputs.size() > 1) {
            Logger.GREGTECH_5_ORE_PROCESSING
                    .warn("Found {} chemical bath recipes: [{}] [{}]", outputs.size(), chemicalBathFluid, input);

            return Optional.empty();
        } else if (outputs.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Iterables.getOnlyElement(outputs));
    }

    Optional<ItemComponent> getFurnaceRecipeOutput(ItemComponent input) {
        return Optional.ofNullable(furnaceData.get(input));
    }
}
