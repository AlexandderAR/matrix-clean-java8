package me.rerere.matrix.util;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.Sets;
import me.rerere.matrix.thirdparty.kotlin.collections.SetsKt;

public final class CollectionsSupport {
    public static final CollectionsSupport INSTANCE = new CollectionsSupport();
    private CollectionsSupport() { }

    public static <T> LinkedHashSet<T> linkedSetOf(T item) {
        LinkedHashSet<T> result = Sets.newLinkedHashSet();
        result.add(item);
        return result;
    }

    public static <E extends Enum<E>> Set<E> enumSetOf(Collection<E> elements) {
        return elements.isEmpty() ? SetsKt.<E>emptySet() : EnumSet.copyOf(elements);
    }

    public static Map<Object, Object> linkedMapOf(Object... pairs) {
        if ((pairs.length & 1) != 0) {
            throw new IllegalArgumentException("Map pairs must be a multiple of 2");
        }
        Map<Object, Object> result = new LinkedHashMap<Object, Object>();
        for (int index = 0; index < pairs.length; index += 2) {
            result.put(pairs[index], pairs[index + 1]);
        }
        return result;
    }
}
