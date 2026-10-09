package me.rerere.matrix.config;

import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntries;
import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntriesKt;

/** Metadata field kinds in their canonical ordinal order. */
public enum MetadataKind {
    WORDS,
    LEFT_PAREN,
    RIGHT_PAREN,
    COMMA,
    WHITE_SPACE;

    private static final EnumEntries<MetadataKind> ENTRIES = EnumEntriesKt.enumEntries(values());

    public static EnumEntries<MetadataKind> entries() {
        return ENTRIES;
    }
}
