package me.rerere.matrix.util.cache;

/** Interpolation modes for cached movement bounds. */
public enum CachePolicy {
    STEPPED, LINEAR, SNAP;

    private static final me.rerere.matrix.thirdparty.kotlin.enums.EnumEntries<CachePolicy> ENTRIES =
            me.rerere.matrix.thirdparty.kotlin.enums.EnumEntriesKt.enumEntries(enumConstants());

    public static me.rerere.matrix.thirdparty.kotlin.enums.EnumEntries<CachePolicy> enumEntries() {
        return ENTRIES;
    }
    private static CachePolicy[] enumConstants() {
        return new CachePolicy[] { STEPPED, LINEAR, SNAP };
    }
}
