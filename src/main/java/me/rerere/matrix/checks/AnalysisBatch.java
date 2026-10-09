package me.rerere.matrix.checks;

import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Kotlin data carrier for a list of analysis entries. */
public final class AnalysisBatch {
    private final List<?> batch;

    public AnalysisBatch(List<?> entries) {
        this.batch = entries;
    }

    public final List<?> getEntries() {
        return batch;
    }

    public final List<?> component1() {
        return batch;
    }

    public final AnalysisBatch copy(List<?> entries) {
        return new AnalysisBatch(entries);
    }

    public static AnalysisBatch copy$default(
            AnalysisBatch self, List<?> entries, int mask, Object marker) {
        if ((mask & 1) != 0) {
            entries = self.batch;
        }
        return self.copy(entries);
    }

    @Override
    public int hashCode() {
        return batch.hashCode();
    }

    @Override
    public String toString() {
        return format(batch);
    }

    private static String format(List<?> entries) {
        return "AnalysisBatch(batch=" + entries + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnalysisBatch)) {
            return false;
        }
        return Intrinsics.areEqual(batch, ((AnalysisBatch) other).batch);
    }
}
