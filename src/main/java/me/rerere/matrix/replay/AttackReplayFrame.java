package me.rerere.matrix.replay;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.player.TargetTrackingSnapshot;

public final class AttackReplayFrame {
    private final PacketStateSnapshot playerState;
    private final TargetTrackingSnapshot targetState;
    private static final long HASH_MULTIPLIER;

    static {
        HASH_MULTIPLIER = -7537116053369782241L;
    }

    public AttackReplayFrame(PacketStateSnapshot playerState,
                             TargetTrackingSnapshot targetState) {
        this.playerState = playerState;
        this.targetState = targetState;
    }

    public PacketStateSnapshot getPlayerState() { return playerState; }
    public TargetTrackingSnapshot getTargetState() { return targetState; }

    /** Convenience aliases for code that uses state-oriented accessor names. */
    public PacketStateSnapshot getPlayerStateAlias() { return playerState; }
    public TargetTrackingSnapshot getTargetStateAlias() { return targetState; }

    public AttackReplayFrame copy(PacketStateSnapshot newPlayerState,
                                  TargetTrackingSnapshot newTargetState) {
        return new AttackReplayFrame(newPlayerState, newTargetState);
    }

    public AttackReplayFrame copyDefaults(PacketStateSnapshot playerState,
                                          TargetTrackingSnapshot targetState,
                                          int mask) {
        if ((mask & 1) != 0) playerState = this.playerState;
        if ((mask & 2) != 0) targetState = this.targetState;
        return copy(playerState, targetState);
    }

    @Override public String toString() {
        return "AttackReplayFrame(playerState=" + playerState + ", targetState=" + targetState + ")";
    }

    @Override public int hashCode() {
        int playerHash = playerState.hashCode();
        int targetHash = targetState == null ? 0 : targetState.hashCode();
        return playerHash * (int) HASH_MULTIPLIER + targetHash;
    }

    @Override public boolean equals(Object otherObject) {
        if (this == otherObject) return true;
        if (!(otherObject instanceof AttackReplayFrame)) return false;
        AttackReplayFrame otherFrame = (AttackReplayFrame) otherObject;
        return Intrinsics.areEqual(playerState, otherFrame.playerState)
                && Intrinsics.areEqual(targetState, otherFrame.targetState);
    }
}
