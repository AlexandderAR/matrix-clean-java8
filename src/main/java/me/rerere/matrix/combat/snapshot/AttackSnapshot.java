package me.rerere.matrix.combat.snapshot;

import me.rerere.matrix.movement.PlayerSnapshot;

    /** Immutable attack state captured with the mapped player snapshot. */
public final class AttackSnapshot {
    private final int entityId;
    private final PlayerSnapshot snapshot;
    private final long createdAt;

    public AttackSnapshot(int entityId, PlayerSnapshot snapshot, long createdAt) {
        this.entityId = entityId;
        this.snapshot = snapshot;
        this.createdAt = createdAt;
    }

    public static AttackSnapshot of(int entityId,
                                    PlayerSnapshot snapshot,
                                    long createdAt) {
        return new AttackSnapshot(entityId, snapshot, createdAt);
    }

    /** Creates a copy, taking each masked field from the source snapshot. */
    public static AttackSnapshot copyWithDefaults(AttackSnapshot source, int entityId,
                                                   PlayerSnapshot snapshot,
                                                   long createdAt, int mask) {
        if ((mask & 1) != 0) entityId = source.entityId;
        if ((mask & 2) != 0) snapshot = source.snapshot;
        if ((mask & 4) != 0) createdAt = source.createdAt;
        return new AttackSnapshot(entityId, snapshot, createdAt);
    }

    public int component1() { return entityId; }
    public PlayerSnapshot component2() { return snapshot; }
    public long component3() { return createdAt; }

    public int entityId() { return entityId; }
    public PlayerSnapshot snapshot() { return snapshot; }
    public long createdAt() { return createdAt; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AttackSnapshot)) return false;
        AttackSnapshot otherSnapshot = (AttackSnapshot) other;
        return entityId == otherSnapshot.entityId
                && me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(snapshot, otherSnapshot.snapshot)
                && createdAt == otherSnapshot.createdAt;
    }

    @Override public int hashCode() {
        int result = Integer.hashCode(entityId);
        result = 31 * result + snapshot.hashCode();
        result = 31 * result + Long.hashCode(createdAt);
        return result;
    }

    @Override public String toString() {
        return "AttackSnapshot(entityId=" + entityId + ", snapshot=" + snapshot
                + ", createdAt=" + createdAt + ")";
    }
}
