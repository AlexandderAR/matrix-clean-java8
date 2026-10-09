package me.rerere.matrix.util;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;

/** Kotlin-generated holder for an optional shared label. */
public final class FallbackLabel {
    private static String label;

    private FallbackLabel() { }
    public FallbackLabel(DefaultConstructorMarker ignoredDefaultConstructorMarker) { this(); }

    public static void setLabel(String value) { label = value; }
    public static String getLabel() { return label; }

    static {
        if (getLabel() != null) {
            setLabel("VB2EK");
        }
    }
}
