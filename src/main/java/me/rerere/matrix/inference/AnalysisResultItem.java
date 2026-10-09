package me.rerere.matrix.inference;

import java.util.UUID;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

public final class AnalysisResultItem {
    private final int prediction;
    private final double confidence;
    private final UUID owner;

    public AnalysisResultItem(int prediction, double confidence, UUID owner) {
        this.prediction = prediction;
        this.confidence = confidence;
        this.owner = owner;
    }

    public int getPrediction() {
        return prediction;
    }

    public double getConfidence() {
        return confidence;
    }

    public UUID getOwner() {
        return owner;
    }

    public int componentPrediction() {
        return prediction;
    }

    public double componentConfidence() {
        return confidence;
    }

    public UUID componentOwner() {
        return owner;
    }

    public AnalysisResultItem copy(int prediction, double confidence, UUID owner) {
        return new AnalysisResultItem(prediction, confidence, owner);
    }

    public static AnalysisResultItem copyDefault(AnalysisResultItem source, int prediction,
            double confidence, UUID owner, int mask, Object marker) {
        if ((mask & 1) != 0) prediction = source.prediction;
        if ((mask & 2) != 0) confidence = source.confidence;
        if ((mask & 4) != 0) owner = source.owner;
        return source.copy(prediction, confidence, owner);
    }

    @Override public boolean equals(Object candidate) {
        if (this == candidate) return true;
        if (!(candidate instanceof AnalysisResultItem)) return false;
        AnalysisResultItem other = (AnalysisResultItem) candidate;
        return prediction == other.prediction
                && Double.compare(confidence, other.confidence) == 0
                && Intrinsics.areEqual(owner, other.owner);
    }

    @Override public int hashCode() {
        return (Integer.hashCode(prediction) * 31 + Double.hashCode(confidence)) * 31
                + owner.hashCode();
    }

    @Override public String toString() {
        return formatAnalysisResultItem(prediction, confidence, owner);
    }

    private static String formatAnalysisResultItem(int prediction, double confidence, UUID owner) {
        return "AnalysisResultItem(prediction=" + prediction + ", confidence=" + confidence + ", owner=" + owner + ")";
    }
}
