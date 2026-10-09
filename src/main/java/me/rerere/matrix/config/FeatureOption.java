package me.rerere.matrix.config;

import me.rerere.matrix.checks.movement.FreecamMovementCheck;

/** Boolean configuration delegate used by the freecam movement check. */
public final class FeatureOption {
    private FeatureOption() { }

    public FeatureOption(me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker marker) { this(); }

    public final void setEnabled(boolean enabled) {
        FreecamMovementCheck.setEnabled(enabled);
    }

    public final boolean isEnabled() {
        return FreecamMovementCheck.enabled();
    }

}
