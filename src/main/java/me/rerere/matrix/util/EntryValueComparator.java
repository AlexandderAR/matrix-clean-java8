package me.rerere.matrix.util;

import java.util.Comparator;
import java.util.Map;

/** Orders entries by the integer value held by their payloads. */
public final class EntryValueComparator implements Comparator {
    public EntryValueComparator() {
    }

    @Override
    public int compare(Object leftObject, Object rightObject) {
        Map.Entry<?, ?> rightEntry = (Map.Entry<?, ?>) rightObject;
        Integer rightValue = Integer.valueOf(((me.rerere.matrix.violation.Violation) rightEntry.getValue()).getLevel());
        Map.Entry<?, ?> leftEntry = (Map.Entry<?, ?>) leftObject;
        Integer leftValue = Integer.valueOf(((me.rerere.matrix.violation.Violation) leftEntry.getValue()).getLevel());
        return me.rerere.matrix.thirdparty.kotlin.comparisons.ComparisonsKt.compareValues(rightValue, leftValue);
    }
}
