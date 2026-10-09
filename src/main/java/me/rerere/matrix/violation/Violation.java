package me.rerere.matrix.violation;

import java.util.UUID;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

public final class Violation {
    private final UUID playerId;
    private final CheckCategory category;
    private int level;
    private static final String NULL_CAST = "null cannot be cast to non-null type me.rerere.matrix.data.player.violations.Violation".intern();

    public Violation(UUID playerId, CheckCategory category) {
        this.playerId = playerId;
        this.category = category;
    }
    public UUID getPlayerId() { return playerId; }
    public CheckCategory getCategory() { return category; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public void clear() {
        level = 0;
    }
    public void add(int amount) {
        level += amount;
        if (level < 0) level = 0;
    }
    public void subtract(int amount) {
        level -= amount;
        if (level < 0) level = 0;
    }
    @Override
    public int hashCode() {
        return 31 * playerId.hashCode() + category.hashCode();
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) return true;
        if (!Intrinsics.areEqual(getClass(), candidate == null ? null : candidate.getClass())) return false;
        Intrinsics.checkNotNull(candidate, NULL_CAST);
        Violation otherViolation = (Violation) candidate;
        return Intrinsics.areEqual(playerId, otherViolation.playerId)
                && category == otherViolation.category;
    }
}
