package me.rerere.matrix.license.network;

import java.security.cert.Certificate;
import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

/** Captured state passed across the HTTP response continuation boundary. */
public final class HttpContinuation {
    public final HttpsURLConnection connection;
    public final Certificate[] certificates;
    public final long startedMillis;
    public final Function1 secondCallback;
    public final boolean foundIntermediatePin;
    public final boolean foundRootPin;

    /** Public boundary for callers in the separate network package. */
    public static HttpContinuation create(HttpsURLConnection connection, Certificate[] certificates,
                                          long startedMillis, Function1 secondCallback,
                                          boolean foundIntermediatePin, boolean foundRootPin) {
        return new HttpContinuation(connection, certificates, startedMillis, secondCallback,
                foundIntermediatePin, foundRootPin);
    }

    private HttpContinuation(HttpsURLConnection connection, Certificate[] certificates,
                     long startedMillis, Function1 secondCallback,
                     boolean foundIntermediatePin, boolean foundRootPin) {
        this.connection = connection;
        this.certificates = certificates;
        this.startedMillis = startedMillis;
        this.secondCallback = secondCallback;
        this.foundIntermediatePin = foundIntermediatePin;
        this.foundRootPin = foundRootPin;
    }

}
