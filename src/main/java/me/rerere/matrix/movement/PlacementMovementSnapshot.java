package me.rerere.matrix.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Immutable movement inputs captured for a placement ray trace. */
public final class PlacementMovementSnapshot {
    public final Location playerLocation;
    public final int blockX;
    public final Vector velocity;
    public final boolean movementFlag;
    public final Player player;
    public final int blockY;
    public final long timestamp;
    public volatile Location rayTraceLocation;
    public final int blockZ;
    public PlacementMovementSnapshot(Player player, int blockX, int blockY,
            int blockZ, Location playerLocation, Location rayTraceLocation,
            Vector velocity, boolean movementFlag, long timestamp) {
        this.player = player;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.playerLocation = playerLocation;
        this.rayTraceLocation = rayTraceLocation;
        this.velocity = velocity;
        this.movementFlag = movementFlag;
        this.timestamp = timestamp;
    }

    public Location getRayTraceLocation() { return rayTraceLocation; }
    public int getBlockZ() { return blockZ; }
    public Location getPlayerLocation() { return playerLocation; }
    public int getBlockY() { return blockY; }
    public int getBlockX() { return blockX; }
    public long getTimestamp() { return timestamp; }
    public boolean isMovementFlagSet() { return movementFlag; }
    public Vector getVelocity() { return velocity; }
    public Player getPlayer() { return player; }
}
