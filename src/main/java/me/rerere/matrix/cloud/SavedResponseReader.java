package me.rerere.matrix.cloud;

import java.io.InputStream;

/** Reads text saved from an HTTP response stream. */
public interface SavedResponseReader {
    String read(InputStream stream) throws Throwable;
}
