package com.github.dcysteine.neicustomdiagram.generators.forge.oredictionary;

import codechicken.lib.gui.GuiDraw;
import com.github.dcysteine.neicustomdiagram.api.Lang;
import com.github.dcysteine.neicustomdiagram.api.diagram.Diagram;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGenerator;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroupInfo;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.Component;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.DisplayComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.ItemComponent;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.CustomInteractable;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.Interactable;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Grid;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Layout;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.SlotGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Text;
import com.github.dcysteine.neicustomdiagram.api.diagram.matcher.CustomDiagramMatcher;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip;
import com.github.dcysteine.neicustomdiagram.util.ComponentTransformer;
import com.github.dcysteine.neicustomdiagram.util.OreDictUtil;
import net.minecraft.init.Items;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates diagrams showing Forge ore dictionary data for any item.
 *
 * <p>This diagram generator generates its diagrams dynamically, and so does not support showing all
 * diagrams.
 */
public final class ForgeOreDictionary implements DiagramGenerator {
    public static final ItemComponent ICON = ItemComponent.create(Items.book, 0);

    private static final String SLOT_GROUP_KEY = "key";

    private final DiagramGroupInfo info;

    public ForgeOreDictionary(String groupId) {
        this.info =
                DiagramGroupInfo.create(
                        Lang.FORGE_ORE_DICTIONARY.trans("groupname"),
                        groupId, ICON, 2, false);
    }

    @Override
    public DiagramGroupInfo info() {
        return info;
    }

    @Override
    public DiagramGroup generate() {
        return new DiagramGroup(
                info, new CustomDiagramMatcher(ForgeOreDictionary::generateDiagrams));
    }

    private static Collection<Diagram> generateDiagrams(
            Interactable.RecipeType recipeType, Component component) {
        return OreDictUtil.getOreNames(component).stream()
                .map(ForgeOreDictionary::generateDiagram)
                .collect(Collectors.toList());
    }

    private static Diagram generateDiagram(String oreName) {
        List<ItemComponent> components = OreDictUtil.getComponents(oreName);
        List<List<DisplayComponent>> displayComponentPermutations =
                components.stream()
                        .map(OreDictUtil::getPermutations)
                        .map(ComponentTransformer::transformToDisplay)
                        .collect(Collectors.toList());

        Diagram.Builder builder = Diagram.builder().addLayout(buildLayout(oreName));
        builder.autoInsertIntoSlotGroup(SLOT_GROUP_KEY)
                .insertEachGroupSafe(displayComponentPermutations);

        return builder.build();
    }

    private static Layout buildLayout(String oreName) {
        boolean small = GuiDraw.getStringWidth(oreName) > Grid.TOTAL_WIDTH - 4;
        Text oreNameText =
                Text.builder(oreName, Grid.GRID.grid(6, 0), Grid.Direction.N)
                        .setSmall(small)
                        .build();
        Interactable oreNameLabel =
                CustomInteractable.builder(oreNameText)
                        .setTooltip(
                                Tooltip.create(
                                        Lang.FORGE_ORE_DICTIONARY.trans("orenamelabel"),
                                        Tooltip.SLOT_FORMATTING))
                        .build();

        return Layout.builder()
                .addInteractable(oreNameLabel)
                .putSlotGroup(
                        SLOT_GROUP_KEY,
                        SlotGroup.builder(9, 8, Grid.GRID.grid(6, 1), Grid.Direction.S)
                                .build())
                .build();
    }
}
