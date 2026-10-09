package me.rerere.matrix.support;

import java.lang.reflect.Field;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import sun.misc.Unsafe;

/** Operations exposed by the Matrix runtime's Unsafe helper. */
public final class UnsafeAccess {
    public static final UnsafeAccess INSTANCE = new UnsafeAccess();
    private static final Lazy<Unsafe> unsafe = LazyKt.lazy(() -> loadUnsafe());

    private UnsafeAccess() {
    }

    private static Unsafe getUnsafe() {
        return unsafe.getValue();
    }

    private static Unsafe loadUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object instance = field.get(null);
            Intrinsics.checkNotNull(instance, "null cannot be cast to non-null type sun.misc.Unsafe");
            return (Unsafe) instance;
        } catch (ReflectiveOperationException exception) {
            return UnsafeAccess.<RuntimeException, Unsafe>throwUnchecked(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T throwUnchecked(Throwable failure) throws E {
        throw (E) failure;
    }

    public final Object allocateInstance(Class<?> type) {
        try {
            return getUnsafe().allocateInstance(type);
        } catch (InstantiationException exception) {
            return UnsafeAccess.<RuntimeException, Object>throwUnchecked(exception);
        }
    }

    public final void writeNullAddress(Object... ignored) {
        getUnsafe().putAddress(0L, 0L);
    }
}
