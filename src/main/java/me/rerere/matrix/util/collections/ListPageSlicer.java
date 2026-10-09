package me.rerere.matrix.util.collections;

import java.util.List;
import java.util.Set;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.ranges.IntRange;
import me.rerere.matrix.thirdparty.kotlin.ranges.RangesKt;

/** Selects the zero-based item range for one page. */
public final class ListPageSlicer {
    private ListPageSlicer() { }

    public static List<?> slice(List<?> values, int pageIndex, int pageSize) {
        IntRange requestedIndices = RangesKt.until(
                pageIndex * pageSize, (pageIndex + 1) * pageSize);
        Set<Integer> validIndices = CollectionsKt.intersect(
                requestedIndices, CollectionsKt.getIndices(values));
        return CollectionsKt.slice(values, validIndices);
    }

}
