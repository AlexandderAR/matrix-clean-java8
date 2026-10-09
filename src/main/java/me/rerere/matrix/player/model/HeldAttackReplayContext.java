package me.rerere.matrix.player.model;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.checks.combat.HeldAttack;
import me.rerere.matrix.player.TargetTrackingSnapshot;
import me.rerere.matrix.replay.PacketStateSnapshot;

/** Immutable held-attack replay context. */
public final class HeldAttackReplayContext {
    private final HeldAttack attack;
    private final PacketStateSnapshot playerState;
    private final TargetTrackingSnapshot targetState;

    public HeldAttackReplayContext(HeldAttack attack,
                                   PacketStateSnapshot playerState,
                                   TargetTrackingSnapshot targetState) {
        this.attack = attack;
        this.playerState = playerState;
        this.targetState = targetState;
    }

    public HeldAttack getAttack() { return attack; }
    public PacketStateSnapshot getPlayerState() { return playerState; }
    public TargetTrackingSnapshot getTargetState() { return targetState; }

    public HeldAttackReplayContext copy(HeldAttack attack,
                                        PacketStateSnapshot playerState,
                                        TargetTrackingSnapshot targetState) {
        return new HeldAttackReplayContext(attack, playerState, targetState);
    }

    private static String formatContext(HeldAttack attack,
                                        PacketStateSnapshot playerState,
                                        TargetTrackingSnapshot targetState) {
        return "HeldAttackReplayContext(attack=" + attack + ", playerState="
                + playerState + ", targetState=" + targetState + ")";
    }

    public static HeldAttackReplayContext copyWithMask(HeldAttackReplayContext original,
                                                       HeldAttack newAttack,
                                                       PacketStateSnapshot newPlayer,
                                                       TargetTrackingSnapshot newTarget,
                                                       int mask) {
        if ((mask & 1) != 0) newAttack = original.attack;
        if ((mask & 2) != 0) newPlayer = original.playerState;
        if ((mask & 4) != 0) newTarget = original.targetState;
        return original.copy(newAttack, newPlayer, newTarget);
    }
    @Override
    public String toString() {
        return formatContext(attack, playerState, targetState);
    }

    @Override
    public int hashCode() {
        int result = attack.hashCode();
        result = result * 31 + playerState.hashCode();
        result = result * 31 + (targetState == null ? 0 : targetState.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HeldAttackReplayContext)) return false;
        HeldAttackReplayContext that = (HeldAttackReplayContext) other;
        return Intrinsics.areEqual(attack, that.attack)
                && Intrinsics.areEqual(playerState, that.playerState)
                && Intrinsics.areEqual(targetState, that.targetState);
    }
}


