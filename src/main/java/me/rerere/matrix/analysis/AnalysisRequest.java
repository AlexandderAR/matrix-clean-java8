package me.rerere.matrix.analysis;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Request carrying an optional owner UUID and an associated data list. */
public final class AnalysisRequest {
    private final List<?> data;
    private UUID owner;

    public AnalysisRequest(UUID owner, List<?> data) {
        this.owner = owner;
        this.data = data;
    }

    public UUID owner() { return owner; }
    public UUID getOwner() { return owner; }
    public List<?> data() { return data; }
    public List<?> getData() { return data; }

    public void setOwner(UUID owner) { this.owner = owner; }

    public AnalysisRequest copy(UUID owner, List<?> data) {
        return new AnalysisRequest(owner, data);
    }

    public static AnalysisRequest copy$default(AnalysisRequest self, UUID owner,
                                                List<?> data, int mask, Object marker) {
        if ((mask & 1) != 0) owner = self.owner;
        if ((mask & 2) != 0) data = self.data;
        return self.copy(owner, data);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AnalysisRequest)) return false;
        AnalysisRequest that = (AnalysisRequest) other;
        return Objects.equals(owner, that.owner) && Objects.equals(data, that.data);
    }

    @Override public int hashCode() {
        return 31 * (owner == null ? 0 : owner.hashCode()) + data.hashCode();
    }

    public static String formattedString(UUID owner, List<?> data) {
        return "AnalysisRequest(owner=" + owner + ", data=" + data + ")";
    }

    @Override public String toString() {
        return formattedString(owner, data);
    }
}
