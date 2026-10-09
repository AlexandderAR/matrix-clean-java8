package me.rerere.matrix.license;

import java.util.HashMap;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.TuplesKt;
import me.rerere.matrix.thirdparty.kotlin.collections.MapsKt;
import me.rerere.matrix.compat.SystemPropertyAccess;

public final class LicenseRequestProperties {
    private LicenseRequestProperties() {
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, Object> build(String ping, Object token, Object hardwareId) {
        Pair<String, Object>[] pairs = new Pair[11];
        pairs[0] = TuplesKt.to("ping", ping);
        pairs[1] = TuplesKt.to("token", token);
        pairs[2] = TuplesKt.to("product", Integer.valueOf(1));
        pairs[3] = TuplesKt.to("hwid", hardwareId);
        pairs[4] = TuplesKt.to("osName", SystemPropertyAccess.getProperty("os.name"));
        pairs[5] = TuplesKt.to("osVersion", SystemPropertyAccess.getProperty("os.version"));
        pairs[6] = TuplesKt.to("osArch", SystemPropertyAccess.getProperty("os.arch"));
        pairs[7] = TuplesKt.to("javaVersion", SystemPropertyAccess.getProperty("java.version"));
        pairs[8] = TuplesKt.to("javaVendor", SystemPropertyAccess.getProperty("java.vendor"));
        pairs[9] = TuplesKt.to("userName", SystemPropertyAccess.getProperty("user.name"));
        pairs[10] = TuplesKt.to("userHome", SystemPropertyAccess.getProperty("user.home"));
        return MapsKt.hashMapOf(pairs);
    }
}
