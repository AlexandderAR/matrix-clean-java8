package me.rerere.matrix.util;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;

/** Kotlin-style singleton holder for a shared integer array. */
public final class IntArrayHolder {
    private static int[] values;
    public IntArrayHolder(DefaultConstructorMarker marker) {
        this();
    }

    private IntArrayHolder() { }

    public static void set(int[] array) {
        values = array;
    }

    public static int[] get() {
        return values;
    }

    static {
        values = new int[4];
    }
}
