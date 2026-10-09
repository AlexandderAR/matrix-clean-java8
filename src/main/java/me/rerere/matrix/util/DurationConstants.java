package me.rerere.matrix.util;

import me.rerere.matrix.thirdparty.kotlin.time.DurationKt;
import me.rerere.matrix.thirdparty.kotlin.time.DurationUnit;

/** Duration values initialized by the original class initializer. */
public final class DurationConstants {
    private static final long ONE_SECOND = DurationKt.toDuration(1, DurationUnit.SECONDS);
    private static final long TEN_MILLISECONDS = DurationKt.toDuration(10, DurationUnit.MILLISECONDS);
    private static final long THIRTY_SECONDS = DurationKt.toDuration(30, DurationUnit.SECONDS);
    private static final long FORTY_MILLISECONDS = DurationKt.toDuration(40, DurationUnit.MILLISECONDS);
    private static final long FIFTY_MILLISECONDS = DurationKt.toDuration(50, DurationUnit.MILLISECONDS);

    private DurationConstants() {
    }

    public static final long getThirtySeconds() { return THIRTY_SECONDS; }
    public static final long getOneSecond() { return ONE_SECOND; }
    public static final long getTenMilliseconds() { return TEN_MILLISECONDS; }
    public static final long getFiftyMilliseconds() { return FIFTY_MILLISECONDS; }
    public static final long getFortyMilliseconds() { return FORTY_MILLISECONDS; }
}
