package me.rerere.matrix.player.model;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;

public final class EntityPositionUpdate {
    public static final EntityPositionUpdateFactory FACTORY = new EntityPositionUpdateFactory();

    private final Function1<? super Vector3d, ? extends PositionPath> pathFactory;
    private final Float yaw;
    private final Float pitch;
    private final boolean positionSync;

    EntityPositionUpdate(Function1<? super Vector3d, ? extends PositionPath> pathFactory,
                         Float yaw, Float pitch, boolean positionSync) {
        this.pathFactory = pathFactory;
        this.yaw = yaw;
        this.pitch = pitch;
        this.positionSync = positionSync;
    }

    public EntityPositionUpdate(Function1<? super Vector3d, ? extends PositionPath> pathFactory,
                                Float yaw, Float pitch, boolean positionSync,
                                DefaultConstructorMarker marker) {
        this(pathFactory, yaw, pitch, positionSync);
    }

    public Float getYaw() {
        return yaw;
    }

    public Float getPitch() {
        return pitch;
    }

    public boolean isPositionSync() {
        return positionSync;
    }

    public PositionPath resolvePath(Vector3d currentPosition) {
        return pathFactory.invoke(currentPosition);
    }
}
