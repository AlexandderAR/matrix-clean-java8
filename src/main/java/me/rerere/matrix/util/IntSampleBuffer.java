package me.rerere.matrix.util;

import java.util.Arrays;

/** Fixed-size integer buffer that shifts its oldest item when full. */
public class IntSampleBuffer {
    private final int[] samples;

    public IntSampleBuffer(int size) {
        if (size <= 1) {
            throw new IllegalArgumentException("size must larger than 1");
        }
        samples = new int[size];
        Arrays.fill(samples, -1);
    }

    public void clear() {
        Arrays.fill(samples, -1);
    }

    public int[] getSamples() {
        return samples;
    }

    public void print() {
        System.out.println(Arrays.toString(samples));
    }

    public boolean isFull() {
        for (int value : samples) {
            if ((float) value == -1.0F) return false;
        }
        return true;
    }

    public void add(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(negativeValueMessage(value));
        }
        if (isFull()) {
            System.arraycopy(samples, 1, samples, 0, samples.length - 1);
            samples[samples.length - 1] = value;
            return;
        }
        for (int index = 0; index < samples.length; index++) {
            if (samples[index] == -1) {
                samples[index] = value;
                break;
            }
        }
    }

    private static String negativeValueMessage(int value) {
        return "can not feed negative data into int buffer: " + value;
    }

    public static void main(String[] arguments) {
        IntSampleBuffer buffer = new IntSampleBuffer(3);
        for (int value = 0; value < 15; value++) {
            buffer.add(value);
            if (buffer.isFull()) buffer.print();
        }
    }
}
