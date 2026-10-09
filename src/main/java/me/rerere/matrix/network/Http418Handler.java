package me.rerere.matrix.network;

import java.io.InputStream;
import java.util.logging.Logger;

/** Preserves the generated warning, delay, halt, and post-halt error path. */
public final class Http418Handler {
    private Http418Handler() { }

    public static void handle(String input, InputStream streamToCloseOnFailure) {
        try {
            Logger logger = me.rerere.matrix.runtime.MatrixTasks.logger();
            String warning = me.rerere.matrix.api.MatrixApiResponseMessages.teapot(input);
            if (logger == null) {
                throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
            }
            logger.warning(warning);
            Thread.sleep(1L);
            Runtime.getRuntime().halt(0);
        } catch (Throwable failure) {
            closeAndRethrow(streamToCloseOnFailure, failure);
            return;
        }
        throw new AfterHaltError();
    }

    private static <T extends Throwable> void closeAndRethrow(InputStream streamToClose, Throwable failure) throws T {
        me.rerere.matrix.thirdparty.kotlin.io.CloseableKt.closeFinally(streamToClose, failure);
        throw (T) failure;
    }

    public static final class AfterHaltError extends Error {
        AfterHaltError() {
            super("native continuation after returning halt is unresolved");
        }
    }
}
