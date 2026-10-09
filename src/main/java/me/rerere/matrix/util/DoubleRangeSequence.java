package me.rerere.matrix.util;

import java.util.Iterator;
import me.rerere.matrix.thirdparty.kotlin.ranges.ClosedRange;
import me.rerere.matrix.thirdparty.kotlin.sequences.SequencesKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.markers.KMappedMarker;
import me.rerere.matrix.util.coroutines.RangeSequenceCoroutine;

public final class DoubleRangeSequence implements Iterable<Double>, KMappedMarker {
    private final ClosedRange<Double> range;
    private final double step;

    public DoubleRangeSequence(ClosedRange<Double> range, double step) {
        this.range = range;
        this.step = step;
    }

    @Override
    public Iterator<Double> iterator() {
        return SequencesKt.iterator(new RangeSequenceCoroutine(range, step, null));
    }

}
