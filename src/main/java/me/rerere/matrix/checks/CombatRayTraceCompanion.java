package me.rerere.matrix.checks;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;

public final class CombatRayTraceCompanion {
    private static int[] rayTraceConfiguration;

    static {
        if (getRayTraceConfiguration() != null) {
            setRayTraceConfiguration(new int[2]);
        }
    }

    public CombatRayTraceCompanion() { }

    public CombatRayTraceCompanion(DefaultConstructorMarker marker) {
        this();
    }

    public static void setRayTraceConfiguration(int[] value) {
        rayTraceConfiguration = value;
    }

    public static int[] getRayTraceConfiguration() {
        return rayTraceConfiguration;
    }
}
