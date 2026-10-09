package me.rerere.matrix.network;

import java.io.InputStream;
import java.net.ProxySelector;
import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.thirdparty.kotlin.io.CloseableKt;
import me.rerere.matrix.license.Http400TerminalResponse;
import me.rerere.matrix.license.network.NonSuccessResponseDispatcher;

/** Routes a non-success HTTPS response to its supplied dispatch and failure boundaries. */
public final class NonSuccessResponseHandler {
    private NonSuccessResponseHandler() {
    }

    public static void run(HttpsURLConnection connection, ProxySelector proxySelector) throws Throwable {
        run(connection, proxySelector, new ProductionDispatch(), new ProductionBoundaries(),
                new ResponseCleanup.ProductionOuter());
    }

    public static void run(
            HttpsURLConnection connection,
            ProxySelector selector,
            Dispatch dispatch,
            ResponseCleanup.RecoveryBoundary recoveryBoundary,
            ResponseCleanup.Outer outer) throws Throwable {
        if (connection == null || dispatch == null || recoveryBoundary == null || outer == null) {
            throw new UnresolvedContinuation("missing selected-path inputs");
        }

        String rayId;
        InputStream errorStream;
        try {
            rayId = connection.getHeaderField("Cf-Ray");
            errorStream = connection.getErrorStream();
        } catch (Throwable failure) {
            if (recoveryBoundary.isUnresolved(failure)) {
                throw failure;
            }
            outer.failure(failure, selector);
            throw new UnresolvedContinuation("outer failure returned");
        }

        if (errorStream == null) {
            outer.success(selector);
            return;
        }

        int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (Throwable failure) {
            if (recoveryBoundary.isUnresolved(failure)) {
                throw failure;
            }
            try {
                CloseableKt.closeFinally(errorStream, failure);
            } catch (Throwable closeFailure) {
                failure = closeFailure;
            }
            outer.failure(failure, selector);
            throw new UnresolvedContinuation("outer failure returned");
        }

        try {
            dispatch.dispatchResponseBody(connection, rayId, errorStream, responseCode);
        } catch (Throwable failure) {
            if (recoveryBoundary.isUnresolved(failure)) {
                throw failure;
            }
            outer.failure(failure, selector);
            throw new UnresolvedContinuation("outer failure returned");
        }
        throw new UnresolvedContinuation("nonnull-stream dispatch returned");
    }

    public interface Dispatch {
        void dispatchResponseBody(HttpsURLConnection connection, String rayId, InputStream stream, int responseCode)
                throws Throwable;
    }

    public static final class ProductionDispatch implements Dispatch {
        public ProductionDispatch() {
        }

        @Override
        public void dispatchResponseBody(HttpsURLConnection connection, String rayId, InputStream stream, int responseCode)
                throws Throwable {
            NonSuccessResponseDispatcher.dispatchAfterSecondStatus(connection, rayId, stream, responseCode);
        }
    }

    public static final class ProductionBoundaries implements ResponseCleanup.RecoveryBoundary {
        @Override
        public boolean isUnresolved(Throwable failure) {
            return failure instanceof UnresolvedContinuation
                    || failure instanceof NonSuccessResponseDispatcher.UnresolvedResponseError
                    || failure instanceof HttpErrorResponseParser.UnsupportedPath
                    || failure instanceof Http400TerminalResponse.UnresolvedAfterTerminal
                    || failure instanceof Http418Handler.AfterHaltError;
        }
    }

    public static final class UnresolvedContinuation extends Error {
        public UnresolvedContinuation(String message) {
            super(message);
        }
    }
}

