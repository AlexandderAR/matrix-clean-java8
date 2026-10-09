package me.rerere.matrix.entity;

import java.util.Arrays;
import me.rerere.matrix.geometry.AxisAlignedBox;

/** A fixed-size float sample window used by movement checks. */
public final class FloatSampleWindow {
    private static final float EMPTY_SAMPLE = -0x1.ca0e56p6F;
    private static final int DEMO_SAMPLE_COUNT = 15;

    private final float[] samples;

    public FloatSampleWindow(int capacity) {
        this(capacity, AxisAlignedBox.isMode());
    }

    private FloatSampleWindow(int capacity, boolean compatibilityMode) {
        if (compatibilityMode) {
            samples = null;
            return;
        }
        if (capacity <= 1) {
            throw new IllegalArgumentException("size must larger than 1");
        }
        samples = new float[capacity];
        Arrays.fill(samples, EMPTY_SAMPLE);
    }

    public boolean isFull() {
        return areSamplesFull();
    }

    public float[] values() {
        return samples;
    }

    public float mean() {
        return calculateMean();
    }

    /** Returns the sum of squared deviations, matching the original implementation. */
    public float variance() {
        return calculateVariance();
    }

    public float standardDeviation() {
        return (float) Math.sqrt(calculateVariance());
    }

    public void reset() {
        Arrays.fill(samples, EMPTY_SAMPLE);
    }

    public void add(float sample) {
        addSample(sample);
    }

    private boolean areSamplesFull() {
        for (float sample : samples) {
            if (Float.compare(sample, EMPTY_SAMPLE) == 0) {
                return false;
            }
        }
        return true;
    }

    private float calculateMean() {
        float sum = 0.0F;
        for (float sample : samples) {
            sum += sample;
        }
        return sum / samples.length;
    }

    private float calculateVariance() {
        float mean = calculateMean();
        float total = 0.0F;
        for (float sample : samples) {
            double squaredDifference = Math.pow(sample - mean, 2.0D);
            total = (float) ((double) total + squaredDifference);
        }
        return total;
    }

    private void addSample(float sample) {
        if (Float.compare(sample, EMPTY_SAMPLE) == 0) {
            System.out.println("[!] FloatBuffer crashed~");
            return;
        }
        if (areSamplesFull()) {
            rotateLeft();
            samples[samples.length - 1] = sample;
            return;
        }
        for (int index = 0; index < samples.length; index++) {
            if (Float.compare(samples[index], EMPTY_SAMPLE) == 0) {
                samples[index] = sample;
                return;
            }
        }
    }

    private void rotateLeft() {
        System.arraycopy(samples, 1, samples, 0, samples.length - 1);
    }

    public static void main(String[] ignored) {
        FloatSampleWindow window = new FloatSampleWindow(4);
        for (int index = 0; index < DEMO_SAMPLE_COUNT; index++) {
            window.add((float) index);
            window.printSamples();
        }
    }

    public void printSamples() {
        System.out.println(Arrays.toString(samples));
    }
}
