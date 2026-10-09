package me.rerere.matrix.config;

/** Maps security mode ordinals to their switch cases. */
public final class ModeOrdinalMap {
    public static final int[] MODE_ORDINAL_TO_CASE;

    static {
        int[] map = new int[me.rerere.matrix.checks.SecurityMode.values().length];
        try {
            map[me.rerere.matrix.checks.SecurityMode.NONE.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            map[me.rerere.matrix.checks.SecurityMode.SILENT.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            map[me.rerere.matrix.checks.SecurityMode.CANCEL.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }
        MODE_ORDINAL_TO_CASE = map;
    }

    private ModeOrdinalMap() {}
}
