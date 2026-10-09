package me.rerere.matrix.switches;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;

/** Enum ordinal map used by server-version dispatch. */
public final class ServerVersionSwitchMap {
    public static final int[] CASE_BY_VERSION;

    private ServerVersionSwitchMap() {
    }

    static {
        int[] cases = new int[ServerVersion.values().length];
        try {
            cases[ServerVersion.V_1_8_8.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            cases[ServerVersion.V_1_13_2.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            cases[ServerVersion.V_1_18_2.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            cases[ServerVersion.V_1_19_3.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            cases[ServerVersion.V_1_19_4.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            cases[ServerVersion.V_1_20_2.ordinal()] = 6;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            cases[ServerVersion.V_1_20_5.ordinal()] = 7;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            cases[ServerVersion.V_1_20_6.ordinal()] = 8;
        } catch (NoSuchFieldError ignored) {
        }
        CASE_BY_VERSION = cases;
    }
}
