package me.rerere.matrix.network;

/** Control-flow sentinel reached when a required outer failure callback returns. */
public final class OuterFailureHandlerReturned extends Error {
    public OuterFailureHandlerReturned() { super("selected outer failure returned"); }
}
