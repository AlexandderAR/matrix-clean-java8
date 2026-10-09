package me.rerere.matrix.crypto;

/** Encodes and decodes text with the shared Base64 text codec. */
public final class KeyedStringCodec {
    private KeyedStringCodec() {}

    public static String encode(String plainText) {
        return me.rerere.matrix.util.Base64TextCodec.encode(plainText);
    }

    public static String decode(String encodedText) {
        return me.rerere.matrix.util.Base64TextCodec.decode(encodedText);
    }
}
