package me.rerere.matrix.util;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import me.rerere.matrix.thirdparty.kotlin.collections.ArrayDeque;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.markers.KMappedMarker;

/** A lock protected, oldest-first bounded deque. */
public final class SafeBuffer<E> implements Iterable<E>, KMappedMarker {
    private final int capacity;
    private final ReentrantLock lock;
    private final ArrayDeque<E> queue;
    private static final String displayPrefix = "SafeBuffer(queue=".intern();

    public SafeBuffer(int capacity) {
        this.capacity = capacity;
        this.queue = new ArrayDeque<>();
        this.lock = new ReentrantLock();
    }

    public final int size() {
        lock.lock();
        try { return queue.size(); }
        finally { lock.unlock(); }
    }

    public final boolean isEmpty() {
        lock.lock();
        try { return queue.isEmpty(); }
        finally { lock.unlock(); }
    }

    public final boolean contains(Object element) {
        lock.lock();
        try { return queue.contains(element); }
        finally { lock.unlock(); }
    }

    public final E lastOrNull() {
        lock.lock();
        try { return queue.lastOrNull(); }
        finally { lock.unlock(); }
    }

    public final void add(E element) {
        lock.lock();
        try {
            queue.add(element);
            if (queue.size() > capacity) queue.removeFirst();
        } finally { lock.unlock(); }
    }

    public final void forEachLocked(Function1<? super E, ?> action) {
        lock.lock();
        try {
            List<E> snapshot = (List<E>) (List<?>) CollectionsKt.toList(queue);
            for (E element : snapshot) action.invoke(element);
        } finally { lock.unlock(); }
    }

    public final List<E> snapshot() {
        lock.lock();
        try { return (List<E>) (List<?>) CollectionsKt.toList(queue); }
        finally { lock.unlock(); }
    }

    public final void clear() {
        lock.lock();
        try { queue.clear(); }
        finally { lock.unlock(); }
    }

    public final boolean remove(Object element) {
        lock.lock();
        try { return queue.remove(element); }
        finally { lock.unlock(); }
    }

    public final boolean hasElements() {
        lock.lock();
        try { return !queue.isEmpty(); }
        finally { lock.unlock(); }
    }

    public final boolean isAtCapacity() {
        lock.lock();
        try { return queue.size() == capacity; }
        finally { lock.unlock(); }
    }

    @Override public Iterator<E> iterator() {
        lock.lock();
        try { return ((List<E>) (List<?>) CollectionsKt.toList(queue)).iterator(); }
        finally { lock.unlock(); }
    }

    @Override public String toString() {
        lock.lock();
        try { return displayPrefix + queue + ')'; }
        finally { lock.unlock(); }
    }

}
