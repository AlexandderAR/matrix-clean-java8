package me.rerere.matrix.player.model;

import org.bukkit.util.Vector;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

public final class PositionRotation {
    private final Vector position;
    private final float yaw;
    private final float pitch;

    public PositionRotation(Vector position, float yaw, float pitch) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Vector getPosition() { return position; }
    public Vector component1() { return position; }
    public float getYaw() { return yaw; }
    public float component2() { return yaw; }
    public float getPitch() { return pitch; }
    public float component3() { return pitch; }

    public PositionRotation copy(Vector position, float yaw, float pitch) {
        return new PositionRotation(position, yaw, pitch);
    }

    public static PositionRotation copyWithDefaults(PositionRotation original, Vector position,
            float yaw, float pitch, int mask) {
        boolean inverseMode = AxisAlignedBox.isInverseMode();
        if (inverseMode && (mask & 1) != 0) position = original.position;
        if (inverseMode && (mask & 2) != 0) yaw = original.yaw;
        if ((mask & 4) != 0) pitch = original.pitch;
        return original.copy(position, yaw, pitch);
    }

    @Override
    public String toString() {
        return new StringBuilder("LocRot(position=").append(position).append(", yaw=")
                .append(yaw).append(", pitch=").append(pitch).append(")").toString();
    }

    @Override
    public int hashCode() {
        int hash = position.hashCode();
        hash = hash * 31 + Float.hashCode(yaw);
        return hash * 31 + Float.hashCode(pitch);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) { return true; }
        if (!(object instanceof PositionRotation)) { return false; }
        PositionRotation other = (PositionRotation) object;
        if (!Intrinsics.areEqual(position, other.position)) { return false; }
        if (Float.compare(yaw, other.yaw) != 0) { return false; }
        return Float.compare(pitch, other.pitch) == 0;
    }
}
