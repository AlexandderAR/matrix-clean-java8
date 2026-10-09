package me.rerere.matrix.world;

import org.bukkit.Location;

/** A location paired with its timestamp. */
public class TimedLocation {
    private long timestamp;
    private Location location;

    public TimedLocation(Location location, long timestamp) {
        this.location = location;
        this.timestamp = timestamp;
    }

    public long timestamp() { return timestamp; }
    public Location location() { return location; }
    public boolean isTimedLocation(Object candidate) { return candidate instanceof TimedLocation; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TimedLocation)) return false;
        TimedLocation that = (TimedLocation) other;
        if (!that.isTimedLocation(this)) return false;
        if (timestamp() != that.timestamp()) return false;
        Location ownLocation = location();
        Location otherLocation = that.location();
        return ownLocation == null ? otherLocation == null : ownLocation.equals(otherLocation);
    }

    @Override public int hashCode() {
        int result = 1;
        long time = timestamp();
        result = 59 * result + (int) (time ^ (time >>> 32));
        Location value = location();
        result = 59 * result + (value == null ? 43 : value.hashCode());
        return result;
    }
}
