package me.rerere.matrix.runtime;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.EntityType;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.entity.EntityTypeEntries;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;

/** Lazily resolves version-dependent entity names and the boat/raft type set. */
public final class EntityCategories {
    public static final EntityCategories INSTANCE;
    private static final Lazy<EntityType> LEGACY_END_CRYSTAL;
    private static final Lazy<EntityType> FIREWORK;
    private static final Lazy<EnumSet<EntityType>> BOAT_TYPES;

    private EntityCategories() {}

    private static EntityType resolveEndCrystal() {
        try {
            return EntityType.valueOf("ENDER_CRYSTAL");
        } catch (Throwable firstFailure) {
            return EntityType.valueOf("END_CRYSTAL");
        }
    }

    private static EntityType resolveFirework() {
        try {
            return EntityType.valueOf("FIREWORK");
        } catch (Throwable firstFailure) {
            return EntityType.valueOf("FIREWORK_ROCKET");
        }
    }

    private static EnumSet<EntityType> resolveBoatTypes() {
        List<EntityType> matching = new ArrayList<>();
        for (EntityType type : EntityTypeEntries.entries) {
            String name = type.name();
            if (name.endsWith("BOAT") || name.endsWith("RAFT")) {
                matching.add(type);
            }
        }
        return EnumSet.copyOf(CollectionsKt.toSet(matching));
    }

    public static EntityType legacyEndCrystal() {
        return LEGACY_END_CRYSTAL.getValue();
    }

    public static EntityType firework() {
        return FIREWORK.getValue();
    }

    public static EnumSet<EntityType> boatTypes() {
        return BOAT_TYPES.getValue();
    }

    public static boolean isBoat(EntityType type) {
        return boatTypes().contains(type);
    }

    static {
        INSTANCE = new EntityCategories();
        LEGACY_END_CRYSTAL = LazyKt.lazy(EntityCategories::resolveEndCrystal);
        FIREWORK = LazyKt.lazy(EntityCategories::resolveFirework);
        BOAT_TYPES = LazyKt.lazy(EntityCategories::resolveBoatTypes);
    }
}
