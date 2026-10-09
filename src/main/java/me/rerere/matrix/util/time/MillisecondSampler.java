package me.rerere.matrix.util.time;

import java.util.Timer;
import java.util.TimerTask;

/** Periodically records wall-clock milliseconds for cheap elapsed-time reads. */
public final class MillisecondSampler extends TimerTask {
    public static final MillisecondSampler INSTANCE;
    private static final long INITIAL_DELAY_MILLIS = 5L;
    private static final Timer TIMER;
    private static volatile long lastSample;
    private static int[] history;

    static {
        setRuntimeArray(new int[4]);
        INSTANCE = new MillisecondSampler();
        TIMER = new Timer();
    }

    private MillisecondSampler() { super(); }

    public static void setRuntimeArray(int[] value) { history = value; }
    public static int[] getRuntimeArray() { return history; }

    public static final double nanosToMillis(long nanos) {
        return nanos / 1000.0;
    }

    public final void start() {
        TIMER.schedule(this, INITIAL_DELAY_MILLIS, 1L);
    }

    public static final long lastSampleMillis() {
        return lastSample;
    }

    public static final long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override public void run() {
        lastSample = currentTimeMillis();
    }

    public final void stop() {
        TIMER.cancel();
    }
}
