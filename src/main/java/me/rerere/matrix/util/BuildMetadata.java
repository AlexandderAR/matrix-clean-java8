package me.rerere.matrix.util;

import me.rerere.matrix.license.LicenseMetadataProvider;

/** Build and runtime metadata constants exposed by the original class. */
public final class BuildMetadata {
    private static final String DISPLAY_LABEL;
    private static final String OPAQUE_IDENTIFIER;
    private static final String REVISION_MARKER;
    private static final String BUILD_NUMBER;
    private static final String ENABLED_FLAG;
    private static final String BUILD_VERSION;
    public static final LicenseMetadataProvider LICENSE_METADATA;

    static {
        LICENSE_METADATA = new LicenseMetadataProvider();
        BUILD_NUMBER = "0";
        REVISION_MARKER = "0";
        DISPLAY_LABEL = "offline";
        OPAQUE_IDENTIFIER = "offlineEnterprisePlanLocalToken";
        ENABLED_FLAG = "true";
        BUILD_VERSION = "7.25.1";
    }

    public BuildMetadata() { }

    public static String displayLabel() {
        return DISPLAY_LABEL;
    }

    public static String enabledFlag() {
        return ENABLED_FLAG;
    }

    public static String buildNumber() {
        return BUILD_NUMBER;
    }

    public static String revisionMarker() {
        return REVISION_MARKER;
    }

    public static String buildVersion() {
        return BUILD_VERSION;
    }

    public static String opaqueIdentifier() {
        return OPAQUE_IDENTIFIER;
    }

}
