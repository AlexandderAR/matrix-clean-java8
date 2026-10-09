package me.rerere.matrix.movement;

import org.bukkit.util.Vector;

/** Stores a movement vector, its timestamp, and associated flags. */
public class MotionVectorState {
    private final double x;
    private double y;
    private final double z;
    private long timestamp;
    private boolean flagB;
    private boolean flagA;
    private boolean internalFlag;

    public MotionVectorState(double x, double y, double z, long timestamp, boolean flagB) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
        this.flagB = flagB;
        this.internalFlag = false;
        this.flagA = false;
    }

    public boolean isFlagA() { return flagA; }
    public boolean isFlagB() { return flagB; }
    public void setFlagA(boolean flagA) { this.flagA = flagA; }
    public void setY(double y) { this.y = y; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public double getX() { return x; }
    public boolean isInternalFlag() { return internalFlag; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double horizontalLength() { return Math.sqrt(x * x + z * z); }
    public Vector toVector() { return new Vector(x, y, z); }
    public void setFlagB(boolean flagB) { this.flagB = flagB; }
    public void setInternalFlag(boolean internalFlag) { this.internalFlag = internalFlag; }
    public long getTimestamp() { return timestamp; }
    public double getYValue() { return y; }

    @Override
    public int hashCode() {
        int result = 1;
        long bits = Double.doubleToLongBits(getX());
        result = 59 * result + (int) (bits ^ (bits >>> 32));
        bits = Double.doubleToLongBits(getYValue());
        result = 59 * result + (int) (bits ^ (bits >>> 32));
        bits = Double.doubleToLongBits(getZ());
        result = 59 * result + (int) (bits ^ (bits >>> 32));
        long time = getTimestamp();
        result = 59 * result + (int) (time ^ (time >>> 32));
        result = 59 * result + (isFlagB() ? 79 : 97);
        result = 59 * result + (isInternalFlag() ? 79 : 97);
        result = 59 * result + (isFlagA() ? 79 : 97);
        return result;
    }

    public boolean canEqual(Object otherObject) {
        return otherObject instanceof MotionVectorState;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) return true;
        if (!(otherObject instanceof MotionVectorState)) return false;
        MotionVectorState otherState = (MotionVectorState) otherObject;
        return otherState.canEqual(this)
                && Double.compare(getX(), otherState.getX()) == 0
                && Double.compare(getYValue(), otherState.getYValue()) == 0
                && Double.compare(getZ(), otherState.getZ()) == 0
                && getTimestamp() == otherState.getTimestamp()
                && isFlagB() == otherState.isFlagB()
                && isInternalFlag() == otherState.isInternalFlag()
                && isFlagA() == otherState.isFlagA();
    }
}
