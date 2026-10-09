package me.rerere.matrix.entity;

import org.bukkit.Location;

/** Holds a movement sample's state flag, distance, and Bukkit location. */
public class MovementLocationSample {
    private static final String NULL_LOCATION_MESSAGE = "location is marked non-null but is null";

    private final boolean stateFlag;
    private double distance;
    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull
    private final Location location;

    public MovementLocationSample(boolean stateFlag, double distance,
            @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull Location location) {
        if (location == null) {
            throw new NullPointerException(NULL_LOCATION_MESSAGE);
        }
        this.stateFlag = stateFlag;
        this.distance = distance;
        this.location = location;
    }

    public boolean hasStateFlag() {
        return stateFlag;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public boolean canEqual(Object other) {
        return other instanceof MovementLocationSample;
    }

    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull
    public Location getLocation() {
        return location;
    }

    @Override
    public int hashCode() {
        int result = 59 + (hasStateFlag() ? 79 : 97);

        long distanceBits = Double.doubleToLongBits(getDistance());
        result = 59 * result + (int) (distanceBits ^ (distanceBits >>> 32));

        Location sampleLocation = getLocation();
        int locationHash = sampleLocation == null ? 43 : sampleLocation.hashCode();
        return 59 * result + locationHash;
    }

    /** Returns the data-class second component (the distance). */
    public double component2() {
        return distance;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof MovementLocationSample)) {
            return false;
        }

        MovementLocationSample otherSample = (MovementLocationSample) other;
        if (!otherSample.canEqual(this)) {
            return false;
        }
        if (hasStateFlag() != otherSample.hasStateFlag()) {
            return false;
        }
        if (Double.compare(getDistance(), otherSample.getDistance()) != 0) {
            return false;
        }

        Location thisLocation = getLocation();
        Location otherLocation = otherSample.getLocation();
        return thisLocation == null ? otherLocation == null : thisLocation.equals(otherLocation);
    }
}
