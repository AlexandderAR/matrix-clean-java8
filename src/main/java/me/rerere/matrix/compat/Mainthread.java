package me.rerere.matrix.compat;

import org.bukkit.entity.Player;
import me.rerere.matrix.config.MatrixConfigBridge;
import me.rerere.matrix.player.PlayerSessionRegistry;
import me.rerere.matrix.player.PlayerCheckContext;

/** Schedules player-bound work on the event loop when the plugin is enabled. */
public final class Mainthread {
    private Mainthread() { }

    public static final void executeOnEventLoop(Player player, Runnable task) {
        MatrixConfigBridge pluginAccessor = me.rerere.matrix.Matrix.Companion;
        me.rerere.matrix.Matrix plugin = pluginAccessor.matrixInstance();
        if (!plugin.isEnabled()) return;

        PlayerSessionRegistry playerRegistry = me.rerere.matrix.Matrix.Companion.matrixInstance().getPlayerDataManager();
        PlayerCheckContext playerState = playerRegistry.find(player.getUniqueId());
        if (playerState == null) return;
        playerState.enqueueAction(task);
    }
}
