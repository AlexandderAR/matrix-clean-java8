package me.rerere.matrix.network;

/** Preserves the explicit failure for an uncomposed trial exception scope. */
public final class TrialExceptionScope extends Error {
    public TrialExceptionScope() {
        super("Trial linker/factory exception scopes require separate composition");
    }
}
