package me.rerere.matrix.world;

import java.util.LinkedHashSet;
import java.util.Set;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.world.PistonPosition;
import org.bukkit.Location;
import org.bukkit.block.Block;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

/** Tracks block coordinates and answers time-limited location queries against them. */
public final class RecentBlockTracker {
    private final Set<PistonPosition> coordinates = new LinkedHashSet<>();

    public RecentBlockTracker() { }

    private static boolean applyPredicate(Function1<Object, Boolean> predicate, Object value) {
        return ((Boolean) predicate.invoke(value)).booleanValue();
    }

    /** Checks matching tracked positions and requires their timestamp to be at most 2 seconds old. */
    public boolean hasRecentBlockAt(Location location) {
        long now = MillisecondSampler.lastSampleMillis();
        for (PistonPosition position : coordinates) {
            if (position.isNear(location)
                    && now - position.getCreatedAtMillis() <= 2000L) {
                return true;
            }
        }
        return false;
    }

    /** Returns true when a coordinate record has aged at least 2.5 seconds. */
    private static boolean isExpired(long now, PistonPosition position) {
        return now - position.getCreatedAtMillis() >= 2500L;
    }

    public void add(Block block) {
        coordinates.add(new PistonPosition(block.getX(), block.getY(), block.getZ()));
    }

    public Set<PistonPosition> getCoordinates() { return coordinates; }

    public void removeExpired() {
        if (coordinates.isEmpty()) return;
        long now = MillisecondSampler.currentTimeMillis();
        Function1<Object, Boolean> expiredPositionPredicate = value ->
                Boolean.valueOf(isExpired(now, (PistonPosition) value));
        coordinates.removeIf(value -> applyPredicate(expiredPositionPredicate, value));
    }

}
