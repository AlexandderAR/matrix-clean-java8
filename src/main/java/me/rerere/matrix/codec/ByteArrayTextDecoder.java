package me.rerere.matrix.codec;

/** Decodes the byte-to-char encoding implemented by the original loop. */
public final class ByteArrayTextDecoder {
    public static final ByteArrayTextDecoder INSTANCE = new ByteArrayTextDecoder();

    private ByteArrayTextDecoder() { }

    /**
     * Reconstructs the exact byte stepping: 1-byte values below 0xC0, 2-byte
     * values below 0xE0, and 3-byte values only when two following bytes exist.
     * Continuation prefixes are masked but not validated by the bytecode.
     */
    private static String decode(byte[] bytes) {
        int decodedCharacterCount = 0;
        int length = bytes.length;
        char[] output = new char[length];
        for (int i = 0; i < length; i++) {
            int value = bytes[i] & 0xff;
            if (value < 0xc0) {
                output[decodedCharacterCount++] = (char) value;
            } else if (value < 0xe0) {
                int decoded = (value & 0x1f) << 6;
                decoded |= bytes[++i] & 0x3f;
                output[decodedCharacterCount++] = (char) decoded;
            } else if (i < length - 2) {
                int decoded = (value & 0x0f) << 12;
                decoded |= (bytes[++i] & 0x3f) << 6;
                decoded |= bytes[++i] & 0x3f;
                output[decodedCharacterCount++] = (char) decoded;
            }
        }
        return new String(output, 0, decodedCharacterCount);
    }
}
