package me.rerere.matrix.compat;

import java.util.List;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.MatrixCommandDispatcher;
import me.rerere.matrix.commands.UpdateCommand;
import me.rerere.matrix.player.PlanState;
import me.rerere.matrix.util.BooleanStateBridge;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

/** Narrow bridge from setup code to Matrix, Bukkit, and option accessors. */
public final class PluginSetupBridge {
    private PluginSetupBridge() {
    }

    public static Object matrixFromHeight() {
        return Matrix.Companion.matrixInstance();
    }

    public static Object pluginCommand() {
        return Bukkit.getPluginCommand("matrix");
    }

    public static Object newCommandExecutor() {
        return new MatrixCommandDispatcher();
    }

    public static void installCommandExecutor(Object matrix, Object executor) {
        ((Matrix) matrix).setMatrixCommand((MatrixCommandDispatcher) executor);
    }

    public static void checkNotNull(Object value) {
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(value);
    }

    public static Object getCommandExecutor(Object matrix) {
        return ((Matrix) matrix).getMatrixCommand();
    }

    public static void setExecutor(Object command, Object executor) {
        ((PluginCommand) command).setExecutor((CommandExecutor) executor);
    }

    public static void setTabCompleter(Object command, Object completer) {
        ((PluginCommand) command).setTabCompleter((TabCompleter) completer);
    }

    public static void configureSkBoolean(boolean value) {
        PlanState.INSTANCE.setCloudEnabled(value);
    }

    public static void configureSkList(List<String> values) {
        PlanState.INSTANCE.setPlanNames(values);
    }

    public static void configureHiBoolean(boolean value) {
        UpdateCommand.COMPANION.setEnabled(value);
    }

    public static void configureJeBoolean(boolean value) {
        BooleanStateBridge.angle.setUpdaterEnabled(value);
    }
}
