package me.rerere.matrix.checks.player;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Location;

public class BlockInteractionSnapshot {
    public int clickSequence;
    public Player player;
    public Block block;
    public Location firstLocation;
    public Location secondLocation;
    public PlayerInteractEvent interactionEvent;
    public Material material;

    public BlockInteractionSnapshot(Player player, Block block, Material material,
                                    Location firstLocation, Location secondLocation,
                                    PlayerInteractEvent interactionEvent, int clickSequence) {
        this.player = player;
        this.block = block;
        this.material = material;
        this.firstLocation = firstLocation;
        this.secondLocation = secondLocation;
        this.interactionEvent = interactionEvent;
        this.clickSequence = clickSequence;
    }

    public boolean isSnapshot(Object value) {
        return value instanceof BlockInteractionSnapshot;
    }

    @Override
    public int hashCode() {
        int result = 59 + clickSequence;
        result = 59 * result + (player == null ? 43 : player.hashCode());
        result = 59 * result + (block == null ? 43 : block.hashCode());
        result = 59 * result + (material == null ? 43 : material.hashCode());
        result = 59 * result + (firstLocation == null ? 43 : firstLocation.hashCode());
        result = 59 * result + (secondLocation == null ? 43 : secondLocation.hashCode());
        result = 59 * result + (interactionEvent == null ? 43 : interactionEvent.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof BlockInteractionSnapshot)) {
            return false;
        }
        BlockInteractionSnapshot otherSnapshot = (BlockInteractionSnapshot) otherObject;
        if (!otherSnapshot.isSnapshot(this)) {
            return false;
        }
        if (clickSequence != otherSnapshot.clickSequence) {
            return false;
        }
        if (player == null ? otherSnapshot.player != null : !player.equals(otherSnapshot.player)) {
            return false;
        }
        if (block == null ? otherSnapshot.block != null : !block.equals(otherSnapshot.block)) {
            return false;
        }
        if (material == null ? otherSnapshot.material != null : !material.equals(otherSnapshot.material)) {
            return false;
        }
        if (firstLocation == null ? otherSnapshot.firstLocation != null
                : !firstLocation.equals(otherSnapshot.firstLocation)) {
            return false;
        }
        if (secondLocation == null ? otherSnapshot.secondLocation != null
                : !secondLocation.equals(otherSnapshot.secondLocation)) {
            return false;
        }
        return interactionEvent == null ? otherSnapshot.interactionEvent == null
                : interactionEvent.equals(otherSnapshot.interactionEvent);
    }

}
