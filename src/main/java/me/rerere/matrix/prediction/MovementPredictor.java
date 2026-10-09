package me.rerere.matrix.prediction;

import me.rerere.matrix.thirdparty.kotlin.collections.ArrayDeque;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.util.cache.CachePolicy;
import me.rerere.matrix.checks.movement.MovementStep;
import me.rerere.matrix.util.FixedPointGeometry;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;

/** Rolling prediction state for a player's moving collision bounds. */
public final class MovementPredictor {
    private AxisAlignedBox recentBounds;
    private AxisAlignedBox latestBounds;
    private AxisAlignedBox minimumBounds;
    private AxisAlignedBox centeredBounds;
    private final ArrayDeque<MovementStep> samples;
    private float accumulatedSampleAge;
    private float sampleWeight;
    private int elapsedTicks;
    private float verticalScale;
    private final CachePolicy capacityMode;
    private final int capacity;
    private float horizontalScale;
    private float normalization;
    private static final double SAMPLE_WINDOW_MILLIS = 10000.0;
    private static final FixedPointGeometry INTERPOLATION = new FixedPointGeometry(null);

    public MovementPredictor(AxisAlignedBox bounds, CachePolicy mode,
                             int capacity, float verticalScale, float horizontalScale) {
        this.recentBounds = bounds;
        this.capacityMode = mode;
        this.capacity = capacity;
        this.verticalScale = verticalScale;
        this.horizontalScale = horizontalScale;
        this.latestBounds = bounds;
        this.centeredBounds = new AxisAlignedBox(
                -(bounds.maxX() - bounds.minX()) / 2.0,
                0.0,
                -(bounds.maxZ() - bounds.minZ()) / 2.0,
                (bounds.maxX() - bounds.minX()) / 2.0,
                bounds.maxY() - bounds.minY(),
                (bounds.maxZ() - bounds.minZ()) / 2.0);
        this.minimumBounds = bounds;
        this.samples = new ArrayDeque<>();
        this.normalization = 1.0F;
    }

    public MovementPredictor(AxisAlignedBox bounds, CachePolicy mode,
                             int capacity, float verticalScale, float horizontalScale,
                             int defaultMask,
                             me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker ignored) {
        this(bounds, mode, capacity,
                (defaultMask & 8) != 0 ? 0.0F : verticalScale,
                (defaultMask & 16) != 0 ? 0.0F : horizontalScale);
    }

    /** Shift an AABB and combine it with its original bounds, matching the predictor's helper. */
    public static AxisAlignedBox translatedBounds(double dx, double dy, double dz,
                                                     AxisAlignedBox bounds) {
        AxisAlignedBox moved = new AxisAlignedBox(
                bounds.minX() + dx,
                bounds.minY() + dy,
                bounds.minZ() + dz,
                bounds.maxX() + dx,
                bounds.maxY() + dy,
                bounds.maxZ() + dz);
        return bounds.union(moved);
    }

    @NotNull
    public AxisAlignedBox getRecentBounds() { return recentBounds; }

    public void setBounds(AxisAlignedBox bounds) { this.recentBounds = bounds; }

    public MovementPredictor copy() {
        MovementPredictor copy = new MovementPredictor(recentBounds, capacityMode, capacity,
                verticalScale, horizontalScale);
        copy.latestBounds = latestBounds;
        copy.centeredBounds = centeredBounds;
        copy.minimumBounds = minimumBounds;
        copy.samples.addAll(samples);
        copy.sampleWeight = sampleWeight;
        copy.accumulatedSampleAge = accumulatedSampleAge;
        copy.normalization = normalization;
        copy.elapsedTicks = elapsedTicks;
        return copy;
    }

    public AxisAlignedBox combinedBounds() {
        AxisAlignedBox result = recentBounds.union(latestBounds);
        for (MovementStep sample : samples)
            result = result.union(sample.position());
        return result;
    }

    /** Generate the bounded forward sequence used by movement comparison. */
    public java.util.List<AxisAlignedBox> projectedBounds() {
        java.util.ArrayList<AxisAlignedBox> result = new java.util.ArrayList<>();
        result.add(recentBounds);
        MovementPredictor copy = copy();
        boolean mode = AxisAlignedBox.isMode();
        int step = 0;
        while (copy.isPredictionAvailable() && !mode && step < 64) {
            copy.update();
            result.add(copy.recentBounds);
            step++;
        }
        if (copy.isPredictionAvailable() && !mode) {
            result.add(copy.combinedBounds());
        }
        return result;
    }


    /** Advance the bounds transition and consume sample time at the configured rate. */
    public void update() {
        if (elapsedTicks > 0) {
            recentBounds = FixedPointGeometry.interpolate(
                    recentBounds, latestBounds, 1.0 / elapsedTicks);
            elapsedTicks--;
            return;
        }

        if (!samples.isEmpty()) {
            while (!samples.isEmpty()) {
                MovementStep oldest = samples.first();
                int age = oldest.ticks();
                AxisAlignedBox oldestBounds = oldest.position();
                if (sampleWeight < age) {
                    recentBounds = FixedPointGeometry.interpolate(
                            minimumBounds, oldestBounds, sampleWeight / age);
                    break;
                }
                sampleWeight -= age;
                minimumBounds = oldestBounds;
                samples.removeFirst();
            }
            if (samples.isEmpty()) recentBounds = latestBounds;

            float averageStep = Math.max(accumulatedSampleAge / capacity, 1.0F);
            normalization += (1.0F / capacity) * (averageStep - normalization);
            float consumed;
            if (normalization < accumulatedSampleAge) {
                consumed = normalization;
            } else {
                normalization = 1.0F;
                consumed = accumulatedSampleAge;
            }
            sampleWeight += consumed;
            accumulatedSampleAge -= consumed;
            return;
        }
        accumulatedSampleAge = 0.0F;
        normalization = 1.0F;
    }

    /** Consume an externally observed sample batch and update the predictor's bounds. */
    public void ingestSamples(java.util.List<MovementStep> incomingSamples,
                              Float newVerticalScale, Float newHorizontalScale,
                              boolean checkLargeDisplacement) {

        AxisAlignedBox observedBounds = null;
        if (incomingSamples != null) {
            MovementStep lastSample = (MovementStep)
                    me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.lastOrNull(incomingSamples);
            if (lastSample != null) observedBounds = lastSample.position();
        }
        if (observedBounds == null) {
            observedBounds = isPredictionAvailable() ? latestBounds : recentBounds;
        }

        float vertical = newVerticalScale == null ? verticalScale : newVerticalScale.floatValue();
        float horizontal = newHorizontalScale == null ? horizontalScale : newHorizontalScale.floatValue();
        boolean largeDisplacement = checkLargeDisplacement
                && FixedPointGeometry.midpointDistanceSquared(recentBounds, observedBounds) > 4096.0;

        if (largeDisplacement && isPredictionAvailable() && capacityMode != CachePolicy.SNAP) {
            double dx = (observedBounds.minX() + observedBounds.maxX()
                    - recentBounds.minX() - recentBounds.maxX()) / 2.0;
            double dy = observedBounds.minY() - recentBounds.minY();
            double dz = (observedBounds.minZ() + observedBounds.maxZ()
                    - recentBounds.minZ() - recentBounds.maxZ()) / 2.0;

            latestBounds = translatedBounds(dx, dy, dz, latestBounds);
            centeredBounds = latestBounds;
            minimumBounds = translatedBounds(dx, dy, dz, minimumBounds);

            java.util.ArrayList<MovementStep> shifted = new java.util.ArrayList<>(samples.size());
            for (MovementStep sample : samples) {
                AxisAlignedBox shiftedBounds = translatedBounds(dx, dy, dz,
                        sample.position());
                shifted.add(new MovementStep(shiftedBounds,
                        sample.ticks()));
            }
            samples.clear();
            samples.addAll(shifted);
            recentBounds = observedBounds;
            verticalScale = vertical;
            horizontalScale = horizontal;
            return;
        }

        if (capacityMode == CachePolicy.SNAP || largeDisplacement) {
            recentBounds = observedBounds;
            samples.clear();
            elapsedTicks = 0;
            accumulatedSampleAge = 0.0F;
            normalization = 1.0F;
        } else if (!(isPredictionAvailable() && java.util.Objects.equals(latestBounds, observedBounds)
                && verticalScale == vertical && horizontalScale == horizontal)) {
            java.util.List<MovementStep> additions = incomingSamples;
            if (additions == null || java.util.Objects.equals(centeredBounds, observedBounds)) {
                additions = java.util.Collections.singletonList(
                        new MovementStep(observedBounds, capacity));
            }

            if (capacityMode == CachePolicy.LINEAR) {
                elapsedTicks = capacity;
            } else {
                if (!isPredictionAvailable()) {
                    minimumBounds = recentBounds;
                    sampleWeight = 1.0F;
                }
                for (MovementStep sample : additions) {
                    samples.addLast(sample);
                    accumulatedSampleAge += sample.ticks();
                }
            }
            centeredBounds = observedBounds;
        }
        latestBounds = observedBounds;
        verticalScale = vertical;
        horizontalScale = horizontal;
    }

    public float predictionAt() {
        if (samples.isEmpty()) return elapsedTicks;
        int predictedAge = 0;
        for (MovementStep sample : samples)
            predictedAge += sample.ticks();
        return predictedAge - sampleWeight;
    }

    public float weightedPredictionError(MovementPredictor other) {
        float ownPrediction = predictionAt();
        float otherPrediction = other.predictionAt();
        return Math.abs(ownPrediction - otherPrediction)
                + capacity * Math.abs(normalization - other.normalization);
    }

    public float[] predictionComponents() {
        return new float[] {predictionAt(), accumulatedSampleAge,
                capacity * normalization};
    }

    public java.util.List<?> featureState() {
        boolean inverseMode = AxisAlignedBox.isInverseMode();
        if (inverseMode && !isPredictionAvailable()) {
            return java.util.Arrays.asList(
                    boundsFeatures(recentBounds), boundsFeatures(centeredBounds),
                    Float.valueOf(verticalScale), Float.valueOf(horizontalScale));
        }

        java.util.ArrayList<Object> sampleFeatures = new java.util.ArrayList<>(samples.size());
        for (MovementStep sample : samples) {
            java.util.List<?> features = boundsFeatures(sample.position());
            sampleFeatures.add(me.rerere.matrix.thirdparty.kotlin.TuplesKt.to(
                    features, Integer.valueOf(sample.ticks())));
        }
        return java.util.Arrays.asList(
                boundsFeatures(recentBounds), boundsFeatures(latestBounds),
                boundsFeatures(centeredBounds), boundsFeatures(minimumBounds),
                sampleFeatures, Long.valueOf(featureMetric(sampleWeight)), Long.valueOf(featureMetric(accumulatedSampleAge)),
                Long.valueOf(featureMetric(normalization)), Integer.valueOf(elapsedTicks),
                Float.valueOf(verticalScale), Float.valueOf(horizontalScale));
    }

    private static java.util.List<?> boundsFeatures(AxisAlignedBox bounds) {
        return FixedPointGeometry.quantizedCoordinates(bounds);
    }

    private static long featureMetric(float value) {
        return FixedPointGeometry.quantize(value);
    }

    public boolean isPredictionAvailable() {
        return !samples.isEmpty() || elapsedTicks > 0;
    }

}
