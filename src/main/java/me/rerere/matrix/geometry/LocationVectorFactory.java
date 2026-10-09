package me.rerere.matrix.geometry;

import org.bukkit.Location;

/** Converts a Bukkit location into Matrix's three-dimensional vector value. */
public final class LocationVectorFactory {
    public LocationVectorFactory() {
    }

    public DuplicatePosition fromLocation(Location location) {
        return new DuplicatePosition(location.getX(), location.getY(), location.getZ());
    }
}
