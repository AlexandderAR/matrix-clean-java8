package me.rerere.matrix.network.packet;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.util.MutableValue;

public class PacketSnapshot {
    private PacketTypeCommon packetType;
    private long timestamp;
    private MutableValue packetPayload;

    public PacketSnapshot(PacketTypeCommon packetType, long timestamp, MutableValue packetPayload) {
        this.packetType = packetType;
        this.timestamp = timestamp;
        this.packetPayload = packetPayload;
    }

    public void copyFrom(PacketSnapshot source) {
        packetType = source.packetType;
        timestamp = source.timestamp;
        packetPayload = source.packetPayload;
    }

    public PacketTypeCommon getType() { return packetType; }
    public long getTimestamp() { return timestamp; }
    public MutableValue getPayload() { return packetPayload; }

    public void setType(PacketTypeCommon packetType) { this.packetType = packetType; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setPayload(MutableValue packetPayload) { this.packetPayload = packetPayload; }

    public boolean canEqual(Object candidate) { return candidate instanceof PacketSnapshot; }

    @Override public String toString() {
        return new StringBuilder(packetType.getName())
                .append(" | ").append(timestamp)
                .append(" | ").append(packetPayload != null)
                .toString();
    }

    @Override public boolean equals(Object candidate) {
        if (candidate == this) return true;
        if (!(candidate instanceof PacketSnapshot)) return false;
        PacketSnapshot other = (PacketSnapshot) candidate;
        if (!other.canEqual(this)
                || getTimestamp() != other.getTimestamp()) {
            return false;
        }
        PacketTypeCommon thisType = getType();
        PacketTypeCommon otherType = other.getType();
        if (thisType == null ? otherType != null : !thisType.equals(otherType)) return false;
        MutableValue thisPayload = getPayload();
        MutableValue otherPayload = other.getPayload();
        return thisPayload == null ? otherPayload == null : thisPayload.equals(otherPayload);
    }

    @Override public int hashCode() {
        long capturedAt = getTimestamp();
        int result = 59 + (int) ((capturedAt >>> 32) ^ capturedAt);
        PacketTypeCommon packetType = getType();
        result = result * 59 + (packetType == null ? 43 : packetType.hashCode());
        MutableValue packetPayload = getPayload();
        return result * 59 + (packetPayload == null ? 43 : packetPayload.hashCode());
    }
}
