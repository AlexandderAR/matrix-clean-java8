package me.rerere.matrix.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

/** Entity type groups consumed by entity classification checks. */
public final class EntityTypeGroups {
    private static final Set<EntityType> END_CRYSTAL_TYPES;
    private static final Set<EntityType> ARMOR_STAND_TYPES;
    private static final Set<EntityType> TARGET_ENTITY_TYPES;

    static {
        END_CRYSTAL_TYPES = Collections.singleton(EntityTypes.END_CRYSTAL);
        ARMOR_STAND_TYPES = Collections.singleton(EntityTypes.ARMOR_STAND);
        TARGET_ENTITY_TYPES = new LinkedHashSet<>(Arrays.asList(
                EntityTypes.PLAYER, EntityTypes.ZOMBIE, EntityTypes.VILLAGER,
                EntityTypes.SKELETON, EntityTypes.HOGLIN, EntityTypes.ENDERMAN));
    }

    private EntityTypeGroups() { }

    public static final Set<EntityType> endCrystalTypes() {
        return END_CRYSTAL_TYPES;
    }

    public static final Set<EntityType> targetEntityTypes() {
        return TARGET_ENTITY_TYPES;
    }

    public static final Set<EntityType> armorStandTypes() {
        return ARMOR_STAND_TYPES;
    }
}
