package me.rerere.matrix.network;

import java.io.File;
import java.util.logging.Logger;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.config.MatrixConfigBridge;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$LongRef;

/** String helpers and initialization diagnostics used by Matrix's HTTPS client. */
public final class HttpResponseDiagnostics {
    private HttpResponseDiagnostics() {}

    public static String escapeJsonString(String value) {
        return escapeJsonStringContent(value);
    }

    /** Escapes JSON string contents (the caller supplies the surrounding quotes). */
    public static String escapeJsonStringContent(String value) {
        StringBuilder escaped = new StringBuilder();
        if (value == null) {
            throw new NullPointerException("\"INVOKEVIRTUAL Int npe\" on -1");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\b': escaped.append("\\b"); break;
                case '\t': escaped.append("\\t"); break;
                case '\n': escaped.append("\\n"); break;
                case '\f': escaped.append("\\f"); break;
                case '\r': escaped.append("\\r"); break;
                case '"': escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                default:
                    if (Character.isISOControl(character)) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.toString();
    }

    private static String statusPrefix(int status, String message) {
        return "[" + status + "] " + message;
    }

    private static String concatenateEight(String a, String b, String c, String d,
            String e, String f, String g, String h) {
        return new StringBuilder(a).append(b).append(c).append(d)
                .append(e).append(f).append(g).append(h).toString();
    }

    private static String wafBlocked(String rayId) {
        return "You have been blocked from accessing Matrix API by WAF. Please contact Dev for more information. (Ray ID: " + rayId + ")";
    }

    private static String productPrefix(String suffix) {
        return "Matrix AntiCheat " + suffix;
    }

    public static long advanceLongRef(Ref$LongRef state) {
        return advanceLongRefState(state);
    }

    private static long advanceLongRefState(Ref$LongRef state) {
        if (state == null) throw new NullPointerException("\"GETFIELD Long npe\" on -1");
        state.element = state.element ^ (state.element >>> 12);
        state.element = state.element ^ (state.element << 25);
        state.element = state.element ^ (state.element >>> 27);
        return state.element * 2685821657736338717L;
    }

    private static String sha256Prefix(String digest) {
        return "sha256:" + digest;
    }

    private static String rateLimited(String rayId) {
        return "Too many requests sent to Matrix API. Please contact Dev for more information. (Ray ID: " + rayId + ")";
    }

    public static void logInitialized() {
        MatrixConfigBridge holder = Matrix.Companion;
        if (holder == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        Matrix plugin = holder.matrixInstance();
        if (plugin == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        Logger logger = plugin.getLogger();
        if (logger == null) throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
        logger.info("Matrix AntiCheat has been fully initialized.");

        holder = Matrix.Companion;
        if (holder == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        plugin = holder.matrixInstance();
        if (plugin == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        File dataFolder = plugin.getDataFolder();
    }

    public static Object dispatchHttpEntry(String method, String requestUrl, String payload,
            Function1<?, ?> onSuccess, Function1<?, ?> onFailure, int flags, Object marker) {
        return HttpResponseBoundary.request(
                method, requestUrl, payload, onSuccess, onFailure, flags, marker);
    }

    private static String unexpectedHttpsResponse(int status, String rayId) {
        return "Unexpected HTTPS response " + status + " (Ray ID: " + rayId + ")";
    }

    private static String teapotResponse(String rayId) {
        return "Sorry, I'm a teapot, please try again later. (Ray ID: " + rayId + ")";
    }

    static {
        if (HttpResponseDiagnostics.class.getClassLoader() == null) {
            throw new IllegalStateException("classloader == null; native FatalError path unsupported");
        }
    }
}
