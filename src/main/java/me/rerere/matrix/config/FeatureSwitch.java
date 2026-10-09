package me.rerere.matrix.config;

import me.rerere.matrix.combat.CombatFlawCheck;

/** Backed configuration switch with the original delegating accessors. */
public final class FeatureSwitch {
    private static boolean enabled;

    private FeatureSwitch() {}
    public FeatureSwitch(me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker marker) { this(); }

    static { if (isDisabled()) setLocalValue(true); }

    public final boolean isEnabled() {
        return CombatFlawCheck.combatChecksEnabled();
    }

    public final void setEnabled(boolean enabled) {
        CombatFlawCheck.setCombatChecksEnabled(enabled);
    }

    public static void setLocalValue(boolean enabled) { FeatureSwitch.enabled = enabled; }
    public static boolean getLocalValue() { return enabled; }

    public static boolean isDisabled() {
        boolean current = getLocalValue();
        return !current;
    }
}
