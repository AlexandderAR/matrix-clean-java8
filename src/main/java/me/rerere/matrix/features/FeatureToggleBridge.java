package me.rerere.matrix.features;

import me.rerere.matrix.checks.block.BlockBreakRotationCheck;

/** Reads and updates the process-wide feature flag consumed by feature passes. */
public final class FeatureToggleBridge {
    public FeatureToggleBridge() {
    }

    public boolean isEnabled() {
        return BlockBreakRotationCheck.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        BlockBreakRotationCheck.setEnabled(enabled);
    }
}
