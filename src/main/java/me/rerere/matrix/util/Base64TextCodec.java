package me.rerere.matrix.util;

import me.rerere.matrix.thirdparty.kotlin.text.Charsets;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import java.util.Base64;

public final class Base64TextCodec {
    public static final Base64TextCodec INSTANCE = new Base64TextCodec();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static boolean enabled;
    private Base64TextCodec() { }

    public static String decode(String value) {
        return new String(DECODER.decode(value), Charsets.UTF_8);
    }

    public static String encode(String value) {
        byte[] bytes = value.getBytes(Charsets.UTF_8);
        Intrinsics.checkNotNullExpressionValue(bytes, "getBytes(...)");
        return new String(ENCODER.encode(bytes), Charsets.UTF_8);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isDisabled() {
        return !enabled;
    }
}
