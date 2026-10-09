package me.rerere.matrix.license;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;

/** Accesses the post-license block placement feature state. */
public final class BlockPlacePostLicenseState {
    private static int storedState;

    private BlockPlacePostLicenseState() {
    }

    public BlockPlacePostLicenseState(DefaultConstructorMarker unusedMarker) {
        this();
    }

    public final void setEnabled(boolean enabled) {
        me.rerere.matrix.checks.movement.MovementPacketSequenceCheck.setEnabled(enabled);
    }

    public final boolean isEnabled() {
        return me.rerere.matrix.checks.movement.MovementPacketSequenceCheck.isEnabled();
    }

    public static void setStoredState(int state) { storedState = state; }
    public static int getStoredState() { return storedState; }

    public static int licenseState() {
        return storedState == 0 ? 75 : 0;
    }

    static {
        if (storedState != 0) storedState = 97;
    }
}
