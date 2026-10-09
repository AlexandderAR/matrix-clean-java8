package me.rerere.matrix.license;

/** Supplies embedded build metadata and the configured authorization token. */
public final class LicenseMetadataProvider {
    private static final String TOKEN_SENTINEL = "%%__TOKEN__%%";

    public LicenseMetadataProvider() { }

    public String getEnabledFlag() {
        return me.rerere.matrix.util.BuildMetadata.enabledFlag();
    }

    public String getDisplayLabel() {
        return me.rerere.matrix.util.BuildMetadata.displayLabel();
    }

    public String getRevisionMarker() {
        return me.rerere.matrix.util.BuildMetadata.revisionMarker();
    }

    public String getBuildNumber() {
        return me.rerere.matrix.util.BuildMetadata.buildNumber();
    }

    public String getEmbeddedToken() {
        return me.rerere.matrix.util.BuildMetadata.opaqueIdentifier();
    }

    public String getBuildVersion() {
        return me.rerere.matrix.util.BuildMetadata.buildVersion();
    }

    public String getAuthorizationToken() {
        String value = getEmbeddedToken();
        if (java.util.Objects.equals(value, TOKEN_SENTINEL)) {
            return me.rerere.matrix.config.MatrixConfiguration.accessToken;
        }
        return value;
    }

}
