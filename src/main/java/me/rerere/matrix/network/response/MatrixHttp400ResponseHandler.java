package me.rerere.matrix.network.response;

import java.io.InputStream;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.thirdparty.kotlin.io.CloseableKt;
import me.rerere.matrix.license.Http400TerminalResponse;
import me.rerere.matrix.license.LicenseLoggerAccess;
import me.rerere.matrix.network.Http400FailureHandler;
import me.rerere.matrix.network.HttpErrorResponseParser;

/** Parses an HTTP 400 response and dispatches the selected action. */
public final class MatrixHttp400ResponseHandler {
    private MatrixHttp400ResponseHandler() {
        super();
    }

    public static void handle(HttpsURLConnection connection, InputStream savedStream)
            throws Throwable {
        if (savedStream == null) {
            throw new IllegalArgumentException("nonnull saved stream required");
        }
        HttpErrorResponseParser.Parsed parsed;
        try {
            parsed = HttpErrorResponseParser.parse(connection, savedStream);
        } catch (Throwable failure) {
            if (failure instanceof HttpErrorResponseParser.UnsupportedPath) {
                throw failure;
            }
            CloseableKt.closeFinally(savedStream, failure);
            throw failure;
        }

        switch (parsed.action) {
            case OTHER_UNRESOLVED:
                Logger logger;
                try {
                    logger = LicenseLoggerAccess.matrixLogger();
                } catch (Throwable failure) {
                    CloseableKt.closeFinally(savedStream, failure);
                    throw failure;
                }
                Http400FailureHandler.handle(
                        logger, savedStream, parsed.code, parsed.error);
                throw new AssertionError("generic branch returned");
            case PLAN:
            case INVALID_TOKEN:
            case BAN:
                Http400TerminalResponse.handle(parsed.action, parsed.code, parsed.error, savedStream);
                throw new AssertionError("terminal branch returned");
            default:
                throw new AssertionError("unmapped parser action");
        }
    }
}
