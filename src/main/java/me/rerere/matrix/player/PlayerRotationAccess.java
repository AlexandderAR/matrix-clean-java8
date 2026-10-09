package me.rerere.matrix.player;

import me.rerere.matrix.license.LicenseRuntimeBridge;

/** Bridge preserving the two compiler-generated MethodHandle bridge calls. */
public final class PlayerRotationAccess {
    private PlayerRotationAccess() {}
    public static boolean get(Object target) {
        return LicenseRuntimeBridge.__ntjGetAngle((LicenseRuntimeBridge) target);
    }

    public static void set(Object target, boolean value) {
        LicenseRuntimeBridge.__ntjSetAngle((LicenseRuntimeBridge) target, value);
    }
}
