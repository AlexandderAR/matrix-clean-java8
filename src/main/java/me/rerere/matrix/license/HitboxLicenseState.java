package me.rerere.matrix.license;

import me.rerere.matrix.checks.combat.CombatReachCheck;
public final class HitboxLicenseState {
    public HitboxLicenseState() { }

    public final boolean isEnabled() {
        return CombatReachCheck.isCheckEnabled();
    }

    public final void setEnabled(boolean enabled) {
        CombatReachCheck.setCheckEnabled(enabled);
    }
}
