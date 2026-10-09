package me.rerere.matrix.compat;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Adapts Paper's entity scheduler to the Kotlin value-class call sites. */
public final class EntitySchedulerProxy {
    public static final Companion Companion = new Companion(null);
    private final Object internalObject;
    private static final Lazy clazz$delegate = LazyKt.lazy(EntitySchedulerProxy::clazz_delegate$lambda$6);
    private static final Lazy runAtFixedRateFn$delegate = LazyKt.lazy(EntitySchedulerProxy::runAtFixedRateFn_delegate$lambda$8);
    private static final Lazy runFn$delegate = LazyKt.lazy(EntitySchedulerProxy::runFn_delegate$lambda$10);
    private static final Lazy runDelayedFn$delegate = LazyKt.lazy(EntitySchedulerProxy::runDelayedFn_delegate$lambda$12);

    private EntitySchedulerProxy(Object scheduler) { this.internalObject = scheduler; }
    public static Object constructorImpl(Object scheduler) { return scheduler; }
    public Object unboxImpl() { return internalObject; }
    public static EntitySchedulerProxy boxImpl(Object value) { return new EntitySchedulerProxy(value); }
    public static boolean equalsImpl(Object scheduler, Object other) {
        if (!(other instanceof EntitySchedulerProxy)) return false;
        return Intrinsics.areEqual(scheduler, ((EntitySchedulerProxy) other).unboxImpl());
    }
    public static boolean equalsImpl0(Object left, Object right) { return Intrinsics.areEqual(left, right); }
    public boolean equals(Object other) { return equalsImpl(internalObject, other); }
    public static int hashCodeImpl(Object scheduler) { return scheduler.hashCode(); }
    public int hashCode() { return hashCodeImpl(internalObject); }
    public static String toStringImpl(Object scheduler) { return formatToString(scheduler); }
    public String toString() { return toStringImpl(internalObject); }

    public static Object run(Object scheduler, Function0 onRetired, Function0 callback) throws Throwable {
        Consumer<Object> retirementCallback = ignored -> onRetired.invoke();
        Runnable scheduledTask = () -> callback.invoke();
        Object taskHandle = Companion.getRunFn().invoke(
                scheduler, Matrix.Companion.matrixInstance(), retirementCallback, scheduledTask);
        return ScheduledTaskProxy.constructorImpl(taskHandle);
    }
    public static Object runDelayed(Object scheduler, Function0 onRetired, Function0 callback, long delay) throws Throwable {
        Consumer<Object> retirementCallback = ignored -> onRetired.invoke();
        Runnable scheduledTask = () -> callback.invoke();
        Object taskHandle = Companion.getRunDelayedFn().invoke(scheduler,
                Matrix.Companion.matrixInstance(), retirementCallback, scheduledTask, delay);
        return ScheduledTaskProxy.constructorImpl(taskHandle);
    }
    public static Object runAtFixedRate(Object scheduler, Function1 onScheduled, Function0 onRetired, long initialDelay, long period) throws Throwable {
        Consumer<Object> scheduledCallback = taskHandle ->
                onScheduled.invoke(ScheduledTaskProxy.boxImpl(ScheduledTaskProxy.constructorImpl(taskHandle)));
        Runnable retirementCallback = () -> onRetired.invoke();
        Object scheduledTaskHandle = Companion.getRunAtFixedRateFn().invoke(scheduler,
                Matrix.Companion.matrixInstance(), scheduledCallback, retirementCallback,
                initialDelay, period);
        return ScheduledTaskProxy.constructorImpl(scheduledTaskHandle);
    }

    private static Class<?> clazz_delegate$lambda$6() {
        try {
            return Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
        } catch (ClassNotFoundException failure) {
            return EntitySchedulerProxy.<Class<?>, RuntimeException>sneakyThrow(failure);
        }
    }
    private static Method findMethod(String name, int arity) {
        for (Method method : Companion.getClazz().getDeclaredMethods()) {
            if (Intrinsics.areEqual(method.getName(), name) && method.getParameterCount() == arity) return method;
        }
        throw new NoSuchElementException("Array contains no element matching the predicate.");
    }
    private static MethodHandle runAtFixedRateFn_delegate$lambda$8() { return methodHandle("runAtFixedRate", 5); }
    private static MethodHandle runFn_delegate$lambda$10() { return methodHandle("run", 3); }
    private static MethodHandle runDelayedFn_delegate$lambda$12() { return methodHandle("runDelayed", 4); }
    private static void runAtFixedRate_Lx6yiw8$lambda$5(Function0 action) { action.invoke(); }
    private static void run_KHBjrWE$lambda$0(Function0 action, Object ignored) { action.invoke(); }
    private static void run_KHBjrWE$lambda$1(Function0 action) { action.invoke(); }
    private static void runDelayed_GX01Rpk$lambda$2(Function0 action, Object ignored) { action.invoke(); }
    private static void runDelayed_GX01Rpk$lambda$3(Function0 action) { action.invoke(); }
    private static void runAtFixedRate_Lx6yiw8$lambda$4(Function1 action, Object value) { action.invoke(ScheduledTaskProxy.boxImpl(ScheduledTaskProxy.constructorImpl(value))); }

    public static Lazy access$getClazz$delegate$cp() { return clazz$delegate; }
    public static Lazy access$getRunFn$delegate$cp() { return runFn$delegate; }
    public static Lazy access$getRunDelayedFn$delegate$cp() { return runDelayedFn$delegate; }
    public static Lazy access$getRunAtFixedRateFn$delegate$cp() { return runAtFixedRateFn$delegate; }
    private static String formatToString(Object scheduler) {
        return "EntitySchedulerProxy(internalObject=" + scheduler + ")";
    }

    private static MethodHandle methodHandle(String name, int arity) {
        try {
            return ReflectionLookup.methodHandle(findMethod(name, arity));
        } catch (IllegalAccessException failure) {
            return EntitySchedulerProxy.<MethodHandle, RuntimeException>sneakyThrow(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T sneakyThrow(Throwable failure) throws E {
        throw (E) failure;
    }

    public static final class Companion {
        private Companion() {}
        public Companion(me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker ignored) { this(); }
        public static Class<?> access$getClazz(Companion self) { return self.getClazz(); }
        public static MethodHandle access$getRunDelayedFn(Companion self) { return self.getRunDelayedFn(); }
        public static MethodHandle access$getRunFn(Companion self) { return self.getRunFn(); }
        public static MethodHandle access$getRunAtFixedRateFn(Companion self) { return self.getRunAtFixedRateFn(); }
        private Class<?> getClazz() { return (Class<?>) access$getClazz$delegate$cp().getValue(); }
        private MethodHandle getRunFn() { return (MethodHandle) access$getRunFn$delegate$cp().getValue(); }
        private MethodHandle getRunDelayedFn() { return (MethodHandle) access$getRunDelayedFn$delegate$cp().getValue(); }
        private MethodHandle getRunAtFixedRateFn() { return (MethodHandle) access$getRunAtFixedRateFn$delegate$cp().getValue(); }
    }
}
