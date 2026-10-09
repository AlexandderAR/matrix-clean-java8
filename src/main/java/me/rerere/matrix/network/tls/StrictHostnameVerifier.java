package me.rerere.matrix.network.tls;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Verifies the configured Matrix service hostname. */
public final class StrictHostnameVerifier implements HostnameVerifier {
    private static final String EXPECTED_HOST = "localhost".intern();
    public static final StrictHostnameVerifier INSTANCE = new StrictHostnameVerifier();

    public StrictHostnameVerifier() {
    }

    @Override
    public final boolean verify(String hostname, SSLSession session) {
        return Intrinsics.areEqual(hostname, EXPECTED_HOST);
    }
}
