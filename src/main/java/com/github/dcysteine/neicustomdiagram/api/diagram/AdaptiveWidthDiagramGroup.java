package com.github.dcysteine.neicustomdiagram.api.diagram;

import java.util.Collection;
import java.util.function.Supplier;

import com.github.dcysteine.neicustomdiagram.api.diagram.matcher.ComponentDiagramMatcher;
import com.github.dcysteine.neicustomdiagram.api.diagram.matcher.DiagramMatcher;
import com.google.common.collect.ImmutableMap;

import codechicken.nei.recipe.GuiRecipeTab;
import codechicken.nei.recipe.HandlerInfo;

public class AdaptiveWidthDiagramGroup extends CustomDiagramGroup {

    protected HandlerInfo handlerInfo;

    public AdaptiveWidthDiagramGroup(DiagramGroupInfo info, ComponentDiagramMatcher matcher,
            ImmutableMap<String, Supplier<Collection<Diagram>>> customBehaviorMap) {
        super(info, matcher, customBehaviorMap);
        this.handlerInfo = GuiRecipeTab.getHandlerInfo(this.getHandlerId(), null);
    }

    public AdaptiveWidthDiagramGroup(DiagramGroupInfo info, DiagramMatcher matcher) {
        super(info, matcher, ImmutableMap.of());
    }

    public AdaptiveWidthDiagramGroup(CustomDiagramGroup parent, Iterable<? extends Diagram> diagrams) {
        super(parent, diagrams);
        this.handlerInfo = GuiRecipeTab.getHandlerInfo(this.getHandlerId(), null);
    }

    @Override
    public AdaptiveWidthDiagramGroup newInstance(Iterable<? extends Diagram> diagrams) {
        return new AdaptiveWidthDiagramGroup(this, diagrams);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        int width = this.diagrams.get(0).dimension(diagramState).width() + 3;
        this.handlerInfo.setHandlerDimensions(width, this.handlerInfo.getHeight());
    }
}
