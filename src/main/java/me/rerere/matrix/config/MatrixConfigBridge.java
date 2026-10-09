package me.rerere.matrix.config;

import java.util.Arrays;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.Matrix;

/** Access to Matrix singleton configuration state. */
public final class MatrixConfigBridge {
    private static String storedValue = "FVenXb";

    public static void setStoredValue(String value) { storedValue = value; }
    public static String getStoredValue() { return storedValue; }

    public final void setEnabled(boolean enabled) {
        Matrix.setRuntimeFlag(enabled);
    }

    public final void logMessage(String message) {
        if (message == null) return;
        if (message.contains("§")) {
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            String formatted = String.format("§6[Matrix] §7%s", Arrays.copyOf(new Object[] { message }, 1));
            Intrinsics.checkNotNullExpressionValue(formatted, "format(...)");
            console.sendMessage(formatted);
            return;
        }
        matrixInstance().getLogger().info(message);
    }

    public final ServerVersion serverVersion() {
        ServerVersion serverVersion = Matrix.getStoredServerVersion();
        if (serverVersion != null) return serverVersion;
        Intrinsics.throwUninitializedPropertyAccessException("serverVersion");
        return null;
    }

    private static String sleepFailureMessage(long duration) {
        return "Failed to sleep " + duration + " ms";
    }

    public final boolean isEnabled() {
        return Matrix.isPlaceholderApiHooked();
    }

    public final boolean isRunning() {
        return Matrix.getRuntimeFlag();
    }

    public final Matrix matrixInstance() {
        Matrix instance = Matrix.getStoredInstance();
        if (instance != null) return instance;
        Intrinsics.throwUninitializedPropertyAccessException("instance");
        return null;
    }

    public final void sleepAndLog(long duration) {
        try {
            Thread.sleep(duration);
        } catch (Throwable failure) {
            Logger logger = Matrix.Companion.matrixInstance().getLogger();
            logger.warning(sleepFailureMessage(duration));
        }
    }

    public final void setDisabled(boolean disabled) {
        Matrix.setPlaceholderApiHooked(disabled);
    }

    private MatrixConfigBridge() { }

    public MatrixConfigBridge(
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker marker) {
        this();
    }
}

