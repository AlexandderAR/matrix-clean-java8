package me.rerere.matrix.compat;

import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.AsyncScheduler;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.FoliaScheduler;

/** Typed boundary for the Folia scheduler bridge. */
public final class FoliaSchedulerBridge {
    private FoliaSchedulerBridge() { }

    public static Object getAsyncScheduler() {
        return FoliaScheduler.getAsyncScheduler();
    }

    public static Object matrixFromHeight() {
        return Matrix.Companion.matrixInstance();
    }

    public static Object runAtFixedRate(
            Object scheduler, Object plugin, Consumer<?> task, long initialDelay, long period) {
        return ((AsyncScheduler) scheduler).runAtFixedRate(
                (Plugin) plugin, (Consumer) task, initialDelay, period);
    }
}
