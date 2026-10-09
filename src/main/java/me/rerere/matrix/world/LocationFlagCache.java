package me.rerere.matrix.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Location;
import me.rerere.matrix.geometry.Vector3;
import me.rerere.matrix.util.time.MillisecondSampler;

/** Synchronized location-result cache with timestamp-based cleanup. */
public final class LocationFlagCache {
    private final Map<Vector3, Boolean> cachedFlags;
    private static final long expirationMillis = 10000L;

    public LocationFlagCache() { cachedFlags = new HashMap<>(); }

    public synchronized void put(Vector3 key, boolean value) {
        cachedFlags.put(key, Boolean.valueOf(value));
    }

    public synchronized void removeExpiredEntries() {
        long now = MillisecondSampler.currentTimeMillis();
        List<Vector3> expiredKeys = cachedFlags.keySet().stream()
                .filter(key -> isExpiredAt(now, key))
                .collect(Collectors.toList());
        expiredKeys.forEach(cachedFlags::remove);
    }

    /** Applies the cache's timestamp comparison to one location key. */
    public static boolean isExpiredAt(long now, Vector3 key) {
        return key.identity < now - expirationMillis;
    }

    public synchronized void clear() { cachedFlags.clear(); }

    /** Performs the canonical cache lookup and recomputes the location flag. */
    public synchronized boolean getOrCompute(Location location) {
        Boolean cached = cachedFlags.get(new Vector3(location));
        if (cached == null) {
            return BlockGeometry.collides(location);
        }
        return cached.booleanValue();
    }

}
