package me.rerere.matrix.network;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.io.OutputStream;
import javax.security.auth.x500.X500Principal;
import me.rerere.matrix.thirdparty.kotlin.Result$Failure;
import me.rerere.matrix.thirdparty.kotlin.ResultKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Handles the pinned HTTPS request and its license-service responses. */
public final class HttpResponseBoundary {
    private HttpResponseBoundary() { }

    static final class ClosedActionFailure extends Error {
        private final Throwable original;
        ClosedActionFailure(Throwable cause) {
            super(null, cause, false, false);
            this.original = cause;
        }
    }

    private static RuntimeException rethrow(Throwable value) {
        return HttpResponseBoundary.<RuntimeException>sneakyThrow(value);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneakyThrow(Throwable value) throws T {
        throw (T) value;
    }

    private static me.rerere.matrix.Matrix matrixFromHeight() {
        me.rerere.matrix.config.MatrixConfigBridge height =
                me.rerere.matrix.Matrix.Companion;
        if (height == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        try {
            MethodHandle handle = MethodHandles.lookup().findVirtual(
                    me.rerere.matrix.config.MatrixConfigBridge.class, "matrixInstance",
                    MethodType.methodType(me.rerere.matrix.Matrix.class));
            return (me.rerere.matrix.Matrix) handle.invokeExact(height);
        } catch (Throwable failure) {
            throw rethrow(failure);
        }
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalArgumentException("Failed requirement.");
    }

    private static String principalName(X500Principal principal) {
        if (principal == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        return principal.getName();
    }

    private static byte[] certificateEncoding(byte[] encoded) {
        if (encoded == null) throw new NullPointerException("\"IASTORE npe\" on -1");
        return encoded;
    }

    private static void writeOutput(OutputStream output, byte[] bytes) throws java.io.IOException {
        if (output == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        output.write(bytes);
    }

    private static Throwable resultExceptionOrNull(Object value) {
        return value instanceof Result$Failure ? ((Result$Failure) value).exception : null;
    }
    private static Object tlsReflectionResult() {
        Object companion = me.rerere.matrix.thirdparty.kotlin.Result.Companion;
        try {
            java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            Intrinsics.checkNotNull(unsafe, "null cannot be cast to non-null type sun.misc.Unsafe");
            return unsafe;
        } catch (Throwable failure) {
            return ResultKt.createFailure(failure);
        }
    }

    private static void tlsPutAddressOrHaltFailure() {
        Object value = tlsReflectionResult();
        Throwable failure = resultExceptionOrNull(value);
        if (failure != null) Runtime.getRuntime().halt(1);
        ResultKt.throwOnFailure(value);
        ((sun.misc.Unsafe) value).putAddress(0L, 0L);
    }

    private static void tlsRejectedFactory() {
        tlsPutAddressOrHaltFailure();
        Runtime.getRuntime().halt(0);
    }

    private static Object tlsPrepareOrReturn(String address) throws Exception {
        java.net.URL url = new java.net.URL(address);
        java.net.URLConnection raw = url.openConnection(java.net.Proxy.NO_PROXY);
        if (!(raw instanceof javax.net.ssl.HttpsURLConnection)) {
            tlsPutAddressOrHaltFailure();
            return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
        }
        javax.net.ssl.HttpsURLConnection https = (javax.net.ssl.HttpsURLConnection) raw;
        java.net.URL actualUrl = https.getURL();
        if (actualUrl == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        String host = actualUrl.getHost();
        if (!Intrinsics.areEqual(host, "localhost")) {
            tlsPutAddressOrHaltFailure();
            return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
        }
        javax.net.ssl.SSLSocketFactory factory = javax.net.ssl.SSLContext.getDefault().getSocketFactory();
        String factoryName = factory.getClass().getName();
        boolean trustAll = me.rerere.matrix.thirdparty.kotlin.text.StringsKt.contains(
                factoryName, "TrustAllSSLSocketFactory", false);
        if (trustAll) tlsRejectedFactory();
        else if (factory.getClass().isAnonymousClass()) tlsRejectedFactory();
        new me.rerere.matrix.crypto.FixedTlsSession();
        https.setHostnameVerifier(me.rerere.matrix.network.tls.StrictHostnameVerifier.INSTANCE);
        return https;
    }

    private static void terminateOnMissingPin() {
        Object value = tlsReflectionResult();
        Throwable failure = resultExceptionOrNull(value);
        if (failure != null) Runtime.getRuntime().halt(1);
        ResultKt.throwOnFailure(value);
        ((sun.misc.Unsafe) value).putAddress(0L, 0L);
        Runtime.getRuntime().halt(0);
    }
    private static void terminateOnRejected200Authority() {
        Object value = tlsReflectionResult();
        Throwable failure = resultExceptionOrNull(value);
        if (failure != null) Runtime.getRuntime().halt(1);
        ResultKt.throwOnFailure(value);
        ((sun.misc.Unsafe) value).putAddress(0L, 0L);
        Runtime.getRuntime().halt(0);
    }
    private static void terminateOnMissing200Ray() {
        Object value = tlsReflectionResult();
        Throwable failure = resultExceptionOrNull(value);
        if (failure != null) Runtime.getRuntime().halt(1);
        ResultKt.throwOnFailure(value);
        ((sun.misc.Unsafe) value).putAddress(0L, 0L);
        Runtime.getRuntime().halt(0);
    }

    private static void closeAfterResponseFailure(java.io.InputStream stream, Throwable failure) {
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.InlineMarker.finallyStart(1);
        me.rerere.matrix.thirdparty.kotlin.io.CloseableKt.closeFinally(stream, failure);
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.InlineMarker.finallyEnd(1);
    }

    private static ClosedActionFailure afterActionFailure(java.io.InputStream stream, Throwable failure) {
        try {
            closeAfterResponseFailure(stream, failure);
        } catch (Throwable closeFailure) {
            return new ClosedActionFailure(closeFailure);
        }
        return new ClosedActionFailure(failure);
    }

    private static void closeSignaled400(java.io.InputStream stream, Throwable failure) {
        try {
            closeAfterResponseFailure(stream, failure);
        } catch (Throwable closeFailure) {
            throw new ClosedActionFailure(closeFailure);
        }
    }

    private static void warning400(java.util.logging.Logger logger, String message, java.io.InputStream stream) {
        try {
            if (logger == null) throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
            logger.warning(message);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static void sleep400(long millis, java.io.InputStream stream) {
        try {
            Thread.sleep(millis);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }
    private static boolean contains400(String text, String search, boolean ignoreCase, java.io.InputStream stream) {
        try {
            return me.rerere.matrix.thirdparty.kotlin.text.StringsKt.contains(text, search, ignoreCase);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static void exit400(int status, java.io.InputStream stream) {
        try {
            System.exit(status);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static void halt400(Runtime runtime, int status, java.io.InputStream stream) {
        try {
            if (runtime == null) throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
            runtime.halt(status);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static Runtime runtimeBan400(java.io.InputStream stream) {
        try {
            return Runtime.getRuntime();
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static Runtime runtime418(java.io.InputStream stream) {
        try {
            return Runtime.getRuntime();
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static void halt418(Runtime runtime, int status, java.io.InputStream stream) {
        try {
            if (runtime == null) throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
            runtime.halt(status);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static int rereadResponseCode(javax.net.ssl.HttpsURLConnection connection, java.io.InputStream stream) {
        try {
            return connection.getResponseCode();
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static me.rerere.matrix.license.LicenseResponseException construct400(String message, java.io.InputStream stream) {
        try {
            return new me.rerere.matrix.license.LicenseResponseException(message);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static me.rerere.matrix.license.LicenseResponseException constructOtherResponse(String message, java.io.InputStream stream) {
        try {
            return new me.rerere.matrix.license.LicenseResponseException(message);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }
    private static String format400(int index, String text, java.io.InputStream stream) {
        try {
            return me.rerere.matrix.network.ApiResponseMessages.formatIndex(index, text);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static String formatGeneric(int status, String rayId, java.io.InputStream stream) {
        try {
            return me.rerere.matrix.network.ApiResponseMessages.unexpectedResponse(status, rayId);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static String format403(String rayId, java.io.InputStream stream) {
        try {
            return me.rerere.matrix.network.ApiResponseMessages.wafBlocked(rayId);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static String format429(String rayId, java.io.InputStream stream) {
        try {
            return me.rerere.matrix.network.ApiResponseMessages.tooManyRequests(rayId);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static String format418(String rayId, java.io.InputStream stream) {
        try {
            return me.rerere.matrix.network.ApiResponseMessages.teapot(rayId);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }
    private static java.util.logging.Logger logger400(me.rerere.matrix.Matrix matrix,
                                                       java.io.InputStream stream) {
        try {
            if (matrix == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
            return ((org.bukkit.plugin.Plugin) matrix).getLogger();
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static java.util.logging.Logger loggerBan400(java.io.InputStream stream) {
        try {
            return me.rerere.matrix.runtime.MatrixTasks.logger();
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static boolean equals400(Object first, Object second, java.io.InputStream stream) {
        try {
            return Intrinsics.areEqual(first, second);
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }

    private static me.rerere.matrix.Matrix matrix400(java.io.InputStream stream) {
        try {
            return matrixFromHeight();
        } catch (Throwable failure) {
            throw afterActionFailure(stream, failure);
        }
    }
    private static me.rerere.matrix.network.http.ParsedBadRequest parse400(javax.net.ssl.HttpsURLConnection connection, java.io.InputStream stream) throws java.io.IOException {
        java.io.InputStreamReader reader = new java.io.InputStreamReader(stream,
                me.rerere.matrix.thirdparty.kotlin.text.Charsets.UTF_8);
        String body = me.rerere.matrix.thirdparty.kotlin.io.TextStreamsKt.readText(reader);
        me.rerere.matrix.thirdparty.com.google.gson.JsonObject json =
                new me.rerere.matrix.thirdparty.com.google.gson.JsonParser().parse(body).getAsJsonObject();
        me.rerere.matrix.thirdparty.com.google.gson.JsonElement errorValue = json.get("error");
        String error = errorValue == null ? "Unknown error" : errorValue.getAsString();
        me.rerere.matrix.thirdparty.com.google.gson.JsonElement codeValue = json.get("code");
        int code = codeValue == null ? connection.getResponseCode() : codeValue.getAsInt();
        return me.rerere.matrix.network.http.ParsedBadRequest.from(error, code);
    }

    private static Object selectedResponse(me.rerere.matrix.license.network.HttpContinuation state) throws Exception {
        int code = state.connection.getResponseCode();
        if (code == 200) return selected200(state);
        String rayId = state.connection.getHeaderField("Cf-Ray");
        java.io.InputStream errorStream = state.connection.getErrorStream();
        return selectedOtherResponse(state, errorStream, rayId, code);
    }

    private static Object selectedOtherResponse(me.rerere.matrix.license.network.HttpContinuation state, java.io.InputStream stream,
                                        String rayId, int status) throws Exception {
        try {
            switch (status) {
                case 400:
                    return selected400(state, stream);
                case 403: {
                    sleep400(3000L, stream);
                    me.rerere.matrix.license.LicenseResponseException response = constructOtherResponse(format403(rayId, stream), stream);
                    closeAfterResponseFailure(stream, response);
                    throw response;
                }
                case 429: {
                    sleep400(3000L, stream);
                    me.rerere.matrix.license.LicenseResponseException response = constructOtherResponse(format429(rayId, stream), stream);
                    closeAfterResponseFailure(stream, response);
                    throw response;
                }
                case 502: {
                    sleep400(1500L, stream);
                    me.rerere.matrix.license.LicenseResponseException response = constructOtherResponse(
                            "[502] Bad Gateway, maybe Matrix API is down", stream);
                    closeAfterResponseFailure(stream, response);
                    throw response;
                }
                case 418: {
                    java.util.logging.Logger logger = loggerBan400(stream);
                    warning400(logger, format418(rayId, stream), stream);
                    sleep400(1L, stream);
                    halt418(runtime418(stream), 0, stream);
                    throw new me.rerere.matrix.network.UnsupportedNativePath("Runtime.halt(0) returned", state);
                }
                default: {
                    sleep400(3000L, stream);
                    int currentStatus = rereadResponseCode(state.connection, stream);
                    me.rerere.matrix.license.LicenseResponseException response = constructOtherResponse(
                            formatGeneric(currentStatus, rayId, stream), stream);
                    closeAfterResponseFailure(stream, response);
                    throw response;
                }
            }
        } catch (ClosedActionFailure failure) {
            throw (failure.original instanceof Exception)
                    ? (Exception) failure.original
                    : rethrow(failure.original);
        }
    }
    private static Object selected200(me.rerere.matrix.license.network.HttpContinuation state) throws Exception {
        try {
            javax.net.ssl.HttpsURLConnection connection = state.connection;
            java.net.URL url = connection.getURL();
            if (url == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
            if (!Intrinsics.areEqual(url.getHost(), "localhost")) {
                terminateOnRejected200Authority();
                throw new me.rerere.matrix.network.UnsupportedNativePath("rejected HTTP 200 authority terminal returned", state);
            }
            if (url.getPort() != 443) {
                java.net.URL secondUrl = connection.getURL();
                if (secondUrl == null) throw new NullPointerException("\"INVOKEVIRTUAL Int npe\" on -1");
                if (secondUrl.getPort() != -1) {
                    terminateOnRejected200Authority();
                    throw new me.rerere.matrix.network.UnsupportedNativePath("rejected HTTP 200 authority terminal returned", state);
                }
            }
            String ray = connection.getHeaderField("Cf-Ray");
            if (ray == null) {
                terminateOnMissing200Ray();
                throw new me.rerere.matrix.network.UnsupportedNativePath("missing HTTP 200 Ray terminal returned", state);
            }
            String secondRay = connection.getHeaderField("Cf-Ray");
            if (Intrinsics.areEqual(secondRay, "")) {
                terminateOnMissing200Ray();
                throw new me.rerere.matrix.network.UnsupportedNativePath("missing HTTP 200 Ray terminal returned", state);
            }
            long elapsedMillis = System.currentTimeMillis() - state.startedMillis;
            if (elapsedMillis <= 49L) {
                Runtime.getRuntime().halt(0);
                throw new me.rerere.matrix.network.UnsupportedNativePath("Runtime.halt(0) returned", state);
            }
            java.io.InputStream responseStream = connection.getInputStream();
            String responseBody;
            try {
                java.io.InputStreamReader reader = new java.io.InputStreamReader(responseStream,
                        me.rerere.matrix.thirdparty.kotlin.text.Charsets.UTF_8);
                responseBody = me.rerere.matrix.thirdparty.kotlin.io.TextStreamsKt.readText(reader);
            } catch (Throwable failure) {
                closeAfterResponseFailure(responseStream, failure);
                throw rethrow(failure);
            }
            try {
                if (state.secondCallback == null) {
                    throw new NullPointerException("\"INVOKEINTERFACE Object npe\" on -1");
                }
                state.secondCallback.invoke(responseBody);
            } catch (Throwable failure) {
                closeAfterResponseFailure(responseStream, failure);
                throw rethrow(failure);
            }
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.InlineMarker.finallyStart(1);
            me.rerere.matrix.thirdparty.kotlin.io.CloseableKt.closeFinally(responseStream, null);
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.InlineMarker.finallyEnd(1);
            return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
        } catch (me.rerere.matrix.network.UnsupportedNativePath path) {
            throw path;
        }
    }
    public static Object request(String address, String method, String body,
                          me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1 configure,
                          me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1 responseCallback,
                          int mask, Object marker) {
        try {
            if ((mask & 4) != 0) body = null;
            if ((mask & 8) != 0) {
                configure = (me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<?, ?>)
                        me.rerere.matrix.network.HttpsConnectionHook.INSTANCE;
            }
            Object prepared = tlsPrepareOrReturn(address);
            if (!(prepared instanceof javax.net.ssl.HttpsURLConnection)) return prepared;
            javax.net.ssl.HttpsURLConnection connection = (javax.net.ssl.HttpsURLConnection) prepared;
            connection.setRequestMethod(method);
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            String version = ((org.bukkit.plugin.Plugin) matrixFromHeight()).getDescription().getVersion();
            connection.setRequestProperty("User-Agent",
                    me.rerere.matrix.network.ApiResponseMessages.matrixVersion(version));
            if (configure == null) throw new NullPointerException("\"INVOKEINTERFACE Object npe\" on -1");
            configure.invoke(connection);
            if (body != null) {
                java.io.OutputStream output = connection.getOutputStream();
                Throwable primary = null;
                try {
                    byte[] encoded = body.getBytes(me.rerere.matrix.thirdparty.kotlin.text.Charsets.UTF_8);
                    Intrinsics.checkNotNullExpressionValue(encoded, "getBytes(...)");
                    writeOutput(output, encoded);
                } catch (Throwable failure) {
                    primary = failure;
                    throw rethrow(failure);
                } finally {
                    me.rerere.matrix.thirdparty.kotlin.jvm.internal.InlineMarker.finallyStart(1);
                    me.rerere.matrix.thirdparty.kotlin.io.CloseableKt.closeFinally(output, primary);
                    me.rerere.matrix.thirdparty.kotlin.jvm.internal.InlineMarker.finallyEnd(1);
                }
            }

            me.rerere.matrix.network.security.CertificateChainGate.State gate =
                    me.rerere.matrix.network.security.CertificateChainGate.check(connection);
            boolean hasPinnedIntermediate = false;
            boolean hasPinnedRoot = false;
            java.security.cert.Certificate[] certificates = gate.certificates;
            for (int index = 0; index < certificates.length; index++) {
                java.security.cert.X509Certificate certificate =
                        (java.security.cert.X509Certificate) certificates[index];
                if (index > 0) {
                    java.security.cert.X509Certificate previous =
                            (java.security.cert.X509Certificate) certificates[index - 1];
                    require(Intrinsics.areEqual(principalName(certificate.getSubjectX500Principal()),
                            principalName(previous.getIssuerX500Principal())));
                }
                if (index == 0) {
                    require(Intrinsics.areEqual(principalName(certificate.getSubjectX500Principal()),
                            "CN=localhost"));
                    require(Intrinsics.areEqual(principalName(certificate.getIssuerX500Principal()),
                            "CN=WE1,O=Google Trust Services,C=US"));
                } else if (index == 2) {
                    require(me.rerere.matrix.thirdparty.kotlin.text.StringsKt.contains(
                            principalName(certificate.getIssuerX500Principal()), "GlobalSign Root CA", false));
                }
            }

            java.util.Iterator<java.security.cert.Certificate> iterator =
                    me.rerere.matrix.thirdparty.kotlin.jvm.internal.ArrayIteratorKt.iterator(certificates);
            while (iterator.hasNext()) {
                java.security.cert.X509Certificate certificate =
                        (java.security.cert.X509Certificate) iterator.next();
                String subject = principalName(certificate.getSubjectX500Principal());
                String issuer = principalName(certificate.getIssuerX500Principal());
                byte[] encoded = certificateEncoding(certificate.getEncoded());
                String digest = me.rerere.matrix.crypto.Sha256Digest.digestHex(encoded);
                String digestLabel = me.rerere.matrix.network.ApiResponseMessages.sha256Prefix(digest);
                if (me.rerere.matrix.thirdparty.kotlin.text.StringsKt.contains(subject, "CN=WE1", false)
                        && me.rerere.matrix.thirdparty.kotlin.text.StringsKt.contains(
                                issuer, "Google Trust Services", false)
                        && Intrinsics.areEqual(digestLabel,
                                "sha256:1dfc1605fbad358d8bc844f76d15203fac9ca5c1a79fd4857ffaf2864fbebf96")) {
                    hasPinnedIntermediate = true;
                }
                if (me.rerere.matrix.thirdparty.kotlin.text.StringsKt.contains(subject, "CN=GTS Root R4", false)
                        && me.rerere.matrix.thirdparty.kotlin.text.StringsKt.contains(
                                issuer, "GlobalSign Root CA", false)
                        && Intrinsics.areEqual(digestLabel,
                                "sha256:76b27b80a58027dc3cf1da68dac17010ed93997d0b603e2fadbe85012493b5a7")) {
                    hasPinnedRoot = true;
                }
            }
            if (!hasPinnedIntermediate || !hasPinnedRoot) terminateOnMissingPin();
            me.rerere.matrix.license.network.HttpContinuation continuation =
                    me.rerere.matrix.license.network.HttpContinuation.create(connection, certificates,
                            gate.startedMillis, responseCallback, hasPinnedIntermediate, hasPinnedRoot);
            return selectedResponse(continuation);
        } catch (me.rerere.matrix.network.UnsupportedNativePath failure) {
            throw failure;
        } catch (me.rerere.matrix.network.TerminalContinuationError failure) {
            throw failure;
        } catch (Throwable failure) {
            return me.rerere.matrix.thirdparty.kotlin.ResultKt.createFailure(failure);
        }
    }
    private static Object selected400(me.rerere.matrix.license.network.HttpContinuation state, java.io.InputStream stream) {
        final me.rerere.matrix.network.http.ParsedBadRequest parsed;
        try {
            parsed = parse400(state.connection, stream);
        } catch (Throwable failure) {
            closeAfterResponseFailure(stream, failure);
            throw rethrow(failure);
        }
        me.rerere.matrix.license.LicenseResponseException response = null;
        try {
            String errorMessage = parsed.error();
            int responseCode = parsed.code();
            if (equals400(errorMessage,
                    "There is no valid plan for this product, you can buy one at our website", stream)) {
                warning400(logger400(matrix400(stream), stream),
                        "You have not bought a plan, please buy one at https://localhost/offline", stream);
                sleep400(3000L, stream);
                exit400(0, stream);
                throw new RuntimeException("System.exit returned normally, while it was supposed to halt JVM.");
            }
            if (equals400(errorMessage, "Invalid token", stream)) {
                warning400(logger400(matrix400(stream), stream),
                        "Invalid token, please check your config.yml You can get Token at https://localhost/offline", stream);
                sleep400(3000L, stream);
                exit400(0, stream);
                throw new RuntimeException("System.exit returned normally, while it was supposed to halt JVM.");
            }
            if (contains400(errorMessage, " ban ", true, stream)) {
                warning400(loggerBan400(stream), format400(responseCode, errorMessage, stream), stream);
                sleep400(1L, stream);
                halt400(runtimeBan400(stream), 0, stream);
            }
            warning400(logger400(matrix400(stream), stream),
                    format400(responseCode, errorMessage, stream), stream);
            String message = format400(responseCode, errorMessage, stream);
            response = construct400(message, stream);
            closeSignaled400(stream, response);
            throw response;
        } catch (me.rerere.matrix.network.UnsupportedNativePath failure) {
            throw failure;
        } catch (Throwable failure) {
            if (failure instanceof ClosedActionFailure) {
                throw rethrow(((ClosedActionFailure) failure).original);
            }
            if (failure == response) {
                throw rethrow(failure);
            }
            throw new me.rerere.matrix.network.UnsupportedNativePath(
                    "unrecovered HTTP 400 exceptional path", state, failure);
        }
    }
}
