package me.rerere.matrix.util;

import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

/** Tracks an integer callback count and periodically resets it. */
public final class ProgressGate {
    private int completedCallbackCount;
    private long lastResetMillis;
    private final int progressThreshold;
    private static final long RESET_INTERVAL_MILLIS = 1000L;

    public ProgressGate(int progressThreshold) {
        this.progressThreshold = progressThreshold;
    }

    private static Unit ignoreProgress(int ignoredCount) { return Unit.INSTANCE; }

    public void runCallbacks(Function0<?> onComplete) {
        Function1<Integer, Unit> noProgress = ProgressGate::ignoreProgress;
        runCallbacks(onComplete, noProgress);
    }

    @SuppressWarnings("rawtypes")
    public void runCallbacks(Function0<?> onComplete, Function1<? super Integer, ?> onProgress) {

        long now = System.currentTimeMillis();
        if (now - lastResetMillis > RESET_INTERVAL_MILLIS) {
            lastResetMillis = now;
            completedCallbackCount = 0;
        }
        if (completedCallbackCount >= progressThreshold) {
            onProgress.invoke(Integer.valueOf(completedCallbackCount));
            return;
        }
        completedCallbackCount++;
        onComplete.invoke();
    }

    /** Kotlin default-argument bridge: bit 2 selects the no-op progress callback. */
    public static void runCallbacksDefault(ProgressGate self, Function0<?> onComplete,
                                           Function1<? super Integer, ?> onProgress,
                                           int mask, Object marker) {
        if ((mask & 2) != 0) {
            Function1<Integer, Unit> noProgress = ProgressGate::ignoreProgress;
            self.runCallbacks(onComplete, noProgress);
        } else {
            self.runCallbacks(onComplete, onProgress);
        }
    }

}
