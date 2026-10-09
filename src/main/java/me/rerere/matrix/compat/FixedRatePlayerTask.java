package me.rerere.matrix.compat;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;

/** Executes while the player is online; otherwise invokes the terminal callback and cancels. */
public final class FixedRatePlayerTask extends BukkitRunnable {
    private final Function0<?> task;
    private final Function0<?> onPlayerOffline;
    private final Player player;

    public FixedRatePlayerTask(Player player, Function0<?> task, Function0<?> onPlayerOffline) {
        this.player = player;
        this.task = task;
        this.onPlayerOffline = onPlayerOffline;
    }

    @Override
    public void run() {
        if (player.isOnline()) {
            task.invoke();
        } else {
            onPlayerOffline.invoke();
            cancel();
        }
    }
}
