package me.rerere.matrix.listeners;

import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntries;
import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntriesKt;

/** Event sources which produce velocity or explosion effects. */
public enum VelocityExplosionType {
    VELOCITY,
    EXPLOSION;

    private static final EnumEntries<VelocityExplosionType> enumEntries;

    public static EnumEntries<VelocityExplosionType> entries() {
        return enumEntries;
    }

    static {
        enumEntries = EnumEntriesKt.enumEntries(values());
    }
}
