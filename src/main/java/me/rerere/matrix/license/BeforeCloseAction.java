package me.rerere.matrix.license;

/** Action performed before the response stream is closed. */
public interface BeforeCloseAction {
    void run(Object closeContext) throws Throwable;
}
