package me.rerere.matrix.switches;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action;

/** Enum ordinal map used by entity-action dispatch. */
public final class EntityActionStateSwitchMap {
    public static final int[] CASE_BY_ACTION;

    private EntityActionStateSwitchMap() {
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
        try {
            cases[WrapperPlayClientEntityAction$Action.START_SPRINTING.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            cases[WrapperPlayClientEntityAction$Action.STOP_SPRINTING.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }
        try {
            cases[WrapperPlayClientEntityAction$Action.START_FLYING_WITH_ELYTRA.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) {
        }
        CASE_BY_ACTION = cases;
    }
}
