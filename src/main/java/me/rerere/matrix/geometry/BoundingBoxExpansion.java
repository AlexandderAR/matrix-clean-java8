package me.rerere.matrix.geometry;

/** Stores a box and the number of expansion steps used to calculate its padding. */
public final class BoundingBoxExpansion {
    private final AxisAlignedBox boundingBox;
    private final int expansionSteps;

    public BoundingBoxExpansion(AxisAlignedBox boundingBox, int expansionSteps) {
        this.boundingBox = boundingBox;
        this.expansionSteps = expansionSteps;
    }

    public AxisAlignedBox getBoundingBox() {
        return boundingBox;
    }

    public int getExpansionSteps() {
        return expansionSteps;
    }

    public double getExpansion() {
        return 0.3 + expansionSteps * 0.1;
    }
}
