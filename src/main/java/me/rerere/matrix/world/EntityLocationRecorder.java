package me.rerere.matrix.world;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Queue;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import me.rerere.matrix.util.time.MillisecondSampler;

/** Tracks a weak entity reference and a bounded sequence of its location snapshots. */
public final class EntityLocationRecorder {
    private int recordedLocationCount;
    private WeakReference<LivingEntity> trackedEntity;
    private final Queue<TimedLocation> locations;
    public EntityLocationRecorder() {
        locations = new ArrayDeque<>();
    }

    public int getRecordedLocationCount() {
        return recordedLocationCount;
    }

    public LivingEntity getTrackedEntity() {
        if (trackedEntity == null) {
            return null;
        }
        return trackedEntity.get();
    }

    public void recordCurrentLocation() {
        LivingEntity entity = getTrackedEntity();
        if (entity == null) return;
        recordedLocationCount++;
        Location location = entity.getLocation().clone();
        long timestamp = MillisecondSampler.currentTimeMillis();
        locations.add(new TimedLocation(location, timestamp));
        if (locations.size() > 20) {
            locations.poll();
        }
    }

    public void updateTrackedEntity(LivingEntity entity) {
        LivingEntity current = getTrackedEntity();
        if (current == null) {
            clear();
            trackedEntity = new WeakReference<>(entity);
            return;
        }
        if (current.equals(entity)) {
            return;
        }
        clear();
        trackedEntity = new WeakReference<>(entity);
    }

    public void clear() {
        recordedLocationCount = 0;
        trackedEntity = null;
        locations.clear();
    }

    public Queue<TimedLocation> getLocations() {
        return locations;
    }

}
