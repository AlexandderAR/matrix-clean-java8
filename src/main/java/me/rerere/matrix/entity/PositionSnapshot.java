package me.rerere.matrix.entity;

import me.rerere.matrix.thirdparty.kotlin.TuplesKt;
import me.rerere.matrix.util.GeometryMath;
import org.bukkit.util.Vector;

/** Immutable position and rotation values with cached Bukkit vector forms. */
public final class PositionSnapshot {
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final Vector positionVector;

    public PositionSnapshot(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.positionVector = new Vector(x, y, z);
    }

    public float getPitch() {
        return pitch;
    }

    public Vector getDirectionVector() {
        return GeometryMath.directionFromYawPitch(TuplesKt.to(Float.valueOf(yaw), Float.valueOf(pitch)));
    }

    public double getX() {
        return x;
    }

    public Vector getPositionVector() {
        return positionVector;
    }

    public double getY() {
        return y;
    }

    public float getYaw() {
        return yaw;
    }

    public double getZ() {
        return z;
    }
}
