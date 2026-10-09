package me.rerere.matrix.compat;

import me.rerere.matrix.compat.Cancelable;

/** Cancel handle returned for a player-bound repeating Bukkit task. */
public final class FixedRateCancelable implements Cancelable {
    public final FixedRatePlayerTask task;

    public FixedRateCancelable(FixedRatePlayerTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
