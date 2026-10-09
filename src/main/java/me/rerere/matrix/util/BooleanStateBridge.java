package me.rerere.matrix.util;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.updates.MatrixVersionCheck;

/** Static state bridge for update-check state. */
public final class BooleanStateBridge {
    private static String stringValue;
    private static boolean enabled;
    public static final MatrixVersionCheck angle;
    private static int[] flags;

    public BooleanStateBridge() {
    }

    public static final void updateSingleton() {
        angle.checkForUpdates();
    }

    public static final String getString() {
        return stringValue;
    }

    public static final void setEnabled(boolean value) {
        enabled = value;
    }

    public static final void setString(String value) {
        stringValue = value;
    }

    public static final boolean isEnabled() {
        return enabled;
    }

    public static void setFlags(int[] values) {
        flags = values;
    }

    public static int[] getFlags() {
        return flags;
    }

    private static String decodeLegacyUtf8(byte[] bytes) {
        int outputIndex = 0;
        int length = bytes.length;
        char[] decoded = new char[length];
        int inputIndex = 0;
        while (inputIndex < length) {
            int first = bytes[inputIndex] & 0xff;
            if (first < 0xc0) {
                decoded[outputIndex++] = (char) first;
            } else if (first < 0xe0) {
                int value = (first & 0x1f) << 6;
                int continuation = bytes[++inputIndex];
                value |= continuation & 0x3f;
                decoded[outputIndex++] = (char) value;
            } else if (inputIndex < length - 2) {
                int value = (first & 0x0f) << 12;
                int continuation = bytes[++inputIndex];
                value = (value | ((continuation & 0x3f) << 6)) & 0xffff;
                continuation = bytes[++inputIndex];
                value = (value | (continuation & 0x3f)) & 0xffff;
                decoded[outputIndex++] = (char) value;
            }
            inputIndex++;
        }
        return new String(decoded, 0, outputIndex);
    }

    static {
        if (getFlags() != null) {
            setFlags(new int[5]);
        }
        String initialValue = "ERROR".intern();
        angle = new MatrixVersionCheck((DefaultConstructorMarker) null);
        stringValue = initialValue;
    }
}
