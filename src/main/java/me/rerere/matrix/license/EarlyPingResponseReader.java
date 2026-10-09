package me.rerere.matrix.license;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.thirdparty.kotlin.io.TextStreamsKt;
import me.rerere.matrix.thirdparty.kotlin.text.Charsets;

/** Reads the input stream after the observed HTTPS response checks have completed. */
public final class EarlyPingResponseReader {
    private EarlyPingResponseReader() {
    }

    public static InputStream acquireAfterObserved200(HttpsURLConnection connection, long requestStartedAt)
            throws IOException {
        if (connection == null) {
            throw new UnresolvedBeforeSavedStream("null HTTPS receiver");
        }

        String host = connection.getURL().getHost();
        if (!"localhost".equals(host)) {
            throw new UnresolvedBeforeSavedStream("host mismatch");
        }

        int port = connection.getURL().getPort();
        if (port != 443) {
            port = connection.getURL().getPort();
            if (port != -1) {
                throw new UnresolvedBeforeSavedStream("other port");
            }
        }

        String firstRayHeader = connection.getHeaderField("Cf-Ray");
        if (firstRayHeader == null) {
            throw new UnresolvedBeforeSavedStream("null first Cf-Ray");
        }
        String secondRayHeader = connection.getHeaderField("Cf-Ray");
        if ("".equals(secondRayHeader)) {
            throw new UnresolvedBeforeSavedStream("empty second Cf-Ray");
        }

        long elapsedMilliseconds = System.currentTimeMillis() - requestStartedAt;
        if (elapsedMilliseconds <= 49L) {
            throw new UnresolvedBeforeSavedStream("elapsed <=49 ms");
        }
        return connection.getInputStream();
    }

    public static String readSavedStream(InputStream input) {
        return TextStreamsKt.readText(new InputStreamReader(input, Charsets.UTF_8));
    }

    public static final class UnresolvedBeforeSavedStream extends Error {
        public UnresolvedBeforeSavedStream(String message) {
            super(message);
        }
    }
}
