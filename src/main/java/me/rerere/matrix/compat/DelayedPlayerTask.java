package me.rerere.matrix.compat;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;

/** Executes its one-shot task only while its player remains online. */
public final class DelayedPlayerTask extends BukkitRunnable {
    private final Function0<?> task;
    private final Player player;

    public DelayedPlayerTask(Player player, Function0<?> task) {
        this.player = player;
        this.task = task;
    }

    @Override
    public void run() {
        if (player.isOnline()) {
            task.invoke();
        }
    }
}
