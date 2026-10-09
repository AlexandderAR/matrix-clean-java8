package me.rerere.matrix.player;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import me.rerere.matrix.logging.DebugLogger;
import me.rerere.matrix.movement.MotionVectorState;
import me.rerere.matrix.util.time.MillisecondSampler;

/** Timestamped velocity history used by player movement checks. */
public class VelocityHistory {
    private long intervalStart;
    private MotionVectorState currentSample;
    private long latestSampleTime;
    private final List<MotionVectorState> samples;

    public VelocityHistory() {
        this.samples = new CopyOnWriteArrayList<>();
        this.intervalStart = 0L;
    }

    public boolean hasRecentData() {
        return MillisecondSampler.lastSampleMillis() - latestSampleTime <= 1500L;
    }

    private static boolean isRecentAndUnflagged(long timestamp, MotionVectorState sample) {
        return timestamp - sample.getTimestamp() < 1500L
                && !sample.isInternalFlag();
    }

    public void recordVelocity(double x, double y, double z) {
        MotionVectorState velocity = new MotionVectorState(
                x, y, z, MillisecondSampler.currentTimeMillis(), false);
        this.currentSample = velocity;
        this.samples.add(velocity);
        if (System.currentTimeMillis() - this.intervalStart > 1500L) {
            this.removeExpiredSamples();
            this.intervalStart = System.currentTimeMillis();
        }
        this.latestSampleTime = System.currentTimeMillis();
        DebugLogger.printDebug(formatVelocityApplication(x, y, z));
    }

    private static double absoluteVerticalMagnitude(MotionVectorState sample) {
        return Math.abs(sample.getY());
    }

    public void markOldestRecentAsApplied() {
        long now = System.currentTimeMillis();
        Optional<MotionVectorState> oldestUnapplied = this.samples.stream()
                .filter(sample -> isRecentAndUnflagged(now, sample))
                .min(Comparator.comparingLong(sample -> sample.getTimestamp()));
        if (oldestUnapplied.isPresent()) {
            oldestUnapplied.get().setInternalFlag(true);
        }
    }

    private static String formatVelocityLabel(String prefix, String suffix, double value) {
        return new StringBuilder(prefix).append(suffix).append(":").append(value).toString();
    }

    public Set<MotionVectorState> findRecentSamplesAtDistance(double target) {
        long now = System.currentTimeMillis();
        Set<MotionVectorState> matchingSamples = new HashSet<>();
        for (MotionVectorState sample : this.samples) {
            long age = now - sample.getTimestamp();
            if (age < 1500L
                    && Math.abs(target - sample.getY()) <= 0x1.47ae147ae147bp-8
                    && !sample.isInternalFlag()) {
                matchingSamples.add(sample);
            }
        }
        return matchingSamples;
    }

    private static boolean isRecent(long timestamp, MotionVectorState sample) {
        return timestamp - sample.getTimestamp() < 1500L;
    }

    public long intervalStart() { return this.intervalStart; }
    public List<MotionVectorState> samples() { return this.samples; }
    private static long sampleTimestamp(MotionVectorState sample) { return sample.getTimestamp(); }

    public void removeExpiredSamples() {
        long now = System.currentTimeMillis();
        this.samples.removeIf(sample -> isExpired(now, sample));
    }

    public double sumRecentVerticalValues() {
        long now = System.currentTimeMillis();
        if (now - this.latestSampleTime > 1500L) return 0.0D;
        long sampleNow = System.currentTimeMillis();
        double verticalTotal = 0.0D;
        for (MotionVectorState sample : this.samples) {
            if (sample != null && sampleNow - sample.getTimestamp() <= 1500L) {
                verticalTotal += sample.horizontalLength();
            }
        }
        return verticalTotal;
    }

    public MotionVectorState currentSample() { return this.currentSample; }

    public String summarizeRecentSamples() {
        if (this.samples.size() == 0) return "none";
        long now = System.currentTimeMillis();
        return this.samples.stream().filter(sample -> isRecent(now, sample))
                .map(VelocityHistory::formatSample)
                .collect(Collectors.joining(", "));
    }

    private static String formatVelocityApplication(double x, double y, double z) {
        return new StringBuilder("[VelApply] ").append(x).append(", ").append(y).append(", ").append(z).toString();
    }

    private static boolean hasRecentTimestamp(MotionVectorState sample) {
        return MillisecondSampler.lastSampleMillis() - sample.getTimestamp() < 1500L;
    }

    public MotionVectorState firstRecentNegativeSample() {
        long now = System.currentTimeMillis();
        for (MotionVectorState sample : this.samples) {
            long age = now - sample.getTimestamp();
            if (age < 1500L && sample.getY() < 0.0D) return sample;
        }
        return null;
    }

    public double sumRecentMagnitudes() {
        long now = System.currentTimeMillis();
        if (now - this.latestSampleTime > 1500L) return 0.0D;
        long sampleNow = System.currentTimeMillis();
        double magnitudeTotal = 0.0D;
        for (MotionVectorState sample : this.samples) {
            if (sample != null && sampleNow - sample.getTimestamp() <= 1500L
                    && !sample.isInternalFlag()) {
                magnitudeTotal += sample.horizontalLength();
            }
        }
        return magnitudeTotal;
    }

    public void recordAppliedVelocity(double x, double y, double z) {
        this.samples.add(new MotionVectorState(x, y, z, MillisecondSampler.currentTimeMillis(), true));
        if (System.currentTimeMillis() - this.intervalStart > 1500L) {
            this.removeExpiredSamples();
            this.intervalStart = System.currentTimeMillis();
        }
        this.latestSampleTime = System.currentTimeMillis();
        DebugLogger.printDebug(formatExplosionValues(x, y, z));
    }

    private static boolean isExpired(long timestamp, MotionVectorState sample) {
        return timestamp - sample.getTimestamp() > 1500L;
    }

    private static boolean isUnapplied(MotionVectorState sample) {
        return !sample.isInternalFlag();
    }

    public double maximumRecentMagnitude() {
        long now = System.currentTimeMillis();
        if (now - this.latestSampleTime > 1500L) return 0.0D;
        double maximum = 0.0D;
        for (MotionVectorState sample : this.samples) {
            if (sample != null && now - sample.getTimestamp() <= 1500L) {
                maximum = Math.max(maximum, Math.abs(sample.getYValue()));
            }
        }
        return maximum;
    }

    public long latestSampleTime() { return this.latestSampleTime; }

    public double sumRecentEligibleMagnitudes() {
        long now = System.currentTimeMillis();
        double total = 0.0D;
        for (MotionVectorState sample : this.samples) {
            if (sample != null && now - sample.getTimestamp() <= 1500L
                    && sample.isFlagB()
                    && sample.isInternalFlag()
                    && !sample.isFlagA()) {
                total += Math.abs(sample.getY());
            }
        }
        return total;
    }

    public void recordExplosion(org.bukkit.util.Vector vector) {
        MotionVectorState sample = new MotionVectorState(vector.getX(), 0x1.bf523125aab47p16, vector.getZ(),
                MillisecondSampler.currentTimeMillis(), true);
        sample.setFlagA(true);
        this.samples.add(sample);
        this.latestSampleTime = System.currentTimeMillis();
        DebugLogger.printDebug("[ExpApply] One Time Explosion");
    }

    private static String formatSample(MotionVectorState sample) {
        String state = sample.isFlagB() ? "E" : "V";
        String applied = sample.isInternalFlag() ? "A" : "U";
        double value = sample.getY();
        return formatVelocityLabel(state, applied, value);
    }

    private static String formatExplosionValues(double x, double y, double z) {
        return new StringBuilder("[ExpApply] ").append(x).append(", ").append(y).append(", ").append(z).toString();
    }

    private static boolean isRecentWithAge(long timestamp, MotionVectorState sample) {
        return timestamp - sample.getTimestamp() < 1500L;
    }

    private static long sampleTimestampDirect(MotionVectorState sample) { return sample.getTimestamp(); }

    private static boolean isNotFlagged(MotionVectorState sample) {
        return !sample.isFlagA();
    }

    public MotionVectorState firstSampleNearMagnitude(double target) {
        long now = System.currentTimeMillis();
        for (MotionVectorState sample : this.samples) {
            long age = now - sample.getTimestamp();
            if (age < 1500L
                    && Math.abs(target - sample.getY()) <= 0x1.47ae147ae147bp-8) return sample;
        }
        return null;
    }

    public Set<MotionVectorState> selectSamplesForMagnitude(double target) {
        long now = System.currentTimeMillis();
        double magnitude = Math.abs(target);
        Set<MotionVectorState> selectedSamples = new HashSet<>();
        double selectedMagnitude = 0.0D;
        List<MotionVectorState> candidates = this.samples.stream()
                .filter(sample -> sample.isFlagB())
                .filter(VelocityHistory::isUnapplied)
                .filter(VelocityHistory::isNotFlagged)
                .filter(sample -> isRecentWithAge(now, sample))
                .sorted(Comparator.comparingLong(sample -> sample.getTimestamp()))
                .collect(Collectors.toList());
        for (MotionVectorState sample : candidates) {
            selectedSamples.add(sample);
            selectedMagnitude += Math.abs(sample.getY());
            double threshold = selectedMagnitude + 0x1.0624dd2f1a9fcp-10;
            boolean belowTarget = Double.isNaN(threshold) || Double.isNaN(magnitude) || threshold < magnitude;
            if (!belowTarget) return selectedSamples;
        }
        selectedSamples.clear();
        return selectedSamples;
    }

    private static boolean isAvailable(MotionVectorState sample) {
        return !sample.isFlagA();
    }

    private static boolean isNotConsumed(MotionVectorState sample) {
        return !sample.isInternalFlag();
    }

}
