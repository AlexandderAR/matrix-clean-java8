package me.rerere.matrix.replay;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import me.rerere.matrix.util.PreviousCurrentPair;

/** Tracks packet and Bukkit movement values together with their previous values. */
public final class PlayerMovementState {
    private final PreviousCurrentPair eyeHeight = new PreviousCurrentPair(1.62);
    private final PreviousCurrentPair packetX = new PreviousCurrentPair(0.0);
    private final PreviousCurrentPair packetY = new PreviousCurrentPair(0.0);
    private final PreviousCurrentPair packetZ = new PreviousCurrentPair(0.0);
    private final PreviousCurrentPair packetYaw = new PreviousCurrentPair(0.0f);
    private final PreviousCurrentPair packetPitch = new PreviousCurrentPair(0.0f);
    private final PreviousCurrentPair onGround = new PreviousCurrentPair(false);
    private final PreviousCurrentPair serverX = new PreviousCurrentPair(0.0);
    private final PreviousCurrentPair serverY = new PreviousCurrentPair(0.0);
    private final PreviousCurrentPair serverZ = new PreviousCurrentPair(0.0);
    private final PreviousCurrentPair serverYaw = new PreviousCurrentPair(0.0f);
    private final PreviousCurrentPair serverPitch = new PreviousCurrentPair(0.0f);
    public PlayerMovementState() {}

    public PreviousCurrentPair getEyeHeight() { return eyeHeight; }
    public PreviousCurrentPair getPacketX() { return packetX; }
    public PreviousCurrentPair getPacketY() { return packetY; }
    public PreviousCurrentPair getPacketZ() { return packetZ; }
    public PreviousCurrentPair getPacketYaw() { return packetYaw; }
    public PreviousCurrentPair getPacketPitch() { return packetPitch; }
    public PreviousCurrentPair getOnGround() { return onGround; }
    public PreviousCurrentPair getServerX() { return serverX; }
    public PreviousCurrentPair getServerY() { return serverY; }
    public PreviousCurrentPair getServerZ() { return serverZ; }
    public PreviousCurrentPair getServerYaw() { return serverYaw; }
    public PreviousCurrentPair getServerPitch() { return serverPitch; }

    public PacketStateSnapshot snapshot() {
        return new PacketStateSnapshot(
                ((Number) packetX.current()).doubleValue(),
                ((Number) packetY.current()).doubleValue(),
                ((Number) packetZ.current()).doubleValue(),
                ((Number) packetX.previous()).doubleValue(),
                ((Number) packetY.previous()).doubleValue(),
                ((Number) packetZ.previous()).doubleValue(),
                ((Number) eyeHeight.current()).doubleValue(),
                ((Number) eyeHeight.previous()).doubleValue(),
                ((Number) packetYaw.current()).floatValue(),
                ((Number) packetPitch.current()).floatValue(),
                ((Number) packetYaw.previous()).floatValue(),
                ((Number) packetPitch.previous()).floatValue());
    }

    public void updatePacket(WrapperPlayClientPlayerPositionAndRotation packet) {
        boolean positionChanged = packet.hasPositionChanged();
        if (positionChanged) {
            packetX.advance(packet.getPosition().x);
            packetY.advance(packet.getPosition().y);
            packetZ.advance(packet.getPosition().z);
        }
        if (!positionChanged) {
            packetX.advance(packetX.current());
            packetY.advance(packetY.current());
            packetZ.advance(packetZ.current());
        }
        boolean rotationChanged = packet.hasRotationChanged();
        if (rotationChanged) {
            packetYaw.advance(packet.getYaw());
            packetPitch.advance(packet.getPitch());
        }
        if (!rotationChanged) {
            packetYaw.advance(packetYaw.current());
            packetPitch.advance(packetPitch.current());
        }
        onGround.advance(packet.isOnGround());
    }

    public List<Vector> getPacketPositionCandidates() {
        PacketStateSnapshot state = snapshot();
        return state.getPositionCandidates();
    }

    public void setEyeHeight(double height) {
        eyeHeight.advance(Double.valueOf(height));
    }

    public double distanceToPacketPosition(Vector position) {
        double deltaX = position.getX()
                - ((Number) packetX.current()).doubleValue();
        double deltaY = position.getY()
                - ((Number) packetY.current()).doubleValue();
        double deltaZ = position.getZ()
                - ((Number) packetZ.current()).doubleValue();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    public List<Vector> getServerPositionCandidates() {
        Vector current = new Vector(
                ((Number) serverX.current()).doubleValue(),
                ((Number) serverY.current()).doubleValue(),
                ((Number) serverZ.current()).doubleValue());
        Vector previous = new Vector(
                ((Number) serverX.previous()).doubleValue(),
                ((Number) serverY.previous()).doubleValue(),
                ((Number) serverZ.previous()).doubleValue());
        boolean equal = current.equals(previous);
        if (equal) {
            return java.util.Collections.singletonList(current);
        }
        return java.util.Arrays.asList(new Vector[] { current, previous });
    }

    public void updateServerLocation(Location location) {
        serverX.advance(location.getX());
        serverY.advance(location.getY());
        serverZ.advance(location.getZ());
        serverYaw.advance(location.getYaw());
        serverPitch.advance(location.getPitch());
    }

    public void resetLocation(Location location) {
        packetX.reset(location.getX());
        packetY.reset(location.getY());
        packetZ.reset(location.getZ());
        packetYaw.reset(location.getYaw());
        packetPitch.reset(location.getPitch());
        serverX.reset(location.getX());
        serverY.reset(location.getY());
        serverZ.reset(location.getZ());
        serverYaw.reset(location.getYaw());
        serverPitch.reset(location.getPitch());
    }

}
