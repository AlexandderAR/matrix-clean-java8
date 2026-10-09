package me.rerere.matrix.network;

import me.rerere.matrix.thirdparty.okhttp3.MediaType;

/** Shared HTTP API settings and JSON media type. */
public final class HttpApiConfiguration {
    private static final String API_URL;
    private static final String PUBLIC_KEY;
    private static final MediaType JSON_MEDIA_TYPE;
    private static boolean configurationFlag;

    private HttpApiConfiguration() {
    }

    public static final MediaType mediaType() {
        return JSON_MEDIA_TYPE;
    }

    public static void setConfigurationFlag(boolean configured) {
        configurationFlag = configured;
    }

    public static boolean getConfigurationFlag() {
        return configurationFlag;
    }

    public static boolean isConfigurationFlagUnset() {
        return !getConfigurationFlag();
    }

    static {
        configurationFlag = true;
        API_URL = "https://localhost/offline";
        PUBLIC_KEY = "offline-mode-no-key-required";
        JSON_MEDIA_TYPE = MediaType.Companion.get("application/json");
    }
}
