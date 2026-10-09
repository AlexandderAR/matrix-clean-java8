package me.rerere.matrix.checks.movement;

import me.rerere.matrix.checks.rotation.TrackingWindow;

/** Switch ordinals for the yaw selection mode enum. */
public final class YawModeSwitchMap {
    public static final int[] switchMap;

    static {
        int[] switchValues = new int[TrackingWindow.values().length];
        try {
            switchValues[TrackingWindow.CURRENT_ONLY.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            switchValues[TrackingWindow.CURRENT_AND_LAST_YAW.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            switchValues[TrackingWindow.ALL.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }
        switchMap = switchValues;
    }

    private YawModeSwitchMap() {
    }
}
