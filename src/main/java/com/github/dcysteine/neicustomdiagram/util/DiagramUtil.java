package com.github.dcysteine.neicustomdiagram.util;

import java.util.function.Predicate;

import com.github.dcysteine.neicustomdiagram.api.diagram.Diagram;

public final class DiagramUtil {

    // Static class.
    private DiagramUtil() {}

    /**
     * Builds a simple empty diagram predicate that requires the diagram to have at least {@code minInsertedComponents}
     * components inserted to be considered non-empty.
     */
    public static Predicate<Diagram> buildEmptyDiagramPredicate(int minInsertedComponents) {
        return diagram -> diagram.slotInsertions().size() < minInsertedComponents;
    }
}
