package me.rerere.matrix.util;

import java.util.Queue;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.file.YamlFile;
import me.rerere.matrix.Matrix;

/** Shared queue and configuration helpers used by the ping manager. */
public final class PingManagerSupport {
    private static boolean status = true;
    private static final String WARNING_SETTING = "disable_ping_manager_warning";
    private static final Lazy<Boolean> WARNING_DISABLED =
            LazyKt.lazy((Function0<Boolean>) PingManagerSupport::warningDisabledFromConfig);

    private PingManagerSupport() {
    }

    public static void invokeQueuedAction(Queue<?> queue) {
        dispatchQueueItem(queue);
    }

    private static void dispatchQueueItem(Queue<?> queue) {
        while (true) {
            Function0<?> task = (Function0<?>) queue.poll();
            if (task == null) return;
            task.invoke();
        }
    }

    private static boolean cachedWarningDisabled() {
        return WARNING_DISABLED.getValue().booleanValue();
    }

    public static boolean isWarningDisabled() {
        return cachedWarningDisabled();
    }

    public static boolean fitsSignedShort(int value) {
        return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
    }

    private static boolean warningDisabledFromConfig() {
        YamlFile config = Matrix.Companion.matrixInstance().getConfigManager()
                .getMainConfiguration().configuration();
        return config.getBoolean(WARNING_SETTING, false);
    }

    public static void setStatus(boolean value) {
        status = value;
    }

    public static boolean status() {
        return status;
    }

    private static boolean isStatusDisabled() {
        return !status;
    }
}
