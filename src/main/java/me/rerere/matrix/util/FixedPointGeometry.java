package me.rerere.matrix.util;

import java.util.List;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;

/** Interpolation, box-center distance and fixed-point conversion used by movement geometry. */
public final class FixedPointGeometry {
    private FixedPointGeometry() {}
    public FixedPointGeometry(DefaultConstructorMarker marker) {}

    public static AxisAlignedBox interpolate(AxisAlignedBox from, AxisAlignedBox to, double alpha) {
        return new AxisAlignedBox(
                from.minX() + (to.minX() - from.minX()) * alpha,
                from.minY() + (to.minY() - from.minY()) * alpha,
                from.minZ() + (to.minZ() - from.minZ()) * alpha,
                from.maxX() + (to.maxX() - from.maxX()) * alpha,
                from.maxY() + (to.maxY() - from.maxY()) * alpha,
                from.maxZ() + (to.maxZ() - from.maxZ()) * alpha);
    }
    public static double midpointDistanceSquared(AxisAlignedBox first, AxisAlignedBox second) {
        double dx = (first.minX() + first.maxX() - second.minX() - second.maxX()) / 2.0;
        double dy = first.minY() - second.minY();
        double dz = (first.minZ() + first.maxZ() - second.minZ() - second.maxZ()) / 2.0;
        return dx * dx + dy * dy + dz * dz;
    }
    public static long quantize(double value) { return Math.round(value * 10000.0); }
    public static long quantize(float value) { return quantize((double) value); }

    /** Returns the six fixed-point coordinates in their original order. */
    public static List<Long> quantizedCoordinates(AxisAlignedBox bounds) {
        Long[] coordinates = new Long[6];
        coordinates[0] = quantize(bounds.minX());
        coordinates[1] = quantize(bounds.minY());
        coordinates[2] = quantize(bounds.minZ());
        coordinates[3] = quantize(bounds.maxX());
        coordinates[4] = quantize(bounds.maxY());
        coordinates[5] = quantize(bounds.maxZ());
        return java.util.Arrays.asList(coordinates);
    }
}
