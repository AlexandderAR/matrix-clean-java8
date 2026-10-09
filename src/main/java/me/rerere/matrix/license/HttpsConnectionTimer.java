package me.rerere.matrix.license;

import java.io.IOException;
import javax.net.ssl.HttpsURLConnection;

public final class HttpsConnectionTimer {
    private HttpsConnectionTimer() {
    }

    public static Connected connectSelected(HttpsURLConnection connection) throws IOException {
        long preConnectMillis = System.currentTimeMillis();
        connection.connect();
        return new Connected(connection, preConnectMillis);
    }

    public static final class Connected {
        public final HttpsURLConnection connection;
        public final long preConnectMillis;

        private Connected(HttpsURLConnection connection, long preConnectMillis) {
            this.connection = connection;
            this.preConnectMillis = preConnectMillis;
        }

    }
}
