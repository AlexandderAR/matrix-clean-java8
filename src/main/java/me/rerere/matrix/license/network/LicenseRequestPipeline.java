package me.rerere.matrix.license.network;

import java.net.ProxySelector;
import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.crypto.PreparedNormalEntry;
import me.rerere.matrix.license.LicensePrefaceCoordinator;
import me.rerere.matrix.license.LicenseRequestResponseScope;
import me.rerere.matrix.license.LicenseRuntimeBridge;
import me.rerere.matrix.license.CertificateChainVerifier;
import me.rerere.matrix.license.LicenseRequestTransport;
import me.rerere.matrix.network.UrlMismatchRefusal;
import me.rerere.matrix.util.jvm.UnsafeMemoryRefusal;
import me.rerere.matrix.crypto.UnresolvedNativePath;
import me.rerere.matrix.crypto.NativeTlsFailureHandler;
import me.rerere.matrix.network.ResponseCleanup;
import me.rerere.matrix.util.Exceptions;

/** Selected normal request route after native entry preparation. */
public final class LicenseRequestPipeline {
    private LicenseRequestPipeline() { }

    public static void continueAfterNativePrologue(LicenseRuntimeBridge receiver, PrefaceRuntimeContext input) {
        PreparedNormalEntry.Prepared prepared = LicensePrefaceCoordinator.prepareAfterNativePrologue(receiver, input);
        LicenseRequestActions actions = new LicenseRequestActions(prepared);
        ProxySelector previous = prepared.previousSelector;
        ResponseCleanup.RecoveryBoundary boundary = LicenseRequestPipeline::isUnresolved;
        try {
            LicenseRequestResponseScope.run(actions, previous, boundary,
                    new LicenseRequestResponseScope.ProductionFailures());
        } catch (Throwable failure) {
            throw Exceptions.propagate(failure);
        }
    }

    private static boolean isUnresolved(Throwable failure) {
        return failure instanceof LicenseRequestTransport.UnsupportedNativePath
                || failure instanceof UrlMismatchRefusal.UnresolvedNativePath
                || failure instanceof UnsafeMemoryRefusal.UnresolvedNativePath
                || failure instanceof UnresolvedNativePath
                || failure instanceof NativeTlsFailureHandler.UnresolvedNativePath
                || failure instanceof CertificateChainVerifier.UnsupportedNativePath;
    }
}
