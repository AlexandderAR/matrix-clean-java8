package me.rerere.matrix.checks.movement;

import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntries;
import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntriesKt;

/** Axes used when calculating movement tolerances. */
public enum DistanceAxis {
    BREAK,
    PLACE;

    private static final EnumEntries<DistanceAxis> ENTRIES = EnumEntriesKt.enumEntries(values());

    public static EnumEntries<DistanceAxis> enumEntries() {
        return ENTRIES;
    }
}
