package me.rerere.matrix.util.jvm;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

/** Recovered refusal path for reflective Unsafe acquisition. */
public final class UnsafeMemoryRefusal {
    private UnsafeMemoryRefusal() {
    }

    public interface Actions {
        Object declaredUnsafeField() throws Exception;
        void makeAccessible(Object field) throws Exception;
        Object readStaticField(Object field) throws Exception;
        boolean isUnsafe(Object value);
        void haltProcess(int status);
        void writeAddress(Object unsafe, long address, long value);
    }

    private static final class Production implements Actions {
        @Override
        public Object declaredUnsafeField() throws Exception {
            return Unsafe.class.getDeclaredField("theUnsafe");
        }

        @Override
        public void makeAccessible(Object field) throws Exception {
            ((Field) field).setAccessible(true);
        }

        @Override
        public Object readStaticField(Object field) throws Exception {
            return ((Field) field).get(null);
        }

        @Override
        public boolean isUnsafe(Object value) {
            return value instanceof Unsafe;
        }

        @Override
        public void haltProcess(int status) {
            Runtime.getRuntime().halt(status);
        }

        @Override
        public void writeAddress(Object unsafe, long address, long value) {
            ((Unsafe) unsafe).putAddress(address, value);
        }
    }

    public static final class UnresolvedNativePath extends Error {
        public UnresolvedNativePath(String message) {
            super(message);
        }
    }

    public static void runRefusalPath(Actions actions) {
        Object result;
        Object resultCompanion = me.rerere.matrix.thirdparty.kotlin.Result.Companion;
        try {
            Object field = actions.declaredUnsafeField();
            actions.makeAccessible(field);
            Object unsafe = actions.readStaticField(field);
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(
                    unsafe, "null cannot be cast to non-null type sun.misc.Unsafe");
            if (!actions.isUnsafe(unsafe)) {
                throw new UnresolvedNativePath("IsInstanceOf false @0x2109fe");
            }
            result = me.rerere.matrix.support.KotlinResultBridge.construct(unsafe);
        } catch (UnresolvedNativePath unresolved) {
            throw unresolved;
        } catch (Throwable failure) {
            result = me.rerere.matrix.support.KotlinResultBridge.construct(
                    me.rerere.matrix.thirdparty.kotlin.ResultKt.createFailure(failure));
        }

        if (me.rerere.matrix.support.KotlinResultBridge.exceptionOrNull(result) != null) {
            actions.haltProcess(1);
        }
        me.rerere.matrix.thirdparty.kotlin.ResultKt.throwOnFailure(result);
        if (!actions.isUnsafe(result)) {
            throw new UnresolvedNativePath("second IsInstanceOf false @0x215673");
        }
        actions.writeAddress(result, 0L, 0L);
        throw new UnresolvedNativePath("continuation after putAddress is not recovered");
    }

    public static void refuseUnsafeAccess() {
        try {
            runRefusalPath(new Production());
        } catch (Throwable failure) {
            throwUnchecked(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable failure) throws T {
        throw (T) failure;
    }
}
