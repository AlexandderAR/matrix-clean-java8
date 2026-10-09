package me.rerere.matrix.util;

/** Formats byte sequences as uppercase hexadecimal pairs separated by hyphens. */
public final class MacAddressFormatter {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private MacAddressFormatter() { }

    public static String format(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < bytes.length; index++) {
            if (index != 0) result.append('-');
            int value = bytes[index] & 0xff;
            result.append(HEX[value >>> 4]);
            result.append(HEX[value & 0x0f]);
        }
        return result.toString();
    }
}
