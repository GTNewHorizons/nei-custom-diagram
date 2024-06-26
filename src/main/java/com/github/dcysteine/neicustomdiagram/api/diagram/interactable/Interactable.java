package com.github.dcysteine.neicustomdiagram.api.diagram.interactable;

import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramState;
import com.github.dcysteine.neicustomdiagram.api.draw.BoundedDrawable;
import com.github.dcysteine.neicustomdiagram.api.draw.Point;

/**
 * This interface represents anything that is drawn on the screen and can be moused over or clicked on.
 */
public interface Interactable extends BoundedDrawable {

    enum RecipeType {
        CRAFTING,
        USAGE;
    }

    default void interact(DiagramState diagramState, RecipeType recipeType) {
        // Default implementation is to do nothing.
    }

    /**
     * Method that is called when mousing over an interactable.
     *
     * <p>
     * Not all interactables are going to want to draw an overlay; in particular, interactables with odd bounding boxes,
     * like text, probably won't want to override this.
     */
    default void drawOverlay(DiagramState diagramState) {
        // Default implementation is to do nothing.
    }

    /**
     * Unlike the other draw methods, tooltips are drawn with absolute mouse coordinates.
     *
     * <p>
     * This is due to how the handle tooltip method gets called.
     */
    default void drawTooltip(DiagramState diagramState, Point mousePos) {
        // Default implementation is to do nothing.
    }

    /** Returns whether the specified point is inside this interactable's bounding box. */
    default boolean checkBoundingBox(Point target) {
        boolean withinX = (target.x() >= position().x() - dimension().width() / 2)
                && (target.x() < position().x() + dimension().width() / 2);
        boolean withinY = (target.y() >= position().y() - dimension().height() / 2)
                && (target.y() < position().y() + dimension().height() / 2);

        return withinX && withinY;
    }
}
