package me.rerere.matrix.checks.entity;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.entity.TrackedEntity;

/** Maps entity ids to their tracked entity state and forwards periodic updates. */
public final class TrackedEntityRegistry {
    private final PlayerCheckContext tracker;
    private final ConcurrentHashMap<Integer, TrackedEntity> entities;

    public TrackedEntityRegistry(PlayerCheckContext tracker) {
        this.tracker = tracker;
        this.entities = new ConcurrentHashMap<>();
    }

    public TrackedEntity get(int entityId) { return entities.get(Integer.valueOf(entityId)); }

    public void removeAll(int[] entityIds) {
        for (int entityId : entityIds) {
            entities.remove(Integer.valueOf(entityId));
        }
    }

    public void updateAll() {
        for (TrackedEntity entity : entities.values()) {
            entity.refreshTrackingState();
        }
    }

    public void put(TrackedEntity trackedEntity) {
        entities.put(Integer.valueOf(trackedEntity.entityId()), trackedEntity);
    }

    public Collection<TrackedEntity> values() { return entities.values(); }

}
