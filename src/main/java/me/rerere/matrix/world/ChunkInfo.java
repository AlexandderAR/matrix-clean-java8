package me.rerere.matrix.world;

import java.util.Objects;

/** Identifies one chunk by world name and horizontal chunk coordinates. */
public final class ChunkInfo {
    private final String world;
    private final int chunkX;
    private final int chunkZ;

    public ChunkInfo(String world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getChunkX() {
        return chunkX;
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + Integer.hashCode(chunkX);
        result = 31 * result + Integer.hashCode(chunkZ);
        return result;
    }

    @Override
    public String toString() {
        return "ChunkInfo(world=" + world + ", chunkX=" + chunkX
                + ", chunkZ=" + chunkZ + ")";
    }

    public int chunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public String world() {
        return world;
    }

    public String worldValue() {
        return world;
    }

    public int chunkZ() {
        return chunkZ;
    }

    public ChunkInfo copy(String world, int chunkX, int chunkZ) {
        return new ChunkInfo(world, chunkX, chunkZ);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ChunkInfo)) {
            return false;
        }
        ChunkInfo that = (ChunkInfo) other;
        return Objects.equals(world, that.world)
                && chunkX == that.chunkX
                && chunkZ == that.chunkZ;
    }
}
