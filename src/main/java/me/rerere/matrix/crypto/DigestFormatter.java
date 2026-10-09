package me.rerere.matrix.crypto;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Shared MD5 digest and byte-to-text helpers used by fingerprint creation. */
public final class DigestFormatter {
    public static final char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    public static MessageDigest DIGEST;

    static {
        try {
            DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException unavailable) {
            unavailable.printStackTrace();
        }
    }

    public DigestFormatter() {
    }

    public static String digestAndHex(byte[] input) {
        return digestBytesAndHex(input);
    }

    private static String digestBytesAndHex(byte[] input) {
        DIGEST.update(input);
        byte[] digest = DIGEST.digest();
        return hex(digest);
    }

    private static void appendHexByte(byte value, StringBuffer output) {
        char leadingDigit = HEX_DIGITS[(value & 15) >> 4];
        char trailingDigit = HEX_DIGITS[value & 15];
        output.append(leadingDigit);
        output.append(trailingDigit);
    }

    private static String encodeBytes(byte[] bytes, int start, int count) {
        int endIndex = start + count;
        StringBuffer output = new StringBuffer(2 * count);
        for (int index = start; index < endIndex; index++) {
            appendHexByte(bytes[index], output);
        }
        return output.toString();
    }

    private static String hex(byte[] bytes) {
        return encodeBytes(bytes, 0, bytes.length);
    }

    public static String hashText(String value) {
        byte[] bytes = value.getBytes(Charset.defaultCharset());
        return digestBytesAndHex(bytes);
    }

}
