package me.rerere.matrix.security;

import java.lang.reflect.Field;
import me.rerere.matrix.runtime.JvmArgumentMatch;
import me.rerere.matrix.support.KotlinResultBridge;
import me.rerere.matrix.thirdparty.kotlin.ResultKt;
import sun.misc.Unsafe;

/** Refuses the selected JVM argument by following the recorded Unsafe path. */
public final class JvmArgumentRefusal {
    private JvmArgumentRefusal() {
    }

    private static Object readUnsafeResult(Actions actions) throws Throwable {
        Object resultCompanion = me.rerere.matrix.thirdparty.kotlin.Result.Companion;
        try {
            Object unsafeField = actions.declaredField();
            actions.accessible(unsafeField);
            Object unsafeInstance = actions.value(unsafeField);
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(
                    unsafeInstance, "null cannot be cast to non-null type sun.misc.Unsafe");
            if (!actions.isUnsafe(unsafeInstance)) {
                throw new UnresolvedNativePath("first IsInstanceOf(Unsafe) false");
            }
            return KotlinResultBridge.construct(unsafeInstance);
        } catch (UnresolvedNativePath unresolved) {
            throw unresolved;
        } catch (Throwable failure) {
            return KotlinResultBridge.construct(ResultKt.createFailure(failure));
        }
    }

    public static void refuseSelected(JvmArgumentMatch match, Actions actions) throws Throwable {
        if (match == JvmArgumentMatch.NONE) {
            throw new IllegalArgumentException("no JVM argument match");
        }

        Object unsafeResult = readUnsafeResult(actions);
        if (KotlinResultBridge.exceptionOrNull(unsafeResult) != null) {
            actions.halt(1);
        }
        ResultKt.throwOnFailure(unsafeResult);
        if (!actions.isUnsafe(unsafeResult)) {
            throw new UnresolvedNativePath("second IsInstanceOf(Unsafe) false");
        }
        actions.putAddress(unsafeResult, 0L, 0L);
        throw new UnresolvedNativePath("putAddress returned; JVM refusal continuation unresolved");
    }

    public static void reject(JvmArgumentMatch match) {
        try {
            refuseSelected(match, new Production());
        } catch (Throwable failure) {
            unchecked(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void unchecked(Throwable failure) throws E {
        throw (E) failure;
    }

    public interface Actions {
        Object declaredField() throws Throwable;
        void accessible(Object field) throws Throwable;
        Object value(Object field) throws Throwable;
        boolean isUnsafe(Object value);
        void halt(int status) throws Throwable;
        void putAddress(Object unsafe, long address, long value) throws Throwable;
    }

    private static final class Production implements Actions {
        private Production() {
        }

        @Override
        public Object declaredField() throws Throwable {
            return Unsafe.class.getDeclaredField("theUnsafe");
        }

        @Override
        public void accessible(Object field) throws Throwable {
            ((Field) field).setAccessible(true);
        }

        @Override
        public Object value(Object field) throws Throwable {
            return ((Field) field).get(null);
        }

        @Override
        public boolean isUnsafe(Object value) {
            return value instanceof Unsafe;
        }

        @Override
        public void halt(int status) {
            Runtime.getRuntime().halt(status);
        }

        @Override
        public void putAddress(Object unsafe, long address, long value) {
            ((Unsafe) unsafe).putAddress(address, value);
        }
    }

    /** Marks a branch whose continuation is not represented by the recovered path. */
    public static final class UnresolvedNativePath extends Error {
        public UnresolvedNativePath(String message) {
            super(message);
        }
    }

}
