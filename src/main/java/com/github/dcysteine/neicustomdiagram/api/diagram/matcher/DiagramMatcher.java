package com.github.dcysteine.neicustomdiagram.api.diagram.matcher;

import java.util.Collection;

import com.github.dcysteine.neicustomdiagram.api.diagram.Diagram;
import com.github.dcysteine.neicustomdiagram.api.diagram.component.Component;
import com.github.dcysteine.neicustomdiagram.api.diagram.interactable.Interactable;

/** This interface contains the logic for figuring out which diagrams match an item or fluid. */
public interface DiagramMatcher {

    Collection<Diagram> all();

    Collection<Diagram> match(Interactable.RecipeType recipeType, Component component);
}
