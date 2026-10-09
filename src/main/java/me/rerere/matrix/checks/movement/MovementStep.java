package me.rerere.matrix.checks.movement;

import java.util.Objects;
import me.rerere.matrix.geometry.AxisAlignedBox;

/** A movement prediction step: position state plus the number of ticks to reach it. */
public final class MovementStep {
    private final AxisAlignedBox position;
    private final int ticks;
    private static final int HASH_MULTIPLIER = (int) -2541976334008057825L;
    public MovementStep(AxisAlignedBox position, int ticks) {
        this.position = position;
        this.ticks = ticks;
    }

    public AxisAlignedBox position() { return position; }
    public int ticks() { return ticks; }
    public AxisAlignedBox getPosition() { return position; }
    public int getTicks() { return ticks; }

    public MovementStep with(AxisAlignedBox position, int ticks) {
        return new MovementStep(position, ticks);
    }

    public MovementStep copy(AxisAlignedBox position, int ticks) {
        return new MovementStep(position, ticks);
    }

    public static MovementStep copyDefault(MovementStep self, AxisAlignedBox position,
            int ticks, int mask) {
        AxisAlignedBox selectedPosition = (mask & 1) != 0 ? self.position : position;
        int selectedTicks = (mask & 2) != 0 ? self.ticks : ticks;
        return self.with(selectedPosition, selectedTicks);
    }

    private static String formatConstructorArgs(AxisAlignedBox position, int ticks) {
        return "MovementStep(position=" + position + ", ticks=" + ticks + ')';
    }

    @Override
    public String toString() {
        return formatConstructorArgs(position, ticks);
    }

    @Override
    public int hashCode() {
        return position.hashCode() * HASH_MULTIPLIER + Integer.hashCode(ticks);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MovementStep)) return false;
        MovementStep otherStep = (MovementStep) other;
        return Objects.equals(position, otherStep.position) && ticks == otherStep.ticks;
    }

}
