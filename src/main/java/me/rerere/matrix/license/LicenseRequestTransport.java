package me.rerere.matrix.license;

import java.net.Proxy;
import java.net.URL;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.config.MatrixConfigBridge;
import me.rerere.matrix.network.UrlMismatchRefusal;
import me.rerere.matrix.api.MatrixApiResponseMessages;
import me.rerere.matrix.network.HttpsRequestBodyWriter;
import me.rerere.matrix.util.EntryFormatter;
import me.rerere.matrix.util.jvm.UnsafeMemoryRefusal;

public final class LicenseRequestTransport {
    private LicenseRequestTransport() {}

    public static String serializeBody(Map<?, ?> fields) {
        @SuppressWarnings("unchecked")
        Iterable<Map.Entry<? extends String, ? extends Object>> entries =
                (Iterable<Map.Entry<? extends String, ? extends Object>>) (Iterable<?>) fields.entrySet();
        String joined = CollectionsKt.joinToString(entries, ",", "", "", -1, "...", EntryFormatter.INSTANCE);
        return "{" + joined + "}";
    }

    public static String bodyForVerifiedUrl(String url, Map<?, ?> fields) {
        if (!me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(url, "https://localhost/offline/api/pub/v1/verifyplus")) {
            UrlMismatchRefusal.reject();
        }
        return serializeBody(fields);
    }

    public static Connected sendUntilConnect(String url, Map<?, ?> fields, TlsSetup tlsSetup) throws Exception {
        return sendPreparedBody(url, bodyForVerifiedUrl(url, fields), tlsSetup);
    }

    public static Connected sendPreparedBody(String url, String body, TlsSetup tlsSetup) throws Exception {
        URL requestUrl = new URL(url);
        java.net.URLConnection opened = requestUrl.openConnection(Proxy.NO_PROXY);
        HttpsURLConnection connection = opened instanceof HttpsURLConnection ? (HttpsURLConnection) opened : null;
        if (connection == null) {
            UnsafeMemoryRefusal.refuseUnsafeAccess();
        }
        tlsSetup.configure(connection, requestUrl);
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);

        MatrixConfigBridge pluginHolder = me.rerere.matrix.Matrix.Companion;
        me.rerere.matrix.Matrix plugin = pluginHolder.matrixInstance();
        String version = plugin.getDescription().getVersion();
        String userAgent = MatrixApiResponseMessages.pluginName(version);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Content-Type", "application/json");
        HttpsRequestBodyWriter.writeBody(connection, body);

        HttpsConnectionTimer.Connected timing = HttpsConnectionTimer.connectSelected(connection);
        return new Connected(timing.connection, timing.preConnectMillis);
    }

    public static final class Connected {
        public final HttpsURLConnection connection;
        public final long preConnectMillis;

        public Connected(HttpsURLConnection connection, long preConnectMillis) {
            this.connection = connection;
            this.preConnectMillis = preConnectMillis;
        }
    }

    public interface TlsSetup {
        void configure(HttpsURLConnection connection, URL url) throws Exception;
    }

    public static final class UnsupportedNativePath extends Error {
        public UnsupportedNativePath(String message) { super(message); }
    }
}
