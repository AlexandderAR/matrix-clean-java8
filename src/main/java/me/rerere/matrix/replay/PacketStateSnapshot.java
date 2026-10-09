package me.rerere.matrix.replay;

import java.util.List;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.TuplesKt;
import me.rerere.matrix.thirdparty.kotlin.NoWhenBranchMatchedException;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.checks.rotation.TrackingWindow;

/** Packet coordinates and rotation together with the preceding packet state. */
public final class PacketStateSnapshot {
    private final double x, y, z, lastX, lastY, lastZ, eyeHeight, lastEyeHeight;
    private final float yaw, pitch, lastYaw, lastPitch;
    private static int candidateModeCode = 22;

    public static void setCandidateModeCode(int value) { candidateModeCode = value; }
    public static int candidateModeCode() { return candidateModeCode; }
    public static int candidateModeStatusCode() {
        return candidateModeCode() == 0 ? 23 : 0;
    }

    public PacketStateSnapshot(double x, double y, double z,
            double lastX, double lastY, double lastZ,
            double eyeHeight, double lastEyeHeight,
            float yaw, float pitch, float lastYaw, float lastPitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.lastX = lastX;
        this.lastY = lastY;
        this.lastZ = lastZ;
        this.eyeHeight = eyeHeight;
        this.lastEyeHeight = lastEyeHeight;
        this.yaw = yaw;
        this.pitch = pitch;
        this.lastYaw = lastYaw;
        this.lastPitch = lastPitch;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getLastX() { return lastX; }
    public double getLastY() { return lastY; }
    public double getLastZ() { return lastZ; }
    public double getEyeHeight() { return eyeHeight; }
    public double getLastEyeHeight() { return lastEyeHeight; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getLastYaw() { return lastYaw; }
    public float getLastPitch() { return lastPitch; }

    public PacketStateSnapshot copy(double x, double y, double z,
            double lastX, double lastY, double lastZ,
            double eyeHeight, double lastEyeHeight,
            float yaw, float pitch, float lastYaw, float lastPitch) {
        return new PacketStateSnapshot(x, y, z, lastX, lastY, lastZ,
                eyeHeight, lastEyeHeight, yaw, pitch, lastYaw, lastPitch);
    }

    public List<Pair<Float, Float>> getRotationCandidates(TrackingWindow mode) {
        switch (mode) {
            case CURRENT_ONLY:
                return CollectionsKt.listOf(TuplesKt.to(yaw, pitch));
            case CURRENT_AND_LAST_YAW:
                Pair<Float, Float>[] yawCandidates = new Pair[2];
                yawCandidates[0] = TuplesKt.to(yaw, pitch);
                yawCandidates[1] = TuplesKt.to(lastYaw, pitch);
                return CollectionsKt.distinct(CollectionsKt.listOf(yawCandidates));
            case ALL:
                List<Pair<Float, Float>> rotationCandidates = CollectionsKt.createListBuilder();
                rotationCandidates.add(TuplesKt.to(yaw, pitch));
                rotationCandidates.add(TuplesKt.to(lastYaw, pitch));
                rotationCandidates.add(TuplesKt.to(yaw, lastPitch));
                rotationCandidates.add(TuplesKt.to(lastYaw, lastPitch));
                return CollectionsKt.distinct(CollectionsKt.build(rotationCandidates));
            default:
                throw new NoWhenBranchMatchedException();
        }
    }

    public List<Vector> getPositionCandidates() {
        Vector currentPosition = new Vector(x, y, z);
        Vector previousPosition = new Vector(lastX, lastY, lastZ);
        if (Intrinsics.areEqual(currentPosition, previousPosition)) {
            return CollectionsKt.listOf(currentPosition);
        }
        Vector[] candidates = new Vector[] { currentPosition, previousPosition };
        return CollectionsKt.listOf(candidates);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PacketStateSnapshot)) return false;
        PacketStateSnapshot state = (PacketStateSnapshot) other;
        return Double.compare(x, state.x) == 0
                && Double.compare(y, state.y) == 0
                && Double.compare(z, state.z) == 0
                && Double.compare(lastX, state.lastX) == 0
                && Double.compare(lastY, state.lastY) == 0
                && Double.compare(lastZ, state.lastZ) == 0
                && Double.compare(eyeHeight, state.eyeHeight) == 0
                && Double.compare(lastEyeHeight, state.lastEyeHeight) == 0
                && Float.compare(yaw, state.yaw) == 0
                && Float.compare(pitch, state.pitch) == 0
                && Float.compare(lastYaw, state.lastYaw) == 0
                && Float.compare(lastPitch, state.lastPitch) == 0;
    }

    @Override
    public int hashCode() {
        int hash = Double.hashCode(x);
        hash = hash * 31 + Double.hashCode(y);
        hash = hash * 31 + Double.hashCode(z);
        hash = hash * 31 + Double.hashCode(lastX);
        hash = hash * 31 + Double.hashCode(lastY);
        hash = hash * 31 + Double.hashCode(lastZ);
        hash = hash * 31 + Double.hashCode(eyeHeight);
        hash = hash * 31 + Double.hashCode(lastEyeHeight);
        hash = hash * 31 + Float.hashCode(yaw);
        hash = hash * 31 + Float.hashCode(pitch);
        hash = hash * 31 + Float.hashCode(lastYaw);
        return hash * 31 + Float.hashCode(lastPitch);
    }

    @Override
    public String toString() {
        return "PacketStateSnapshot(x=" + x + ", y=" + y + ", z=" + z
                + ", lastX=" + lastX + ", lastY=" + lastY + ", lastZ=" + lastZ
                + ", eyeHeight=" + eyeHeight + ", lastEyeHeight=" + lastEyeHeight
                + ", yaw=" + yaw + ", pitch=" + pitch + ", lastYaw=" + lastYaw
                + ", lastPitch=" + lastPitch + ")";
    }
}
