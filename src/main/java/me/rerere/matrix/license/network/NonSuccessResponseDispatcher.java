package me.rerere.matrix.license.network;

import java.io.InputStream;
import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.license.LicenseResponseException;
import me.rerere.matrix.network.Http418Handler;
import me.rerere.matrix.api.MatrixApiResponseMessages;
import me.rerere.matrix.network.response.MatrixHttp400ResponseHandler;

/** Routes non-200 HTTP responses through the recovered status-specific actions. */
public final class NonSuccessResponseDispatcher {
    private NonSuccessResponseDispatcher() {
    }

    public static final class UnresolvedResponseError extends Error {
        UnresolvedResponseError(String message) {
            super("unresolved native return from " + message);
        }
    }

    public static void dispatchAfterSecondStatus(HttpsURLConnection connection, String url,
                                         InputStream response, int status) {
        if (response == null) {
            return;
        }
        if (status == 400) {
            try {
                MatrixHttp400ResponseHandler.handle(connection, response);
            } catch (Throwable failure) {
                throw unchecked(failure);
            }
            throw new UnresolvedResponseError("HTTP400");
        }
        if (status == 418) {
            Http418Handler.handle(url, response);
            throw new UnresolvedResponseError("HTTP418");
        }

        try {
            switch (status) {
                case 502:
                    Thread.sleep(1500L);
                    throw new LicenseResponseException(
                            "[502] Bad Gateway, maybe Matrix API is down");
                case 403:
                    Thread.sleep(3000L);
                    throw new LicenseResponseException(MatrixApiResponseMessages.wafBlocked(url));
                case 429:
                    Thread.sleep(3000L);
                    throw new LicenseResponseException(MatrixApiResponseMessages.rateLimit(url));
                default:
                    Thread.sleep(3000L);
                    throw new LicenseResponseException(
                            MatrixApiResponseMessages.unexpectedResponse(connection.getResponseCode(), url));
            }
        } catch (Throwable failure) {
            me.rerere.matrix.thirdparty.kotlin.io.CloseableKt.closeFinally(response, failure);
            throw unchecked(failure);
        }
    }

    private static RuntimeException unchecked(Throwable failure) {
        return NonSuccessResponseDispatcher.<RuntimeException>throwAny(failure);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T throwAny(Throwable failure) throws T {
        throw (T) failure;
    }
}
