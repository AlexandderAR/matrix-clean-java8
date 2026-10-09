package me.rerere.matrix.movement;

import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.sequences.Sequence;
import me.rerere.matrix.player.model.PositionRotation;
import me.rerere.matrix.player.model.MovementFrameFactory;
import me.rerere.matrix.prediction.CollisionProbeSequence;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.license.LicenseResponseException;

/** Immutable pair of movement snapshots for the current and previous tick. */
public final class PlayerSnapshot {
    private static int sequenceMode = 125;
    private static final long HASH_MULTIPLIER = 6837066122475339807L;
    public static final MovementFrameFactory companion = new MovementFrameFactory((me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker) null);
    private final PositionRotation current;
    private final PositionRotation previous;

    public PlayerSnapshot(PositionRotation current, PositionRotation previous) {
        this.current = current;
        this.previous = previous;
    }

    public final PositionRotation previousAliasA() { return previous; }
    public final PositionRotation previousAliasB() { return previous; }
    public final PositionRotation currentAliasA() { return current; }
    public final PositionRotation currentAliasB() { return current; }
    public final PlayerSnapshot create(PositionRotation current, PositionRotation previous) {
        return new PlayerSnapshot(current, previous);
    }

    public static PlayerSnapshot copyDefault(PlayerSnapshot receiver,
            PositionRotation current, PositionRotation previous, int defaultMask) {
        if ((defaultMask & 1) != 0) current = receiver.current;
        if ((defaultMask & 2) != 0) previous = receiver.previous;
        return receiver.copy(current, previous);
    }

    public final PlayerSnapshot copy(PositionRotation current, PositionRotation previous) {
        return new PlayerSnapshot(current, previous);
    }

    public final Sequence<?> sequence(PlayerCheckContext generator, List<?> collisionProbes) {
        final CollisionProbeSequence coroutine =
                new CollisionProbeSequence(this, collisionProbes, generator, null);
        return me.rerere.matrix.thirdparty.kotlin.sequences.SequencesKt.sequence(coroutine);
    }

    public static void setSequenceMode(int value) { sequenceMode = value; }
    public static int sequenceMode() { return sequenceMode; }
    public static int normalizedSequenceMode() {
        try { return sequenceMode == 0 ? 15 : 0; }
        catch (LicenseResponseException failure) { throw failure; }
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PlayerSnapshot)) return false;
        PlayerSnapshot that = (PlayerSnapshot) other;
        return me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(current, that.current)
                && me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(previous, that.previous);
    }

    @Override public int hashCode() {
        int result = current.hashCode();
        result = result * (int) HASH_MULTIPLIER + previous.hashCode();
        return result;
    }

    @Override public String toString() {
        return "PlayerSnapshot(current=" + current + ", previous=" + previous + ")";
    }
}
