package me.rerere.matrix.checks;

import org.bukkit.entity.Pose;

/** Switch-map data for pose handling, preserving compatibility with older Bukkit enums. */
public final class PoseSwitchMap {
    public static final int[] angle;
    static {
        angle = new int[Pose.values().length];
        try { angle[Pose.SNEAKING.ordinal()] = 1; } catch (NoSuchFieldError ignored) { }
        try { angle[Pose.SWIMMING.ordinal()] = 2; } catch (NoSuchFieldError ignored) { }
    }
    private PoseSwitchMap() { }
}