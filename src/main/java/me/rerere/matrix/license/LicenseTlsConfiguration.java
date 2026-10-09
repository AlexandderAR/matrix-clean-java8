package me.rerere.matrix.license;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import me.rerere.matrix.crypto.FixedTlsSession;
import me.rerere.matrix.crypto.NativeTlsFailureHandler;
import me.rerere.matrix.network.tls.StrictHostnameVerifier;
import me.rerere.matrix.crypto.TlsRefusalPaths;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;

public final class LicenseTlsConfiguration {
    private LicenseTlsConfiguration() {
    }

    public static void configureAfterHostCheck(HttpsURLConnection connection) throws Exception {
        boolean suspiciousFactory;
        try {
            SSLContext context = SSLContext.getDefault();
            SSLSocketFactory socketFactory = context.getSocketFactory();
            String factoryName = socketFactory.getClass().getName();
            suspiciousFactory = StringsKt.contains(factoryName, "TrustAllSSLSocketFactory", false);
            if (!suspiciousFactory) {
                suspiciousFactory = socketFactory.getClass().isAnonymousClass();
            }
        } catch (Exception failure) {
            NativeTlsFailureHandler.rejectOperation(failure);
            throw new AssertionError("terminal TLS refusal returned");
        }
        if (suspiciousFactory) {
            TlsRefusalPaths.rejectFactory();
        }
        try {
            new FixedTlsSession();
        } catch (Exception failure) {
            NativeTlsFailureHandler.rejectConstructor(failure);
        }
        connection.setHostnameVerifier(StrictHostnameVerifier.INSTANCE);
    }
}
