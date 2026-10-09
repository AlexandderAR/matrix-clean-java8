package me.rerere.matrix.compat;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

public final class AsyncSchedulerProxy {
    private static final Lazy<Class<?>> schedulerClass = LazyKt.lazy(AsyncSchedulerProxy::loadSchedulerClass);
    private static final Lazy<MethodHandle> runNowMethod = LazyKt.lazy(() -> resolveMethod("runNow", 2));
    private static final Lazy<MethodHandle> runAtFixedRateMethod = LazyKt.lazy(() -> resolveMethod("runAtFixedRate", 5));
    private static final Lazy<MethodHandle> runDelayedMethod = LazyKt.lazy(() -> resolveMethod("runDelayed", 4));
    private static final Lazy<MethodHandle> cancelTasksMethod = LazyKt.lazy(() -> resolveMethod("cancelTasks", 1));
    private final Object internalObject;

    private AsyncSchedulerProxy(Object internalObject) {
        this.internalObject = internalObject;
    }

    private static Class<?> loadSchedulerClass() {
        try {
            return Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
        } catch (ClassNotFoundException failure) {
            return AsyncSchedulerProxy.<RuntimeException, Class<?>>propagate(failure);
        }
    }

    private static MethodHandle resolveMethod(String methodName, int parameterCount) {
        Method[] methods = schedulerClass.getValue().getDeclaredMethods();
        for (Method method : methods) {
            if (Intrinsics.areEqual(method.getName(), methodName)
                    && method.getParameterCount() == parameterCount) {
                try {
                    return ReflectionLookup.methodHandle(method);
                } catch (Throwable failure) {
                    return AsyncSchedulerProxy.<RuntimeException, MethodHandle>propagate(failure);
                }
            }
        }
        throw new NoSuchElementException("Array contains no element matching the predicate.");
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T propagate(Throwable failure) throws E {
        throw (E) failure;
    }

    public static Object constructValue(Object scheduler) {
        return scheduler;
    }

    public static AsyncSchedulerProxy wrap(Object scheduler) {
        return new AsyncSchedulerProxy(scheduler);
    }

    public Object unwrap() {
        return internalObject;
    }

    public static Object runNow(Object scheduler, Function1<?, ?> callback) throws Throwable {
        Consumer<Object> action = scheduledTask -> ((Function1) callback).invoke(
                ScheduledTaskProxy.boxImpl(ScheduledTaskProxy.constructorImpl(scheduledTask)));
        MethodHandle method = runNowMethod.getValue();
        Matrix pluginInstance = Matrix.getInstance();
        Object scheduledTask = (Object) method.invoke(scheduler, pluginInstance, action);
        return ScheduledTaskProxy.constructorImpl(scheduledTask);
    }

    public static Object runDelayed(Object scheduler, Function1<?, ?> callback, long delayTicks) throws Throwable {
        Consumer<Object> action = scheduledTask -> ((Function1) callback).invoke(
                ScheduledTaskProxy.boxImpl(ScheduledTaskProxy.constructorImpl(scheduledTask)));
        MethodHandle method = runDelayedMethod.getValue();
        Matrix pluginInstance = Matrix.getInstance();
        Object scheduledTask = (Object) method.invoke(
                scheduler, pluginInstance, action, delayTicks * 50L, TimeUnit.MILLISECONDS);
        return ScheduledTaskProxy.constructorImpl(scheduledTask);
    }

    public static Object runAtFixedRate(Object scheduler, Function1<?, ?> callback, long delayTicks,
                                       long periodTicks) throws Throwable {
        Consumer<Object> action = scheduledTask -> ((Function1) callback).invoke(
                ScheduledTaskProxy.boxImpl(ScheduledTaskProxy.constructorImpl(scheduledTask)));
        MethodHandle method = runAtFixedRateMethod.getValue();
        Matrix pluginInstance = Matrix.getInstance();
        Object scheduledTask = (Object) method.invoke(scheduler, pluginInstance, action,
                delayTicks * 50L, periodTicks * 50L,
                TimeUnit.MILLISECONDS);
        return ScheduledTaskProxy.constructorImpl(scheduledTask);
    }

    public static void cancelTasks(Object scheduler) throws Throwable {
        MethodHandle method = cancelTasksMethod.getValue();
        Matrix pluginInstance = Matrix.getInstance();
        method.invoke(scheduler, pluginInstance);
    }

    public static String toStringValue(Object value) {
        return new StringBuilder("AsyncSchedulerProxy(internalObject=").append(value).append(")").toString();
    }

    public static int hashCodeValue(Object value) {
        return value.hashCode();
    }

    public static boolean equalsBoxed(Object value, Object other) {
        if (!(other instanceof AsyncSchedulerProxy)) { return false; }
        Object otherValue = ((AsyncSchedulerProxy) other).unwrap();
        return Intrinsics.areEqual(value, otherValue);
    }

    public static boolean equalsValues(Object first, Object second) {
        return Intrinsics.areEqual(first, second);
    }

    @Override
    public String toString() { return toStringValue(internalObject); }

    @Override
    public int hashCode() { return hashCodeValue(internalObject); }

    @Override
    public boolean equals(Object other) { return equalsBoxed(internalObject, other); }
}
