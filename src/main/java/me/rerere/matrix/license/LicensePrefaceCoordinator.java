package me.rerere.matrix.license;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import me.rerere.matrix.util.Exceptions;

/** Coordinates the exact preface gates and prepared request construction. */
public final class LicensePrefaceCoordinator {
    private LicensePrefaceCoordinator() {}

    public static boolean allTemplatePlaceholders(me.rerere.matrix.network.TemplateExitMetadata metadata) {
        return me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(
                        metadata.userKey(), "%%__USER_KEY__%%")
                && me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(
                        metadata.pluginCloud(), "%%__PLUGINCLOUD__%%")
                && me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(
                        metadata.token(), "%%__TOKEN__%%")
                && me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(
                        timestampOrRethrow(metadata), "%%__TIMESTAMP__%%");
    }

    private static String timestampOrRethrow(
            me.rerere.matrix.network.TemplateExitMetadata metadata) {
        try {
            return metadata.timestamp();
        } catch (Throwable failure) {
            throw Exceptions.propagate(failure);
        }
    }

    public static me.rerere.matrix.crypto.PreparedNormalEntry.Prepared prepareAfterNativePrologue(
            me.rerere.matrix.license.LicenseRuntimeBridge receiver,
            me.rerere.matrix.config.PrefaceRuntimeContext input) {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        me.rerere.matrix.runtime.JvmArgumentMatch match = me.rerere.matrix.runtime.JvmArgumentScanner
                .scan(runtime);
        if (match != me.rerere.matrix.runtime.JvmArgumentMatch.NONE) {
            me.rerere.matrix.security.JvmArgumentRefusal.reject(match);
        }
        me.rerere.matrix.security.SecurityProviderScanner.scanSystemProviders();
        if (input == null) {
            throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        }

        if (allTemplatePlaceholders(new me.rerere.matrix.license.TemplateExitInput(input))) {
            me.rerere.matrix.plugin.MatrixPluginAccessor.getMatrix();
            String token = me.rerere.matrix.config.MatrixConfiguration.accessToken;
            return prepareWithSavedSelector(token, receiver);
        }

        me.rerere.matrix.crypto.AuthenticatedPrefaceDecoder.Fields fields = me.rerere.matrix.crypto.AuthenticatedPrefaceDecoder
                .decode(input);
        String token = me.rerere.matrix.crypto.EncodedTokenSelector.fromInput(input);
        me.rerere.matrix.license.TokenVerificationInput.verifySelected(
                input, fields.index1, fields.index0,
                fields.index2, token);
        return prepareWithSavedSelector(token, receiver);
    }

    public static me.rerere.matrix.crypto.PreparedNormalEntry.Prepared prepareWithSavedSelector(
            String token, me.rerere.matrix.license.LicenseRuntimeBridge receiver) {
        try {
            return (me.rerere.matrix.crypto.PreparedNormalEntry.Prepared)
                    me.rerere.matrix.network.ProxySelectorScope.run(
                            new me.rerere.matrix.license.ProxyPreparationActions(token, receiver));
        } catch (Throwable failure) {
            unchecked(failure);
            throw new AssertionError();
        }
    }

    private static void unchecked(Throwable failure) {
        LicensePrefaceCoordinator.<RuntimeException>sneakyThrow(failure);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable failure) throws T {
        throw (T) failure;
    }
}
