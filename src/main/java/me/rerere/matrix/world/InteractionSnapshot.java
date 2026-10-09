package me.rerere.matrix.world;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import me.rerere.matrix.checks.movement.DistanceAxis;
import me.rerere.matrix.entity.PositionSnapshot;

public final class InteractionSnapshot {
    private final long id;
    private final int x;
    private final int y;
    private final int z;
    private final BlockFace face;
    private final DistanceAxis interactionType;
    private final PositionSnapshot primary;
    private PositionSnapshot secondary;
    private int counter;
    private boolean enabled = true;

    public InteractionSnapshot(long id, DistanceAxis interactionType, int x, int y, int z,
                               BlockFace face, PositionSnapshot primary, PositionSnapshot secondary) {
        this.id = id;
        this.interactionType = interactionType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
        this.primary = primary;
        this.secondary = secondary;
    }
    public InteractionSnapshot(long id, DistanceAxis interactionType, int x, int y, int z,
                               BlockFace face, PositionSnapshot primary, PositionSnapshot secondary,
                               int mask, me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker marker) {
        this(id, interactionType, x, y, z, face, primary, (mask & 128) != 0 ? null : secondary);
    }
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    public PositionSnapshot primary() { return primary; }
    public DistanceAxis interactionType() { return interactionType; }
    public int x() { return x; }
    public Block block(World world) { return world.getBlockAt(x, y, z); }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        return id == ((InteractionSnapshot) other).id;
    }
    public boolean enabled() { return enabled; }
    public void setCounter(int value) { counter = value; }
    public long id() { return id; }
    public int y() { return y; }
    public int z() { return z; }
    public void setSecondary(PositionSnapshot value) { secondary = value; }
    public BlockFace face() { return face; }
    public PositionSnapshot secondary() { return secondary; }
    public int counter() { return counter; }
    public void setEnabled(boolean value) { enabled = value; }
}
