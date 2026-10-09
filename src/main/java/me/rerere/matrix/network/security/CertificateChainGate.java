package me.rerere.matrix.network.security;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;

public final class CertificateChainGate {
    private CertificateChainGate() {
    }

    public static State check(HttpsURLConnection connection) throws IOException {
        long startedMillis = System.currentTimeMillis();
        connection.connect();
        Certificate[] certificates = connection.getServerCertificates();
        if (certificates == null) {
            throw new NullPointerException("\"ARRAYLENGTH npe\" on -1");
        }
        for (Certificate certificate : certificates) {
            if (!(certificate instanceof X509Certificate)) {
                Runtime.getRuntime().halt(0);
            }
        }
        if (certificates.length != 3) {
            Runtime.getRuntime().halt(0);
        }
        return new State(startedMillis, certificates);
    }

    public static final class State {
        public final long startedMillis;
        public final Certificate[] certificates;

        private State(long startedMillis, Certificate[] certificates) {
            this.startedMillis = startedMillis;
            this.certificates = certificates;
        }
    }
}
