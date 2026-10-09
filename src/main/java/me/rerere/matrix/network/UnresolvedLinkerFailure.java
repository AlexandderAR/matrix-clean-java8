package me.rerere.matrix.network;

/** Error raised when the trial method-handle bootstrap cannot link its target. */
public final class UnresolvedLinkerFailure extends Error {
    public UnresolvedLinkerFailure(Throwable cause) {
        super("native linkCallSite failure scope", cause);
    }
}
