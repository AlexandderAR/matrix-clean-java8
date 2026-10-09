package me.rerere.matrix.crypto;

public interface UnsafeAccessActions {
    Object declaredField() throws Throwable;
    void accessible(Object field) throws Throwable;
    Object value(Object field) throws Throwable;
    boolean isUnsafe(Object value);
    void halt(int status) throws Throwable;
    long getAndAddLong(Object unsafe, Object base, long offset, long delta) throws Throwable;
    void putAddress(Object unsafe, long address, long value) throws Throwable;
}
