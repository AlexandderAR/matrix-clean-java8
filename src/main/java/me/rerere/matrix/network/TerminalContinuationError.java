package me.rerere.matrix.network;

/** Signals that execution reached the terminal boundary before the native continuation. */
public final class TerminalContinuationError extends Error {
    public TerminalContinuationError(int rva) {
        super("native ix.w continuation after terminal action at RVA 0x" + Integer.toHexString(rva));
    }
}
