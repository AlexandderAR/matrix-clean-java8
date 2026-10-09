package me.rerere.matrix.util;

import me.rerere.matrix.checks.player.RotationMiningCheck;

/** Adapts the rotation mining check's recording flag to the runtime guard API. */
public final class RuntimeGuard {
    public RuntimeGuard() { }

    public boolean isEnabled() {
        return RotationMiningCheck.recording();
    }

    public void setEnabled(boolean enabled) {
        RotationMiningCheck.setRecording(enabled);
    }
}
