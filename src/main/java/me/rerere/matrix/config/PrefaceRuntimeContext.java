package me.rerere.matrix.config;

import me.rerere.matrix.Matrix;

public final class PrefaceRuntimeContext {
    private final String token;
    private final String accessToken;
    private final String userKey;
    private final String displayLabel;
    private final String timestamp;
    private final String buildNumber;
    private final String pluginCloud;
    private static Matrix[] matrixInstances;

    public PrefaceRuntimeContext(String token, String accessToken, String userKey,
                                 String displayLabel, String timestamp,
                                 String buildNumber, String pluginCloud) {
        this.token = token;
        this.accessToken = accessToken;
        this.userKey = userKey;
        this.displayLabel = displayLabel;
        this.timestamp = timestamp;
        this.buildNumber = buildNumber;
        this.pluginCloud = pluginCloud;
    }

    public String getToken() { return token; }
    public String getAccessToken() { return accessToken; }
    public String getUserKey() { return userKey; }
    public String getDisplayLabel() { return displayLabel; }
    public String getTimestamp() { return timestamp; }
    public String getBuildNumber() { return buildNumber; }
    public String getPluginCloud() { return pluginCloud; }

    public static void setMatrixInstances(Matrix[] values) { matrixInstances = values; }
    public static Matrix[] getMatrixInstances() { return matrixInstances; }

    static {
        if (getMatrixInstances() == null) {
            setMatrixInstances(new Matrix[2]);
        }
    }
}
