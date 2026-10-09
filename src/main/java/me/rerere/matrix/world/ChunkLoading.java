package me.rerere.matrix.world;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

/** Coordinate-to-chunk conversion and loaded-chunk queries. */
public final class ChunkLoading {
    public ChunkLoading() {}

    public static int floorCoordinate(double coordinate) {
        return NumberConversions.floor(coordinate);
    }

    public static boolean areChunksLoaded(World world, Vector position) {
        return isChunkLoaded(world, position.getX(), position.getZ());
    }

    public static boolean isChunkLoaded(Location location) {
        return isChunkLoaded(Objects.requireNonNull(location.getWorld()), location.getX(), location.getZ());
    }

    public static boolean isChunkLoaded(World world, Location location) {
        return isChunkLoaded(world, location.getX(), location.getZ());
    }

    public static boolean isChunkLoaded(World world, double x, double z) {
        return world.isChunkLoaded(NumberConversions.floor(x) >> 4, NumberConversions.floor(z) >> 4);
    }

    public static boolean areSurroundingChunksLoaded(World world, double x, double z) {
        int chunkX = NumberConversions.floor(x) >> 4;
        int chunkZ = NumberConversions.floor(z) >> 4;
        for (int currentX = chunkX - 1; currentX <= chunkX + 1; currentX++) {
            for (int currentZ = chunkZ - 1; currentZ <= chunkZ + 1; currentZ++) {
                if (!world.isChunkLoaded(currentX, currentZ)) return false;
            }
        }
        return true;
    }

    public static List<Chunk> loadedChunksWithin(Chunk center, int radius) {
        int centerX = center.getX();
        int centerZ = center.getZ();
        World world = center.getWorld();
        return Stream.of(world.getLoadedChunks())
                .filter(chunk -> isWithinChunkRadius(centerX, radius, centerZ, chunk))
                .collect(Collectors.toList());
    }

    private static boolean isWithinChunkRadius(int centerX, int radius, int centerZ, Chunk chunk) {
        return Math.abs(chunk.getX() - centerX) <= radius
                && Math.abs(chunk.getZ() - centerZ) <= radius;
    }
}
