package me.rerere.matrix.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CommonLookup {
    private static final Map<String, me.rerere.matrix.features.FeatureRegistry> entries;

    static {
        Map<String, me.rerere.matrix.features.FeatureRegistry> featureEntries = new LinkedHashMap<>();
        featureEntries.put("common", me.rerere.matrix.features.FeatureRegistry.INSTANCE);
        entries = featureEntries;
    }

    private CommonLookup() {}

    public static Map<String, me.rerere.matrix.features.FeatureRegistry> entries() {
        return entries;
    }
}
