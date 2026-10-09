package me.rerere.matrix.geometry;

/** Immutable three-dimensional position used for block and movement comparisons. */
public final class DuplicatePosition {
    private final double x;
    private final double y;
    private final double z;
    public static final LocationVectorFactory LOCATION_FACTORY =
            new LocationVectorFactory();

    public DuplicatePosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public double component1() { return x; }
    public double component2() { return y; }
    public double component3() { return z; }

    public DuplicatePosition of(double x, double y, double z) {
        return new DuplicatePosition(x, y, z);
    }

    public static DuplicatePosition copyWithDefaults(
            DuplicatePosition original, double x, double y, double z, int defaultMask) {
        if ((defaultMask & 1) != 0) x = original.x;
        if ((defaultMask & 2) != 0) y = original.y;
        if ((defaultMask & 4) != 0) z = original.z;
        return original.of(x, y, z);
    }

    public static DuplicatePosition fromLocation(org.bukkit.Location location) {
        return LOCATION_FACTORY.fromLocation(location);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DuplicatePosition)) return false;
        DuplicatePosition that = (DuplicatePosition) other;
        return Double.compare(x, that.x) == 0
                && Double.compare(y, that.y) == 0
                && Double.compare(z, that.z) == 0;
    }

    @Override public int hashCode() {
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        return 31 * result + Double.hashCode(z);
    }

    @Override public String toString() {
        return format(x, y, z);
    }

    private static String format(double x, double y, double z) {
        return new StringBuilder("DuplicatePosition(x=").append(x)
                .append(", y=").append(y)
                .append(", z=").append(z)
                .append(')').toString();
    }
}
