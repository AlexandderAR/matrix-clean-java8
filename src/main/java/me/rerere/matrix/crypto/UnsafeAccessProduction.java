package me.rerere.matrix.crypto;

import java.lang.reflect.Field;
import sun.misc.Unsafe;
public final class UnsafeAccessProduction implements UnsafeAccessActions {
    public UnsafeAccessProduction() {}
    public Object declaredField() throws Throwable {
        return Unsafe.class.getDeclaredField("theUnsafe");
    }

    public void accessible(Object field) throws Throwable {
        ((Field) field).setAccessible(true);
    }

    public Object value(Object field) throws Throwable {
        return ((Field) field).get(null);
    }

    public boolean isUnsafe(Object candidate) {
        return candidate instanceof Unsafe;
    }

    public void halt(int status) {
        Runtime.getRuntime().halt(status);
    }

    public long getAndAddLong(Object unsafe, Object base, long offset, long delta) {
        return ((Unsafe) unsafe).getAndAddLong(base, offset, delta);
    }

    public void putAddress(Object unsafe, long address, long value) {
        ((Unsafe) unsafe).putAddress(address, value);
    }
}
