package com.github.dcysteine.neicustomdiagram.generators.enderstorage.chestoverview;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

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
import com.github.dcysteine.neicustomdiagram.net.MessageEnderStorageReq;
import com.github.dcysteine.neicustomdiagram.util.enderstorage.EnderStorageFrequency;
import com.github.dcysteine.neicustomdiagram.util.enderstorage.EnderStorageUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import codechicken.enderstorage.api.EnderStorageManager;
import codechicken.enderstorage.storage.item.EnderItemStorage;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.util.NBTJson;

public final class EnderStorageChestOverview implements DiagramGenerator {

    public static EnderStorageChestOverview INSTANCE;

    public static final ItemComponent ICON = EnderStorageUtil.getItem(EnderStorageUtil.Type.CHEST);
    public static final String LOOKUP_GLOBAL_CHESTS_SUFFIX = "-global";
    public static final String LOOKUP_PERSONAL_CHESTS_SUFFIX = "-personal";

    private static final ImmutableList<EnderStorageUtil.Type> ACCEPTED_TYPES = ImmutableList
            .of(EnderStorageUtil.Type.CHEST, EnderStorageUtil.Type.POUCH);

    private static final ItemComponent GLOBAL_ICON = ItemComponent.create(Items.wooden_door, 0);
    private static final ItemComponent PERSONAL_ICON = ItemComponent.create(Items.iron_door, 0);
    private static final CustomInteractable GLOBAL_LABEL = CustomInteractable
            .builder(ComponentLabel.create(GLOBAL_ICON, Grid.GRID.grid(4, 0)))
            .setTooltip(Tooltip.create(Lang.ENDER_STORAGE_CHEST_OVERVIEW.trans("globallabel"), Tooltip.INFO_FORMATTING))
            .build();
    private static final CustomInteractable PERSONAL_LABEL = CustomInteractable
            .builder(ComponentLabel.create(PERSONAL_ICON, Grid.GRID.grid(4, 0)))
            .setTooltip(
                    Tooltip.create(Lang.ENDER_STORAGE_CHEST_OVERVIEW.trans("personallabel"), Tooltip.INFO_FORMATTING))
            .build();

    private static final Layout.SlotGroupKey SLOT_GROUP_FREQUENCY = Layout.SlotGroupKey.create("frequency");
    private static final Layout.SlotGroupKey SLOT_GROUP_INVENTORY = Layout.SlotGroupKey.create("inventory");

    private final DiagramGroupInfo info;
    private Layout layout;
    private Layout noDataLayout;

    private static JsonArray globalData = new JsonArray();
    private static JsonArray personalData = new JsonArray();
    private static boolean skipRemote = false;

    public EnderStorageChestOverview(String groupId) {
        this.info = DiagramGroupInfo.builder(Lang.ENDER_STORAGE_CHEST_OVERVIEW.trans("groupname"), groupId, ICON, 4)
                .setDescription(
                        "This diagram displays ender chest used frequencies and contents."
                                + "\nUnfortunately, it doesn't work well on servers.")
                .build();
        INSTANCE = this;
    }

    @Override
    public DiagramGroupInfo info() {
        return info;
    }

    @Override
    public CustomDiagramGroup generate() {
        layout = buildLayout();
        noDataLayout = buildNoDataLayout();

        ImmutableMap<String, Supplier<Collection<Diagram>>> customBehaviorMap = ImmutableMap.of(
                info.groupId() + LOOKUP_GLOBAL_CHESTS_SUFFIX,
                () -> generateDiagrams(EnderStorageUtil.Owner.GLOBAL),
                info.groupId() + LOOKUP_PERSONAL_CHESTS_SUFFIX,
                () -> generateDiagrams(EnderStorageUtil.Owner.PERSONAL));
        return new CustomDiagramGroup(info, new CustomDiagramMatcher(this::generateDiagrams), customBehaviorMap);
    }

    private Collection<Diagram> generateDiagrams(Interactable.RecipeType recipeType, Component component) {
        Optional<EnderStorageUtil.Type> type = EnderStorageUtil.getType(component);
        if (!type.isPresent() || !ACCEPTED_TYPES.contains(type.get())) {
            return Lists.newArrayList();
        }

        return generateDiagrams(EnderStorageUtil.Owner.GLOBAL, true);
    }

    private Collection<Diagram> generateDiagrams(EnderStorageUtil.Owner owner) {
        return generateDiagrams(owner, false);
    }

    private Collection<Diagram> generateDiagrams(EnderStorageUtil.Owner owner, boolean recipeInit) {
        Map<EnderStorageFrequency, EnderItemStorage> chests;
        if (Minecraft.getMinecraft().isSingleplayer()) {
            chests = EnderStorageUtil.getEnderChests(owner);
        } else {
            if (recipeInit) return Lists.newArrayList(buildNoDataDiagram(owner));

            chests = new HashMap<>();
            JsonArray data;
            if (!skipRemote) {
                new MessageEnderStorageReq(owner, EnderStorageUtil.Type.CHEST).sendToServer();
                return Lists.newArrayList(buildNoDataDiagram(owner));
            } else {
                skipRemote = false;
            }
            switch (owner) {
                case GLOBAL:
                    data = globalData;
                    break;
                case PERSONAL:
                    data = personalData;
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled owner: " + owner);
            }
            for (JsonElement jsonElement : data) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                EnderItemStorage value = new EnderItemStorage(
                        EnderStorageManager.instance(false),
                        owner.stringParam(),
                        jsonObject.get("frequency").getAsInt());
                value.loadFromTag((NBTTagCompound) NBTJson.toNbt(jsonObject.get("tag")));
                chests.put(EnderStorageFrequency.create(jsonObject.get("frequency").getAsInt()), value);
            }
        }

        List<Diagram> diagrams = chests.entrySet().stream().filter(entry -> !EnderStorageUtil.isEmpty(entry.getValue()))
                .map(entry -> buildDiagram(owner, entry.getKey(), entry.getValue())).collect(Collectors.toList());

        if (diagrams.isEmpty()) {
            return Lists.newArrayList(buildNoDataDiagram(owner));
        } else {
            return diagrams;
        }
    }

    private Diagram buildDiagram(EnderStorageUtil.Owner owner, EnderStorageFrequency frequency,
            EnderItemStorage storage) {
        Diagram.Builder builder = Diagram.builder().addLayout(layout);
        switch (owner) {
            case GLOBAL:
                builder.addInteractable(GLOBAL_LABEL);
                break;

            case PERSONAL:
                builder.addInteractable(PERSONAL_LABEL);
                break;
        }

        builder.autoInsertIntoSlotGroup(SLOT_GROUP_FREQUENCY).insertIntoNextSlot(frequency.colour1().icon())
                .insertIntoNextSlot(frequency.colour2().icon()).insertIntoNextSlot(frequency.colour3().icon());

        Diagram.Builder.SlotGroupManualSubBuilder slotBuilder = builder.manualInsertIntoSlotGroup(SLOT_GROUP_INVENTORY);
        for (int i = 0; i < EnderStorageUtil.getChestSize(); i++) {
            ItemStack itemStack = storage.getStackInSlot(i);
            if (itemStack != null) {
                slotBuilder.insertIntoSlot(i % 9, i / 9, DisplayComponent.builderWithNbt(itemStack).build());
            }
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

    private Layout buildLayout() {
        int inventoryRows = EnderStorageUtil.getChestSize() / 9;
        return Layout.builder().addInteractable(buildGlobalButton()).addInteractable(buildPersonalButton())
                .putSlotGroup(
                        SLOT_GROUP_FREQUENCY,
                        SlotGroup.builder(3, 1, Grid.GRID.grid(6, 0), Grid.Direction.E)
                                .setDefaultTooltip(
                                        Tooltip.create(
                                                Lang.ENDER_STORAGE_CHEST_OVERVIEW.trans("frequencyslot"),
                                                Tooltip.SLOT_FORMATTING))
                                .build())
                .putSlotGroup(
                        SLOT_GROUP_INVENTORY,
                        SlotGroup.builder(9, inventoryRows, Grid.GRID.grid(6, 2), Grid.Direction.S)
                                .setDefaultTooltip(
                                        Tooltip.create(
                                                Lang.ENDER_STORAGE_CHEST_OVERVIEW.trans("inventoryslot"),
                                                Tooltip.SLOT_FORMATTING))
                                .build())
                .build();
    }

    private Layout buildNoDataLayout() {
        return Layout.builder().addInteractable(buildGlobalButton()).addInteractable(buildPersonalButton())
                .addLabel(
                        Text.builder(
                                Lang.ENDER_STORAGE_CHEST_OVERVIEW.trans("nodataheader"),
                                Grid.GRID.grid(0, 2),
                                Grid.Direction.E).build())
                .addAllLabels(
                        Text.multiLineBuilder(Grid.GRID.grid(0, 3), Grid.Direction.SE).setSmall(true)
                                .addLine(Lang.ENDER_STORAGE_CHEST_OVERVIEW.trans("nodatasubheader1"))
                                .addLine(Lang.ENDER_STORAGE_CHEST_OVERVIEW.trans("nodatasubheader2")).build())
                .build();
    }

    private CustomInteractable buildGlobalButton() {
        return CustomInteractable.builder(ComponentLabel.create(GLOBAL_ICON, Grid.GRID.grid(0, 0))).setTooltip(
                Tooltip.create(Lang.ENDER_STORAGE_CHEST_OVERVIEW.trans("globalbutton"), Tooltip.SPECIAL_FORMATTING))
                .setInteract(info.groupId() + LOOKUP_GLOBAL_CHESTS_SUFFIX).setDrawBackground(Draw::drawRaisedSlot)
                .setDrawOverlay(pos -> Draw.drawOverlay(pos, Draw.Colour.OVERLAY_BLUE)).build();
    }

    private CustomInteractable buildPersonalButton() {
        return CustomInteractable.builder(ComponentLabel.create(PERSONAL_ICON, Grid.GRID.grid(2, 0)))
                .setTooltip(
                        Tooltip.builder().setFormatting(Tooltip.SPECIAL_FORMATTING)
                                .addTextLine(Lang.ENDER_STORAGE_CHEST_OVERVIEW.trans("personalbutton")).addSpacing()
                                .setFormatting(Tooltip.INFO_FORMATTING)
                                .addTextLine(Lang.ENDER_STORAGE_CHEST_OVERVIEW.trans("personalitemlabel"))
                                .addComponent(EnderStorageUtil.getPersonalItem()).build())
                .setInteract(info.groupId() + LOOKUP_PERSONAL_CHESTS_SUFFIX).setDrawBackground(Draw::drawRaisedSlot)
                .setDrawOverlay(pos -> Draw.drawOverlay(pos, Draw.Colour.OVERLAY_BLUE)).build();
    }

    public void updateJsonData(EnderStorageUtil.Owner owner, JsonArray newData) {
        Preconditions.checkNotNull(owner, "Owner cannot be null");
        Preconditions.checkNotNull(newData, "New data cannot be null");

        String id = info.groupId();
        switch (owner) {
            case GLOBAL:
                globalData = newData;
                id += LOOKUP_GLOBAL_CHESTS_SUFFIX;
                break;
            case PERSONAL:
                personalData = newData;
                id += LOOKUP_PERSONAL_CHESTS_SUFFIX;
                break;
            default:
                throw new IllegalArgumentException("Unhandled owner: " + owner);
        }
        skipRemote = true;
        GuiUsageRecipe.openRecipeGui(id);
    }
}
