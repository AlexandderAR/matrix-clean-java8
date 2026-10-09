package me.rerere.matrix.switches;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action;

/** Enum ordinal map used by the sneak-action switch. */
public final class EntityActionSneakSwitchMap {
    public static final int[] CASE_BY_ACTION;

    private EntityActionSneakSwitchMap() {
    }

    static {
        int[] cases = new int[WrapperPlayClientEntityAction$Action.values().length];
        try {
            cases[WrapperPlayClientEntityAction$Action.START_SNEAKING.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            cases[WrapperPlayClientEntityAction$Action.STOP_SNEAKING.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }
        CASE_BY_ACTION = cases;
    }
}
