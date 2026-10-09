package me.rerere.matrix.inventory;

import java.util.Arrays;
import java.util.List;

/** Ordered inventory slots used by the settings click handler. */
public final class InventorySettingsSlots {
    private static final List<Integer> SETTINGS_CONTROL_SLOTS = Arrays.asList(
            24, 25, 34, 35, 9, 19, 15, 27);

    private InventorySettingsSlots() { }

    public static List<Integer> settingsControlSlots() {
        return SETTINGS_CONTROL_SLOTS;
    }
}
