package me.rerere.matrix.network;

import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.crypto.EmbeddedAuthDecoder;

/** Decodes the three fields carried in the connection preface. */
public final class PrefaceFieldDecoder {
    private static final byte[] KEY1 = new byte[] {52, 97, 54, 99, 109, 109, 55, 83, 75, 82, 108, 51, 83, 106, 81, 118, 69, 54, 51, 48};
    private static final byte[] KEY2 = new byte[] {121, 48, 119, 76, 116, 55, 48, 101, 50, 77, 88, 53, 72, 86, 50, 89, 90, 51, 83, 77};
    private PrefaceFieldDecoder() { }

    public static Fields afterRuntimeGuards(PrefaceRuntimeContext guarded) {
        if (guarded == null) throw new UnresolvedNativePath("null ur outside selected JNI path");
        boolean allTemplate = "%%__USER_KEY__%%".equals(guarded.getUserKey())
                && "%%__PLUGINCLOUD__%%".equals(guarded.getPluginCloud())
                && "%%__TOKEN__%%".equals(guarded.getToken())
                && "%%__TIMESTAMP__%%".equals(guarded.getTimestamp());
        if (allTemplate) throw new UnresolvedNativePath("all-template native branch at 0xd724b");
        return new Fields(
                decodeNth(guarded.getUserKey(), 0),
                decodeNth(guarded.getUserKey(), 1),
                decodeNth(guarded.getUserKey(), 2));
    }

    public static Fields fromEncodedTriple(String first, String second, String third) {
        return new Fields(decodeNth(first, 0), decodeNth(second, 1), decodeNth(third, 2));
    }

    private static String decodeNth(String encoded, int index) {
        if (encoded == null) throw new UnresolvedNativePath("null encoded input outside selected decoder path");
        byte[] decoded = EmbeddedAuthDecoder.decode(encoded, KEY1, KEY2, 5556565728L, 0);
        return selectDecoded(me.rerere.matrix.thirdparty.kotlin.text.StringsKt.decodeToString(decoded), index);
    }

    public static String selectDecoded(String decoded, int index) {
        if (decoded == null || index < 0 || index > 2) throw new UnresolvedNativePath("outside selected split/index contract");
        java.util.List<String> nonEmptySegments = new java.util.ArrayList<>();
        for (String part : me.rerere.matrix.thirdparty.kotlin.text.StringsKt.split(decoded, new String[] {"#"}, false, 0)) {
            if (!part.isEmpty()) nonEmptySegments.add(part);
        }
        return nonEmptySegments.size() <= index ? "" : nonEmptySegments.get(index);
    }

    public static final class Fields {
        public final String index0;
        public final String index1;
        public final String index2;
        private Fields(String index0, String index1, String index2) {
            this.index0 = index0; this.index1 = index1; this.index2 = index2;
        }
    }

    public static final class UnresolvedNativePath extends Error {
        public UnresolvedNativePath(String message) { super(message); }
    }
}

