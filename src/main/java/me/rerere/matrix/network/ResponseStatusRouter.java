package me.rerere.matrix.network;

import javax.net.ssl.HttpsURLConnection;
import java.net.ProxySelector;
import me.rerere.matrix.network.ResponseCleanup.Outer;
import me.rerere.matrix.network.ResponseCleanup.RecoveryBoundary;

/** Routes a completed HTTPS response by status and delegates exception ownership. */
public final class ResponseStatusRouter {
    private ResponseStatusRouter() {}

    public interface SuccessfulResponseHandler {
        void run(HttpsURLConnection connection) throws Throwable;
    }

    public static void routeResponse(HttpsURLConnection connection, ProxySelector proxySelector,
            SuccessfulResponseHandler on200, RecoveryBoundary recoveryBoundary,
            Outer outer) throws Throwable {
        if (connection == null || on200 == null || recoveryBoundary == null || outer == null) {
            throw new NonSuccessResponseHandler.UnresolvedContinuation("missing response router input");
        }
        final int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (Throwable failure) {
            if (recoveryBoundary.isUnresolved(failure)) {
                throw failure;
            }
            outer.failure(failure, proxySelector);
            throw new NonSuccessResponseHandler.UnresolvedContinuation("outer failure returned");
        }
        if (responseCode == 200) {
            on200.run(connection);
        } else {
            NonSuccessResponseHandler.run(connection, proxySelector,
                    new NonSuccessResponseHandler.ProductionDispatch(), recoveryBoundary, outer);
        }
    }
}
