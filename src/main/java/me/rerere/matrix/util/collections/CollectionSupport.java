package me.rerere.matrix.util.collections;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/** Compatibility helpers for reading and modifying collection endpoints. */
public final class CollectionSupport {
    private static final String SEQUENCED_COLLECTION_ERROR =
            "java.util.SequencedCollection.addLast not implemented for ";
    private CollectionSupport() { }

    /** Removes and returns the first item, using the collection's native front operation. */
    public static Object removeFirst(Collection<?> collection) {
        if (collection instanceof List<?>) return ((List<?>) collection).remove(0);
        if (collection instanceof Deque<?>) return ((Deque<?>) collection).removeFirst();
        Iterator<?> iterator = collection.iterator();
        Object first = iterator.next();
        iterator.remove();
        return first;
    }

    /** Adds at the end where that operation is represented by List or Deque. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void append(Collection collection, Object value) {
        if (collection instanceof List) {
            ((List) collection).add(value);
            return;
        }
        if (collection instanceof Deque) {
            ((Deque) collection).addLast(value);
            return;
        }
        throw new UnsupportedOperationException(SEQUENCED_COLLECTION_ERROR
                + collection.getClass().getName());
    }

    /** Returns the first item without removing it. */
    public static Object first(Collection<?> collection) {
        if (collection instanceof List<?>) return ((List<?>) collection).get(0);
        if (collection instanceof Deque<?>) return ((Deque<?>) collection).getFirst();
        return collection.iterator().next();
    }

}


