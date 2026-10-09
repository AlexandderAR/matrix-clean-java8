package me.rerere.matrix.license.network;

import java.io.InputStream;

/** Opens an HTTP response stream. */
public interface ResponseStreamOpener {
    InputStream open() throws Throwable;
}
