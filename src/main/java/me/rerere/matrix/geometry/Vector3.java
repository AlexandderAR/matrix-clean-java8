package me.rerere.matrix.geometry;

import me.rerere.matrix.util.time.MillisecondSampler;

/** Coordinate triple used by movement and collision calculations. */
public class Vector3 {
    public double x;
    public double y;
    public double z;
    public long identity;

    public Vector3(double x, double y, double z, long identity) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.identity = identity;
    }

    public Vector3(double x, double y, double z) {
        this(x, y, z, MillisecondSampler.currentTimeMillis());
    }

    public Vector3(org.bukkit.Location location) {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        // The Location constructor in the ASM leaves the identity field at its JVM default.
        this.identity = 0L;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public long identityValue() {
        return identity;
    }

    public boolean isVector(Object other) {
        return other instanceof Vector3;
    }

    @Override
    public int hashCode() {
        int result = 1;
        long bits = Double.doubleToLongBits(x());
        result = 59 * result + (int) (bits ^ (bits >>> 32));
        bits = Double.doubleToLongBits(y());
        result = 59 * result + (int) (bits ^ (bits >>> 32));
        bits = Double.doubleToLongBits(z());
        result = 59 * result + (int) (bits ^ (bits >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Vector3)) return false;
        Vector3 that = (Vector3) other;
        if (!that.isVector(this)) return false;
        return Double.compare(x(), that.x()) == 0
                && Double.compare(y(), that.y()) == 0
                && Double.compare(z(), that.z()) == 0;
    }

}
