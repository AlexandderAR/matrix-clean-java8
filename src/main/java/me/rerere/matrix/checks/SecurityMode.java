package me.rerere.matrix.checks;

import me.rerere.matrix.checks.combat.HitBoxCancelWayResolver;

import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntries;
import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntriesKt;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;

/** Recovered three-state mode used by Matrix checks. */
public enum SecurityMode {
    NONE,
    SILENT,
    CANCEL;

    private static final SecurityMode[] ALL = valuesArray();
    private static final EnumEntries<SecurityMode> ENTRIES = EnumEntriesKt.enumEntries(ALL);
    @NotNull
    public static final HitBoxCancelWayResolver auxiliaryMarker =
            new HitBoxCancelWayResolver(null);
    private static final SecurityMode[] valuesArray() {
        return values();
    }

    @NotNull
    public static EnumEntries<SecurityMode> entries() {
        return ENTRIES;
    }

}
