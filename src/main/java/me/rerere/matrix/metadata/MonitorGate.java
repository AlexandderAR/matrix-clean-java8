package me.rerere.matrix.metadata;

import me.rerere.matrix.input.PacketKnownInput;

public final class MonitorGate {
    public PacketKnownInput create() {
        return PacketKnownInput.empty();
    }

    private MonitorGate() { }

    public MonitorGate(
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker marker) {
        this();
    }

}
