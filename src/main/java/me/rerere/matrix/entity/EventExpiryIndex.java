package me.rerere.matrix.entity;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.time.DurationKt;
import me.rerere.matrix.thirdparty.kotlin.time.DurationUnit;
import me.rerere.matrix.compat.KotlinTimeAccess;
import me.rerere.matrix.model.RecordProjection;
import me.rerere.matrix.player.PlayerCheckContext;

/** Time-window index that expires timestamped events and retains the newest bounded set. */
public final class EventExpiryIndex {
    private static final int MAXIMUM_CAPACITY = 1024;

    private final Set<RecordProjection> events = new LinkedHashSet<RecordProjection>();
    private final PlayerCheckContext owner;

    public EventExpiryIndex(PlayerCheckContext owner) {
        this.owner = owner;
    }

    /** Prunes the prior window before appending the new event. */
    public void addEvent(RecordProjection event) {
        cleanup(this, KotlinTimeAccess.zeroDuration(), 0, 3);
        events.add(event);
    }

    private static boolean hasExpired(long windowDuration, long cutoffTime,
            RecordProjection event) {
        long expirationTime = event.getPrimaryValue()
                + durationToWholeMilliseconds(windowDuration);
        return expirationTime < cutoffTime;
    }

    private static boolean invokePredicate(Function1 predicate, Object event) {
        return (Boolean) predicate.invoke(event);
    }

    private static void cleanup(EventExpiryIndex owner, long windowDuration, int capacity,
            int policyFlags) {
        if ((policyFlags & 1) != 0) {
            windowDuration = DurationKt.toDuration(5, DurationUnit.MINUTES);
        }
        if ((policyFlags & 2) != 0) {
            capacity = MAXIMUM_CAPACITY;
        }
        owner.prune(windowDuration, capacity);
    }

    /** Applies the real default-mask flags to typed cleanup parameters. */
    public static void runCleanupBridge(EventExpiryIndex owner, long duration,
            int capacity, int policyFlags) {
        cleanup(owner, duration, capacity, policyFlags);
    }

    private void prune(long windowDuration, int capacity) {
        long cutoffTime = System.currentTimeMillis();
        Function1 expiry = event -> Boolean.valueOf(hasExpired(windowDuration, cutoffTime,
                (RecordProjection) event));
        events.removeIf(event -> invokePredicate(expiry, event));
        if (events.size() <= capacity) return;

        List<RecordProjection> orderedEvents = CollectionsKt.sortedWith(
                events, Comparator.comparingLong(RecordProjection::getPrimaryValue));
        events.removeAll(CollectionsKt.toSet(CollectionsKt.take(
                orderedEvents, events.size() - capacity)));
    }

    private static long durationToWholeMilliseconds(long encodedDuration) {
        return KotlinTimeAccess.wholeMilliseconds(encodedDuration);
    }

}
