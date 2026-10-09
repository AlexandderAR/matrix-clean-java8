package me.rerere.matrix.checks.combat;

import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.player.TargetTrackingSnapshot;
import me.rerere.matrix.replay.AttackReplayFrame;
import me.rerere.matrix.replay.PacketStateSnapshot;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;

/** Immutable snapshot of player and target state captured during attack replay. */
public final class AttackReplaySnapshot {
    private final PacketStateSnapshot playerState;
    private final TargetTrackingSnapshot targetState;
    private final List<AttackReplayFrame> frames;
    private final List<PacketStateSnapshot> playerStates;
    private final List<TargetTrackingSnapshot> targetStates;
    private final boolean cancelledOriginal;

    public AttackReplaySnapshot(PacketStateSnapshot playerState,
                                TargetTrackingSnapshot targetState,
                                List<AttackReplayFrame> frames,
                                List<PacketStateSnapshot> playerStates,
                                List<TargetTrackingSnapshot> targetStates,
                                boolean cancelledOriginal) {
        this.playerState = playerState;
        this.targetState = targetState;
        this.frames = frames;
        this.playerStates = playerStates;
        this.targetStates = targetStates;
        this.cancelledOriginal = cancelledOriginal;
    }

    /** Kotlin default-argument constructor; mask bits 4/8/16/32 select derived defaults. */
    public AttackReplaySnapshot(PacketStateSnapshot playerState,
                                TargetTrackingSnapshot targetState,
                                List<AttackReplayFrame> frames,
                                List<PacketStateSnapshot> playerStates,
                                List<TargetTrackingSnapshot> targetStates,
                                boolean cancelledOriginal, int mask,
                                DefaultConstructorMarker marker) {
        if ((mask & 4) != 0) {
            frames = CollectionsKt.listOf(new AttackReplayFrame(playerState, targetState));
        }
        if ((mask & 8) != 0) {
            List mappedPlayerStates = new ArrayList(frames.size());
            for (Object frame : frames) {
                AttackReplayFrame replayFrame = (AttackReplayFrame) frame;
                mappedPlayerStates.add(replayFrame.getPlayerState());
            }
            playerStates = CollectionsKt.distinct(mappedPlayerStates);
        }
        if ((mask & 16) != 0) {
            List mappedTargetStates = new ArrayList();
            for (Object frame : frames) {
                TargetTrackingSnapshot frameTarget = ((AttackReplayFrame) frame).getTargetState();
                if (frameTarget != null) mappedTargetStates.add(frameTarget);
            }
            targetStates = CollectionsKt.distinct(mappedTargetStates);
        }
        if ((mask & 32) != 0) cancelledOriginal = false;
        this.playerState = playerState;
        this.targetState = targetState;
        this.frames = frames;
        this.playerStates = playerStates;
        this.targetStates = targetStates;
        this.cancelledOriginal = cancelledOriginal;
    }

    public PacketStateSnapshot playerState() { return playerState; }
    public TargetTrackingSnapshot targetState() { return targetState; }
    public List frames() { return frames; }
    public List getFrames() { return frames; }
    public List playerStates() { return playerStates; }
    public List getPlayerStates() { return playerStates; }
    public List targetStates() { return targetStates; }
    public List getTargetStates() { return targetStates; }
    public boolean cancelledOriginal() { return cancelledOriginal; }
    public boolean isCancelledOriginal() { return cancelledOriginal; }
    public PacketStateSnapshot getPlayerState() { return playerState; }
    public TargetTrackingSnapshot getTargetState() { return targetState; }

    public AttackReplaySnapshot copy(PacketStateSnapshot playerState,
                                     TargetTrackingSnapshot targetState,
                                     List<AttackReplayFrame> frames,
                                     List<PacketStateSnapshot> playerStates,
                                     List<TargetTrackingSnapshot> targetStates,
                                     boolean cancelledOriginal) {
        return new AttackReplaySnapshot(playerState, targetState, frames, playerStates,
                targetStates, cancelledOriginal);
    }

    public static AttackReplaySnapshot copyWithDefaults(AttackReplaySnapshot original,
            PacketStateSnapshot playerState, TargetTrackingSnapshot targetState,
            List<AttackReplayFrame> frames, List<PacketStateSnapshot> playerStates,
            List<TargetTrackingSnapshot> targetStates, boolean cancelledOriginal, int mask) {
        if ((mask & 1) != 0) playerState = original.playerState;
        if ((mask & 2) != 0) targetState = original.targetState;
        if ((mask & 4) != 0) frames = original.frames;
        if ((mask & 8) != 0) playerStates = original.playerStates;
        if ((mask & 16) != 0) targetStates = original.targetStates;
        if ((mask & 32) != 0) cancelledOriginal = original.cancelledOriginal;
        return original.copy(playerState, targetState, frames, playerStates, targetStates,
                cancelledOriginal);
    }

    public AttackReplaySnapshot withTarget(TargetTrackingSnapshot target) {
        return update(playerState, target);
    }

    public AttackReplaySnapshot withPlayer(PacketStateSnapshot player) {
        return update(player, targetState);
    }

    /** Adds a frame and distinct player/target histories while retaining this snapshot's primary player. */
    public AttackReplaySnapshot update(PacketStateSnapshot player,
                                       TargetTrackingSnapshot target) {
        if (target == null) {
            List updatedFrames = CollectionsKt.distinct(CollectionsKt.plus(
                    (java.util.Collection) frames,
                    new AttackReplayFrame(player, null)));
            List updatedPlayerStates = CollectionsKt.distinct(CollectionsKt.plus(
                    (java.util.Collection) playerStates, player));
            return copyWithDefaults(this, null, null, updatedFrames, updatedPlayerStates, null,
                    false, 51);
        }
        List updatedFrames = CollectionsKt.distinct(CollectionsKt.plus(
                (java.util.Collection) frames,
                    new AttackReplayFrame(player, target)));
        List updatedPlayerStates = CollectionsKt.distinct(CollectionsKt.plus(
                (java.util.Collection) playerStates, player));
        List updatedTargetStates = CollectionsKt.distinct(CollectionsKt.plus(
                (java.util.Collection) targetStates, target));
        TargetTrackingSnapshot selectedTarget =
                this.targetState == null ? target : this.targetState;
        return copyWithDefaults(this, null, selectedTarget, updatedFrames, updatedPlayerStates,
                updatedTargetStates, false, 33);
    }

    /** Returns this when a target is already present or the requested target is null. */
    public AttackReplaySnapshot addTargetIfMissing(TargetTrackingSnapshot target) {
        if (targetState != null || target == null) return this;
        return update(playerState, target);
    }

    public AttackReplaySnapshot withCancelledOriginal(boolean value) {
        return copyWithDefaults(this, null, null, null, null, null, value, 31);
    }

    private static String format(PacketStateSnapshot player, TargetTrackingSnapshot target,
            List<AttackReplayFrame> frames, List<PacketStateSnapshot> playerHistory,
            List<TargetTrackingSnapshot> targetHistory, boolean cancelled) {
        return new StringBuilder("AttackReplaySnapshot(playerState=").append((Object) player)
                .append(", targetState=").append((Object) target)
                .append(", frames=").append((Object) frames)
                .append(", playerStates=").append((Object) playerHistory)
                .append(", targetStates=").append((Object) targetHistory)
                .append(", cancelledOriginal=").append(cancelled).append(")").toString();
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AttackReplaySnapshot)) return false;
        AttackReplaySnapshot that = (AttackReplaySnapshot) other;
        return Intrinsics.areEqual(playerState, that.playerState)
                && Intrinsics.areEqual(targetState, that.targetState)
                && Intrinsics.areEqual(frames, that.frames)
                && Intrinsics.areEqual(playerStates, that.playerStates)
                && Intrinsics.areEqual(targetStates, that.targetStates)
                && cancelledOriginal == that.cancelledOriginal;
    }

    @Override public int hashCode() {
        int result = playerState.hashCode();
        result = 31 * result + (targetState == null ? 0 : targetState.hashCode());
        result = 31 * result + frames.hashCode();
        result = 31 * result + playerStates.hashCode();
        result = 31 * result + targetStates.hashCode();
        return 31 * result + Boolean.hashCode(cancelledOriginal);
    }

    @Override public String toString() {
        return format(playerState, targetState, frames, playerStates, targetStates,
                cancelledOriginal);
    }
}
