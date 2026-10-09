package me.rerere.matrix.analysis;

import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Immutable result returned by an analysis. */
public final class AnalysisResult {
    private final List<?> result;

    public AnalysisResult(List<?> result) {
        this.result = result;
    }

    public List<?> getResult() {
        return result;
    }

    public List<?> component1() {
        return result;
    }

    public AnalysisResult copy(List<?> result) {
        return new AnalysisResult(result);
    }

    public static AnalysisResult copy$default(AnalysisResult self, List<?> result, int mask, Object marker) {
        if ((mask & 1) != 0) {
            result = self.result;
        }
        return self.copy(result);
    }

    public static String toString(List<?> result) {
        return "AnalysisResult(result=" + result + ")";
    }

    @Override
    public String toString() {
        return toString(result);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AnalysisResult)) return false;
        AnalysisResult otherResult = (AnalysisResult) other;
        return Intrinsics.areEqual(result, otherResult.result);
    }

    @Override
    public int hashCode() {
        return result.hashCode();
    }
}
