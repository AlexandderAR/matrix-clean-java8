package me.rerere.matrix.compat.scheduler;

import java.util.function.Consumer;

/** Operations required to schedule the Matrix trial task on the asynchronous scheduler. */
public interface TrialSchedulerActions {
    Object getAsyncScheduler();
    Object matrixFromHeight();
    Object runAtFixedRate(
            Object scheduler,
            Object plugin,
            Consumer<Object> task,
            long initialDelay,
            long period);
}
