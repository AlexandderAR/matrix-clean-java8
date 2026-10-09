package me.rerere.matrix.network;

import javax.net.ssl.HttpsURLConnection;

/** Checks the HTTPS response host, port, Cf-Ray headers, and elapsed time. */
public final class TlsResponseGuard {
    private TlsResponseGuard() {
    }

    public static void check(HttpsURLConnection connection, long startedAtMillis) {
        if (connection == null) {
            throw new UnresolvedGuard("null HTTPS receiver");
        }
        String host = connection.getURL().getHost();
        if (!"localhost".equals(host)) {
            throw new UnresolvedGuard("host mismatch");
        }
        int port = connection.getURL().getPort();
        if (port != 443) {
            port = connection.getURL().getPort();
            if (port != -1) {
                throw new UnresolvedGuard("other port");
            }
        }
        String firstCfRay = connection.getHeaderField("Cf-Ray");
        if (firstCfRay == null) {
            throw new UnresolvedGuard("null first Cf-Ray");
        }
        String secondCfRay = connection.getHeaderField("Cf-Ray");
        if ("".equals(secondCfRay)) {
            throw new UnresolvedGuard("empty second Cf-Ray");
        }
        long elapsed = System.currentTimeMillis() - startedAtMillis;
        if (elapsed <= 49L) {
            throw new UnresolvedGuard("elapsed <=49 ms");
        }
    }

    public static final class UnresolvedGuard extends Error {
        public UnresolvedGuard(String message) {
            super(message);
        }
    }
}
