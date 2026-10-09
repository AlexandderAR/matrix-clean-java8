package me.rerere.matrix.license;

import java.net.ProxySelector;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.network.OuterFailureHandlerReturned;
import me.rerere.matrix.network.ResponseCleanup.Outer;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

public final class LicenseResponsePreconditions {
    private LicenseResponsePreconditions() {
    }

    public interface GuardedCall {
        Object run() throws Throwable;
    }

    public static final class UnresolvedPrecondition extends Error {
        public UnresolvedPrecondition(String message) {
            super(message);
        }
    }

    private static Object call(GuardedCall operation, ProxySelector originalSelector,
                               Outer failures) throws Throwable {
        try {
            return operation.run();
        } catch (Throwable failure) {
            if (failure instanceof UnresolvedPrecondition) {
                throw failure;
            }
            failures.failure(failure, originalSelector);
            throw new OuterFailureHandlerReturned();
        }
    }

    public static void check(HttpsURLConnection connection, long preConnectMillis,
                             ProxySelector originalSelector,
                             Outer failures) throws Throwable {
        if (connection == null || failures == null) {
            throw new UnresolvedPrecondition("missing 200 receiver/outer");
        }
        URL requestUrl = (URL) call(connection::getURL, originalSelector, failures);
        if (requestUrl == null) {
            failures.failure(
                    new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1"), originalSelector);
            throw new UnresolvedPrecondition("outer returned after null host URL");
        }
        String host = (String) call(requestUrl::getHost, originalSelector, failures);
        boolean expectedHost = ((Boolean) call(() -> Boolean.valueOf(Intrinsics.areEqual(host, "localhost")),
                originalSelector, failures)).booleanValue();
        if (!expectedHost) {
            throw new UnresolvedPrecondition("host mismatch");
        }
        URL portUrl = (URL) call(connection::getURL, originalSelector, failures);
        if (portUrl == null) {
            failures.failure(
                    new NullPointerException("\"INVOKEVIRTUAL Int npe\" on -1"), originalSelector);
            throw new UnresolvedPrecondition("outer returned after null first port URL");
        }
        int port = ((Integer) call(portUrl::getPort, originalSelector, failures)).intValue();
        if (port != 443) {
            URL fallbackPortUrl = (URL) call(connection::getURL, originalSelector, failures);
            if (fallbackPortUrl == null) {
                failures.failure(
                        new NullPointerException("\"INVOKEVIRTUAL Int npe\" on -1"), originalSelector);
                throw new UnresolvedPrecondition("outer returned after null second port URL");
            }
            port = ((Integer) call(fallbackPortUrl::getPort, originalSelector, failures)).intValue();
            if (port != -1) {
                throw new UnresolvedPrecondition("other port");
            }
        }
        String firstRayHeader = (String) call(() -> connection.getHeaderField("Cf-Ray"), originalSelector, failures);
        if (firstRayHeader == null) {
            throw new UnresolvedPrecondition("null first Cf-Ray");
        }
        String secondRayHeader = (String) call(() -> connection.getHeaderField("Cf-Ray"), originalSelector, failures);
        boolean emptyRay = ((Boolean) call(() -> Boolean.valueOf(Intrinsics.areEqual(secondRayHeader, "")),
                originalSelector, failures)).booleanValue();
        if (emptyRay) {
            throw new UnresolvedPrecondition("empty second Cf-Ray");
        }
        long currentTimeMillis = ((Long) call(System::currentTimeMillis, originalSelector, failures)).longValue();
        long elapsedMillis = currentTimeMillis - preConnectMillis;
        if (elapsedMillis <= 49L) {
            throw new UnresolvedPrecondition("elapsed <=49 ms");
        }
    }
}
