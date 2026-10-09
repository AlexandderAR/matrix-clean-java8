package me.rerere.matrix.license;

import me.rerere.matrix.network.ResponseCleanup;
import me.rerere.matrix.network.TrialExceptionScope;

public final class LicenseUnresolvedPathClassifier implements ResponseCleanup.RecoveryBoundary {
    public LicenseUnresolvedPathClassifier() {
    }

    @Override
    public boolean isUnresolved(Throwable failure) {
        return failure instanceof TrialExceptionScope
                || failure instanceof EarlyPingResponseReader.UnresolvedBeforeSavedStream
                || failure instanceof LicenseResponseParser.UnsupportedLicenseResponsePath
                || failure instanceof me.rerere.matrix.security.UnsupportedNativePath
                || failure instanceof me.rerere.matrix.errors.UnsupportedNativePath;
    }
}
