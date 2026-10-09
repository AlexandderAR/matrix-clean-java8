package me.rerere.matrix.license;

import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.crypto.TlsRefusalPaths;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

public final class LicenseHostGuard {
    private LicenseHostGuard() { }

    public static void checkBeforeDefaultTls(HttpsURLConnection connection) {
        if (connection == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        URL requestUrl = connection.getURL();
        if (requestUrl == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        if (!Intrinsics.areEqual(requestUrl.getHost(), "localhost")) {
            TlsRefusalPaths.rejectHost();
        }
    }
}
