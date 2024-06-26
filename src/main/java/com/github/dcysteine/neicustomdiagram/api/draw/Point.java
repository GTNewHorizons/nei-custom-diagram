package com.github.dcysteine.neicustomdiagram.api.draw;

import com.google.auto.value.AutoValue;

/**
 * Immutable class representing a point in 2D space.
 *
 * <p>
 * While this class's contents are the same as those of {@link Vector} and {@link Dimension}, for type-safety, we treat
 * this as a separate class.
 */
@AutoValue
public abstract class Point {

    public static Point create(int x, int y) {
        return new AutoValue_Point(x, y);
    }

    public abstract int x();

    public abstract int y();

    /**
     * Returns the point that is the projection of {@code b} onto the vertical line intersecting {@code a}.
     *
     * <p>
     * This method is useful for drawing lines to connect two points that aren't orthogonal.
     */
    public static Point projectX(Point a, Point b) {
        return Point.create(a.x(), b.y());
    }

    /**
     * Returns the point that is the projection of {@code b} onto the horizontal line intersecting {@code a}.
     *
     * <p>
     * This method is useful for drawing lines to connect two points that aren't orthogonal.
     */
    public static Point projectY(Point a, Point b) {
        return Point.create(b.x(), a.y());
    }

    /** Returns a new point object; the original is not modified. */
    public Point translate(int dx, int dy) {
        return Point.create(x() + dx, y() + dy);
    }

    /** Returns a new point object; the original is not modified. */
    public Point translate(Vector other) {
        return Point.create(x() + other.x(), y() + other.y());
    }

    public Vector difference(Point other) {
        return Vector.create(x() - other.x(), y() - other.y());
    }

    public boolean isOrthogonal(Point pos) {
        return x() == pos.x() || y() == pos.y();
    }
}
