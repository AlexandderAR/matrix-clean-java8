package me.rerere.matrix.errors;

/** Error used when a native response path is unsupported. */
public class UnsupportedNativePath extends Error {
    public UnsupportedNativePath(String message, Throwable cause) {
        super(message, cause);
    }
}

