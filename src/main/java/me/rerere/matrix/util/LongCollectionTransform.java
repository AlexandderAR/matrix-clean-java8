package me.rerere.matrix.util;

import java.util.Collection;

/** Functional contract for transforming a collection using a long key. */
@FunctionalInterface
public interface LongCollectionTransform {
    Collection apply(long key, Collection values);
}
