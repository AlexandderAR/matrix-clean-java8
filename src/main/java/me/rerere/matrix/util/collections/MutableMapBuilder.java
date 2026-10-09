package me.rerere.matrix.util.collections;

import java.util.Map;
import me.rerere.matrix.util.CollectionUtilities;

public final class MutableMapBuilder<K, V> {
    private final Map<K, V> entries;

    @SuppressWarnings("unchecked")
    public MutableMapBuilder() {
        entries = (Map<K, V>) CollectionUtilities.newMap();
    }

    public MutableMapBuilder<K, V> put(K key, V value) {
        entries.put(key, value);
        return this;
    }

    public MutableMapBuilder<K, V> putArguments(K key, V value) {
        return put(key, value);
    }

    public Map<K, V> getEntries() {
        return entries;
    }
}
