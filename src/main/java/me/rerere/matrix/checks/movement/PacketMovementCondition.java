package me.rerere.matrix.checks.movement;

import me.rerere.matrix.checks.CheckRegistry;
import me.rerere.matrix.checks.ElytraFlightCheck;
import me.rerere.matrix.checks.MovementPhysicsCheck;
import me.rerere.matrix.geometry.DuplicatePosition;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.world.Location;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;

/** Tracks the last client-reported position and rotation for a player. */
public final class PacketMovementCondition {
    private double positionY;
    private double positionX;
    private float yaw;
    private boolean initialized;
    private final me.rerere.matrix.player.PlayerCheckContext playerTracker;
    private boolean onGround;
    private boolean enabled;
    private float pitch;
    private double positionZ;

    public PacketMovementCondition(me.rerere.matrix.player.PlayerCheckContext playerTracker) {
        this.playerTracker = playerTracker;
    }

    public boolean enabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }

    public boolean processPacket(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        boolean hasPosition = packetType == PacketType$Play$Client.PLAYER_POSITION
                || packetType == PacketType$Play$Client.PLAYER_POSITION_AND_ROTATION;
        boolean hasRotation = packetType == PacketType$Play$Client.PLAYER_ROTATION
                || packetType == PacketType$Play$Client.PLAYER_POSITION_AND_ROTATION;
        WrapperPlayClientPlayerPositionAndRotation wrapper =
                new WrapperPlayClientPlayerPositionAndRotation(event);

        Location location = hasPosition ? wrapper.getLocation() : null;
        double nextX = hasPosition ? location.getX() : positionX;
        double nextY = hasPosition ? location.getY() : positionY;
        double nextZ = hasPosition ? location.getZ() : positionZ;
        float nextYaw = hasRotation ? wrapper.getYaw() : yaw;
        float nextPitch = hasRotation ? wrapper.getPitch() : pitch;
        boolean nextOnGround = wrapper.isOnGround();

        if (!initialized) {
            if (hasPosition) {
                initialized = true;
                positionX = nextX;
                positionY = nextY;
                positionZ = nextZ;
                yaw = nextYaw;
                pitch = nextPitch;
                onGround = nextOnGround;
            }
            return false;
        }

        boolean unchangedPositionAndGround =
                positionX == nextX && positionY == nextY && positionZ == nextZ
                        && onGround == nextOnGround;
        boolean correctionRecorded = false;
        if (unchangedPositionAndGround
                && packetType == PacketType$Play$Client.PLAYER_POSITION_AND_ROTATION
                && playerTracker.clientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
            CheckRegistry checks = playerTracker.checks();
            MovementPhysicsCheck movementCheck =
                    (MovementPhysicsCheck) checks.firstCheckOfType(MovementPhysicsCheck.class);
            if (movementCheck != null) {
                DuplicatePosition correction = new DuplicatePosition(nextX, nextY, nextZ);
                movementCheck.advanceFirstMoveBudget();
                movementCheck.onCorrection(correction);
            }

            checks = playerTracker.checks();
            ElytraFlightCheck elytraCheck =
                    (ElytraFlightCheck) checks.firstCheckOfType(ElytraFlightCheck.class);
            if (elytraCheck != null) {
                DuplicatePosition correction = new DuplicatePosition(nextX, nextY, nextZ);
                // The exemption handler reads the correction from payload zero.
                elytraCheck.addExemptLocation(correction);
            }
            correctionRecorded = true;
        }

        if (unchangedPositionAndGround
                && playerTracker.clientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)
                && playerTracker.isInVehicle()) {
            if (enabled) correctionRecorded = true;
            enabled = false;
        }

        positionX = nextX;
        positionY = nextY;
        positionZ = nextZ;
        yaw = nextYaw;
        pitch = nextPitch;
        onGround = nextOnGround;
        return correctionRecorded;
    }
}
