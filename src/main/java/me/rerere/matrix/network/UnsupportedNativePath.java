package me.rerere.matrix.network;

import me.rerere.matrix.license.network.HttpContinuation;

/** Retains the message, continuation state, and optional cause for unsupported paths. */
public final class UnsupportedNativePath extends Error {
    public final HttpContinuation continuation;

    public UnsupportedNativePath(String message, HttpContinuation continuation) {
        super(message);
        this.continuation = continuation;
    }

    public UnsupportedNativePath(String message, HttpContinuation continuation,
                                 Throwable cause) {
        super(message, cause);
        this.continuation = continuation;
    }
}
