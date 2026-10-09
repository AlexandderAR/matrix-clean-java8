package me.rerere.matrix.util;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ObjectSampleBuffer {
    private Object[] samples;
    public ObjectSampleBuffer(Class<?> elementType, int size) {
        if (size <= 1) throw new IllegalArgumentException("size must larger than 1");
        samples = (Object[]) Array.newInstance(elementType, size);
    }
    public Object[] getSamples() { return samples; }
    public void clear() { Arrays.fill(samples, null); }
    public void print() { System.out.println(Arrays.toString(samples)); }
    public boolean isFull() {
        for (int index = 0; index < samples.length; index++) {
            if (samples[index] == null) return false;
        }
        return true;
    }
    public void add(Object sample) {
        if (sample == null) throw new IllegalArgumentException("can not feed null data");
        boolean full = isFull();
        if (full) {
            System.arraycopy(samples, 1, samples, 0, samples.length - 1);
            samples[samples.length - 1] = sample;
            return;
        }
        for (int index = 0; index < samples.length; index++) {
            if (samples[index] == null) { samples[index] = sample; break; }
        }
    }
    public static void main(String[] arguments) {
        ObjectSampleBuffer buffer = new ObjectSampleBuffer(Integer.class, 5);
        for (int value = 0; value < 20; value++) {
            buffer.add(value);
            if (buffer.isFull()) buffer.print();
        }
    }
}
