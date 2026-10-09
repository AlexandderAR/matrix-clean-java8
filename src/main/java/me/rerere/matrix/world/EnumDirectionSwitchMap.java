package me.rerere.matrix.world;

import net.minecraft.server.v1_12_R1.EnumDirection;

/** Switch table used for the six EnumDirection constants. */
public final class EnumDirectionSwitchMap {
    public static final int[] CASES;
    private EnumDirectionSwitchMap() {
    }

    static {
        CASES = new int[EnumDirection.values().length];
        try { CASES[EnumDirection.UP.ordinal()] = 1; } catch (NoSuchFieldError ignored) { }
        try { CASES[EnumDirection.DOWN.ordinal()] = 2; } catch (NoSuchFieldError ignored) { }
        try { CASES[EnumDirection.NORTH.ordinal()] = 3; } catch (NoSuchFieldError ignored) { }
        try { CASES[EnumDirection.SOUTH.ordinal()] = 4; } catch (NoSuchFieldError ignored) { }
        try { CASES[EnumDirection.WEST.ordinal()] = 5; } catch (NoSuchFieldError ignored) { }
        try { CASES[EnumDirection.EAST.ordinal()] = 6; } catch (NoSuchFieldError ignored) { }
    }
}
