package me.rerere.matrix.network.packet;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.ranges.IntRange;

public final class RandomTransactionIdPool {
    private final Queue<Integer> ids = new ConcurrentLinkedQueue<>();
    private final int minimumId = -32768;
    private final int maximumId = -1;
    private static final String EMPTY_AFTER_REFILL = "Failed to get next ID from a non-empty queue after refill. This indicates a critical logic error.".intern();
    public RandomTransactionIdPool() { refill(); }
    public final int size() { return ids.size(); }
    private final void refill() {
        ids.clear();
        List<Integer> available = CollectionsKt.toMutableList(new IntRange(minimumId, maximumId));
        Collections.shuffle(available);
        for (Integer id : available) {
            ids.offer(id.intValue());
        }
    }
    public final int nextId() {
        if (ids.isEmpty()) refill();
        Integer result = ids.poll();
        if (result == null) throw new IllegalStateException(EMPTY_AFTER_REFILL);
        return result;
    }
}
