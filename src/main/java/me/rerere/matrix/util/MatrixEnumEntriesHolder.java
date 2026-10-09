package me.rerere.matrix.util;

import me.rerere.matrix.api.HackType;
import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntries;
import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntriesKt;

/** Holds Kotlin-compatible enum entries. */
public final class MatrixEnumEntriesHolder {
    public static final EnumEntries<HackType> ENTRIES;

    static {
        ENTRIES = EnumEntriesKt.enumEntries(HackType.values());
    }
}
