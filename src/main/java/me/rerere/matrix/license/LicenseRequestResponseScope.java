package me.rerere.matrix.license;

import java.net.ProxySelector;
import javax.net.ssl.HttpsURLConnection;

public final class LicenseRequestResponseScope {
    private LicenseRequestResponseScope() {
    }

    public static void run(
            Actions actions,
            ProxySelector originalSelector,
            me.rerere.matrix.network.ResponseCleanup.RecoveryBoundary recoveryBoundary,
            Failures failures) throws Throwable {
        String body;
        try {
            body = actions.body();
        } catch (Throwable failure) {
            // Recovery-boundary failures must escape to the outer recovery path unchanged.
            if (recoveryBoundary.isUnresolved(failure)) {
                throw failure;
            }
            failures.body(failure, originalSelector);
            throw new AssertionError("native body failure handler returned");
        }

        LicenseRequestTransport.Connected connected;
        try {
            connected = actions.send(body);
            actions.certificates(connected.connection);
        } catch (Throwable failure) {
            if (recoveryBoundary.isUnresolved(failure)) {
                throw failure;
            }
            failures.connection(failure, originalSelector);
            throw new AssertionError("native connection failure handler returned");
        }

        actions.response(connected.connection, connected.preConnectMillis);
    }

    public interface Actions {
        String body() throws Throwable;

        LicenseRequestTransport.Connected send(String body) throws Throwable;

        void certificates(HttpsURLConnection connection) throws Throwable;

        void response(HttpsURLConnection connection, long preConnectMillis) throws Throwable;
    }

    public interface Failures {
        void body(Throwable failure, ProxySelector originalSelector) throws Throwable;

        void connection(Throwable failure, ProxySelector originalSelector) throws Throwable;
    }

    public static final class ProductionFailures implements Failures {
        public ProductionFailures() {
        }

        @Override
        public void body(Throwable failure, ProxySelector originalSelector) throws Throwable {
            me.rerere.matrix.network.OuterFailureHandler.restoreAndRethrow(failure,
                    originalSelector,
                    new me.rerere.matrix.network.OuterFailureHandler.Production());
        }

        @Override
        public void connection(Throwable failure, ProxySelector originalSelector) throws Throwable {
            me.rerere.matrix.network.OuterFailureHandler.wrapRestoreAndRethrow(failure, originalSelector);
        }
    }
}
