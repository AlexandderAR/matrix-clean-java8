package me.rerere.matrix.compat;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import me.rerere.matrix.util.reflect.ReflectionLookup;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Value wrapper for Paper's optional threaded-region ScheduledTask API. */
public final class ScheduledTaskProxy implements Cancelable {
    private static final Lazy<Class<?>> classDelegate;
    private static final Lazy<MethodHandle> repeatingDelegate;
    private static final Lazy<MethodHandle> cancelDelegate;
    public static final Companion Companion;
    private final Object internalObject;

    static {
        Companion = new Companion(null);
        classDelegate = LazyKt.lazy(new Function0<Class<?>>() {
            @Override public Class<?> invoke() { return loadScheduledTaskClass(); }
        });
        repeatingDelegate = LazyKt.lazy(new Function0<MethodHandle>() {
            @Override public MethodHandle invoke() { return resolveRepeatingMethodHandle(); }
        });
        cancelDelegate = LazyKt.lazy(new Function0<MethodHandle>() {
            @Override public MethodHandle invoke() { return resolveCancelMethodHandle(); }
        });
    }

    private ScheduledTaskProxy(Object internalObject) { this.internalObject = internalObject; }

    public static Object constructorImpl(Object value) { return value; }
    public static final boolean isRepeatingTaskImpl(Object value) {
        try { return (boolean) Companion.isRepeatingTaskFn().invoke(value); }
        catch (Throwable failure) { throw ScheduledTaskProxy.<RuntimeException, RuntimeException>unchecked(failure); }
    }
    public static void cancelImpl(Object value) {
        try { Companion.getCancelFn().invoke(value); }
        catch (Throwable failure) { throw ScheduledTaskProxy.<RuntimeException, RuntimeException>unchecked(failure); }
    }
    @Override public String toString() { return toStringImpl(internalObject); }
    public static int hashCodeImpl(Object value) { return value.hashCode(); }
    @Override public int hashCode() { return hashCodeImpl(internalObject); }
    public final Object unboxImpl() { return internalObject; }
    public static boolean equalsImpl(Object value, Object other) {
        if (!(other instanceof ScheduledTaskProxy)) return false;
        return Intrinsics.areEqual(value, ((ScheduledTaskProxy) other).unboxImpl());
    }
    public static final boolean equalsImpl0(Object left, Object right) { return Intrinsics.areEqual(left, right); }
    private static final String concatToStringImpl(Object value) {
        return "ScheduledTaskProxy(internalObject=" + value + ")";
    }
    public static String toStringImpl(Object value) { return concatToStringImpl(value); }
    @Override public boolean equals(Object other) { return equalsImpl(internalObject, other); }
    @Override public void cancel() { cancelImpl(internalObject); }
    public static final ScheduledTaskProxy boxImpl(Object value) { return new ScheduledTaskProxy(value); }

    public static final Lazy<MethodHandle> cancelDelegateAccessor() { return cancelDelegate; }
    public static final Lazy<Class<?>> classDelegateAccessor() { return classDelegate; }
    public static final Lazy<MethodHandle> repeatingDelegateAccessor() { return repeatingDelegate; }

    private static final MethodHandle resolveRepeatingMethodHandle() {
        return resolveNoArg("isRepeatingTask");
    }

    private static final MethodHandle resolveCancelMethodHandle() {
        return resolveNoArg("cancel");
    }

    private static final Class<?> loadScheduledTaskClass() {
        try {
            return Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
        } catch (ClassNotFoundException failure) {
            throw ScheduledTaskProxy.<RuntimeException, RuntimeException>unchecked(failure);
        }
    }

    private static MethodHandle resolveNoArg(String methodName) {
        try {
            Method method = Companion.getScheduledTaskClass().getDeclaredMethod(methodName);
            return ReflectionLookup.methodHandle(method);
        } catch (Throwable failure) {
            throw ScheduledTaskProxy.<RuntimeException, RuntimeException>unchecked(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T unchecked(Throwable failure) throws E {
        throw (E) failure;
    }

    /** Lazily resolved Paper scheduler compatibility handles. */
    public static final class Companion {
        private Companion() { }
        public Companion(DefaultConstructorMarker ignored) { this(); }
    
        private final Class<?> getScheduledTaskClass() {
            return ScheduledTaskProxy.classDelegateAccessor().getValue();
        }

        private final MethodHandle isRepeatingTaskFn() {
            return ScheduledTaskProxy.repeatingDelegateAccessor().getValue();
        }

        private final MethodHandle getCancelFn() {
            return ScheduledTaskProxy.cancelDelegateAccessor().getValue();
        }

        public static final MethodHandle accessIsRepeatingTask(Companion self) {
            return self.isRepeatingTaskFn();
        }

        public static final MethodHandle accessGetCancelFn(Companion self) {
            return self.getCancelFn();
        }

        public static final Class<?> accessGetClaszz(Companion self) {
            return self.getScheduledTaskClass();
        }
    }
}
