package me.rerere.matrix.crypto;

import java.lang.reflect.Field;
import me.rerere.matrix.thirdparty.kotlin.ResultKt;
import sun.misc.Unsafe;

/** Preserves the canonical Unsafe-based certificate pin refusal path. */
public final class CertificatePinRefusal {
    private CertificatePinRefusal() {
    }

    public static void run(Actions actions) throws Throwable {
        Object companion = me.rerere.matrix.thirdparty.kotlin.Result.Companion;
        Object result;
        try {
            Object declaredField = actions.declaredField();
            actions.accessible(declaredField);
            Object fieldValue = actions.value(declaredField);
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(
                    fieldValue, "null cannot be cast to non-null type sun.misc.Unsafe");
            Object unsafe = actions.castUnsafe(fieldValue);
            result = me.rerere.matrix.support.KotlinResultBridge.construct(unsafe);
        } catch (Throwable failure) {
            result = me.rerere.matrix.support.KotlinResultBridge.construct(ResultKt.createFailure(failure));
        }

        if (me.rerere.matrix.support.KotlinResultBridge.exceptionOrNull(result) != null) {
            actions.halt(1);
        }
        ResultKt.throwOnFailure(result);
        actions.putAddress(result, 0L, 0L);
        actions.halt(0);
    }

    public static void reject() {
        try {
            run(new Production(null));
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
        Object castUnsafe(Object value) throws Throwable;
        void putAddress(Object unsafe, long address, long value) throws Throwable;
        void halt(int status) throws Throwable;
    }

    private static final class Production implements Actions {
        private Production() {
        }

        Production(ProductionMarker marker) {
            this();
        }

        @Override
        public Object declaredField() throws Throwable {
            return Unsafe.class.getDeclaredField("theUnsafe");
        }

        @Override
        public void accessible(Object field) {
            ((Field) field).setAccessible(true);
        }

        @Override
        public Object value(Object field) throws Throwable {
            return ((Field) field).get(null);
        }

        @Override
        public Object castUnsafe(Object value) {
            return (Unsafe) value;
        }

        @Override
        public void putAddress(Object unsafe, long address, long value) {
            ((Unsafe) unsafe).putAddress(address, value);
        }

        @Override
        public void halt(int status) {
            Runtime.getRuntime().halt(status);
        }
    }

    private static final class ProductionMarker {
    }
}
