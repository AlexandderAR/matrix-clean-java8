package me.rerere.matrix.player;

import java.util.Objects;
import java.util.UUID;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.listeners.VelocityExplosionType;

/** Velocity sample data recovered from the original immutable Kotlin data class. */
public final class VelocityData {
    private final double x;
    private final double y;
    private final double z;
    private final VelocityExplosionType type;
    private final long time;
    private final UUID id;
    private boolean applied;
    private long appliedAt;

    public VelocityData(double x, double y, double z, VelocityExplosionType type,
                        long time, UUID id, boolean applied, long appliedAt) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.time = time;
        this.id = id;
        this.applied = applied;
        this.appliedAt = appliedAt;
    }

    public VelocityData(double x, double y, double z, VelocityExplosionType type,
                        long time, UUID id, boolean applied, long appliedAt,
                        int mask, DefaultConstructorMarker marker) {
        this(x, y, z, type,
                (mask & 16) != 0 ? System.currentTimeMillis() : time,
                (mask & 32) != 0 ? UUID.randomUUID() : id,
                (mask & 64) != 0 ? false : applied,
                (mask & 128) != 0 ? 0L : appliedAt);
    }

    public VelocityData copy(double x, double y, double z, VelocityExplosionType type,
                             long time, UUID id, boolean applied, long appliedAt) {
        return new VelocityData(x, y, z, type, time, id, applied, appliedAt);
    }

    public long getAppliedAt() { return appliedAt; }
    public double getY() { return y; }
    public VelocityExplosionType getType() { return type; }
    public double getZ() { return z; }
    public UUID getId() { return id; }
    public long getTime() { return time; }
    public boolean isApplied() { return applied; }
    public double getX() { return x; }
    public void setApplied(boolean value) { applied = value; }
    public void setAppliedAt(long value) { appliedAt = value; }

    private static String formatValues(double x, double y, double z,
            VelocityExplosionType type, long time, UUID id, boolean applied, long appliedAt) {
        return new StringBuilder("Data(x=").append(x).append(", y=").append(y)
                .append(", z=").append(z).append(", type=").append(type)
                .append(", time=").append(time).append(", id=").append(id)
                .append(", applied=").append(applied).append(", appliedAt=")
                .append(appliedAt).append(")").toString();
    }

    @Override
    public String toString() {
        return formatValues(x, y, z, type, time, id, applied, appliedAt);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || other.getClass() != getClass()) return false;
        return Objects.equals(id, ((VelocityData) other).id);
    }
}
