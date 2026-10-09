package me.rerere.matrix.player.model;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.movement.PlayerSnapshot;
import me.rerere.matrix.replay.PacketStateSnapshot;
import org.bukkit.util.Vector;

public final class MovementFrameFactory {
    private MovementFrameFactory() { }
    public MovementFrameFactory(DefaultConstructorMarker marker) { this(); }
    public final PlayerSnapshot createFromMovement(PacketStateSnapshot packetState) {
        PositionRotation current = new PositionRotation(new Vector(
                packetState.getX(), packetState.getY(), packetState.getZ()),
                packetState.getYaw(), packetState.getPitch());
        PositionRotation previous = new PositionRotation(new Vector(
                packetState.getLastX(), packetState.getLastY(), packetState.getLastZ()),
                packetState.getLastYaw(), packetState.getLastPitch());
        return new PlayerSnapshot(current, previous);
    }
    public final PlayerSnapshot createFromPlayer(PlayerCheckContext playerContext) {
        return createFromMovement(playerContext.movementState().snapshot());
    }
}
