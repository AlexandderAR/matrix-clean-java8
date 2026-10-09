package me.rerere.matrix.entity;

import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntries;
import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntriesKt;
import org.bukkit.entity.EntityType;

/** Kotlin-style enum entries for Bukkit entity types. */
public final class EntityTypeEntries {
    public static final EnumEntries<EntityType> entries;
    private static boolean initializationGuard;

    static {
        if (!getInitializationGuard()) {
            setInitializationGuard(true);
        }
        entries = EnumEntriesKt.enumEntries(EntityType.values());
    }

    public static void setInitializationGuard(boolean initialized) {
        initializationGuard = initialized;
    }

    public static boolean getInitializationGuard() {
        return initializationGuard;
    }

    public static boolean isInitializationGuardClear() {
        return !getInitializationGuard();
    }

    private EntityTypeEntries() {}
}
