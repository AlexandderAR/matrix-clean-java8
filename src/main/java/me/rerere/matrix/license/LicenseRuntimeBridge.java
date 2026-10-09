package me.rerere.matrix.license;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;
import java.util.logging.Logger;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.AsyncScheduler;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.config.MatrixConfigBridge;
import me.rerere.matrix.util.CallbackMetafactoryBootstrap;
import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.util.Exceptions;


/** License-route callbacks, response text, trial notification, and runtime limit handling. */
public final class LicenseRuntimeBridge {
    private boolean angle;

    public LicenseRuntimeBridge() {}

    private static String sha256Prefix(String value) { return "sha256:" + value; }

    private static String zeroPrefix(String value) { return "0" + value; }

    public void scheduleSelectedCallback(PrefaceRuntimeContext licenseRequest) {
        ClassLoader loader = getClass().getClassLoader();
        if (loader == null) throw new AssertionError("Unmodeled native FatalError: classloader == null");
        if (licenseRequest == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        String userKey = licenseRequest.getUserKey();
        if (StringsKt.isBlank(userKey)) return;

        AsyncScheduler scheduler = FoliaScheduler.getAsyncScheduler();
        MatrixConfigBridge configBridge = Matrix.Companion;
        if (configBridge == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        Matrix plugin = configBridge.matrixInstance();
        MethodHandle callbackFactory = CallbackMetafactoryBootstrap.link(
                MethodHandles.lookup(), loader);
        @SuppressWarnings("unchecked")
        Consumer<Object> callback = (Consumer<Object>) (Consumer<?>)
                CallbackMetafactoryBootstrap.invokeFactory(
                        callbackFactory, this, licenseRequest);
        if (scheduler == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        scheduler.runNow(plugin, callback);
    }

    private static String tooManyRequests(String rayId) {
        return "Too many requests sent to Matrix API. Please contact Dev for more information. (Ray ID: " + rayId + ")";
    }

    private static void runCallback(LicenseRuntimeBridge owner,
            PrefaceRuntimeContext licenseRequest, Object callbackPayload) {
        if (LicenseRuntimeBridge.class.getClassLoader() == null) {
            throw new AssertionError("Unmodeled native FatalError: classloader == null");
        }
        try {
            LicenseInitializationCallback.initializeLicense(owner, licenseRequest,
                    (callbackOwner, value) -> ((LicenseRuntimeBridge) callbackOwner)
                            .continueSelectedRoute((PrefaceRuntimeContext) value));
        } catch (InterruptedException failure) {
            throw Exceptions.propagate(failure);
        }
    }

    private static String teapotResponse(String rayId) {
        return "Sorry, I'm a teapot, please try again later. (Ray ID: " + rayId + ")";
    }

    private static String jsonObject(String contents) { return "{" + contents + "}"; }

    private static String statusPrefix(int status, String message) {
        return "[" + status + "] " + message;
    }

    private static String unexpectedHttpsResponse(int status, String rayId) {
        return "Unexpected HTTPS response " + status + " (Ray ID: " + rayId + ")";
    }

    private static void warnTrialLicense(Object ignored) {
        MatrixConfigBridge configBridge = Matrix.Companion;
        if (configBridge == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        Matrix plugin = configBridge.matrixInstance();
        if (plugin == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        Logger logger = plugin.getLogger();
        if (logger == null) throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
        logger.warning("You are using trial version of Matrix AntiCheat, please considering buying a license to get full experience.");
    }

    private static String invalidKey(String key) { return "Invalid key: " + key; }

    private static String productPrefix(String suffix) { return "Matrix AntiCheat " + suffix; }

    private static String concatenate(String first, String second) {
        return new StringBuilder(first).append(second).toString();
    }

    private static String concatenateEight(String a, String b, String c, String d,
            String e, String f, String g, String h) {
        return new StringBuilder(a).append(b).append(c).append(d).append(e)
                .append(f).append(g).append(h).toString();
    }

    private static String wafBlocked(String rayId) {
        return "You have been blocked from accessing Matrix API by WAF. Please contact Dev for more information. (Ray ID: " + rayId + ")";
    }

    private static void enforce24HourLimit(Object ignored) {
        MatrixConfigBridge configBridge = Matrix.Companion;
        if (configBridge == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        Matrix plugin = configBridge.matrixInstance();
        if (plugin == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        Logger logger = plugin.getLogger();
        if (logger == null) throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
        logger.warning("Server will shut down due to the 24-hour limit.");
        org.bukkit.Bukkit.shutdown();
        System.exit(1);
        throw new RuntimeException("System.exit returned normally, while it was supposed to halt JVM.");
    }

    private static String appendStatus(String prefix, int status, String detail, String suffix) {
        return new StringBuilder(prefix).append(status).append(detail).append(suffix).toString();
    }

    private void continueSelectedRoute(PrefaceRuntimeContext request) {
        LicenseRouteEntry.continueSelected(this, request);
    }

    public static void __ntjSetAngle(LicenseRuntimeBridge instance, boolean enabled) {
        instance.angle = enabled;
    }

    public static boolean __ntjGetAngle(LicenseRuntimeBridge instance) {
        return instance.angle;
    }

    public static MethodHandles.Lookup __ntjTrialLookup() { return MethodHandles.lookup(); }

    static {
        if (LicenseRuntimeBridge.class.getClassLoader() == null) {
            throw new IllegalStateException("classloader == null; native FatalError path unsupported");
        }
    }
}
