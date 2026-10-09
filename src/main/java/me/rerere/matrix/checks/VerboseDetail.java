package me.rerere.matrix.checks;

import java.util.Map;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Recovered message plus tooltip map value object. */
public final class VerboseDetail {
    private final String message;
    private final Map<String, String> tooltips;

    public VerboseDetail(String message, Map<String, String> tooltips) {
        this.message = message;
        this.tooltips = tooltips;
    }

    public static VerboseDetail create(String message, Map<String, String> tooltips) {
        return new VerboseDetail(message, tooltips);
    }

    public VerboseDetail copy(String message, Map<String, String> tooltips) {
        return new VerboseDetail(message, tooltips);
    }

    public Map<String, String> tooltips() { return tooltips; }
    public String message() { return message; }

    public static VerboseDetail copyWithMask(VerboseDetail original,
                                               String replacementMessage,
                                               Map<String, String> replacementTooltips,
                                               int mask) {
        if ((mask & 1) != 0) replacementMessage = original.message;
        if ((mask & 2) != 0) replacementTooltips = original.tooltips;
        return original.copy(replacementMessage, replacementTooltips);
    }

    @Override public String toString() {
        return new StringBuilder("VerboseDetail(message=").append(message)
                .append(", tooltips=").append((Object) tooltips).append(')').toString();
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof VerboseDetail)) return false;
        VerboseDetail value = (VerboseDetail) other;
        return Intrinsics.areEqual(message, value.message) && Intrinsics.areEqual(tooltips, value.tooltips);
    }

    @Override public int hashCode() {
        int value = message.hashCode();
        value = value * 31 + tooltips.hashCode();
        return value;
    }
}

