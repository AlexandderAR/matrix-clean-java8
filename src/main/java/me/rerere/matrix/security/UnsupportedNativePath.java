package me.rerere.matrix.security;

/** Preserves the original fatal error for unsupported native response setup. */
public final class UnsupportedNativePath extends Error {
    public UnsupportedNativePath(String message) { super(message); }
    public UnsupportedNativePath(String message, Throwable cause) { super(message, cause); }
}
