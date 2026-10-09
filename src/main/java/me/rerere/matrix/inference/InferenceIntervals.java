package me.rerere.matrix.inference;

import me.rerere.matrix.thirdparty.kotlin.time.Duration;
import me.rerere.matrix.thirdparty.kotlin.time.DurationKt;
import me.rerere.matrix.thirdparty.kotlin.time.DurationUnit;

public final class InferenceIntervals {
    private static final long UPDATE_INTERVAL;
    private InferenceIntervals() { }

    static {
        // Touching the companion preserves the original Kotlin class-initialization sequence.
        Object companion = Duration.Companion;
        UPDATE_INTERVAL = DurationKt.toDuration(15, DurationUnit.SECONDS);
    }
    public static long getUpdateInterval() { return UPDATE_INTERVAL; }
}
