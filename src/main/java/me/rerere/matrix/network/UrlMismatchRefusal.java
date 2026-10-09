package me.rerere.matrix.network;

import java.lang.reflect.Field;
import me.rerere.matrix.util.Exceptions;
import sun.misc.Unsafe;

/** Handles URL-mismatch refusal through reflective access to Unsafe. */
public final class UrlMismatchRefusal {
    private UrlMismatchRefusal() {
    }

    public interface Actions {
        Object declaredField() throws Exception;
        void accessible(Object field) throws Exception;
        Object value(Object field) throws Exception;
        boolean isUnsafe(Object value);
        void halt(int status);
        void putAddress(Object unsafe, long address, long value);
    }

    private static final class ProductionActions implements Actions {
        private ProductionActions() {
        }

        @Override
        public Object declaredField() throws Exception {
            return Unsafe.class.getDeclaredField("theUnsafe");
        }

        @Override
        public void accessible(Object field) throws Exception {
            ((Field) field).setAccessible(true);
        }

        @Override
        public Object value(Object field) throws Exception {
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

    public static final class UnresolvedNativePath extends Error {
        public UnresolvedNativePath(String message) {
            super(message);
        }
    }

    public static void run(Actions actions) {
        Object resultCompanion = me.rerere.matrix.thirdparty.kotlin.Result.Companion;
        Object result;
        try {
            Object field = actions.declaredField();
            actions.accessible(field);
            Object unsafe = actions.value(field);
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(
                    unsafe, "null cannot be cast to non-null type sun.misc.Unsafe");
            if (!actions.isUnsafe(unsafe)) {
                throw new UnresolvedNativePath("IsInstanceOf false @0x20a30b joins 0x292810");
            }
            result = me.rerere.matrix.support.KotlinResultBridge.construct(unsafe);
        } catch (UnresolvedNativePath unresolved) {
            throw unresolved;
        } catch (Throwable failure) {
            result = me.rerere.matrix.support.KotlinResultBridge.construct(
                    me.rerere.matrix.thirdparty.kotlin.ResultKt.createFailure(failure));
        }

        if (me.rerere.matrix.support.KotlinResultBridge.exceptionOrNull(result) != null) {
            actions.halt(1);
        }
        me.rerere.matrix.thirdparty.kotlin.ResultKt.throwOnFailure(result);
        actions.putAddress(result, 0L, 0L);
    }

    public static void reject() {
        try {
            run(new ProductionActions());
        } catch (Throwable failure) {
            throwUnchecked(failure);
        }
    }

    private static void throwUnchecked(Throwable failure) {
        throw Exceptions.propagate(failure);
    }
}
