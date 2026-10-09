package me.rerere.matrix.switches;

import me.rerere.matrix.api.HackType;

/** Enum ordinal map used by HackType dispatch. */
public final class HackTypeSwitchMap {
    public static final int[] CASE_BY_HACK_TYPE;

    private HackTypeSwitchMap() {
    }

    static {
        int[] cases = new int[HackType.values().length];
        try {
            cases[HackType.KILLAURA.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.CLICK.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.HITBOX.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.MOVE.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.BADPACKETS.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.DELAY.ordinal()] = 6;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.BLOCK.ordinal()] = 7;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.SCAFFOLD.ordinal()] = 8;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.VELOCITY.ordinal()] = 9;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.CHAT.ordinal()] = 10;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.INTERACT.ordinal()] = 11;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.PHASE.ordinal()] = 12;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.AUTOBOT.ordinal()] = 13;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.ELYTRA.ordinal()] = 14;
        } catch (NoSuchFieldError ignored) { }
        try {
            cases[HackType.VEHICLE.ordinal()] = 15;
        } catch (NoSuchFieldError ignored) { }
        CASE_BY_HACK_TYPE = cases;
    }
}
