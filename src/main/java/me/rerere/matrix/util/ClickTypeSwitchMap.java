package me.rerere.matrix.util;

import org.bukkit.event.inventory.ClickType;

public final class ClickTypeSwitchMap {
    public static final int[] CLICK_TYPE_CASES;

    private ClickTypeSwitchMap() {
    }

    static {
        int[] cases = new int[ClickType.values().length];
        try {
            cases[ClickType.LEFT.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            cases[ClickType.RIGHT.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }
        CLICK_TYPE_CASES = cases;
    }
}
