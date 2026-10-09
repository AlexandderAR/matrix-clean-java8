package me.rerere.matrix.util;

import java.util.Map;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.network.HttpResponseDiagnostics;

/** Formats map entries into the quoted representation used by Matrix reports. */
public final class EntryFormatter implements Function1<Map.Entry<? extends String, ? extends Object>, CharSequence> {
    private static final String KEY_VALUE_SEPARATOR;
    public static final EntryFormatter INSTANCE;

    static {
        KEY_VALUE_SEPARATOR = "\":".intern();
        INSTANCE = new EntryFormatter();
    }

    public EntryFormatter() {}

    @Override
    public CharSequence invoke(Map.Entry<? extends String, ? extends Object> entry) {
        return formatEntry(entry);
    }

    public CharSequence formatEntry(Map.Entry<? extends String, ? extends Object> entry) {
        String key = (String) entry.getKey();
        Object value = entry.getValue();
        String rendered;
        if (value instanceof String) {
            rendered = quoteValue(HttpResponseDiagnostics.escapeJsonString((String) value));
        } else if (value instanceof Number) {
            rendered = value.toString();
        } else {
            rendered = HttpResponseDiagnostics.escapeJsonString(value.toString());
        }
        return quoteKeyValue(key, rendered);
    }

    private static String quoteKeyValue(String key, String value) {
        return "\"" + key + KEY_VALUE_SEPARATOR + value;
    }

    private static String quoteValue(String value) {
        return "\"" + value + "\"";
    }
}
