package me.rerere.matrix.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import me.rerere.matrix.thirdparty.kotlin.time.Duration;
import me.rerere.matrix.thirdparty.kotlin.time.TimeSource$Monotonic;
import me.rerere.matrix.thirdparty.kotlin.time.TimeSource$Monotonic$ValueTimeMark;

/** Java access to the shaded Kotlin value-class ABI retained in the dependency. */
public final class KotlinTimeAccess {
    private KotlinTimeAccess() {}

    private static final class Elapsed {
        static final MethodHandle METHOD = resolve(TimeSource$Monotonic$ValueTimeMark.class,
                "elapsedNow-UwyO8pc", MethodType.methodType(long.class, long.class));
    }

    private static final class CompareDuration {
        static final MethodHandle METHOD = resolve(Duration.class,
                "compareTo-LRDsOJo", MethodType.methodType(int.class, long.class, long.class));
    }

    private static final class MarkNow {
        static final MethodHandle METHOD = resolveVirtual(TimeSource$Monotonic.class,
                "markNow-z9LOYto", MethodType.methodType(long.class));
    }

    private static final class SubtractDuration {
        static final MethodHandle METHOD = resolve(TimeSource$Monotonic$ValueTimeMark.class,
                "minus-LRDsOJo", MethodType.methodType(long.class, long.class, long.class));
    }

    private static final class ZeroDuration {
        static final MethodHandle METHOD = resolveVirtual(Duration.Companion.getClass(),
                "getZERO-UwyO8pc", MethodType.methodType(long.class));
    }

    private static final class MultiplyDuration {
        static final MethodHandle METHOD = resolve(Duration.class,
                "times-UwyO8pc", MethodType.methodType(long.class, long.class, double.class));
    }

    private static final class AddDuration {
        static final MethodHandle METHOD = resolve(Duration.class,
                "plus-LRDsOJo", MethodType.methodType(long.class, long.class, long.class));
    }

    private static final class SubtractDurations {
        static final MethodHandle METHOD = resolve(Duration.class,
                "minus-LRDsOJo", MethodType.methodType(long.class, long.class, long.class));
    }

    public static long subtractDurations(long first, long second) {
        try {
            return (long) SubtractDurations.METHOD.invokeExact(first, second);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, Long>propagate(failure);
        }
    }

    private static final class WholeMilliseconds {
        static final MethodHandle METHOD = resolve(Duration.class,
                "getInWholeMilliseconds-impl", MethodType.methodType(long.class, long.class));
    }

    private static final class AddToMark {
        static final MethodHandle METHOD = resolve(TimeSource$Monotonic$ValueTimeMark.class,
                "plus-LRDsOJo", MethodType.methodType(long.class, long.class, long.class));
    }

    private static final class CompareMarks {
        static final MethodHandle METHOD = resolve(TimeSource$Monotonic$ValueTimeMark.class,
                "compareTo-6eNON_k", MethodType.methodType(int.class, long.class, long.class));
    }

    private static final class SubtractMarks {
        static final MethodHandle METHOD = resolve(TimeSource$Monotonic$ValueTimeMark.class,
                "minus-6eNON_k", MethodType.methodType(long.class, long.class, long.class));
    }

    public static long subtractMarks(long first, long second) {
        try { return (long) SubtractMarks.METHOD.invokeExact(first, second); }
        catch (Throwable failure) { return KotlinTimeAccess.<RuntimeException, Long>propagate(failure); }
    }

    private static final class DurationString {
        static final MethodHandle METHOD = resolve(Duration.class,
                "toString-impl", MethodType.methodType(String.class, long.class));
    }

    public static long addDurationToMark(long mark, long duration) {
        try {
            return (long) AddToMark.METHOD.invokeExact(mark, duration);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, Long>propagate(failure);
        }
    }

    public static int compareMarks(long first, long second) {
        try {
            return (int) CompareMarks.METHOD.invokeExact(first, second);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, Integer>propagate(failure);
        }
    }

    public static String durationToString(long duration) {
        try {
            return (String) DurationString.METHOD.invokeExact(duration);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, String>propagate(failure);
        }
    }

    public static long markNow() {
        try {
            return (long) MarkNow.METHOD.invoke(TimeSource$Monotonic.INSTANCE);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, Long>propagate(failure);
        }
    }

    public static long subtractDuration(long mark, long duration) {
        try {
            return (long) SubtractDuration.METHOD.invokeExact(mark, duration);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, Long>propagate(failure);
        }
    }

    public static long zeroDuration() {
        try {
            return (long) ZeroDuration.METHOD.invoke(Duration.Companion);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, Long>propagate(failure);
        }
    }

    public static long multiplyDuration(long duration, double factor) {
        try {
            return (long) MultiplyDuration.METHOD.invokeExact(duration, factor);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, Long>propagate(failure);
        }
    }

    public static long addDurations(long first, long second) {
        try {
            return (long) AddDuration.METHOD.invokeExact(first, second);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, Long>propagate(failure);
        }
    }

    public static long wholeMilliseconds(long duration) {
        try {
            return (long) WholeMilliseconds.METHOD.invokeExact(duration);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, Long>propagate(failure);
        }
    }

    public static long elapsedNow(long mark) {
        try {
            return (long) Elapsed.METHOD.invokeExact(mark);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, Long>propagate(failure);
        }
    }

    public static int compareDurations(long first, long second) {
        try {
            return (int) CompareDuration.METHOD.invokeExact(first, second);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, Integer>propagate(failure);
        }
    }

    private static MethodHandle resolve(Class<?> owner, String name, MethodType type) {
        try {
            return MethodHandles.publicLookup().findStatic(owner, name, type);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, MethodHandle>propagate(failure);
        }
    }

    private static MethodHandle resolveVirtual(Class<?> owner, String name, MethodType type) {
        try {
            return MethodHandles.publicLookup().findVirtual(owner, name, type);
        } catch (Throwable failure) {
            return KotlinTimeAccess.<RuntimeException, MethodHandle>propagate(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T propagate(Throwable failure) throws E {
        throw (E) failure;
    }
}
