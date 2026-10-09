package me.rerere.matrix.compat;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.compat.ScheduledTaskProxy;

/** Kotlin value wrapper for Paper's global scheduler, with lazily resolved method handles. */
public final class GlobalRegionSchedulerProxy {
    public static final Companion Companion = new Companion(null);
    private static final Lazy<Class<?>> clazzDelegate = LazyKt.lazy(() -> findSchedulerClass());
    private static final Lazy<MethodHandle> runDelegate = LazyKt.lazy(() -> findRunHandle());
    private static final Lazy<MethodHandle> delayedDelegate = LazyKt.lazy(() -> findRunDelayedHandle());
    private final Object internalObject;

    private GlobalRegionSchedulerProxy(Object value) { internalObject = value; }
    public static Object constructor(Object value) { return value; }
    public Object unbox() { return internalObject; }
    public static Lazy<?> getClazzDelegate() { return clazzDelegate; }
    public static Lazy<?> getRunDelegate() { return runDelegate; }
    public static Lazy<?> getDelayedDelegate() { return delayedDelegate; }
    public static GlobalRegionSchedulerProxy box(Object value) { return new GlobalRegionSchedulerProxy(value); }
    @Override public boolean equals(Object other) { return equalsValue(internalObject, other); }
    public static boolean equalsValue(Object value, Object other) {
        return other instanceof GlobalRegionSchedulerProxy
            && me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(value, ((GlobalRegionSchedulerProxy) other).unbox());
    }
    public static boolean valuesEqual(Object a, Object b) { return me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.areEqual(a,b); }
    @Override public int hashCode() { return valueHashCode(internalObject); }
    public static int valueHashCode(Object value) { return value.hashCode(); }
    @Override public String toString() { return valueToString(internalObject); }
    public static String valueToString(Object value) { return "GlobalRegionSchedulerProxy(internalObject=" + value + ")"; }

    private static Class<?> findSchedulerClass() {
        try { return Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler"); }
        catch (ClassNotFoundException error) { return GlobalRegionSchedulerProxy.<RuntimeException, Class<?>>sneakyThrow(error); }
    }
    @SuppressWarnings("unchecked") private static <E extends Throwable,T> T sneakyThrow(Throwable t) throws E { throw (E)t; }
    private static MethodHandle findRunHandle() {
        for (Method method : Companion.getClazz().getDeclaredMethods())
            if (method.getName().equals("run") && method.getParameterCount()==2)
                try { return ReflectionLookup.methodHandle(method); }
                catch (IllegalAccessException failure) {
                    return GlobalRegionSchedulerProxy.<RuntimeException, MethodHandle>sneakyThrow(failure);
                }
        throw new NoSuchElementException("Array contains no element matching the predicate.");
    }
    private static MethodHandle findRunDelayedHandle() {
        for (Method method : Companion.getClazz().getDeclaredMethods())
            if (method.getName().equals("runDelayed") && method.getParameterCount()==3)
                try { return ReflectionLookup.methodHandle(method); }
                catch (IllegalAccessException failure) {
                    return GlobalRegionSchedulerProxy.<RuntimeException, MethodHandle>sneakyThrow(failure);
                }
        throw new NoSuchElementException("Array contains no element matching the predicate.");
    }
    public static Object run(Object receiver, Function0<?> action) {
        Consumer<Object> consumer = ignored -> action.invoke();
        try {
            Object value = Companion.getRunFn().invoke(receiver, Matrix.Companion.matrixInstance(), consumer);
            return ScheduledTaskProxy.constructorImpl(value);
        } catch (Throwable failure) {
            return GlobalRegionSchedulerProxy.<RuntimeException, Object>sneakyThrow(failure);
        }
    }
    public static Object runDelayed(Object receiver, Function0<?> action, long delay) {
        Consumer<Object> consumer = ignored -> action.invoke();
        try {
            Object value = Companion.getRunDelayedFn().invoke(receiver, Matrix.Companion.matrixInstance(), consumer, delay);
            return ScheduledTaskProxy.constructorImpl(value);
        } catch (Throwable failure) {
            return GlobalRegionSchedulerProxy.<RuntimeException, Object>sneakyThrow(failure);
        }
    }
    public static final class Companion {
        private Companion() {}
        public Companion(Object marker) { this(); }
        public Class<?> getClazz() { return (Class<?>) clazzDelegate.getValue(); }
        public MethodHandle getRunFn() { return (MethodHandle) runDelegate.getValue(); }
        public MethodHandle getRunDelayedFn() { return (MethodHandle) delayedDelegate.getValue(); }
        public static Class<?> accessGetClazz(Companion self) { return self.getClazz(); }
        public static MethodHandle accessGetRunFn(Companion self) { return self.getRunFn(); }
        public static MethodHandle accessGetRunDelayedFn(Companion self) { return self.getRunDelayedFn(); }
    }
}
