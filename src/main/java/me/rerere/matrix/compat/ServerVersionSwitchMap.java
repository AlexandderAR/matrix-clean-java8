package me.rerere.matrix.compat;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;

public final class ServerVersionSwitchMap {
    public static final int[] LEGACY_CASES;
    static {
        int[] cases = new int[ServerVersion.values().length];
        try { cases[ServerVersion.V_1_8_8.ordinal()] = 1; }
        catch (NoSuchFieldError ignored) { }
        LEGACY_CASES = cases;
    }
}
