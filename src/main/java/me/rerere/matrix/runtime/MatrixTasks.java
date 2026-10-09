package me.rerere.matrix.runtime;

import java.util.Collection;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;

/** Matrix-owned Bukkit task and notification helpers. */
public final class MatrixTasks {
    private MatrixTasks() {}

    private static void invokeFunction(Function0<?> function) {
        function.invoke();
    }

    public static void runOnServerThread(Function0<?> function) {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        Matrix plugin = Matrix.Companion.matrixInstance();
        scheduler.runTask(plugin, () -> invokeFunction(function));
    }

    public static Logger logger() {
        return Matrix.Companion.matrixInstance().getLogger();
    }

    public static void broadcast(String message) {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        for (Player player : onlinePlayers) {
            player.sendMessage(message);
        }
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(message);
    }

}
