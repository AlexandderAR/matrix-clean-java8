package me.rerere.matrix.checks.rotation;

import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntries;
import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntriesKt;

/** Rotation samples included by a check. */
public enum TrackingWindow {
    CURRENT_ONLY,
    CURRENT_AND_LAST_YAW,
    ALL;

    private static final EnumEntries<TrackingWindow> ENTRIES = EnumEntriesKt.enumEntries(values());

    public static EnumEntries<TrackingWindow> entries() { return ENTRIES; }

}
