package me.rerere.matrix.network;

import java.io.InputStream;
import java.util.logging.Logger;
import me.rerere.matrix.license.LicenseResponseException;
import me.rerere.matrix.api.MatrixApiResponseMessages;
import me.rerere.matrix.thirdparty.kotlin.io.CloseableKt;

/** Logs the decoded message for an HTTP 400 response and raises Matrix's refusal exception. */
public final class Http400FailureHandler {
    private Http400FailureHandler() {}

    public static void handle(Logger logger, InputStream response, int status, String context) {
        Throwable failure = null;
        try {
            String warning = MatrixApiResponseMessages.bracketedValue(status, context);
            if (logger == null) {
                throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
            }
            logger.warning(warning);
            throw new LicenseResponseException(
                    MatrixApiResponseMessages.bracketedValue(status, context));
        } catch (Throwable thrown) {
            failure = thrown;
            Http400FailureHandler.<RuntimeException>throwUnchecked(thrown);
            return;
        } finally {
            CloseableKt.closeFinally(response, failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable thrown) throws T {
        throw (T) thrown;
    }
}
