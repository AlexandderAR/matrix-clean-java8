package me.rerere.matrix.util;

import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Captured Kotlin-style function that formats a byte as two lowercase hex digits. */
public final class ByteHexFormatter implements Function1<Number, CharSequence> {
    public static final ByteHexFormatter INSTANCE;

    static {
        INSTANCE = new ByteHexFormatter();
    }

    public ByteHexFormatter() { super(); }

    public final CharSequence formatByte(byte value) {
        String formatted = String.format("%02x",
                new Object[] { Byte.valueOf(value) });
        Intrinsics.checkNotNullExpressionValue(formatted, "format(...)");
        return formatted;
    }

    @Override
    public CharSequence invoke(Number argument) {
        return formatByte(argument.byteValue());
    }
}


