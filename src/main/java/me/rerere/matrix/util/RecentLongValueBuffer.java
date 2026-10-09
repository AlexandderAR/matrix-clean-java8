package me.rerere.matrix.util;

import java.util.Arrays;

/** A sentinel-filled bounded history of long values. */
public final class RecentLongValueBuffer {
    private final long[] values;

    public RecentLongValueBuffer(int capacity) {
        if (capacity <= 1) throw new IllegalArgumentException("size must larger than 1");
        values = new long[capacity];
        Arrays.fill(values, -1L);
    }

    public void printContents() { System.out.println(Arrays.toString(values)); }

    public boolean isFull() {
        for (long value : values) {
            if ((float) value == -1.0F) return false;
        }
        return true;
    }

    public void addValue(long value) {
        if (value < 0L) {
            throw new IllegalArgumentException(negativeValueMessage(value));
        }
        if (isFull()) {
            System.arraycopy(values, 1, values, 0, values.length - 1);
            values[values.length - 1] = value;
            return;
        }
        for (int index = 0; index < values.length; index++) {
            if (values[index] == -1L) {
                values[index] = value;
                break;
            }
        }
    }

    public static void main(String[] ignored) {
        RecentLongValueBuffer buffer = new RecentLongValueBuffer(3);
        int item = 0;
        while (item < 15) {
            buffer.addValue(item);
            if (buffer.isFull()) buffer.printContents();
            item++;
        }
        buffer.addValue(Long.MAX_VALUE);
        buffer.printContents();
    }

    private static String negativeValueMessage(long value) {
        return "can not feed negative data into int buffer: " + value;
    }

    public long[] values() { return values; }

}
