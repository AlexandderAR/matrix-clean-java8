package me.rerere.matrix.network.packet;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;

/** Holds a packet wrapper together with its timestamp. */
public final class PacketWrapperSnapshot {
    private final long timestamp;
    private final PacketWrapper<?> packetWrapper;

    public PacketWrapperSnapshot(PacketWrapper<?> packetWrapper, long timestamp) {
        this.packetWrapper = packetWrapper;
        this.timestamp = timestamp;
    }

    public final PacketWrapper<?> getPacketWrapper() {
        return packetWrapper;
    }

    public final long getTimestamp() {
        return timestamp;
    }

}
