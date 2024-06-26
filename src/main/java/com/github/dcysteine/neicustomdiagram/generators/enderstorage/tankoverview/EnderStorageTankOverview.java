package com.github.dcysteine.neicustomdiagram.generators.enderstorage.tankoverview;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.minecraft.init.Items;

import com.github.dcysteine.neicustomdiagram.api.diagram.CustomDiagramGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.Diagram;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGenerator;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroupInfo;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.Component;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.DisplayComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.CustomInteractable;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.Interactable;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.ComponentLabel;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Grid;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Layout;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.SlotGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Text;
import com.github.dcysteine.neicustomdiagram.api.diagram.matcher.CustomDiagramMatcher;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip;
import com.github.dcysteine.neicustomdiagram.api.draw.Draw;
import com.github.dcysteine.neicustomdiagram.main.Lang;
import com.github.dcysteine.neicustomdiagram.util.enderstorage.EnderStorageFrequency;
import com.github.dcysteine.neicustomdiagram.util.enderstorage.EnderStorageUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import codechicken.enderstorage.storage.liquid.EnderLiquidStorage;

public final class EnderStorageTankOverview implements DiagramGenerator {

    public static final ItemComponent ICON = EnderStorageUtil.getItem(EnderStorageUtil.Type.TANK);
    public static final String LOOKUP_GLOBAL_TANKS_SUFFIX = "-global";
    public static final String LOOKUP_PERSONAL_TANKS_SUFFIX = "-personal";

    private static final ItemComponent GLOBAL_ICON = ItemComponent.create(Items.wooden_door, 0);
    private static final ItemComponent PERSONAL_ICON = ItemComponent.create(Items.iron_door, 0);
    private static final CustomInteractable GLOBAL_LABEL = CustomInteractable
            .builder(ComponentLabel.create(GLOBAL_ICON, Grid.GRID.grid(4, 0)))
            .setTooltip(Tooltip.create(Lang.ENDER_STORAGE_TANK_OVERVIEW.trans("globallabel"), Tooltip.INFO_FORMATTING))
            .build();
    private static final CustomInteractable PERSONAL_LABEL = CustomInteractable
            .builder(ComponentLabel.create(PERSONAL_ICON, Grid.GRID.grid(4, 0)))
            .setTooltip(
                    Tooltip.create(Lang.ENDER_STORAGE_TANK_OVERVIEW.trans("personallabel"), Tooltip.INFO_FORMATTING))
            .build();

    private static final int TANKS_PER_DIAGRAM = 12;
    private static final ImmutableList<Layout.SlotGroupKey> SLOT_GROUP_TANKS = ImmutableList.copyOf(
            IntStream.range(0, TANKS_PER_DIAGRAM).mapToObj(i -> Layout.SlotGroupKey.create("tanks-" + i))
                    .collect(Collectors.toList()));

    private final DiagramGroupInfo info;
    private Layout headerLayout;
    private List<Layout> tankLayouts;
    private Layout noDataLayout;

    public EnderStorageTankOverview(String groupId) {
        this.info = DiagramGroupInfo.builder(Lang.ENDER_STORAGE_TANK_OVERVIEW.trans("groupname"), groupId, ICON, 2)
                .setDescription(
                        "This diagram displays ender tank used frequencies and contents."
                                + "\nUnfortunately, it doesn't work on servers.")
                .build();
    }

    @Override
    public DiagramGroupInfo info() {
        return info;
    }

    @Override
    public CustomDiagramGroup generate() {
        headerLayout = buildHeaderLayout();
        tankLayouts = IntStream.range(0, TANKS_PER_DIAGRAM).mapToObj(EnderStorageTankOverview::buildTanksLayout)
                .collect(Collectors.toList());
        noDataLayout = buildNoDataLayout();

        ImmutableMap<String, Supplier<Collection<Diagram>>> customBehaviorMap = ImmutableMap.of(
                info.groupId() + LOOKUP_GLOBAL_TANKS_SUFFIX,
                () -> generateDiagrams(EnderStorageUtil.Owner.GLOBAL),
                info.groupId() + LOOKUP_PERSONAL_TANKS_SUFFIX,
                () -> generateDiagrams(EnderStorageUtil.Owner.PERSONAL));
        return new CustomDiagramGroup(info, new CustomDiagramMatcher(this::generateDiagrams), customBehaviorMap);
    }

    private Collection<Diagram> generateDiagrams(Interactable.RecipeType recipeType, Component component) {
        Optional<EnderStorageUtil.Type> type = EnderStorageUtil.getType(component);
        if (!type.isPresent() || type.get() != EnderStorageUtil.Type.TANK) {
            return Lists.newArrayList();
        }

        return generateDiagrams(EnderStorageUtil.Owner.GLOBAL);
    }

    private Collection<Diagram> generateDiagrams(EnderStorageUtil.Owner owner) {
        List<Map.Entry<EnderStorageFrequency, EnderLiquidStorage>> tanks = EnderStorageUtil.getEnderTanks(owner)
                .entrySet().stream().filter(entry -> !EnderStorageUtil.isEmpty(entry.getValue()))
                .collect(Collectors.toList());

        // Break up the list into sub-lists of length <= TANKS_PER_DIAGRAM.
        List<Diagram> diagrams = Lists.partition(tanks, TANKS_PER_DIAGRAM).stream()
                .map(subList -> buildDiagram(owner, subList)).collect(Collectors.toList());

        if (diagrams.isEmpty()) {
            return Lists.newArrayList(buildNoDataDiagram(owner));
        } else {
            return diagrams;
        }
    }

    /** {@code tanks} must have size less than or equal to {@code TANKS_PER_DIAGRAM}. */
    private Diagram buildDiagram(EnderStorageUtil.Owner owner,
            List<Map.Entry<EnderStorageFrequency, EnderLiquidStorage>> tanks) {
        Preconditions.checkArgument(tanks.size() <= TANKS_PER_DIAGRAM, "Too many tanks: " + tanks);

        Diagram.Builder builder = Diagram.builder().addLayout(headerLayout).addAllOptionalLayouts(tankLayouts);

        switch (owner) {
            case GLOBAL:
                builder.addInteractable(GLOBAL_LABEL);
                break;

            case PERSONAL:
                builder.addInteractable(PERSONAL_LABEL);
                break;
        }

        for (int i = 0; i < tanks.size(); i++) {
            Map.Entry<EnderStorageFrequency, EnderLiquidStorage> tank = tanks.get(i);
            EnderStorageFrequency frequency = tank.getKey();
            DisplayComponent fluid = DisplayComponent.builderWithNbt(tank.getValue().getFluid()).build();

            builder.autoInsertIntoSlotGroup(SLOT_GROUP_TANKS.get(i)).insertIntoNextSlot(frequency.colour1().icon())
                    .insertIntoNextSlot(frequency.colour2().icon()).insertIntoNextSlot(frequency.colour3().icon())
                    .insertIntoNextSlot(fluid);
        }

        return builder.build();
    }

    private Diagram buildNoDataDiagram(EnderStorageUtil.Owner owner) {
        Diagram.Builder builder = Diagram.builder().addLayout(noDataLayout);
        switch (owner) {
            case GLOBAL:
                builder.addInteractable(GLOBAL_LABEL);
                break;

            case PERSONAL:
                builder.addInteractable(PERSONAL_LABEL);
                break;
        }

        return builder.build();
    }

    private Layout buildHeaderLayout() {
        return Layout.builder().addInteractable(buildGlobalButton()).addInteractable(buildPersonalButton()).build();
    }

    private static Layout buildTanksLayout(int i) {
        // Two-column layout.
        int gridX = i % 2 == 0 ? 0 : 12;
        int gridY = 2 + 2 * (i / 2);
        Grid.Direction direction = i % 2 == 0 ? Grid.Direction.E : Grid.Direction.W;

        return Layout.builder()
                .putSlotGroup(
                        SLOT_GROUP_TANKS.get(i),
                        SlotGroup.builder(4, 1, Grid.GRID.grid(gridX, gridY), direction)
                                .setDefaultTooltip(
                                        Tooltip.create(
                                                Lang.ENDER_STORAGE_TANK_OVERVIEW.trans("frequencyslot"),
                                                Tooltip.SLOT_FORMATTING))
                                .setSlot(
                                        3,
                                        0,
                                        SlotGroup.slotBuilder()
                                                .setTooltip(
                                                        Tooltip.create(
                                                                Lang.ENDER_STORAGE_TANK_OVERVIEW.trans("tankslot"),
                                                                Tooltip.SLOT_FORMATTING))
                                                .build())
                                .build())
                .build();
    }

    private Layout buildNoDataLayout() {
        return Layout.builder().addInteractable(buildGlobalButton()).addInteractable(buildPersonalButton())
                .addLabel(
                        Text.builder(
                                Lang.ENDER_STORAGE_TANK_OVERVIEW.trans("nodataheader"),
                                Grid.GRID.grid(0, 2),
                                Grid.Direction.E).build())
                .addLabel(
                        Text.builder(
                                Lang.ENDER_STORAGE_TANK_OVERVIEW.trans("nodatasubheader"),
                                Grid.GRID.grid(0, 3),
                                Grid.Direction.E).setSmall(true).build())
                .build();
    }

    private CustomInteractable buildGlobalButton() {
        return CustomInteractable.builder(ComponentLabel.create(GLOBAL_ICON, Grid.GRID.grid(0, 0))).setTooltip(
                Tooltip.create(Lang.ENDER_STORAGE_TANK_OVERVIEW.trans("globalbutton"), Tooltip.SPECIAL_FORMATTING))
                .setInteract(info.groupId() + LOOKUP_GLOBAL_TANKS_SUFFIX).setDrawBackground(Draw::drawRaisedSlot)
                .setDrawOverlay(pos -> Draw.drawOverlay(pos, Draw.Colour.OVERLAY_BLUE)).build();
    }

    private CustomInteractable buildPersonalButton() {
        return CustomInteractable.builder(ComponentLabel.create(PERSONAL_ICON, Grid.GRID.grid(2, 0)))
                .setTooltip(
                        Tooltip.builder().setFormatting(Tooltip.SPECIAL_FORMATTING)
                                .addTextLine(Lang.ENDER_STORAGE_TANK_OVERVIEW.trans("personalbutton")).addSpacing()
                                .setFormatting(Tooltip.INFO_FORMATTING)
                                .addTextLine(Lang.ENDER_STORAGE_TANK_OVERVIEW.trans("personalitemlabel"))
                                .addComponent(EnderStorageUtil.getPersonalItem()).build())
                .setInteract(info.groupId() + LOOKUP_PERSONAL_TANKS_SUFFIX).setDrawBackground(Draw::drawRaisedSlot)
                .setDrawOverlay(pos -> Draw.drawOverlay(pos, Draw.Colour.OVERLAY_BLUE)).build();
    }
}
