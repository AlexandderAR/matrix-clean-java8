package me.rerere.matrix.util.time;

import me.rerere.matrix.compat.KotlinTimeAccess;

public final class TimeMarkOperations {
    private TimeMarkOperations() {
    }

    public static long elapsedSince(long earlierMillis) {
        return currentTimeMillis() - earlierMillis;
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static long selectMark(long accumulatedMark, long savedMark) {
        return KotlinTimeAccess.compareMarks(accumulatedMark, savedMark) <= 0
                ? savedMark : accumulatedMark;
    }

    public static long subtractEncodedValues(long minuend, long subtrahend) {
        return minuend - subtrahend;
    }

    public static long markNow() {
        return KotlinTimeAccess.markNow();
    }
}
