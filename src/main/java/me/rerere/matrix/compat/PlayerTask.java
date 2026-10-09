package me.rerere.matrix.compat;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;

/** Runs for an online player, and cancels after invoking the terminal callback when offline. */
public final class PlayerTask extends BukkitRunnable {
    private final Function0<?> tired;
    private final Player player;
    private final Function0<?> task;

    public PlayerTask(Player player, Function0<?> task, Function0<?> tired) {
        this.player = player;
        this.task = task;
        this.tired = tired;
    }

    @Override
    public void run() {
        if (player.isOnline()) {
            task.invoke();
        } else {
            tired.invoke();
            cancel();
        }
    }
}
