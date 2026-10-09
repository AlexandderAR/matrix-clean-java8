package me.rerere.matrix.world;

import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.TuplesKt;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;

/** Coordinate offsets used by the movement checks. */
public final class CoordinateOffsets {
    public static final double BASE_RATIO = 0.08d;
    public static final int CONFIGURATION_INDEX;
    public static final List<Pair<Object, Object>> OFFSETS;
    public static final long CONFIGURATION_DURATION;
    public static final double RADIUS = 6.0d;

    private CoordinateOffsets() {
    }

    public static List<Pair<Object, Object>> getOffsets() {
        return OFFSETS;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Pair<Object, Object> pair(Object x, Object z) {
        return (Pair) TuplesKt.to(x, z);
    }

    static {
        CONFIGURATION_INDEX = 20;
        CONFIGURATION_DURATION = 1000L;
        Pair[] offsets = new Pair[] {
            pair(Double.valueOf(0.0d), Double.valueOf(0.0d)),
            pair(Double.valueOf(0.03d), Double.valueOf(0.0d)),
            pair(Double.valueOf(-0.03d), Double.valueOf(0.0d)),
            pair(Double.valueOf(0.0d), Double.valueOf(0.03d)),
            pair(Double.valueOf(0.0d), Double.valueOf(-0.03d)),
            pair(Double.valueOf(0.06d), Double.valueOf(0.0d)),
            pair(Double.valueOf(-0.06d), Double.valueOf(0.0d)),
            pair(Double.valueOf(0.0d), Double.valueOf(0.06d)),
            pair(Double.valueOf(0.0d), Double.valueOf(-0.06d))
        };
        OFFSETS = (List) CollectionsKt.listOf(offsets);
    }
}
