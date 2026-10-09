package me.rerere.matrix.entity;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import me.rerere.matrix.geometry.ObservedEntityDimensions;

/** Adapts entity pose dimension lookups to the shared dimension resolver. */
public final class EntityPoseDimensions {
    private EntityPoseDimensions() {
    }

    public static float resolveFirst(EntityType entityType, EntityPose pose) {
        return ObservedEntityDimensions.INSTANCE.getHeight(entityType, pose);
    }

    public static float resolveSecond(EntityType entityType, EntityPose pose) {
        return ObservedEntityDimensions.INSTANCE.getWidth(entityType, pose);
    }
}
