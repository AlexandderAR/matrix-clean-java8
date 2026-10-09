package me.rerere.matrix.commands.setup;

import me.rerere.matrix.compat.scheduler.TrialSchedulerActions;

/** Callback invoked by the trial scheduler. */
@FunctionalInterface
public interface TrialSchedule {

    void run(TrialSchedulerActions actions) throws Throwable;
}
