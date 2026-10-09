package me.rerere.matrix.movement;

public class VerticalVelocitySample {
    private double verticalVelocity;
    private long timestamp;

    public VerticalVelocitySample(double verticalVelocity, long timestamp) {
        this.verticalVelocity = verticalVelocity;
        this.timestamp = timestamp;
    }

    public double getVerticalVelocity() { return verticalVelocity; }
    public double componentVerticalVelocity() { return verticalVelocity; }
    public long getTimestamp() { return timestamp; }
    public long componentTimestamp() { return timestamp; }

    public void setVerticalVelocity(double value) { verticalVelocity = value; }
    public void setTimestamp(long value) { timestamp = value; }

    public boolean canEqual(Object candidate) { return candidate instanceof VerticalVelocitySample; }

    @Override public boolean equals(Object candidate) {
        if (candidate == this) return true;
        if (!(candidate instanceof VerticalVelocitySample)) return false;
        VerticalVelocitySample other = (VerticalVelocitySample) candidate;
        return other.canEqual(this)
                && Double.compare(getVerticalVelocity(), other.getVerticalVelocity()) == 0
                && getTimestamp() == other.getTimestamp();
    }

    @Override public int hashCode() {
        long bits = Double.doubleToLongBits(getVerticalVelocity());
        int result = 59 + (int) ((bits >>> 32) ^ bits);
        long time = getTimestamp();
        return result * 59 + (int) ((time >>> 32) ^ time);
    }
}
