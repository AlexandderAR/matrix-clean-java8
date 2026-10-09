package me.rerere.matrix.input;

import me.rerere.matrix.metadata.MonitorGate;

/** Immutable packet movement state. */
public final class PacketKnownInput {
    private final boolean forward;
    private final boolean backward;
    private final boolean left;
    private final boolean right;
    private final boolean jump;
    private final boolean shift;
    private final boolean sprint;
    public static final MonitorGate companion;
    private static final PacketKnownInput EMPTY;

    static {
        companion = new MonitorGate(null);
        EMPTY = new PacketKnownInput(false, false, false, false, false, false, false);
    }

    public PacketKnownInput(boolean forward, boolean backward, boolean left, boolean right,
            boolean jump, boolean shift, boolean sprint) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.jump = jump;
        this.shift = shift;
        this.sprint = sprint;
    }

    public boolean isForward() { return forward; }
    public boolean isBackward() { return backward; }
    public boolean isLeft() { return left; }
    public boolean isRight() { return right; }
    public boolean isJumping() { return jump; }
    public boolean isSneaking() { return shift; }
    public boolean isSprinting() { return sprint; }

    public boolean hasAnyPrimaryFlag() {
        return forward || backward || left || right || jump;
    }

    public int lateralDifference() {
        return (left ? 1 : 0) - (right ? 1 : 0);
    }

    public int forwardBackwardDifference() {
        return (forward ? 1 : 0) - (backward ? 1 : 0);
    }

    public boolean component1() { return forward; }
    public boolean component2() { return backward; }
    public boolean component3() { return left; }
    public boolean component4() { return right; }
    public boolean component5() { return jump; }
    public boolean component6() { return shift; }
    public boolean component7() { return sprint; }
    public PacketKnownInput copy(boolean forward, boolean backward, boolean left,
            boolean right, boolean jump, boolean shift, boolean sprint) {
        return new PacketKnownInput(forward, backward, left, right, jump, shift, sprint);
    }

    public static PacketKnownInput empty() {
        return EMPTY;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PacketKnownInput)) return false;
        PacketKnownInput otherInput = (PacketKnownInput) other;
        return forward == otherInput.forward && backward == otherInput.backward
                && left == otherInput.left && right == otherInput.right
                && jump == otherInput.jump && shift == otherInput.shift
                && sprint == otherInput.sprint;
    }

    @Override public int hashCode() {
        int result = Boolean.hashCode(forward);
        result = 31 * result + Boolean.hashCode(backward);
        result = 31 * result + Boolean.hashCode(left);
        result = 31 * result + Boolean.hashCode(right);
        result = 31 * result + Boolean.hashCode(jump);
        result = 31 * result + Boolean.hashCode(shift);
        return 31 * result + Boolean.hashCode(sprint);
    }

    @Override public String toString() {
        return "PacketKnownInput(forward=" + forward + ", backward=" + backward
                + ", left=" + left + ", right=" + right + ", jump=" + jump
                + ", shift=" + shift + ", sprint=" + sprint + ")";
    }
}
