package me.rerere.matrix.security;

import java.util.logging.Logger;
import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.api.MatrixApiResponseMessages;
import me.rerere.matrix.runtime.MatrixTasks;

/** Logs a token mismatch and then exits with status zero. */
public final class PostTokenMismatchHandler {
    private PostTokenMismatchHandler() { }

    public static void runSelected(PrefaceRuntimeContext selection) {
        Logger logger = MatrixTasks.logger();
        if (selection == null) throw new NullPointerException("INVOKEVIRTUAL Object npe");
        runAfterLoggerAndInput(logger, selection.getUserKey(), System::exit);
    }

    public static void runAfterLoggerAndInput(Logger logger, String input,
                                               ExitAction exitAction) {
        String message = MatrixApiResponseMessages.invalidKey(input);
        if (logger == null) throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
        logger.severe(message);
        exitAction.exit(0);
        throw new RuntimeException("System.exit returned normally, while it was supposed to halt JVM.");
    }


    public interface ExitAction {
        void exit(int code);
    }
}
