package me.rerere.matrix.checks;

import me.rerere.matrix.checks.InteractionRayTraceCheck;
import me.rerere.matrix.license.LicenseResponseException;

public final class InteractionRayTraceSettings {
    private static boolean settingEnabled;

    public InteractionRayTraceSettings() {}
    public final boolean isEnabled() { return InteractionRayTraceCheck.isRaytraceEnabled(); }
    public final void setEnabled(boolean enabled) { InteractionRayTraceCheck.setRaytraceEnabled(enabled); }

    public static void setSettingEnabled(boolean enabled) { settingEnabled = enabled; }
    public static boolean isSettingEnabled() { return settingEnabled; }
    public static boolean isSettingDisabled() {
        boolean enabled = isSettingEnabled();
        try {
            if (!enabled) {
                return true;
            }
            return false;
        } catch (LicenseResponseException failure) {
            throw failure;
        }
    }

    static {
        if (!isSettingEnabled()) {
            setSettingEnabled(true);
        }
    }
}
