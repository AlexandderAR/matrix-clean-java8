package me.rerere.matrix.compat;

/** A scheduled task or operation that can be cancelled. */
public interface Cancelable {
    void cancel();
}
