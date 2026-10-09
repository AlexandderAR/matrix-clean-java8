package me.rerere.matrix.util;

import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.collections.ArraysKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;

/** Creates a mutable list wrapper from an object array. */
public final class ObjectListFactory {
    private ObjectListFactory() { }

    public ObjectListFactory(DefaultConstructorMarker marker) {
        this();
    }

    public DelegatingMutableList<Object> mutableCopy(Object... values) {
        List<Object> copiedValues = new ArrayList<>(ArraysKt.toList(values));
        return new DelegatingMutableList<>(copiedValues);
    }
}
