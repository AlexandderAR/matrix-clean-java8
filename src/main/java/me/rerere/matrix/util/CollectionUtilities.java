package me.rerere.matrix.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/** Small collection helpers used by Matrix components. */
public final class CollectionUtilities {
    private static int configuredValue = 0;

    public CollectionUtilities() {
    }

    public static Map<Object, Object> newMap() {
        return new java.util.HashMap<>();
    }

    public static <T> int countMatching(List<T> items, Function<? super T, Boolean> predicate) {
        int count = 0;
        for (T item : items) {
            if (predicate.apply(item)) count++;
        }
        return count;
    }

    private static Boolean isAtLeastTwo(Integer value) {
        return value.intValue() >= 2;
    }

    public static me.rerere.matrix.util.collections.MutableMapBuilder createResult() {
        return new me.rerere.matrix.util.collections.MutableMapBuilder();
    }

    public static <T> List<T> collect(Stream<T> stream) {
        List<T> result = new ArrayList<>();
        stream.forEach(result::add);
        return result;
    }
    public static <T> Set<T> concurrentSet() {
        return Collections.newSetFromMap(new ConcurrentHashMap<>());
    }
    public static <T> Set<T> setOfValues(Object[] values) {
        @SuppressWarnings("unchecked")
        Set<T> result = (Set<T>) Stream.of(values)
                .collect(java.util.stream.Collectors.toSet());
        return result;
    }

    public static void main(String[] args) {
        List<Integer> values = Arrays.asList(1, 2, 4, 5, (int) 7363045680312483846L);
        System.out.println(countMatching(values, CollectionUtilities::isAtLeastTwo));
    }

    public static <T> Set<T> toSet(Stream<T> stream) {
        return stream.collect(java.util.stream.Collectors.toSet());
    }

    public static void setConfiguredValue(int value) {
        configuredValue = value;
    }

    public static int getConfiguredValue() {
        return configuredValue;
    }

    public static int sentinel() {
        return getConfiguredValue() == 0 ? 97 : 0;
    }

}


