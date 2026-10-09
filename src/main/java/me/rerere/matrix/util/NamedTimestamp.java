package me.rerere.matrix.util;

import java.util.Objects;

public class NamedTimestamp {
    private final String name;
    private long timestamp;

    public NamedTimestamp(String name, long timestamp) {
        this.name = name;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return format(name, timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, Long.valueOf(timestamp));
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long newTimestamp) {
        timestamp = newTimestamp;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null) {
            return false;
        }
        if (getClass() != otherObject.getClass()) {
            return false;
        }
        NamedTimestamp other = (NamedTimestamp) otherObject;
        int timestampComparison = Long.compare(timestamp, other.timestamp);
        return timestampComparison == 0 && Objects.equals(name, other.name);
    }

    public String getName() {
        return name;
    }

    public void resetTimestamp() {
        timestamp = 0L;
    }

    private static String format(String name, long timestamp) {
        return new StringBuilder(name).append("|").append(timestamp).toString();
    }

}
