package com.github.dcysteine.neicustomdiagram.generators.gregtech5.materialtools;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import com.github.dcysteine.neicustomdiagram.api.Formatter;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.DisplayComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip;
import com.github.dcysteine.neicustomdiagram.main.Lang;
import com.github.dcysteine.neicustomdiagram.main.Mods;
import com.github.dcysteine.neicustomdiagram.util.gregtech5.GregTechFormatting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.ruling_0.materiallib.api.Material;

import detrav.items.DetravMetaGeneratedTool01;
import gregtech.api.items.MetaGeneratedTool;
import gregtech.api.recipe.RecipeMaps;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.MetaGeneratedTool01;
import gtPlusPlus.xmod.gregtech.common.items.MetaGeneratedGregtechTools;

/**
 * Class that finds GregTech tools by looking up recipes, and provides access to them by primary material.
 */
class RecipeHandler {

    /**
     * This is basically a struct class that holds an {@link ItemComponent} without NBT, as well as additional data that
     * would normally be stored in NBT. We use this as a key in our multimaps to allow us to group together tools with
     * different NBT but that are the same base tool.
     */
    private static class BaseTool {

        public final ItemComponent itemComponent;
        public final Material primaryMaterial;

        private BaseTool(ItemStack itemStack) {
            itemComponent = ItemComponent.create(itemStack);
            primaryMaterial = MetaGeneratedTool.getPrimaryMaterialML(itemStack);
        }

        public static BaseTool create(ItemStack itemStack) {
            return new BaseTool(itemStack);
        }

        @Override
        public boolean equals(Object object) {
            if (object == this) return true;
            if (!(object instanceof BaseTool)) return false;
            BaseTool baseTool = (BaseTool) object;
            return baseTool.primaryMaterial == primaryMaterial && baseTool.itemComponent.item() == itemComponent.item()
                    && baseTool.itemComponent.damage() == itemComponent.damage();
        }

        @Override
        public int hashCode() {
            int result = primaryMaterial.getIndex();
            result = 31 * result + itemComponent.item().hashCode();
            result = 31 * result + itemComponent.damage();
            return result;
        }
    }

    /** Comparator that takes EU capacity into account. */
    private static final Comparator<DisplayComponent> EU_CAPACITY_COMPARATOR = Comparator
            .<DisplayComponent, Long>comparing(d -> getEuCapacity((ItemComponent) d.component()).orElse(-1L))
            .thenComparing(Comparator.naturalOrder());

    private static final ImmutableList<Integer> TURBINE_TOOL_IDS = ImmutableList.of(
            IDMetaTool01.TURBINE_SMALL.ID,
            IDMetaTool01.TURBINE.ID,
            IDMetaTool01.TURBINE_LARGE.ID,
            IDMetaTool01.TURBINE_HUGE.ID);

    private static final int ELECTRIC_SCANNER_ID_START = 100;

    /**
     * Map of base tool (without NBT) to a set of tool item components (with NBT).
     * <p>
     * This is an intermediary data structure which we use to group tools together ignoring extraneous NBT such as
     * electrical stats.
     */
    private final HashMap<BaseTool, HashSet<ItemComponent>> tools = new HashMap<>();

    /**
     * Map of base tool (without NBT) to sorted set of tool item components (with NBT).
     * <p>
     * This is an intermediary data structure which we use to group tools together ignoring extraneous NBT such as
     * electrical stats.
     */
    private final HashMap<BaseTool, HashSet<ItemComponent>> gtPlusPlusTools = new HashMap<>();

    /**
     * Map of base tool (without NBT) to sorted set of Detrav scanner components (with NBT).
     * <p>
     * This is an intermediary data structure which we use to group tools together ignoring extraneous NBT such as
     * electrical stats.
     */
    private final HashMap<BaseTool, HashSet<ItemComponent>> scanners = new HashMap<>();

    /**
     * Multimap of material to list of lists of tools with that primary material.
     * <p>
     * We group together tools with the same base item but different NBT (which will be electrical stats). This is why
     * values will be lists of lists.
     */
    private final ListMultimap<Material, ImmutableList<DisplayComponent>> materialToolsMultimap;

    /**
     * Multimap of material to list of lists of turbines with that primary material.
     * <p>
     * We group together tools with the same base item but different NBT (which will be electrical stats). This is why
     * values will be lists of lists. Though in practice, turbines don't have electric stats, so each inner list will
     * have size 1.
     */
    private final ListMultimap<Material, ImmutableList<DisplayComponent>> materialTurbinesMultimap;

    /**
     * Multimap of material to list of lists of Detrav scanners with that primary material.
     * <p>
     * We group together tools with the same base item but different NBT (which will be electrical stats). This is why
     * values will be lists of lists. Though in practice, scanners don't have electric stats, so each inner list will
     * have size 1.
     */
    private final ListMultimap<Material, ImmutableList<DisplayComponent>> materialScannersMultimap;

    /**
     * Multimap of material to list of lists of Detrav electric scanners with that primary material.
     * <p>
     * We group together tools with the same base item but different NBT (which will be electrical stats). This is why
     * values will be lists of lists. Though in practice, scanners don't have varying electric stats, so each inner list
     * will have size 1.
     */
    private final ListMultimap<Material, ImmutableList<DisplayComponent>> materialElectricScannersMultimap;

    RecipeHandler() {
        this.materialToolsMultimap = MultimapBuilder.hashKeys().arrayListValues().build();
        this.materialTurbinesMultimap = MultimapBuilder.hashKeys().arrayListValues().build();
        this.materialScannersMultimap = MultimapBuilder.hashKeys().arrayListValues().build();
        this.materialElectricScannersMultimap = MultimapBuilder.hashKeys().arrayListValues().build();
    }

    /** This method must be called before any other methods are called. */
    @SuppressWarnings("unchecked")
    void initialize() {
        // First pass: find all tools with recipes, and group them by base NBT item stack.
        ((List<IRecipe>) CraftingManager.getInstance().getRecipeList())
                .forEach(recipe -> addTool(recipe.getRecipeOutput()));
        RecipeMaps.assemblerRecipes.getAllRecipes().forEach(recipe -> addTool(recipe.getOutput(0)));

        // Second pass: iterate through and construct DisplayComponents for found tools.
        for (HashMap.Entry<BaseTool, HashSet<ItemComponent>> entry : tools.entrySet()) {
            BaseTool baseTool = entry.getKey();
            HashSet<ItemComponent> itemComponents = entry.getValue();

            ImmutableList<DisplayComponent> displayComponents = ImmutableList.copyOf(
                    itemComponents.stream().map(RecipeHandler::buildDisplayComponent).sorted(EU_CAPACITY_COMPARATOR)
                            .collect(Collectors.toList()));

            if (TURBINE_TOOL_IDS.contains(baseTool.itemComponent.damage())) {
                materialTurbinesMultimap.put(baseTool.primaryMaterial, displayComponents);
            } else {
                materialToolsMultimap.put(baseTool.primaryMaterial, displayComponents);
            }
        }

        for (HashMap.Entry<BaseTool, HashSet<ItemComponent>> entry : gtPlusPlusTools.entrySet()) {
            BaseTool baseTool = entry.getKey();
            HashSet<ItemComponent> itemComponents = entry.getValue();

            ImmutableList<DisplayComponent> displayComponents = ImmutableList.copyOf(
                    itemComponents.stream().map(RecipeHandler::buildDisplayComponent).sorted(EU_CAPACITY_COMPARATOR)
                            .collect(Collectors.toList()));

            materialToolsMultimap.put(baseTool.primaryMaterial, displayComponents);
        }

        for (HashMap.Entry<BaseTool, HashSet<ItemComponent>> entry : scanners.entrySet()) {
            BaseTool baseTool = entry.getKey();
            HashSet<ItemComponent> itemComponents = entry.getValue();

            ImmutableList<DisplayComponent> displayComponents = ImmutableList.copyOf(
                    itemComponents.stream().map(RecipeHandler::buildDisplayComponent).sorted(EU_CAPACITY_COMPARATOR)
                            .collect(Collectors.toList()));

            if (baseTool.itemComponent.damage() >= ELECTRIC_SCANNER_ID_START) {
                materialElectricScannersMultimap.put(baseTool.primaryMaterial, displayComponents);
            } else {
                materialScannersMultimap.put(baseTool.primaryMaterial, displayComponents);
            }
        }
    }

    /**
     * Returns a list of lists of tools with the specified primary material.
     *
     * <p>
     * We group together tools with the same base item but different NBT (which will be electrical stats). This is why
     * values will be lists of lists.
     */
    ImmutableList<ImmutableList<DisplayComponent>> getTools(Material material) {
        return ImmutableList.copyOf(materialToolsMultimap.get(material));
    }

    /**
     * Returns a list of lists of turbines with the specified primary material.
     *
     * <p>
     * We group together tools with the same base item but different NBT (which will be electrical stats). This is why
     * values will be lists of lists. Though in practice, turbines don't have electric stats, so each inner list will
     * have size 1.
     */
    ImmutableList<ImmutableList<DisplayComponent>> getTurbines(Material material) {
        return ImmutableList.copyOf(materialTurbinesMultimap.get(material));
    }

    /**
     * Returns a list of lists of Detrav scanners with the specified primary material.
     *
     * <p>
     * We group together tools with the same base item but different NBT (which will be electrical stats). This is why
     * values will be lists of lists. Though in practice, scanners don't have electric stats, so each inner list will
     * have size 1.
     */
    ImmutableList<ImmutableList<DisplayComponent>> getScanners(Material material) {
        return ImmutableList.copyOf(materialScannersMultimap.get(material));
    }

    /**
     * Returns a list of lists of Detrav electric scanners with the specified primary material.
     *
     * <p>
     * We group together tools with the same base item but different NBT (which will be electrical stats). This is why
     * values will be lists of lists. Though in practice, scanners don't have varying electric stats, so each inner list
     * will have size 1.
     */
    ImmutableList<ImmutableList<DisplayComponent>> getElectricScanners(Material material) {
        return ImmutableList.copyOf(materialElectricScannersMultimap.get(material));
    }

    private void addTool(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return;
        }

        if (itemStack.getItem() == MetaGeneratedTool01.INSTANCE) {
            tools.computeIfAbsent(BaseTool.create(itemStack), tool -> new HashSet<>())
                    .add(ItemComponent.createWithNbt(itemStack));
        }

        if (Mods.GT_PLUS_PLUS.isLoaded() && itemStack.getItem() == MetaGeneratedGregtechTools.INSTANCE) {
            gtPlusPlusTools.computeIfAbsent(BaseTool.create(itemStack), tool -> new HashSet<>())
                    .add(ItemComponent.createWithNbt(itemStack));
        }

        if (Mods.DETRAV_SCANNER.isLoaded() && itemStack.getItem() == DetravMetaGeneratedTool01.INSTANCE) {
            scanners.computeIfAbsent(BaseTool.create(itemStack), tool -> new HashSet<>())
                    .add(ItemComponent.createWithNbt(itemStack));
        }
    }

    /** Returns the EU capacity of the given item, if available. */
    private static Optional<Long> getEuCapacity(ItemComponent itemComponent) {
        Long[] electricStats = ((MetaGeneratedTool) itemComponent.item()).getElectricStats(itemComponent.stack());
        if (electricStats == null) {
            return Optional.empty();
        } else {
            // The first entry in electricStats is the max energy capacity.
            return Optional.of(electricStats[0]);
        }
    }

    private static DisplayComponent buildDisplayComponent(ItemComponent itemComponent) {
        DisplayComponent.Builder builder = DisplayComponent.builder(itemComponent);

        ItemStack itemStack = itemComponent.stack();
        Material primaryMaterial = MetaGeneratedTool.getPrimaryMaterialML(itemStack);
        Material secondaryMaterial = MetaGeneratedTool.getSecondaryMaterialML(itemStack);
        builder.setAdditionalTooltip(
                Tooltip.builder().setFormatting(Tooltip.INFO_FORMATTING)
                        .addTextLine(
                                Lang.GREGTECH_5_MATERIAL_TOOLS.transf(
                                        "primarymateriallabel",
                                        GregTechFormatting.getMaterialDescription(primaryMaterial)))
                        .addTextLine(
                                Lang.GREGTECH_5_MATERIAL_TOOLS.transf(
                                        "secondarymateriallabel",
                                        GregTechFormatting.getMaterialDescription(secondaryMaterial)))
                        .build());

        getEuCapacity(itemComponent)
                .ifPresent(euCapacity -> builder.setAdditionalInfo(Formatter.smartFormatInteger(euCapacity)));

        return builder.build();
    }
}
