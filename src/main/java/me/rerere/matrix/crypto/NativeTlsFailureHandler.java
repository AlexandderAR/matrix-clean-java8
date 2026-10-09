package me.rerere.matrix.crypto;

import java.lang.reflect.Field;
import sun.misc.Unsafe;
import me.rerere.matrix.thirdparty.kotlin.Result;
import me.rerere.matrix.thirdparty.kotlin.ResultKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.support.KotlinResultBridge;

/** Preserves the generated local-catch behavior used by the native TLS bridge. */
public final class NativeTlsFailureHandler {
    private NativeTlsFailureHandler() {}

    public interface Actions {
        Object declaredField();
        void accessible(Object field);
        Object value(Object field);
        boolean isUnsafe(Object value);
        void halt(int status);
        long getAndAddLong(Object unsafe, Object base, long offset, long delta);
        void putAddress(Object unsafe, long address, long value);
    }

    public static final class UnresolvedNativePath extends Error {
        public UnresolvedNativePath(String message) { super(message); }
    }

    static final class Production implements Actions {
        private Production() {}


        public Object declaredField() {
            try {
                return Unsafe.class.getDeclaredField("theUnsafe");
            } catch (NoSuchFieldException failure) {
                NativeTlsFailureHandler.<RuntimeException>raise(failure);
                return null;
            }
        }
        public void accessible(Object field) {
            ((Field) field).setAccessible(true);
        }
        public Object value(Object field) {
            try {
                return ((Field) field).get(null);
            } catch (IllegalAccessException failure) {
                NativeTlsFailureHandler.<RuntimeException>raise(failure);
                return null;
            }
        }
        public boolean isUnsafe(Object value) { return value instanceof Unsafe; }
        public void halt(int status) { Runtime.getRuntime().halt(status); }
        public long getAndAddLong(Object unsafe, Object base, long offset, long delta) {
            return ((Unsafe) unsafe).getAndAddLong(base, offset, delta);
        }
        public void putAddress(Object unsafe, long address, long value) {
            ((Unsafe) unsafe).putAddress(address, value);
        }
    }

    private static Object reflectedUnsafe(Actions actions) {
        touchResultCompanion(Result.Companion);
        try {
            Object field = actions.declaredField();
            actions.accessible(field);
            Object value = actions.value(field);
            Intrinsics.checkNotNull(value, "null cannot be cast to non-null type sun.misc.Unsafe");
            if (!actions.isUnsafe(value)) {
                throw new UnresolvedNativePath("first native IsInstanceOf(Unsafe) false");
            }
            return KotlinResultBridge.construct(value);
        } catch (UnresolvedNativePath unresolved) {
            throw unresolved;
        } catch (Throwable failure) {
            return KotlinResultBridge.construct(ResultKt.createFailure(failure));
        }
    }

    private static void refuse(Throwable failure, Actions actions, boolean operation) {
        if (!(failure instanceof Exception)) NativeTlsFailureHandler.<RuntimeException>raise(failure);
        Object result = reflectedUnsafe(actions);
        if (KotlinResultBridge.exceptionOrNull(result) != null) actions.halt(1);
        ResultKt.throwOnFailure(result);
        if (!actions.isUnsafe(result)) {
            throw new UnresolvedNativePath("second native IsInstanceOf(Unsafe) false");
        }
        if (operation) actions.getAndAddLong(result, "", 0L, 0L);
        actions.putAddress(result, 0L, 0L);
        actions.halt(0);
        throw new UnresolvedNativePath("terminal halt returned; later native continuation unresolved");
    }

    public static void operationFailure(Throwable failure, Actions actions) {
        refuse(failure, actions, true);
    }
    public static void constructorFailure(Throwable failure, Actions actions) {
        refuse(failure, actions, false);
    }

    public static void rejectOperation(Throwable failure) {
        try {
            operationFailure(failure, new Production());
        } catch (Throwable nested) {
            unchecked(nested);
        }
    }
    public static void rejectConstructor(Throwable failure) {
        try {
            constructorFailure(failure, new Production());
        } catch (Throwable nested) {
            unchecked(nested);
        }
    }
    private static void touchResultCompanion(Object ignored) {}
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void raise(Throwable failure) throws T { throw (T) failure; }
    private static void unchecked(Throwable failure) { NativeTlsFailureHandler.<RuntimeException>raise(failure); }
}





