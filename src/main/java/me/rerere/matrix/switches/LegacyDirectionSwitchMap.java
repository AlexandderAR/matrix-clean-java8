package me.rerere.matrix.switches;

import net.minecraft.server.v1_8_R3.EnumDirection;

/** Enum ordinal map used by legacy BlockFace conversion. */
public final class LegacyDirectionSwitchMap {
    public static final int[] CASE_BY_DIRECTION;

    private LegacyDirectionSwitchMap() {
    }

    static {
        CASE_BY_DIRECTION = new int[EnumDirection.values().length];
        int[] cases = CASE_BY_DIRECTION;
        try { cases[EnumDirection.UP.ordinal()] = 1; } catch (NoSuchFieldError ignored) { }
        try { cases[EnumDirection.DOWN.ordinal()] = 2; } catch (NoSuchFieldError ignored) { }
        try { cases[EnumDirection.NORTH.ordinal()] = 3; } catch (NoSuchFieldError ignored) { }
        try { cases[EnumDirection.SOUTH.ordinal()] = 4; } catch (NoSuchFieldError ignored) { }
        try { cases[EnumDirection.WEST.ordinal()] = 5; } catch (NoSuchFieldError ignored) { }
        try { cases[EnumDirection.EAST.ordinal()] = 6; } catch (NoSuchFieldError ignored) { }
    }
}
