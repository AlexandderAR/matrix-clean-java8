package me.rerere.matrix.player.model;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import me.rerere.matrix.util.cache.CachePolicy;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

public final class EntityInterpolationPolicy {
    public EntityInterpolationPolicy() { }

    /** Returns the interpolation step count used for an entity type. */
    public int getInterpolationSteps(EntityType entityType) {
        if (Intrinsics.areEqual(entityType, EntityTypes.PLAYER)) {
            return 2;
        }
        if (Intrinsics.areEqual(entityType, EntityTypes.MANNEQUIN)) {
            return 2;
        }
        return Intrinsics.areEqual(entityType, EntityTypes.ALLAY) ? 2 : 3;
    }

    /** Selects the cache policy for the entity type. */
    public CachePolicy getMode(EntityType entityType) {
        if (Intrinsics.areEqual(entityType, EntityTypes.SHULKER)) {
            return CachePolicy.SNAP;
        }
        if (EntityTypes.isTypeInstanceOf(entityType, EntityTypes.LIVINGENTITY)) {
            return CachePolicy.STEPPED;
        }
        if (EntityTypes.isTypeInstanceOf(entityType, EntityTypes.BOAT)
                || EntityTypes.isTypeInstanceOf(entityType, EntityTypes.MINECART_ABSTRACT)
                || Intrinsics.areEqual(entityType, EntityTypes.EXPERIENCE_ORB)
                || Intrinsics.areEqual(entityType, EntityTypes.FISHING_BOBBER)) {
            return CachePolicy.LINEAR;
        }
        return CachePolicy.SNAP;
    }
}
