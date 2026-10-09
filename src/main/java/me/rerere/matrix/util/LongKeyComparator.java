package me.rerere.matrix.util;

import java.util.Comparator;
import me.rerere.matrix.model.RecordProjection;

/** Orders keyed values by the long returned by their key interface. */
public final class LongKeyComparator implements Comparator<Object> {
    @Override public int compare(Object leftRecord, Object rightRecord) {
        Long leftKey = Long.valueOf(((RecordProjection) leftRecord).getPrimaryValue());
        Long rightKey = Long.valueOf(((RecordProjection) rightRecord).getPrimaryValue());
        return me.rerere.matrix.thirdparty.kotlin.comparisons.ComparisonsKt.compareValues(leftKey, rightKey);
    }
}
