package me.rerere.matrix.util;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.checks.packet.BadPacketCheck;

/** Typed bridge to the canonical boolean feature dispatcher. */
public final class BooleanDispatchBridge {
    private BooleanDispatchBridge() { }

    public BooleanDispatchBridge(DefaultConstructorMarker marker) {
        this();
    }

    public final void set(boolean value) {
        BadPacketCheck.setEnabled(value);
    }

    public final boolean query() {
        return BadPacketCheck.isEnabled();
    }
}
