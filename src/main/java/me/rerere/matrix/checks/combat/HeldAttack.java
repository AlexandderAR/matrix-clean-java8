package me.rerere.matrix.checks.combat;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Captures a packet and entity snapshot used for held-attack replay. */
public final class HeldAttack {
    private final PacketWrapper packet;
    private final long createdAt;
    private final AttackReplaySnapshot snapshot;
    private final boolean shouldReplay;
    private final int entityId;

    public HeldAttack(PacketWrapper packet, int entityId, AttackReplaySnapshot snapshot,
                      boolean shouldReplay, long createdAt) {
        this.packet = packet;
        this.entityId = entityId;
        this.snapshot = snapshot;
        this.shouldReplay = shouldReplay;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "HeldAttack(packet=" + packet + ", entityId=" + entityId
                + ", snapshot=" + snapshot + ", shouldReplay=" + shouldReplay
                + ", createdAt=" + createdAt + ")";
    }

    public final long getCreatedAt() { return createdAt; }
    public final PacketWrapper getPacket() { return packet; }

    public final HeldAttack copy(PacketWrapper packet, int entityId, AttackReplaySnapshot snapshot,
                                 boolean shouldReplay, long createdAt) {
        return new HeldAttack(packet, entityId, snapshot, shouldReplay, createdAt);
    }

    public final boolean isShouldReplay() { return shouldReplay; }

    @Override
    public int hashCode() {
        int result = packet.hashCode();
        result = 31 * result + Integer.hashCode(entityId);
        result = 31 * result + snapshot.hashCode();
        result = 31 * result + Boolean.hashCode(shouldReplay);
        result = 31 * result + Long.hashCode(createdAt);
        return result;
    }

    public final int getEntityId() { return entityId; }
    public final AttackReplaySnapshot getSnapshot() { return snapshot; }
    public final boolean replayFlag() { return shouldReplay; }
    public final AttackReplaySnapshot snapshotValue() { return snapshot; }
    public final int entityIdValue() { return entityId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HeldAttack)) return false;
        HeldAttack otherAttack = (HeldAttack) other;
        if (!Intrinsics.areEqual(packet, otherAttack.packet)) return false;
        if (entityId != otherAttack.entityId) return false;
        if (!Intrinsics.areEqual(snapshot, otherAttack.snapshot)) return false;
        if (shouldReplay != otherAttack.shouldReplay) return false;
        return createdAt == otherAttack.createdAt;
    }

    public final long timestamp() { return createdAt; }
    public final PacketWrapper packetValue() { return packet; }
}
