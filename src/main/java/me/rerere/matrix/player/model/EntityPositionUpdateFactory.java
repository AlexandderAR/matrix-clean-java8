package me.rerere.matrix.player.model;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.LinearPositionPath;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.vecdelta.VecDelta;

public final class EntityPositionUpdateFactory {
    EntityPositionUpdateFactory() {
    }

    public final EntityPositionUpdate fromPath(
            PositionPath path, float yaw, float pitch, boolean positionSync) {
        return new EntityPositionUpdate(
                currentPosition -> path, Float.valueOf(yaw), Float.valueOf(pitch), positionSync);
    }

    public final EntityPositionUpdate fromDelta(VecDelta delta, Float yaw, Float pitch) {
        return new EntityPositionUpdate(
                currentPosition -> delta.applyAsPath(currentPosition), yaw, pitch, false);
    }

    public final EntityPositionUpdate rotationOnly(float yaw, float pitch) {
        return new EntityPositionUpdate(
                currentPosition -> null, Float.valueOf(yaw), Float.valueOf(pitch), false);
    }

    public final EntityPositionUpdate fromOffset(
            double deltaX, double deltaY, double deltaZ, Float yaw, Float pitch) {
        return new EntityPositionUpdate(
                currentPosition -> new LinearPositionPath(currentPosition.add(deltaX, deltaY, deltaZ)),
                yaw, pitch, false);
    }

    public static EntityPositionUpdate fromDeltaDefaults(
                                                        EntityPositionUpdateFactory factory, VecDelta delta,
                                                        Float yaw, Float pitch, int defaultMask, Object unusedMarker) {
        if ((defaultMask & 2) != 0) {
            yaw = null;
        }
        if ((defaultMask & 4) != 0) {
            pitch = null;
        }
        return factory.fromDelta(delta, yaw, pitch);
    }

    public static EntityPositionUpdate fromPathDefaults(
                                                       EntityPositionUpdateFactory factory, PositionPath path,
                                                       float yaw, float pitch, boolean positionSync,
                                                       int defaultMask, Object unusedMarker) {
        if ((defaultMask & 8) != 0) {
            positionSync = false;
        }
        return factory.fromPath(path, yaw, pitch, positionSync);
    }

    public static EntityPositionUpdate fromOffsetDefaults(
                                                         EntityPositionUpdateFactory factory,
                                                         double deltaX, double deltaY, double deltaZ,
                                                         Float yaw, Float pitch, int defaultMask, Object unusedMarker) {
        if ((defaultMask & 8) != 0) {
            yaw = null;
        }
        if ((defaultMask & 16) != 0) {
            pitch = null;
        }
        return factory.fromOffset(deltaX, deltaY, deltaZ, yaw, pitch);
    }
}
