package me.rerere.matrix.util;

import java.util.Iterator;
import me.rerere.matrix.thirdparty.kotlin.collections.ArrayDeque;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.markers.KMappedMarker;

/** A bounded FIFO collection whose oldest value is evicted after capacity is exceeded. */
public final class RecentElements<T> implements Iterable<T>, KMappedMarker {
    private final ArrayDeque<T> elements;
    private final int capacity;

    public RecentElements(int capacity) {
        this.capacity = capacity;
        this.elements = new ArrayDeque<T>();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public boolean isEmptyOrNotEmpty() {
        return !elements.isEmpty();
    }

    public void add(T value) {
        elements.add(value);
        if (elements.size() > capacity) {
            elements.removeFirst();
        }
    }

    public int size() {
        return elements.size();
    }

    public boolean contains(Object value) {
        return me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.contains(this, value);
    }

    /** Returns null when the requested offset is at or beyond the current size. */
    public T getFromNewest(int index) {
        if (elements.size() <= index) {
            return null;
        }
        return elements.get(elements.size() - 1 - index);
    }

    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }

    public boolean isAtCapacity() {
        return elements.size() == capacity;
    }

    public void remove(Object value) {
        elements.remove(value);
    }

    public void clear() {
        elements.clear();
    }

    public T last() {
        return elements.last();
    }

}
