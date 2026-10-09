package me.rerere.matrix.world;

import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import me.rerere.matrix.geometry.AxisAlignedBox;

/** Coordinate-wise interpolation and union of the six bounds coordinates. */
public final class EntityBoundsInterpolator {
    private AxisAlignedBox startBounds;
    private final int stepCount;
    private int rangeEnd;
    private final AxisAlignedBox endBounds;
    private int currentIndex;

    public EntityBoundsInterpolator(AxisAlignedBox startBounds,
            AxisAlignedBox endBounds, EntityType entityType) {
        this.startBounds = startBounds;
        this.endBounds = endBounds;
        int steps = 1;
        if (EntityTypes.isTypeInstanceOf(entityType, EntityTypes.BOAT)) steps = 10;
        else if (EntityTypes.isTypeInstanceOf(entityType, EntityTypes.MINECART_ABSTRACT)) steps = 5;
        else if (EntityTypes.isTypeInstanceOf(entityType, EntityTypes.SHULKER)) steps = 1;
        else if (EntityTypes.isTypeInstanceOf(entityType, EntityTypes.LIVINGENTITY)) steps = 3;
        this.stepCount = steps;
        this.rangeEnd = steps;
    }

    public List<AxisAlignedBox> interpolatedBounds() {
        double[] perStepDeltas = perStepDeltas();
        List<AxisAlignedBox> result = new ArrayList<AxisAlignedBox>(10);
        int firstIndex = currentIndex;
        int lastIndex = rangeEnd;
        for (int i = firstIndex; i <= lastIndex; i++) result.add(boundsAtStep(i, perStepDeltas));
        return result;
    }

    public AxisAlignedBox interpolateToIndex() {
        double[] perStepDeltas = perStepDeltas();
        AxisAlignedBox accumulated = boundsAtStep(currentIndex, perStepDeltas);
        int firstIndex = currentIndex + 1;
        int lastIndex = rangeEnd;
        for (int index = firstIndex; index <= lastIndex; index++) {
            accumulated = accumulated.union(boundsAtStep(index, perStepDeltas));
        }
        return accumulated;
    }

    public AxisAlignedBox getEndBounds() {
        return endBounds.copy();
    }

    public void advance(boolean advanceCurrent) {
        if (advanceCurrent) currentIndex = Math.min(currentIndex + 1, stepCount);
        rangeEnd = Math.min(rangeEnd + 1, stepCount);
    }

    public void unionBounds(AxisAlignedBox bounds) {
        startBounds = startBounds.union(bounds);
    }

    // Coordinate order matches the six AxisAlignedBox min/max accessors used below.
    private double[] perStepDeltas() {
        return new double[] {
            (endBounds.minX() - startBounds.minX()) / stepCount,
            (endBounds.minY() - startBounds.minY()) / stepCount,
            (endBounds.minZ() - startBounds.minZ()) / stepCount,
            (endBounds.maxX() - startBounds.maxX()) / stepCount,
            (endBounds.maxY() - startBounds.maxY()) / stepCount,
            (endBounds.maxZ() - startBounds.maxZ()) / stepCount
        };
    }

    private AxisAlignedBox boundsAtStep(int index, double[] perStepDeltas) {
        return new AxisAlignedBox(
            startBounds.minX() + perStepDeltas[0] * index,
            startBounds.minY() + perStepDeltas[1] * index,
            startBounds.minZ() + perStepDeltas[2] * index,
            startBounds.maxX() + perStepDeltas[3] * index,
            startBounds.maxY() + perStepDeltas[4] * index,
            startBounds.maxZ() + perStepDeltas[5] * index);
    }
}
