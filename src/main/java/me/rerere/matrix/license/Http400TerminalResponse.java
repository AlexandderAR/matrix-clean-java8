package me.rerere.matrix.license;

import java.io.InputStream;
import java.util.logging.Logger;
import me.rerere.matrix.network.HttpErrorResponseParser;
import me.rerere.matrix.api.MatrixApiResponseMessages;
import me.rerere.matrix.runtime.MatrixTasks;
import me.rerere.matrix.thirdparty.kotlin.io.CloseableKt;
import me.rerere.matrix.license.LicenseLoggerAccess;

public final class Http400TerminalResponse {
    private Http400TerminalResponse() {}
    public static final class UnresolvedAfterTerminal extends Error {
        public UnresolvedAfterTerminal() { super("native continuation after returning terminal call is unresolved"); }
    }
    private static void requireLogger(Logger logger) {
        if (logger == null) throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
    }
    public static void handle(HttpErrorResponseParser.Action action, int code, String message, InputStream savedStream) throws Throwable {
        if (savedStream == null) throw new IllegalArgumentException("nonnull saved stream required");
        if (action != HttpErrorResponseParser.Action.PLAN && action != HttpErrorResponseParser.Action.INVALID_TOKEN
                && action != HttpErrorResponseParser.Action.BAN) throw new IllegalArgumentException("not a selected terminal action");
        try {
            switch (action) {
                case PLAN: {
                    Logger logger = LicenseLoggerAccess.matrixLogger();
                    requireLogger(logger);
                    logger.warning("You have not bought a plan, please buy one at https://localhost/offline");
                    Thread.sleep(3000L);
                    System.exit(0);
                    break;
                }
                case INVALID_TOKEN: {
                    Logger logger = LicenseLoggerAccess.matrixLogger();
                    requireLogger(logger);
                    logger.warning("Invalid token, please check your config.yml You can get Token at https://localhost/offline");
                    Thread.sleep(3000L);
                    System.exit(0);
                    break;
                }
                case BAN: {
                    Logger logger = MatrixTasks.logger();
                    String text = MatrixApiResponseMessages.bracketedValue(code, message);
                    requireLogger(logger);
                    logger.warning(text);
                    Thread.sleep(1L);
                    Runtime.getRuntime().halt(0);
                    break;
                }
                default: throw new AssertionError("unreachable action");
            }
        } catch (Throwable failure) {
            CloseableKt.closeFinally(savedStream, failure);
            throw failure;
        }
        throw new UnresolvedAfterTerminal();
    }
}
